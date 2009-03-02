package org.python.pydev.parser.grammarcommon;

import java.util.ArrayList;
import java.util.Iterator;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Suite;
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
        if (stack.nodeArity() > 0 && stack.peekNode().getId() == JJTCOMMA)
            stack.popNode();
        return makeExprs(stack.nodeArity());
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
        Suite suite = (Suite) stack.popNode();
        stmtType[] body;
        body = suite.body;
        if (suite.specialsBefore != null && suite.specialsBefore.size() > 0) {
            body[0].getSpecialsBefore().addAll(suite.specialsBefore);
        }

        if (suite.specialsAfter != null && suite.specialsAfter.size() > 0) {
            body[body.length - 1].getSpecialsAfter().addAll(suite.specialsAfter);
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
                //keyword
                keywordsl.add(node);

            } else if (isArg(node)) {
                //default
                argsl.add(node);

            } else if (node instanceof Comprehension) {
                argsl.add(new ListComp((exprType) iter.next(), new comprehensionType[] { (comprehensionType) node }));

            } else if (node instanceof ComprehensionCollection) {
                //list comp (2 nodes: comp type and the elt -- what does elt mean by the way?) 
                argsl.add(new ListComp((exprType) iter.next(), ((ComprehensionCollection) node).getGenerators()));

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
