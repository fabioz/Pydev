package org.python.pydev.parser.grammarcommon;

import java.util.ArrayList;
import java.util.Iterator;

import org.python.pydev.parser.jython.ISpecialStr;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.SpecialStr;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.comprehensionType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.keywordType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.suiteType;

/**
 * Provides a bunch of helpers useful when creating a tree builder.
 * 
 * @author Fabio
 */
public abstract class AbstractTreeBuilderHelpers implements ITreeBuilder, ITreeConstants {

    protected final JJTPythonGrammarState stack;
    protected final CtxVisitor ctx;
    protected SimpleNode lastPop;

    public AbstractTreeBuilderHelpers(JJTPythonGrammarState stack) {
        this.stack = stack;
        this.ctx = new CtxVisitor();
    }

    protected final stmtType[] makeStmts(int l) {
        stmtType[] stmts = new stmtType[l];
        for (int i = l - 1; i >= 0; i--) {
            stmts[i] = (stmtType) stack.popNode();
        }
        return stmts;
    }

    protected final stmtType[] popSuite() {
        return getBodyAndSpecials();
    }

    protected final exprType[] makeExprs() {
        SimpleNode commaNode = null;
        if (stack.nodeArity() > 0 && stack.peekNode().getId() == JJTCOMMA){
            commaNode = stack.popNode();
        }
        int arity = stack.nodeArity();
        exprType[] exprs = makeExprs(arity);
        if(commaNode != null && arity > 0){
            exprs[arity-1].addSpecial(new SpecialStr(",", commaNode.beginLine, commaNode.beginColumn), true);
        }
        return exprs;
    }
    
    
    protected final SimpleNode makeTuple(SimpleNode n)  throws ParseException{
        try {
            boolean endsWithComma = false;
            //There's a detail with tuples: if we have a tuple with a single element and it doesn't end with a comma,
            //it's not actually treated as a tuple, but as its only item.
            if (stack.nodeArity() > 0 && stack.peekNode().getId() == JJTCOMMA){
                endsWithComma = true;
            }
            if(!endsWithComma && stack.nodeArity() == 1){
                java.util.List<Object> tokenSourceSpecialTokensList = this.stack.getGrammar().getTokenSourceSpecialTokensList();
                for(Object object:tokenSourceSpecialTokensList){
                    if(object instanceof Object[]){
                        Object[] objects = (Object[]) object;
                        object=objects[0];
                    }
                    if(object instanceof ISpecialStr){
                        ISpecialStr specialStr = (ISpecialStr) object;
                        if(specialStr.toString().equals(",")){
                            endsWithComma = true;
                            break;
                        }
                    }
                }
            }
            
            final exprType[] exp = makeExprs();
            Tuple t = new Tuple(exp, Tuple.Load, endsWithComma);
            addSpecialsAndClearOriginal(n, t);
            return t;
        } catch (ClassCastException e) {
            if(e.getMessage().equals(ExtraArgValue.class.getName())){
                this.stack.getGrammar().addAndReport(
                        new ParseException("Token: '*' is not expected inside tuples.", lastPop), 
                        "Treated class cast exception on tuple");
            }
            this.stack.getGrammar().addAndReport(
                    new ParseException("Syntax error while detecting tuple.", lastPop), 
                    "Treated class cast exception on tuple");
            
            while(stack.nodeArity() > 0){
                //clear whatever we had in this construct...
                stack.popNode();
            }
            
            //recover properly!
            return new Tuple(new exprType[0], Tuple.Load, false);

        }
    }

    protected final exprType[] makeExprs(int l) {
        exprType[] exprs = new exprType[l];
        for (int i = l - 1; i >= 0; i--) {
            lastPop = stack.popNode();
            exprs[i] = (exprType) lastPop;
        }
        return exprs;
    }

    protected final NameTok makeName(int ctx) {
        Name name = (Name) stack.popNode();
        return makeName(ctx, name);
    }

    protected final NameTok makeName(int ctx, Name name) {
        NameTok n = new NameTok(name.id, ctx);
        n.beginColumn = name.beginColumn;
        n.beginLine = name.beginLine;
        addSpecials(name, n);
        //we have to create it because it could be that specials are added later
        //(so, the instance must be already created even if not used)
        name.specialsBefore = n.getSpecialsBefore();
        name.specialsAfter = n.getSpecialsAfter();
        return n;
    }

    protected final NameTok[] makeIdentifiers(int ctx) {
        int l = stack.nodeArity();
        return makeIdentifiers(ctx, l);
    }

    protected final NameTok[] makeIdentifiers(int ctx, int arity) {
        NameTok[] ids = new NameTok[arity];
        for (int i = arity - 1; i >= 0; i--) {
            ids[i] = makeName(ctx);
        }
        return ids;
    }

    protected final suiteType popSuiteAndSuiteType() {
        Suite s = (Suite) stack.popNode();
        suiteType orelseSuite = (suiteType) stack.popNode();
        orelseSuite.body = s.body;
        addSpecialsAndClearOriginal(s, orelseSuite);
        return orelseSuite;
    }

    protected final void addSpecialsAndClearOriginal(SimpleNode from, SimpleNode to) {
        addSpecials(from, to);
        if (from.specialsBefore != null) {
            from.specialsBefore.clear();
        }
        if (from.specialsAfter != null) {
            from.specialsAfter.clear();
        }
    }

