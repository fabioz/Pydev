package org.python.pydev.parser.grammar25;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Visitor;
import org.python.pydev.parser.jython.ast.Assert;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.AugAssign;
import org.python.pydev.parser.jython.ast.BinOp;
import org.python.pydev.parser.jython.ast.BoolOp;
import org.python.pydev.parser.jython.ast.Break;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Compare;
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.Continue;
import org.python.pydev.parser.jython.ast.Delete;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.Ellipsis;
import org.python.pydev.parser.jython.ast.Exec;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.Expression;
import org.python.pydev.parser.jython.ast.ExtSlice;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Global;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.IfExp;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Index;
import org.python.pydev.parser.jython.ast.Interactive;
import org.python.pydev.parser.jython.ast.Lambda;
import org.python.pydev.parser.jython.ast.List;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Print;
import org.python.pydev.parser.jython.ast.Raise;
import org.python.pydev.parser.jython.ast.Repr;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.Slice;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.StrJoin;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.UnaryOp;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.With;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.comprehensionType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.excepthandlerType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.expr_contextType;
import org.python.pydev.parser.jython.ast.keywordType;
import org.python.pydev.parser.jython.ast.sliceType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.suiteType;

public final class TreeBuilder25 implements PythonGrammar25TreeConstants {
    private JJTPythonGrammar25State stack;
    private CtxVisitor ctx;
    private SimpleNode lastPop;
    
    public TreeBuilder25(JJTPythonGrammar25State stack) {
        this.stack = stack;
        this.ctx = new CtxVisitor();
    }

    private stmtType[] makeStmts(int l) {
        stmtType[] stmts = new stmtType[l];
        for (int i = l-1; i >= 0; i--) {
            stmts[i] = (stmtType) stack.popNode();
        }
        return stmts;
    }

    private stmtType[] popSuite() {
        return getBodyAndSpecials();
    }

    private exprType[] makeExprs() {
        if (stack.nodeArity() > 0 && stack.peekNode().getId() == JJTCOMMA)
			stack.popNode();
        return makeExprs(stack.nodeArity());
    }

    private exprType[] makeExprs(int l) {
        exprType[] exprs = new exprType[l];
        for (int i = l-1; i >= 0; i--) {
            lastPop = stack.popNode();
            exprs[i] = (exprType) lastPop;
        }
        return exprs;
    }

    private NameTok makeName(int ctx) {
        Name name = (Name) stack.popNode();
        NameTok n = new NameTok(name.id, ctx);
        n.beginColumn = name.beginColumn;
        n.beginLine = name.beginLine;
        addSpecials(name, n);
        name.specialsBefore = n.getSpecialsBefore();
        name.specialsAfter = n.getSpecialsAfter();
        return n;
    }
    
    private NameTok[] makeIdentifiers(int ctx) {
        int l = stack.nodeArity();
        NameTok[] ids = new NameTok[l];
        for (int i = l - 1; i >= 0; i--) {
            ids[i] = makeName(ctx);
        }
        return ids;
    }

    private aliasType[] makeAliases(int l) {
        aliasType[] aliases = new aliasType[l];
        for (int i = l-1; i >= 0; i--) {
            aliases[i] = (aliasType) stack.popNode();
        }
        return aliases;
    }

    private static SimpleNode[] nodes = new SimpleNode[PythonGrammar25TreeConstants.jjtNodeName.length];

