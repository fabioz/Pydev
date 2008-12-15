package org.python.pydev.parser.grammarcommon;

import java.util.ArrayList;

import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.AugAssign;
import org.python.pydev.parser.jython.ast.Break;
import org.python.pydev.parser.jython.ast.Continue;
import org.python.pydev.parser.jython.ast.Exec;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.ExtSlice;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.comprehensionType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.sliceType;
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
                
            case JJTSTRING:
            case JJTUNICODE:
            case JJTBINARY:
                //the actual number will be set during the parsing (token image) -- see Num construct
                ret = new Str(null, -1, false, false, false);
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

            case JJTBEGIN_DECORATOR:
                ret = new decoratorsType(null,null,null,null, null);
                break;
                
            case JJTIF_STMT:
                ret = new If(null, null, null);
                break;
                
            case JJTAUG_PLUS:     
                ret = new AugAssign(null, AugAssign.Add, null);
                break;
                
            case JJTAUG_MINUS:   
                ret = new AugAssign(null, AugAssign.Sub, null);
                break;
                
            case JJTAUG_MULTIPLY:  
                ret = new AugAssign(null, AugAssign.Mult, null);
                break;
                
            case JJTAUG_DIVIDE:   
                ret = new AugAssign(null, AugAssign.Div, null);
                break;
                
            case JJTAUG_MODULO:  
                ret = new AugAssign(null, AugAssign.Mod, null);
                break;
                
            case JJTAUG_AND:    
                ret = new AugAssign(null, AugAssign.BitAnd, null);
                break;
                
            case JJTAUG_OR:    
                ret = new AugAssign(null, AugAssign.BitOr, null);
                break;
                
            case JJTAUG_XOR:  
                ret = new AugAssign(null, AugAssign.BitXor, null);
                break;
                
            case JJTAUG_LSHIFT:   
                ret = new AugAssign(null, AugAssign.LShift, null);
                break;
                
            case JJTAUG_RSHIFT:  
                ret = new AugAssign(null, AugAssign.RShift, null);
                break;
                
            case JJTAUG_POWER:  
                ret = new AugAssign(null, AugAssign.Pow, null);
                break;
                
            case JJTAUG_FLOORDIVIDE:  
                ret = new AugAssign(null, AugAssign.FloorDiv, null);
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
        exprType test;

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
            case JJTSTRING:
            case JJTUNICODE:
            case JJTBINARY:
            case JJTBEGIN_DECORATOR:
                return n; //it's already the correct node (and it's value is already properly set)
                
                
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
                
                
            case JJTBEGIN_ELIF_STMT:
                return new If(null, null, null);

                
            case JJTIF_STMT:
                stmtType[] orelse = null;
                if ((arity+1) % 3 == 1){
                    arity--;
                    orelse = getBodyAndSpecials();
                }
                
                //make the suite
                Suite suite = (Suite)stack.popNode();
                arity--;
                body = suite.body;
                test = (exprType) stack.popNode();
                arity--;
                
                //make the if
                If last;
                if(arity == 0){
                    //last If found
                    last = (If) n;
                }else{
                    last = (If) stack.popNode();
                    arity--;
                }
                last.test = test;
                last.body = body;
                last.orelse = orelse;
                addSpecialsAndClearOriginal(suite, last);
                
                while(arity > 0) {
                    suite = (Suite)stack.popNode();
                    arity--;
                    
                    body = suite.body;
                    test = (exprType) stack.popNode();
                    arity--;
                    
                    stmtType[] newOrElse = new stmtType[] { last };
                    if(arity == 0){
                        //last If found
                        last = (If) n;
                    }else{
                        last = (If) stack.popNode();
                        arity--;
                    }
                    last.test = test;
                    last.body = body;
                    last.orelse = newOrElse;
                    addSpecialsAndClearOriginal(suite, last);
                }
                return last;

                
            case JJTEXEC_STMT:
                exprType locals = arity >= 3 ? ((exprType) stack.popNode()) : null;
                exprType globals = arity >= 2 ? ((exprType) stack.popNode()) : null;
                value = (exprType) stack.popNode();
                Exec exec = (Exec) n;
                exec.body = value;
                exec.locals = locals;
                exec.globals = globals;
                return exec;
                
                
            case JJTDECORATORS:
                ArrayList<SimpleNode> list2 = new ArrayList<SimpleNode>();
                ArrayList<SimpleNode> listArgs = new ArrayList<SimpleNode>();
                while(stack.nodeArity() > 0){
                    SimpleNode node = stack.popNode();
                    while(!(node instanceof decoratorsType)){
                        if(node instanceof comprehensionType){
                            listArgs.add(node);
                            listArgs.add(stack.popNode()); //target
                        }else if(node instanceof ComprehensionCollection){
                            listArgs.add(((ComprehensionCollection)node).getGenerators()[0]);
                            listArgs.add(stack.popNode()); //target
                            
                        }else{
                            listArgs.add(node);
                        }
                        node = stack.popNode();
                    }
                    listArgs.add(node);//the decoratorsType
                    list2.add(0,makeDecorator(listArgs));
                    listArgs.clear();
                }
                return new Decorators((decoratorsType[]) list2.toArray(new decoratorsType[0]), JJTDECORATORS);
                
                
            case JJTSUBSCRIPTLIST:
                sliceType[] dims = new sliceType[arity];
                for (int i = arity - 1; i >= 0; i--) {
                    SimpleNode sliceNode = stack.popNode();
                    if(sliceNode instanceof sliceType){
                        dims[i] = (sliceType) sliceNode;
                        
                    }else if(sliceNode instanceof IdentityNode){
                        //this should be ignored...
                        //this happens when parsing something like a[1,], whereas a[1,2] would not have this.
                        
                    }else{
                        throw new RuntimeException("Expected a sliceType or an IdentityNode. Received :"+sliceNode.getClass());
                    }
                }
                return new ExtSlice(dims);
                
                
            case JJTAUG_PLUS:     
            case JJTAUG_MINUS:   
            case JJTAUG_MULTIPLY:  
            case JJTAUG_DIVIDE:   
            case JJTAUG_MODULO:  
            case JJTAUG_AND:    
            case JJTAUG_OR:    
            case JJTAUG_XOR:  
            case JJTAUG_LSHIFT:   
            case JJTAUG_RSHIFT:  
            case JJTAUG_POWER:  
            case JJTAUG_FLOORDIVIDE:  
                fillAugAssign((AugAssign) n);
                return n;
        }

        //if we found a node not expected in the base, let's give subclasses an opportunity for dealing with it.
        return onCloseNode(n, arity);
    }


}
