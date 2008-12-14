package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Break;
import org.python.pydev.parser.jython.ast.Continue;
import org.python.pydev.parser.jython.ast.Exec;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.suiteType;

/**
 * Provides the basic behavior for a tree builder (opening and closing node scopes).
 * 
 * Subclasses must provide actions where it's not common.
 * 
 * @author Fabio
 */
public abstract class AbstractTreeBuilder extends AbstractTreeBuilderHelpers {

    /**
     * Keeps the last opened node.
     */
    private SimpleNode lastOpened;

    /**
     * @return the last opened node.
     */
    public final SimpleNode getLastOpened() {
        return lastOpened;
    }

    /**
     * Constructor
     */
    public AbstractTreeBuilder(AbstractJJTPythonGrammarState stack) {
        super(stack);
    }
    

    /**
     * Subclasses must implement this method to deal with any node that's not properly handled in this base.
     * @param n the node that should be closed
     * @param arity the current number of nodes in the stack (found after the context was opened)
     * @return a new node representing the node that's having it's context closed.
     * @throws Exception
     */
    protected abstract SimpleNode onCloseNode(SimpleNode n, int arity) throws Exception;

    
    /**
     * Opens a new scope and returns a node to be used in this scope. This same node will later be called
     * in {@link #closeNode(SimpleNode, int)} to have its scope closed (and at that time it may be changed
     * for a new node that represents the scope more accurately.
     */
    public final SimpleNode openNode(int id) {
        SimpleNode ret;

        switch (id) {

            case JJTFILE_INPUT:
                ret = new Module(null);
                break;

            case JJTFALSE:
                ret = new Name("False", Name.Load, true);
                break;

            case JJTTRUE:
                ret = new Name("True", Name.Load, true);
                break;

            case JJTNONE:
                ret = new Name("None", Name.Load, true);
                break;

            case JJTNAME:
                //the actual name will be set during the parsing (token image) -- see Name construct
                ret = new Name(null, Name.Load, false);
                break;
                
            case JJTNUM:
                //the actual number will be set during the parsing (token image) -- see Num construct
                ret = new Num(null, -1, null);
                break;

            case JJTFOR_STMT:
                ret = new For(null, null, null, null);
                break;

            case JJTEXEC_STMT:
                ret = new Exec(null, null, null);
                break;
                
            case JJTPASS_STMT:
                ret = new Pass();
                break;
                
            case JJTBREAK_STMT:
                ret = new Break();
                break;
                
            case JJTCONTINUE_STMT:
                ret = new Continue();
                break;


            default:
                ret = new IdentityNode(id);
        }
        ret.setId(id);
        lastOpened = ret;
        return ret;
    }

    
    /**
     * Subclasses must implement this method to deal with any node that's not properly handled in this base.
     * @param n the node that should be closed
     * @param arity the current number of nodes in the stack (found after the context was opened)
     * @return a new node representing the node that's having it's context closed.
     * @throws Exception
     */
    public SimpleNode closeNode(SimpleNode n, int arity) throws Exception {
        exprType value;
        suiteType orelseSuite;
        stmtType[] body;
        exprType iter;
        exprType target;

        if(DEBUG_TREE_BUILDER){
            System.out.println("\n\n\n---------------------------");
            System.out.println("Closing node scope: "+n);
            System.out.println("Arity: "+arity);
            if(arity > 0){
                System.out.println("Nodes in scope: ");
                for(int i=0;i<arity;i++){
                    System.out.println(stack.peekNode(i));
                }
            }
        }
        
        switch(n.getId()){
            case -1:
                throw new ParseException("Illegal node found: "+n, n);
                
            case JJTFILE_INPUT:
                Module m = (Module) n;
                m.body = makeStmts(arity);
                return m;
                
                
            case JJTFALSE:
            case JJTTRUE:
            case JJTNONE:
            case JJTNAME:
            case JJTNUM:
            case JJTPASS_STMT:
            case JJTBREAK_STMT:
            case JJTCONTINUE_STMT:
                return n; //it's already the correct node (and it's value is already properly set)
                
                
            case JJTBINARY:
            case JJTUNICODE:
            case JJTSTRING:
                Object[] image = (Object[]) n.getImage();
                return new Str((String)image[0], (Integer)image[3], (Boolean)image[1], (Boolean)image[2], (Boolean)image[4]);

                
            case JJTSUITE:
                stmtType[] stmts = new stmtType[arity];
                for (int i = arity-1; i >= 0; i--) {
                    SimpleNode yield_or_stmt = stack.popNode();
                    if(yield_or_stmt instanceof Yield){
                        stmts[i] = new Expr((Yield)yield_or_stmt);
                        
                    }else{
                        stmts[i] = (stmtType) yield_or_stmt;
                    }
                }
                return new Suite(stmts);

                
            case JJTFOR_STMT:
                orelseSuite = null;
                if (stack.nodeArity() == 5){
                    orelseSuite = popSuiteAndSuiteType();
                }
                
                body = popSuite();
                iter = (exprType) stack.popNode();
                target = (exprType) stack.popNode();
                ctx.setStore(target);
                
                For forStmt = (For) n;
                forStmt.target = target;
                forStmt.iter = iter;
                forStmt.body = body;
                forStmt.orelse = orelseSuite;
                return forStmt;

                
            case JJTEXEC_STMT:
                exprType locals = arity >= 3 ? ((exprType) stack.popNode()) : null;
                exprType globals = arity >= 2 ? ((exprType) stack.popNode()) : null;
                value = (exprType) stack.popNode();
                Exec exec = (Exec) n;
                exec.body = value;
                exec.locals = locals;
                exec.globals = globals;
                return exec;
        }

        //if we found a node not expected in the base, let's give subclasses an opportunity for dealing with it.
        return onCloseNode(n, arity);
    }


}