    public SimpleNode openNode(int id) {
        if (nodes[id] == null)
            nodes[id] = new IdentityNode(id);
        return nodes[id];
    }

    
    public SimpleNode closeNode(SimpleNode n, int arity) throws Exception {
        exprType value;
        exprType[] exprs;

        int l;
        switch (n.getId()) {
        case -1:
            System.out.println("Illegal node");
        case JJTSINGLE_INPUT:
            return new Interactive(makeStmts(arity));
        case JJTFILE_INPUT:
            return new Module(makeStmts(arity));
        case JJTEVAL_INPUT:
            return new Expression(((exprType) stack.popNode()));

        case JJTNAME:
            Name name = new Name(n.getImage().toString(), Name.Load);
            addSpecialsAndClearOriginal(n, name);
            return name;
        case JJTNUM:
        	Object[] numimage = (Object[]) n.getImage();
            return new Num(numimage[0], (Integer)numimage[1], (String)numimage[2]);
        case JJTUNICODE:
        case JJTSTRING:
            Object[] image = (Object[]) n.getImage();
            return new Str((String)image[0], (Integer)image[3], (Boolean)image[1], (Boolean)image[2]);

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
        case JJTEXPR_STMT:
            value = (exprType) stack.popNode();
            if (arity > 1) {
                exprs = makeExprs(arity-1);
                ctx.setStore(exprs);
                return new Assign(exprs, value);
            } else {
                return new Expr(value);
            }
        case JJTINDEX_OP:
            sliceType slice = (sliceType) stack.popNode();
            value = (exprType) stack.popNode();
            return new Subscript(value, slice, Subscript.Load);
        case JJTDOT_OP:
            NameTok attr = makeName(NameTok.Attrib);
            value = (exprType) stack.popNode();
            return new Attribute(value, attr, Attribute.Load);
        case JJTBEGIN_DEL_STMT:
        	return new Delete(null);
        case JJTDEL_STMT:
            exprs = makeExprs(arity-1);
            ctx.setDelete(exprs);
            Delete d = (Delete) stack.popNode();
            d.targets = exprs;
            return d;
        case JJTPRINT_STMT:
            boolean nl = true;
            if (stack.nodeArity() == 0){
            	Print p = new Print(null, null, true);
            	p.getSpecialsBefore().add(0, "print ");
                return p;
            }
            
            if (stack.peekNode().getId() == JJTCOMMA) {
                stack.popNode();
                nl = false;
            }
            Print p = new Print(null, makeExprs(), nl);
        	p.getSpecialsBefore().add(0, "print ");
            return p;
        case JJTPRINTEXT_STMT:
            nl = true;
            if (stack.peekNode().getId() == JJTCOMMA) {
                stack.popNode();
                nl = false;
            }
            exprs = makeExprs(stack.nodeArity()-1);
            p = new Print(((exprType) stack.popNode()), exprs, nl);
            p.getSpecialsBefore().add(0, ">> ");
        	p.getSpecialsBefore().add(0, "print ");
            return p;
        case JJTBEGIN_FOR_STMT:
            return new For(null,null,null,null);
        case JJTFOR_STMT:
            suiteType orelseSuite = null;
            if (stack.nodeArity() == 6){
                orelseSuite = popSuiteAndSuiteType();
            }
            
            stmtType[] body = popSuite();
            exprType iter = (exprType) stack.popNode();
            exprType target = (exprType) stack.popNode();
            ctx.setStore(target);
            
            For forStmt = (For) stack.popNode();
            forStmt.target = target;
            forStmt.iter = iter;
            forStmt.body = body;
            forStmt.orelse = orelseSuite;
            return forStmt;
        case JJTBEGIN_FOR_ELSE_STMT:
            return new suiteType(null);
        case JJTBEGIN_ELSE_STMT:
            return new suiteType(null);
        case JJTBEGIN_WHILE_STMT:
            return new While(null, null, null);
        case JJTWHILE_STMT:
            orelseSuite = null;
            if (stack.nodeArity() == 5){
                orelseSuite = popSuiteAndSuiteType();
            }
            
            body = popSuite();
            exprType test = (exprType) stack.popNode();
            While w = (While) stack.popNode();
            w.test = test;
            w.body = body;
            w.orelse = orelseSuite;
            return w;
        case JJTBEGIN_IF_STMT:
            return new If(null, null, null);
        case JJTBEGIN_ELIF_STMT:
            return new If(null, null, null);
        case JJTIF_STMT:
            stmtType[] orelse = null;
            //arity--;//because of the beg if stmt
            if (arity % 3 == 1){
                orelse = getBodyAndSpecials();
            }
            
            //make the suite
            Suite suite = (Suite)stack.popNode();
            body = suite.body;
            test = (exprType) stack.popNode();
            
            //make the if
            If last = (If) stack.popNode();
            last.test = test;
            last.body = body;
            last.orelse = orelse;
            addSpecialsAndClearOriginal(suite, last);
            
            for (int i = 0; i < (arity / 3)-1; i++) {
                //arity--;//because of the beg if stmt

                suite = (Suite)stack.popNode();
                body = suite.body;
                test = (exprType) stack.popNode();
                stmtType[] newOrElse = new stmtType[] { last };
                last = (If) stack.popNode();
                last.test = test;
                last.body = body;
                last.orelse = newOrElse;
                addSpecialsAndClearOriginal(suite, last);
            }
            return last;
        case JJTPASS_STMT:
            return new Pass();
        case JJTBREAK_STMT:
            return new Break();
        case JJTCONTINUE_STMT:
            return new Continue();
        case JJTBEGIN_DECORATOR:
            return new decoratorsType(null,null,null,null, null);
        case JJTDECORATORS:
            ArrayList<SimpleNode> list2 = new ArrayList<SimpleNode>();
            ArrayList<SimpleNode> listArgs = new ArrayList<SimpleNode>();
            while(stack.nodeArity() > 0){
                SimpleNode node = stack.popNode();
                while(!(node instanceof decoratorsType)){
                    if(node instanceof ComprehensionCollection){
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
        case JJTCALL_OP:
            exprType starargs = null;
            exprType kwargs = null;

            l = arity - 1;
            if (l > 0 && stack.peekNode().getId() == JJTEXTRAKEYWORDVALUELIST) {
                ExtraArgValue nkwargs = (ExtraArgValue) stack.popNode();
                kwargs = nkwargs.value;
                this.addSpecialsAndClearOriginal(nkwargs, kwargs);
                l--;
            }
            if (l > 0 && stack.peekNode().getId() == JJTEXTRAARGVALUELIST) {
                ExtraArgValue nstarargs = (ExtraArgValue) stack.popNode();
                starargs = nstarargs.value;
                this.addSpecialsAndClearOriginal(nstarargs, starargs);
                l--;
            }
            
            int nargs = l;

            SimpleNode[] tmparr = new SimpleNode[l]; 
            for (int i = l - 1; i >= 0; i--) {
                tmparr[i] = stack.popNode();
                if (tmparr[i] instanceof keywordType) {
                    nargs = i;
                }
            }
            
            exprType[] args = new exprType[nargs];
            for (int i = 0; i < nargs; i++) {
                //what can happen is something like print sum(x for x in y), where we have already passed x in the args, and then get 'for x in y'
                if(tmparr[i] instanceof ComprehensionCollection){
                    args = new exprType[]{
                        new ListComp(args[0], ((ComprehensionCollection)tmparr[i]).getGenerators())};
                }else{
                    args[i] = (exprType) tmparr[i];
                }
            }

            keywordType[] keywords = new keywordType[l - nargs];
            for (int i = nargs; i < l; i++) {
                if (!(tmparr[i] instanceof keywordType))
                    throw new ParseException(
                        "non-keyword argument following keyword", tmparr[i]);
                keywords[i - nargs] = (keywordType) tmparr[i];
            }
            exprType func = (exprType) stack.popNode();
            Call c = new Call(func, args, keywords, starargs, kwargs);
            addSpecialsAndClearOriginal(n, c);
            return c;
        case JJTFUNCDEF:
            //get the decorators
            //and clear them for the next call (they always must be before a function def)
            suite = (Suite) stack.popNode();
            body = suite.body;
            
            argumentsType arguments = makeArguments(stack.nodeArity() - 2);
            NameTok nameTok = makeName(NameTok.FunctionName);
            Decorators decs = (Decorators) stack.popNode() ;
            decoratorsType[] decsexp = decs.exp;
            FunctionDef funcDef = new FunctionDef(nameTok, arguments, body, decsexp);
            if(decs.exp.length == 0){
                addSpecialsBefore(decs, funcDef);
            }
            addSpecialsAndClearOriginal(suite, funcDef);
            setParentForFuncOrClass(body, funcDef);
            return funcDef;
        case JJTDEFAULTARG:
            value = (arity == 1) ? null : ((exprType) stack.popNode());
            return new DefaultArg(((exprType) stack.popNode()), value);
        case JJTEXTRAARGLIST:
            return new ExtraArg(makeName(NameTok.VarArg), JJTEXTRAARGLIST);
        case JJTEXTRAKEYWORDLIST:
            return new ExtraArg(makeName(NameTok.KwArg), JJTEXTRAKEYWORDLIST);
        case JJTCLASSDEF:
            suite = (Suite) stack.popNode();
            body = suite.body;
            exprType[] bases = makeExprs(stack.nodeArity() - 1);
            nameTok = makeName(NameTok.ClassName);
            ClassDef classDef = new ClassDef(nameTok, bases, body);
            addSpecialsAndClearOriginal(suite, classDef);
            setParentForFuncOrClass(body, classDef);
            return classDef;
        case JJTBEGIN_RETURN_STMT:
            return new Return(null);
        case JJTRETURN_STMT:
            value = arity == 2 ? ((exprType) stack.popNode()) : null;
            Return ret = (Return) stack.popNode();
            ret.value = value;
            return ret;
        case JJTYIELD_STMT:
            return stack.popNode();
        case JJTYIELD_EXPR:
            exprType yieldExpr = null;
            if(arity > 0){
                //we may have an empty yield, so, we have to check it before
                yieldExpr = (exprType) stack.popNode();
            }
            return new Yield(yieldExpr);
        case JJTRAISE_STMT:
            exprType tback = arity >= 3 ? ((exprType) stack.popNode()) : null;
            exprType inst = arity >= 2 ? ((exprType) stack.popNode()) : null;
            exprType type = arity >= 1 ? ((exprType) stack.popNode()) : null;
            return new Raise(type, inst, tback);
        case JJTGLOBAL_STMT:
            Global global = new Global(makeIdentifiers(NameTok.GlobalName));
            return global;
        case JJTEXEC_STMT:
            exprType globals = arity >= 3 ? ((exprType) stack.popNode()) : null;
            exprType locals = arity >= 2 ? ((exprType) stack.popNode()) : null;
            value = (exprType) stack.popNode();
            return new Exec(value, locals, globals);
        case JJTASSERT_STMT:
            exprType msg = arity == 2 ? ((exprType) stack.popNode()) : null;
            test = (exprType) stack.popNode();
            return new Assert(test, msg);
        case JJTBEGIN_TRY_STMT:
            //we do that just to get the specials
            return new TryExcept(null, null, null);
        case JJTTRYELSE_STMT:
            orelseSuite = popSuiteAndSuiteType();
            return orelseSuite;
        case JJTTRYFINALLY_OUTER_STMT:
            orelseSuite = popSuiteAndSuiteType();
            return new TryFinally(null, orelseSuite); //it does not have a body at this time... it will be filled with the inner try..except
        case JJTTRY_STMT:
            TryFinally outer = null;
            if(stack.peekNode() instanceof TryFinally){
                outer = (TryFinally) stack.popNode();
                arity--;
            }
            orelseSuite = null;
            if(stack.peekNode() instanceof suiteType){
                orelseSuite = (suiteType) stack.popNode();
                arity--;
            }
            
            l = arity ;
            excepthandlerType[] handlers = new excepthandlerType[l];
            for (int i = l - 1; i >= 0; i--) {
                handlers[i] = (excepthandlerType) stack.popNode();
            }
            suite = (Suite)stack.popNode();
            TryExcept tryExc = (TryExcept) stack.popNode();
            if (outer != null) {
                outer.beginLine = tryExc.beginLine - 1;
            }
            tryExc.body = suite.body;
            tryExc.handlers = handlers;
            tryExc.orelse = orelseSuite;
            addSpecials(suite, tryExc);
            if (outer == null){
                return tryExc;
            }else{
                if(outer.body != null){
                    throw new RuntimeException("Error. Expecting null body to be filled on try..except..finally");
                }
                outer.body = new stmtType[]{tryExc};
                return outer;
            }
        case JJTBEGIN_TRY_ELSE_STMT:
            //we do that just to get the specials
            return new suiteType(null);
        case JJTBEGIN_EXCEPT_CLAUSE:
        	return new excepthandlerType(null,null,null);
        case JJTEXCEPT_CLAUSE:
            suite = (Suite) stack.popNode();
            body = suite.body;
            exprType excname = arity == 4 ? ((exprType) stack.popNode()) : null;
            if (excname != null){    
                ctx.setStore(excname);
            }
            type = arity >= 3 ? ((exprType) stack.popNode()) : null;
            excepthandlerType handler = (excepthandlerType) stack.popNode(); 
        	handler.type = type;
        	handler.name = excname;
        	handler.body = body;
            addSpecials(suite, handler);
            return handler;
        case JJTBEGIN_FINALLY_STMT:
            //we do that just to get the specials
            return new suiteType(null);
        case JJTTRYFINALLY_STMT:
            suiteType finalBody = popSuiteAndSuiteType();
            body = popSuite();
            //We have a try..except in the stack, but we will change it for a try..finally
            //This is because we recognize a try..except in the 'try:' token, but actually end up with a try..finally
            TryExcept tryExcept = (TryExcept) stack.popNode();
            TryFinally tryFinally = new TryFinally(body, finalBody);
            tryFinally.beginLine = tryExcept.beginLine;
            tryFinally.beginColumn = tryExcept.beginColumn;
            addSpecialsAndClearOriginal(tryExcept, tryFinally);
            return tryFinally;
            
        case JJTWITH_STMT:
            suite = (Suite) stack.popNode();
            arity--;
            
            exprType asOrExpr = (exprType) stack.popNode();
            arity--;
            
            exprType expr=null;
            if(arity > 0){
                expr = (exprType) stack.popNode();
                arity--;
            }else{
                expr = asOrExpr;
                asOrExpr = null;
            }
            
            suiteType s = new suiteType(suite.body);
            addSpecialsAndClearOriginal(suite, s);
            
            return new With(expr, asOrExpr, s);
        case JJTWITH_VAR:
            expr = (exprType) stack.popNode(); //expr
            if (expr != null){    
                ctx.setStore(expr);
            }
            return expr;
        case JJTOR_BOOLEAN:
            return new BoolOp(BoolOp.Or, makeExprs());
        case JJTAND_BOOLEAN:
            return new BoolOp(BoolOp.And, makeExprs());
        case JJTCOMPARISION:
            l = arity / 2;
            exprType[] comparators = new exprType[l];
            int[] ops = new int[l];
            for (int i = l-1; i >= 0; i--) {
                comparators[i] = (exprType) stack.popNode();
                SimpleNode op = stack.popNode();
                switch (op.getId()) {
                case JJTLESS_CMP:          ops[i] = Compare.Lt; break;
                case JJTGREATER_CMP:       ops[i] = Compare.Gt; break;
                case JJTEQUAL_CMP:         ops[i] = Compare.Eq; break;
                case JJTGREATER_EQUAL_CMP: ops[i] = Compare.GtE; break;
                case JJTLESS_EQUAL_CMP:    ops[i] = Compare.LtE; break;
                case JJTNOTEQUAL_CMP:      ops[i] = Compare.NotEq; break;
                case JJTIN_CMP:            ops[i] = Compare.In; break;
                case JJTNOT_IN_CMP:        ops[i] = Compare.NotIn; break;
                case JJTIS_NOT_CMP:        ops[i] = Compare.IsNot; break;
                case JJTIS_CMP:            ops[i] = Compare.Is; break;
                default:
                    throw new RuntimeException("Unknown cmp op:" + op.getId());
                }
            }
            return new Compare(((exprType) stack.popNode()), ops, comparators);
        case JJTLESS_CMP:
        case JJTGREATER_CMP:
        case JJTEQUAL_CMP:
        case JJTGREATER_EQUAL_CMP:
        case JJTLESS_EQUAL_CMP:
        case JJTNOTEQUAL_CMP:
        case JJTIN_CMP:
        case JJTNOT_IN_CMP:
        case JJTIS_NOT_CMP:
        case JJTIS_CMP:
            return n;
        case JJTOR_2OP:
            return makeBinOp(BinOp.BitOr);
        case JJTXOR_2OP:
            return makeBinOp(BinOp.BitXor);
        case JJTAND_2OP:
            return makeBinOp(BinOp.BitAnd);
        case JJTLSHIFT_2OP:
            return makeBinOp(BinOp.LShift);
        case JJTRSHIFT_2OP:
            return makeBinOp(BinOp.RShift);
        case JJTADD_2OP:  
            return makeBinOp(BinOp.Add);
        case JJTSUB_2OP: 
            return makeBinOp(BinOp.Sub);
        case JJTMUL_2OP:
            return makeBinOp(BinOp.Mult);
        case JJTDIV_2OP: 
            return makeBinOp(BinOp.Div);
        case JJTMOD_2OP:
            return makeBinOp(BinOp.Mod);
        case JJTPOW_2OP:
            return makeBinOp(BinOp.Pow);
        case JJTFLOORDIV_2OP:
            return makeBinOp(BinOp.FloorDiv);
        case JJTPOS_1OP:
            return new UnaryOp(UnaryOp.UAdd, ((exprType) stack.popNode()));
        case JJTNEG_1OP:
            return new UnaryOp(UnaryOp.USub, ((exprType) stack.popNode()));
        case JJTINVERT_1OP:
            return new UnaryOp(UnaryOp.Invert, ((exprType) stack.popNode()));
        case JJTNOT_1OP:
            return new UnaryOp(UnaryOp.Not, ((exprType) stack.popNode()));
        case JJTEXTRAKEYWORDVALUELIST:
            return new ExtraArgValue(((exprType) stack.popNode()), JJTEXTRAKEYWORDVALUELIST);
        case JJTEXTRAARGVALUELIST:
            return new ExtraArgValue(((exprType) stack.popNode()), JJTEXTRAARGVALUELIST);
        case JJTKEYWORD:
            value = (exprType) stack.popNode();
            nameTok = makeName(NameTok.KeywordName);
            return new keywordType(nameTok, value);
        case JJTTUPLE:
            if (stack.nodeArity() > 0) {
                SimpleNode peeked = stack.peekNode();
                if(peeked instanceof ComprehensionCollection){
                    ComprehensionCollection col = (ComprehensionCollection) stack.popNode();
                    return new ListComp(((exprType) stack.popNode()), col.getGenerators());
                }
            }
            try {
                exprType[] exp = makeExprs();
                Tuple t = new Tuple(exp, Tuple.Load);
                addSpecialsAndClearOriginal(n, t);
                return t;
            } catch (ClassCastException e) {
                if(e.getMessage().equals(ExtraArgValue.class.getName())){
                    throw new ParseException("Token: '*' is not expected inside tuples.", lastPop);
                }
                e.printStackTrace();
                throw new ParseException("Syntax error while detecting tuple.", lastPop);
            }
        case JJTLIST:
            if (stack.nodeArity() > 0 && stack.peekNode() instanceof ComprehensionCollection) {
                ComprehensionCollection col = (ComprehensionCollection) stack.popNode();
                return new ListComp(((exprType) stack.popNode()), col.getGenerators());
            }
            return new List(makeExprs(), List.Load);
        case JJTDICTIONARY:
            l = arity / 2;
            exprType[] keys = new exprType[l];
            exprType[] vals = new exprType[l];
            for (int i = l - 1; i >= 0; i--) {
                vals[i] = (exprType) stack.popNode();
                keys[i] = (exprType) stack.popNode();
            }
            return new Dict(keys, vals);
        case JJTSTR_1OP:
            return new Repr(((exprType) stack.popNode()));
        case JJTSTRJOIN:
            Str str2 = (Str) stack.popNode();
            Object o = stack.popNode();
            if(o instanceof Str){
                Str str1 = (Str) o;
                return new StrJoin(new exprType[]{str1, str2});
            }else{
                StrJoin strJ = (StrJoin) o;
                exprType[] newStrs = new exprType[strJ.strs.length +1];
                System.arraycopy(strJ.strs, 0, newStrs, 0, strJ.strs.length);
                newStrs[strJ.strs.length] = str2;
                strJ.strs = newStrs;
                return strJ;
            }
        case JJTTEST:
            if(arity == 2){
                IfExp node = (IfExp) stack.popNode();
                node.body = (exprType) stack.popNode();
                return node;
            }else{
                return stack.popNode();
            }
        case JJTIF_EXP:
            exprType ifExprOrelse=(exprType) stack.popNode();
            exprType ifExprTest=(exprType) stack.popNode();
            return new IfExp(ifExprTest,null,ifExprOrelse);
        case JJTOLD_LAMBDEF:
        case JJTLAMBDEF:
            test = (exprType) stack.popNode();
            arguments = makeArguments(arity - 1);
            Lambda lambda = new Lambda(arguments, test);
            if(arguments == null || arguments.args == null || arguments.args.length == 0){
                lambda.getSpecialsBefore().add("lambda");
            }else{
                lambda.getSpecialsBefore().add("lambda ");
            }
            return lambda;
        case JJTELLIPSES:
            return new Ellipsis();
        case JJTSLICE:
            SimpleNode[] arr = new SimpleNode[arity];
            for (int i = arity-1; i >= 0; i--) {
                arr[i] = stack.popNode();
            }

            exprType[] values = new exprType[3];
            int k = 0;
            java.util.List<Object> specialsBefore = new ArrayList<Object>();
            java.util.List<Object> specialsAfter = new ArrayList<Object>();
            for (int j = 0; j < arity; j++) {
                if (arr[j].getId() == JJTCOLON){
                    if(arr[j].specialsBefore != null){
                        specialsBefore.addAll(arr[j].specialsBefore);
                        arr[j].specialsBefore.clear(); //this nodes may be reused among parses, so, we have to erase the specials
                    }
                    if(arr[j].specialsAfter != null){
                        specialsAfter.addAll(arr[j].specialsAfter);
                        arr[j].specialsAfter.clear();
                    }
                    k++;
                }else{
                    values[k] = (exprType) arr[j];
                    if(specialsBefore.size() > 0){
                        values[k].getSpecialsBefore().addAll(specialsBefore);
                        specialsBefore.clear();
                    }
                    if(specialsAfter.size() > 0){
                        values[k].getSpecialsBefore().addAll(specialsAfter);
                        specialsAfter.clear();
                    }
                }
            }
            SimpleNode sliceRet;
            if (k == 0) {
                sliceRet = new Index(values[0]);
            } else {
                sliceRet = new Slice(values[0], values[1], values[2]);
            }
            //this may happen if we have no values
            sliceRet.getSpecialsBefore().addAll(specialsBefore);
            sliceRet.getSpecialsAfter().addAll(specialsAfter);
            specialsBefore.clear();
            specialsAfter.clear();
            return sliceRet;
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
            return makeAugAssign(AugAssign.Add);
        case JJTAUG_MINUS:   
            return makeAugAssign(AugAssign.Sub);
        case JJTAUG_MULTIPLY:  
            return makeAugAssign(AugAssign.Mult);
        case JJTAUG_DIVIDE:   
            return makeAugAssign(AugAssign.Div);
        case JJTAUG_MODULO:  
            return makeAugAssign(AugAssign.Mod);
        case JJTAUG_AND:    
            return makeAugAssign(AugAssign.BitAnd);
        case JJTAUG_OR:    
            return makeAugAssign(AugAssign.BitOr);
        case JJTAUG_XOR:  
            return makeAugAssign(AugAssign.BitXor);
        case JJTAUG_LSHIFT:   
            return makeAugAssign(AugAssign.LShift);
        case JJTAUG_RSHIFT:  
            return makeAugAssign(AugAssign.RShift);
        case JJTAUG_POWER:  
            return makeAugAssign(AugAssign.Pow);
        case JJTAUG_FLOORDIVIDE:  
            return makeAugAssign(AugAssign.FloorDiv);
        case JJTLIST_FOR:
            ComprehensionCollection col = null;
            if(stack.peekNode() instanceof ComprehensionCollection){
                col = (ComprehensionCollection) stack.popNode();
                arity--;
            }else{
                col = new ComprehensionCollection();
            }
            
            ArrayList<exprType> ifs = new ArrayList<exprType>();
            for (int i = arity-3; i >= 0; i--) {
                SimpleNode ifsNode = stack.popNode();
                ifs.add((exprType) ifsNode);
            }
            iter = (exprType) stack.popNode();
            target = (exprType) stack.popNode();
            ctx.setStore(target);
            col.added.add(new Comprehension(target, iter, ifs.toArray(new exprType[0])));
            return col;
        case JJTIMPORTFROM:
            ArrayList<aliasType> aliastL = new ArrayList<aliasType>();
            while(arity > 0 && stack.peekNode() instanceof aliasType){
                aliastL.add(0, (aliasType) stack.popNode());
                arity--;
            }
            NameTok nT;
            if(arity > 0){
                nT = makeName(NameTok.ImportModule);
            }else{
                nT = new NameTok("", NameTok.ImportModule);
            }
            return new ImportFrom((NameTokType)nT, aliastL.toArray(new aliasType[0]), 0);
        case JJTIMPORT:
            return new Import(makeAliases(arity));
    
        case JJTDOTTED_NAME:
            name = new Name(null, Name.Load);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < arity; i++) {
                if (i > 0){
                    sb.insert(0, '.');
                }
                Name name0 = (Name) stack.popNode();
                sb.insert(0, name0.id);
                addSpecials(name0, name);
                //we have to set that, because if we later add things to the previous Name, we will now want it to be added to
                //the new name (comments will only appear later and may be added to the previous name -- so, we replace the previous
                //name specials list).
                name0.specialsBefore = name.getSpecialsBefore();
                name0.specialsAfter = name.getSpecialsAfter();
            }
            name.id = sb.toString();
            return name;

        case JJTDOTTED_AS_NAME:
            NameTok asname = null;
            if (arity > 1){
                asname = makeName(NameTok.ImportName);
            }
            return new aliasType(makeName(NameTok.ImportName), asname);

        case JJTIMPORT_AS_NAME:
            asname = null;
            if (arity > 1){
                asname = makeName(NameTok.ImportName);
            }
            return new aliasType(makeName(NameTok.ImportName), asname);
        case JJTCOMMA:
        case JJTCOLON:
            return n;
        default:
            System.out.println("Error at TreeBuilder: default not treated:"+n.getId());
            return null;
        }
    }

	private void setParentForFuncOrClass(stmtType[] body, SimpleNode classDef) {
		for(stmtType b:body){
			if(b instanceof ClassDef || b instanceof FunctionDef){
				b.parent = classDef;
			}
		}
	}

    private suiteType popSuiteAndSuiteType() {
        Suite s = (Suite) stack.popNode();
        suiteType orelseSuite = (suiteType) stack.popNode();
        orelseSuite.body = s.body;
        addSpecialsAndClearOriginal(s, orelseSuite);
        return orelseSuite;
    }

    private void addSpecialsAndClearOriginal(SimpleNode from, SimpleNode to) {
    	addSpecials(from, to);
        if(from.specialsBefore != null){
            from.specialsBefore.clear();
        }
        if(from.specialsAfter != null){
            from.specialsAfter.clear();
        }
	}

	private void addSpecials(SimpleNode from, SimpleNode to) {
        if(from.specialsBefore != null && from.specialsBefore.size() > 0){
            to.getSpecialsBefore().addAll(from.specialsBefore);
        }
        if(from.specialsAfter != null && from.specialsAfter.size() > 0){
            to.getSpecialsAfter().addAll(from.specialsAfter);
        }
    }
    
    private void addSpecialsBefore(SimpleNode from, SimpleNode to) {
        if(from.specialsBefore != null && from.specialsBefore.size() > 0){
            to.getSpecialsBefore().addAll(from.specialsBefore);
        }
        if(from.specialsAfter != null && from.specialsAfter.size() > 0){
            to.getSpecialsBefore().addAll(from.specialsAfter);
        }
    }

    /**
     * @param suite
     * @return
     */
    private stmtType[] getBodyAndSpecials() {
        Suite suite = (Suite)stack.popNode();
        stmtType[] body;
        body = suite.body;
        if(suite.specialsBefore != null && suite.specialsBefore.size() > 0){
            body[0].getSpecialsBefore().addAll(suite.specialsBefore);
        }
        
        if(suite.specialsAfter != null && suite.specialsAfter.size() > 0){
            body[body.length-1].getSpecialsAfter().addAll(suite.specialsAfter);
        }
        return body;
    }

    
    SimpleNode makeDecorator(java.util.List<SimpleNode> nodes){
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
                
            } else if(node instanceof keywordType){
                //keyword
                keywordsl.add(node);
                
            } else if(isArg(node)){
                //default
                argsl.add(node);

            } else if(node instanceof Comprehension){
                argsl.add( new ListComp((exprType)iter.next(), new comprehensionType[]{(comprehensionType) node}) );
                
            } else if(node instanceof ComprehensionCollection){
                //list comp (2 nodes: comp type and the elt -- what does elt mean by the way?) 
                argsl.add( new ListComp((exprType)iter.next(), ((ComprehensionCollection)node).getGenerators()));
                
            } else if(node instanceof decoratorsType){
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
    
    private stmtType makeAugAssign(int op) throws Exception {
        exprType value = (exprType) stack.popNode();
        exprType target = (exprType) stack.popNode();
        ctx.setAugStore(target);
        return new AugAssign(target, op, value);
    }


    BinOp makeBinOp(int op) {
        exprType right = (exprType) stack.popNode();
        exprType left = (exprType) stack.popNode();
        return new BinOp(left, op, right);
    }

    
    boolean isArg(SimpleNode n){
        if (n instanceof ExtraArg)
            return true;
        if (n instanceof DefaultArg)
            return true;
        if (n instanceof keywordType)
            return true;
        return false;
    }
    
    NameTok[] getVargAndKwarg(java.util.List<SimpleNode> args) throws Exception {
        NameTok varg = null;
        NameTok kwarg = null;
        for (Iterator<SimpleNode> iter = args.iterator(); iter.hasNext();) {
            SimpleNode node = iter.next();
            if(node.getId() == JJTEXTRAKEYWORDLIST){
                ExtraArg a = (ExtraArg)node;
                kwarg = a.tok;
                addSpecialsAndClearOriginal(a, kwarg);
                
            }else if(node.getId() == JJTEXTRAARGLIST){
                ExtraArg a = (ExtraArg)node;
                varg = a.tok;
                addSpecialsAndClearOriginal(a, varg);
            }
        }
        return new NameTok[]{varg, kwarg};
    }
    
    private argumentsType makeArguments(DefaultArg[] def, NameTok varg, NameTok kwarg) throws Exception {
        exprType fpargs[] = new exprType[def.length];
        exprType defaults[] = new exprType[def.length];
        int startofdefaults = 0;
        boolean defaultsSet = false;
        for(int i = 0 ; i< def.length; i++){
            DefaultArg node = def[i];
            exprType parameter = node.parameter;
            fpargs[i] = parameter;

            if(node.specialsBefore != null && node.specialsBefore.size() > 0){
                parameter.getSpecialsBefore().addAll(node.specialsBefore);
            }
            if(node.specialsAfter != null && node.specialsAfter.size() > 0){
                parameter.getSpecialsAfter().addAll(node.specialsAfter);
            }
            
            ctx.setParam(fpargs[i]);
            defaults[i] = node.value;
            if (node.value != null && defaultsSet == false){
                defaultsSet = true;
                startofdefaults = i;
            }
        }
        
        // System.out.println("start "+ startofdefaults + " " + l);
        exprType[] newdefs = new exprType[def.length - startofdefaults];
        System.arraycopy(defaults, startofdefaults, newdefs, 0, newdefs.length);
        return new argumentsType(fpargs, varg, kwarg, newdefs);

    }
    
    private argumentsType makeArguments(int l) throws Exception {
        NameTok kwarg = null;
        NameTok stararg = null;
        if (l > 0 && stack.peekNode().getId() == JJTEXTRAKEYWORDLIST) {
        	ExtraArg node = (ExtraArg) stack.popNode();
            kwarg = node.tok;
            l--;
            addSpecialsAndClearOriginal(node, kwarg);
        }
        if (l > 0 && stack.peekNode().getId() == JJTEXTRAARGLIST) {
        	ExtraArg node = (ExtraArg) stack.popNode();
            stararg = node.tok;
            l--;
            addSpecialsAndClearOriginal(node, stararg);
        }
        ArrayList<SimpleNode> list = new ArrayList<SimpleNode>();
        for (int i = l-1; i >= 0; i--) {
            SimpleNode popped = null;
            try{
                popped = stack.popNode();
                list.add((DefaultArg) popped);
            }catch(ClassCastException e){
                throw new ParseException("Internal error (ClassCastException):"+e.getMessage()+"\n"+popped, popped);
            }
        }
        Collections.reverse(list);//we get them in reverse order in the stack
        return makeArguments((DefaultArg[]) list.toArray(new DefaultArg[0]), stararg, kwarg);
    }
}



class ComprehensionCollection extends SimpleNode{
    public ArrayList<Comprehension> added = new ArrayList<Comprehension>();

    public comprehensionType[] getGenerators() {
        ArrayList<Comprehension> f = added;
        added = null;
        Collections.reverse(f);
        return f.toArray(new comprehensionType[0]);
    }
}

class Decorators extends SimpleNode {
    public decoratorsType[] exp;
    private int id;
    Decorators(decoratorsType[] exp, int id) {
        this.exp = exp;
        this.id = id;
    }
    public int getId() {
        return id;
    }
}


class DefaultArg extends SimpleNode {
    public exprType parameter;
    public exprType value;
    DefaultArg(exprType parameter, exprType value) {
        this.parameter = parameter;
        this.value = value;
    }
}

class ExtraArg extends SimpleNode {
    public int id;
    NameTok tok;
    ExtraArg(NameTok tok, int id) {
        this.tok = tok;
        this.id = id;
    }
    public int getId() {
        return id;
    }
}


class ExtraArgValue extends SimpleNode {
    public exprType value;
    public int id;
    ExtraArgValue(exprType value, int id) {
        this.value = value;
        this.id = id;
    }
    public int getId() {
        return id;
    }
}


class IdentityNode extends SimpleNode {
    public int id;
    public Object image;

    IdentityNode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setImage(Object image) {
        this.image = image;
    }

    public Object getImage() {
        return image;
    }

    public String toString() {
        return "IdNode[" + PythonGrammar25TreeConstants.jjtNodeName[id] + ", " +
                image + "]";
    }
}

class CtxVisitor extends Visitor {
    private int ctx;

    public CtxVisitor() { }

    public void setParam(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.Param;
        visit(node);
    }

    public void setStore(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.Store;
        visit(node);
    }

    public void setStore(SimpleNode[] nodes) throws Exception {
        for (int i = 0; i < nodes.length; i++) 
            setStore(nodes[i]);
    }

    public void setDelete(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.Del;
        visit(node);
    }

    public void setDelete(SimpleNode[] nodes) throws Exception {
        for (int i = 0; i < nodes.length; i++) 
            setDelete(nodes[i]);
    }

    public void setAugStore(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.AugStore;
        visit(node);
    }

    public Object visitName(Name node) throws Exception {
        node.ctx = ctx;
        return null;
    }

    public Object visitAttribute(Attribute node) throws Exception {
        node.ctx = ctx;
        return null;
    }

    public Object visitSubscript(Subscript node) throws Exception {
        node.ctx = ctx;
        return null;
    }

    public Object visitList(List node) throws Exception {
        if (ctx == expr_contextType.AugStore) {
            throw new ParseException(
                    "augmented assign to list not possible", node);
        }
        node.ctx = ctx;
        traverse(node);
        return null;
    }

    public Object visitTuple(Tuple node) throws Exception {
        if (ctx == expr_contextType.AugStore) {
            throw new ParseException(
                    "augmented assign to tuple not possible", node);
        }
        node.ctx = ctx;
        traverse(node);
        return null;
    }

    public Object visitCall(Call node) throws Exception {
        throw new ParseException("can't assign to function call", node);
    }

    public Object visitListComp(Call node) throws Exception {
        throw new ParseException("can't assign to list comprehension call",
                                 node);
    }

    public Object unhandled_node(SimpleNode node) throws Exception {
        throw new ParseException("can't assign to operator", node);
    }
}