    protected final void addSpecials(SimpleNode from, SimpleNode to) {
        if (from.specialsBefore != null && from.specialsBefore.size() > 0) {
            to.getSpecialsBefore().addAll(from.specialsBefore);
        }
        if (from.specialsAfter != null && from.specialsAfter.size() > 0) {
            to.getSpecialsAfter().addAll(from.specialsAfter);
        }
    }

    protected final void addSpecialsBefore(SimpleNode from, SimpleNode to) {
        if (from.specialsBefore != null && from.specialsBefore.size() > 0) {
            to.getSpecialsBefore().addAll(from.specialsBefore);
        }
        if (from.specialsAfter != null && from.specialsAfter.size() > 0) {
            to.getSpecialsBefore().addAll(from.specialsAfter);
        }
    }

    protected final void setParentForFuncOrClass(stmtType[] body, SimpleNode classDef) {
        for (stmtType b : body) {
            if (b instanceof ClassDef || b instanceof FunctionDef) {
                b.parent = classDef;
            }
        }
    }

    /**
     * @param suite
     * @return
     */
    protected final stmtType[] getBodyAndSpecials() {
        final Suite suite = (Suite) stack.popNode();
        stmtType[] body = suite.body;
        if(body == null){
            //This can happen when we have errors in the grammar.
            body = new stmtType[0];
        }
        if(body.length > 0){
            //Check size (this can happen when parsing wrong grammar files)
            if (suite.specialsBefore != null && suite.specialsBefore.size() > 0) {
                body[0].getSpecialsBefore().addAll(suite.specialsBefore);
            }
    
            if (suite.specialsAfter != null && suite.specialsAfter.size() > 0) {
                body[body.length - 1].getSpecialsAfter().addAll(suite.specialsAfter);
            }
        }
        return body;
    }

    protected final SimpleNode makeDecorator(java.util.List<SimpleNode> nodes) {
        exprType starargs = null;
        exprType kwargs = null;

        exprType func = null;
        ArrayList<SimpleNode> keywordsl = new ArrayList<SimpleNode>();
        ArrayList<SimpleNode> argsl = new ArrayList<SimpleNode>();
        for (Iterator<SimpleNode> iter = nodes.iterator(); iter.hasNext();) {
            SimpleNode node = iter.next();

            if (node.getId() == JJTEXTRAKEYWORDVALUELIST) {
                final ExtraArgValue extraArg = (ExtraArgValue) node;
                kwargs = (extraArg).value;
                this.addSpecialsAndClearOriginal(extraArg, kwargs);
                extraArg.specialsBefore = kwargs.getSpecialsBefore();
                extraArg.specialsAfter = kwargs.getSpecialsAfter();

            } else if (node.getId() == JJTEXTRAARGVALUELIST) {
                final ExtraArgValue extraArg = (ExtraArgValue) node;
                starargs = extraArg.value;
                this.addSpecialsAndClearOriginal(extraArg, starargs);
                extraArg.specialsBefore = starargs.getSpecialsBefore();
                extraArg.specialsAfter = starargs.getSpecialsAfter();

            } else if (node instanceof keywordType) {
                keywordType keyword = (keywordType) node;
                if(starargs == null){
                    keyword.afterstarargs = true;
                }
                keywordsl.add(keyword);

            } else if (isArg(node)) {
                //default
                argsl.add(node);

            } else if (node instanceof Comprehension) {
                argsl.add(new ListComp((exprType) iter.next(), new comprehensionType[] { (comprehensionType) node }, ListComp.EmptyCtx));

            } else if (node instanceof ComprehensionCollection) {
                //list comp (2 nodes: comp type and the elt -- what does elt mean by the way?) 
                argsl.add(new ListComp((exprType) iter.next(), ((ComprehensionCollection) node).getGenerators(), ListComp.EmptyCtx));

            } else if (node instanceof decoratorsType) {
                func = (exprType) stack.popNode();//the func is the last thing in the stack
                decoratorsType d = (decoratorsType) node;
                d.func = func;
                d.args = (exprType[]) argsl.toArray(new exprType[0]);
                d.keywords = (keywordType[]) keywordsl.toArray(new keywordType[0]);
                d.starargs = starargs;
                d.kwargs = kwargs;
                return d;

            } else {
                argsl.add(node);
            }

        }
        throw new RuntimeException("Something wrong happened while making the decorators...");

    }

    protected final aliasType[] makeAliases(int l) {
        aliasType[] aliases = new aliasType[l];
        for (int i = l - 1; i >= 0; i--) {
            aliases[i] = (aliasType) stack.popNode();
        }
        return aliases;
    }

    protected final boolean isArg(SimpleNode n) {
        return n instanceof ExtraArg || n instanceof DefaultArg || n instanceof keywordType;
    }
    
    
    protected final SimpleNode defaultCreateDictionary(int arity) {
        boolean isDictComplete = arity % 2 == 0;
        
        int l = arity / 2;
        exprType[] keys;
        if(isDictComplete){
            keys = new exprType[l];
        }else{
            keys = new exprType[l+1]; //we have 1 additional entry in the keys (parse error actually, but let's recover at this point!)
        }
        exprType[] vals = new exprType[l];
        for (int i = l - 1; i >= 0; i--) {
            vals[i] = (exprType) stack.popNode();
            keys[i] = (exprType) stack.popNode();
        }
        if(!isDictComplete){
            keys[keys.length-1] = (exprType) stack.popNode();
        }
        return new Dict(keys, vals);
    }




}
