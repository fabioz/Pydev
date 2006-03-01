package org.python.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.python.parser.ast.Assert;
import org.python.parser.ast.Assign;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.AugAssign;
import org.python.parser.ast.BinOp;
import org.python.parser.ast.BoolOp;
import org.python.parser.ast.Break;
import org.python.parser.ast.Call;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.Compare;
import org.python.parser.ast.Comprehension;
import org.python.parser.ast.Continue;
import org.python.parser.ast.Delete;
import org.python.parser.ast.Dict;
import org.python.parser.ast.Ellipsis;
import org.python.parser.ast.Exec;
import org.python.parser.ast.Expr;
import org.python.parser.ast.Expression;
import org.python.parser.ast.ExtSlice;
import org.python.parser.ast.For;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Global;
import org.python.parser.ast.If;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.Index;
import org.python.parser.ast.Interactive;
import org.python.parser.ast.Lambda;
import org.python.parser.ast.List;
import org.python.parser.ast.ListComp;
import org.python.parser.ast.Module;
import org.python.parser.ast.Name;
import org.python.parser.ast.NameTok;
import org.python.parser.ast.Num;
import org.python.parser.ast.Pass;
import org.python.parser.ast.Print;
import org.python.parser.ast.Raise;
import org.python.parser.ast.Repr;
import org.python.parser.ast.Return;
import org.python.parser.ast.Slice;
import org.python.parser.ast.Str;
import org.python.parser.ast.StrJoin;
import org.python.parser.ast.Subscript;
import org.python.parser.ast.Suite;
import org.python.parser.ast.TryExcept;
import org.python.parser.ast.TryFinally;
import org.python.parser.ast.Tuple;
import org.python.parser.ast.UnaryOp;
import org.python.parser.ast.While;
import org.python.parser.ast.Yield;
import org.python.parser.ast.aliasType;
import org.python.parser.ast.argumentsType;
import org.python.parser.ast.comprehensionType;
import org.python.parser.ast.decoratorsType;
import org.python.parser.ast.excepthandlerType;
import org.python.parser.ast.exprType;
import org.python.parser.ast.expr_contextType;
import org.python.parser.ast.keywordType;
import org.python.parser.ast.sliceType;
import org.python.parser.ast.stmtType;
import org.python.parser.ast.suiteType;

public class TreeBuilder implements PythonGrammarTreeConstants {
    private JJTPythonGrammarState stack;
    CtxVisitor ctx;
    private SimpleNode lastPop;
    
    public TreeBuilder(JJTPythonGrammarState stack) {
        this.stack = stack;
        ctx = new CtxVisitor();
    }

    private stmtType[] makeStmts(int l) {
        stmtType[] stmts = new stmtType[l];
        for (int i = l-1; i >= 0; i--) {
            stmts[i] = (stmtType) stack.popNode();
        }
        return stmts;
    }

    private stmtType[] popSuite() {
        return ((Suite) popNode()).body;
    }

    private exprType[] makeExprs() {
        if (stack.nodeArity() > 0 && peekNode().getId() == JJTCOMMA)
            popNode();
        return makeExprs(stack.nodeArity());
    }

    private exprType[] makeExprs(int l) {
        exprType[] exprs = new exprType[l];
        for (int i = l-1; i >= 0; i--) {
            lastPop = (SimpleNode) stack.popNode();
            exprs[i] = (exprType) lastPop;
        }
        return exprs;
    }

    private exprType makeExpr() {
        return (exprType) stack.popNode();
    }

    private String makeIdentifier() {
        return ((Name) stack.popNode()).id;
    }

    private NameTok makeName(int ctx) {
        Name name = (Name) stack.popNode();
        NameTok n = new NameTok(name.id, ctx);
        n.beginColumn = name.beginColumn;
        n.beginLine = name.beginLine;
        addSpecials(name, n);
        name.specialsBefore = n.specialsBefore;
        name.specialsAfter = n.specialsAfter;
        return n;
    }
    
    private String[] makeIdentifiers() {
        int l = stack.nodeArity();
        String[] ids = new String[l];
        for (int i = l - 1; i >= 0; i--) {
            ids[i] = makeIdentifier();
        }
        return ids;
    }

    private aliasType[] makeAliases() {
        return makeAliases(stack.nodeArity());
    }

    private aliasType[] makeAliases(int l) {
        aliasType[] aliases = new aliasType[l];
        for (int i = l-1; i >= 0; i--) {
            aliases[i] = (aliasType) stack.popNode();
        }
        return aliases;
    }

    private static SimpleNode[] nodes =
        new SimpleNode[PythonGrammarTreeConstants.jjtNodeName.length];

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
            return new Expression(makeExpr());

        case JJTNAME:
            Name name = new Name(n.getImage().toString(), Name.Load);
            addSpecialsAndClearOriginal(n, name);
            return name;
        case JJTNUM:
            //throw new RuntimeException("how to handle this? -- fabio")
            return new Num(n.getImage());
        case JJTUNICODE:
        case JJTSTRING:
            Object[] image = (Object[]) n.getImage();
            return new Str((String)image[0], (Integer)image[3], (Boolean)image[1], (Boolean)image[2]);

        case JJTSUITE:
            stmtType[] stmts = new stmtType[arity];
            for (int i = arity-1; i >= 0; i--) {
                stmts[i] = (stmtType) popNode();
            }
            return new Suite(stmts);
        case JJTEXPR_STMT:
            value = makeExpr();
            if (arity > 1) {
                exprs = makeExprs(arity-1);
                ctx.setStore(exprs);
                return new Assign(exprs, value);
            } else {
                return new Expr(value);
            }
        case JJTINDEX_OP:
            sliceType slice = (sliceType) stack.popNode();
            value = makeExpr();
            return new Subscript(value, slice, Subscript.Load);
        case JJTDOT_OP:
            NameTok attr = makeName(NameTok.Attrib);
            value = makeExpr();
            return new Attribute(value, attr, Attribute.Load);
        case JJTBEGIN_DEL_STMT:
        	return new Delete(null);
        case JJTDEL_STMT:
            exprs = makeExprs(arity-1);
            ctx.setDelete(exprs);
            Delete d = (Delete) popNode();
            d.targets = exprs;
            return d;
        case JJTPRINT_STMT:
            boolean nl = true;
            if (stack.nodeArity() == 0){
            	Print p = new Print(null, null, true);
            	p.specialsBefore.add(0, "print ");
                return p;
            }
            
            if (peekNode().getId() == JJTCOMMA) {
                popNode();
                nl = false;
            }
            Print p = new Print(null, makeExprs(), nl);
        	p.specialsBefore.add(0, "print ");
            return p;
        case JJTPRINTEXT_STMT:
            nl = true;
            if (peekNode().getId() == JJTCOMMA) {
                popNode();
                nl = false;
            }
            exprs = makeExprs(stack.nodeArity()-1);
            p = new Print(makeExpr(), exprs, nl);
            p.specialsBefore.add(0, ">> ");
        	p.specialsBefore.add(0, "print ");
            return p;
        case JJTBEGIN_FOR_STMT:
            return new For(null,null,null,null);
        case JJTFOR_STMT:
            suiteType orelseSuite = null;
            if (stack.nodeArity() == 6){
                orelseSuite = popSuiteAndSuiteType();
            }
            
            stmtType[] body = popSuite();
            exprType iter = makeExpr();
            exprType target = makeExpr();
            ctx.setStore(target);
            
            For forStmt = (For) popNode();
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
            exprType test = makeExpr();
            While w = (While) popNode();
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
            Suite suite = (Suite)popNode();
            body = suite.body;
            test = makeExpr();
            
            //make the if
            If last = (If) popNode();
            last.test = test;
            last.body = body;
            last.orelse = orelse;
            addSpecialsAndClearOriginal(suite, last);
            
            for (int i = 0; i < (arity / 3)-1; i++) {
                //arity--;//because of the beg if stmt

                suite = (Suite)popNode();
                body = suite.body;
                test = makeExpr();
                stmtType[] newOrElse = new stmtType[] { last };
                last = (If) popNode();
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
            ArrayList list2 = new ArrayList();
            ArrayList listArgs = new ArrayList();
            while(stack.nodeArity() > 0){
                SimpleNode node = (SimpleNode) stack.popNode();
                while(!(node instanceof decoratorsType)){
                    if(node instanceof comprehensionType){
                        listArgs.add(node);
                        listArgs.add(stack.popNode()); //target
                        
                    }else{
                        listArgs.add(node);
                    }
                    node = (SimpleNode) stack.popNode();
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
            if (l > 0 && peekNode().getId() == JJTEXTRAKEYWORDVALUELIST) {
                ExtraArgValue nkwargs = (ExtraArgValue) popNode();
                kwargs = nkwargs.value;
                this.addSpecialsAndClearOriginal(nkwargs, kwargs);
                l--;
            }
            if (l > 0 && peekNode().getId() == JJTEXTRAARGVALUELIST) {
                ExtraArgValue nstarargs = (ExtraArgValue) popNode();
                starargs = nstarargs.value;
                this.addSpecialsAndClearOriginal(nstarargs, starargs);
                l--;
            }
            
            int nargs = l;

            SimpleNode[] tmparr = new SimpleNode[l]; 
            for (int i = l - 1; i >= 0; i--) {
                tmparr[i] = popNode();
                if (tmparr[i] instanceof keywordType) {
                    nargs = i;
                }
            }
            
            exprType[] args = new exprType[nargs];
            for (int i = 0; i < nargs; i++) {
                //what can happen is something like print sum(x for x in y), where we have already passed x in the args, and then get 'for x in y'
                if(tmparr[i] instanceof comprehensionType){
                    args = new exprType[]{
                        new ListComp(args[0], new comprehensionType[]{
                                (comprehensionType)tmparr[i]
                            })
                        };
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
            exprType func = makeExpr();
            Call c = new Call(func, args, keywords, starargs, kwargs);
            addSpecialsAndClearOriginal(n, c);
            return c;
        case JJTFUNCDEF:
            //get the decorators
            //and clear them for the next call (they always must be before a function def)
            Suite s = (Suite) popNode();
            body = s.body;
            
            argumentsType arguments = makeArguments(stack.nodeArity() - 2);
            NameTok nameTok = makeName(NameTok.FunctionName);
            Decorators decs = (Decorators) popNode() ;
            decoratorsType[] decsexp = decs.exp;
            FunctionDef funcDef = new FunctionDef(nameTok, arguments, body, decsexp);
            if(decs.exp.length == 0){
                addSpecialsBefore(decs, funcDef);
            }
            addSpecialsAndClearOriginal(s, funcDef);
            return funcDef;
        case JJTDEFAULTARG:
            value = (arity == 1) ? null : makeExpr();
            return new DefaultArg(makeExpr(), value);
        case JJTEXTRAARGLIST:
            return new ExtraArg(makeName(NameTok.VarArg), JJTEXTRAARGLIST);
        case JJTEXTRAKEYWORDLIST:
            return new ExtraArg(makeName(NameTok.KwArg), JJTEXTRAKEYWORDLIST);
/*
        case JJTFPLIST:
            fpdefType[] list = new fpdefType[arity];
            for (int i = arity-1; i >= 0; i--) {
                list[i] = popFpdef();
            }
            return new FpList(list);
*/
        case JJTCLASSDEF:
            body = popSuite();
            exprType[] bases = makeExprs(stack.nodeArity() - 1);
            nameTok = makeName(NameTok.ClassName);
            return new ClassDef(nameTok, bases, body);
        case JJTBEGIN_RETURN_STMT:
            return new Return(null);
        case JJTRETURN_STMT:
            value = arity == 2 ? makeExpr() : null;
            Return ret = (Return) popNode();
            ret.value = value;
            return ret;
        case JJTYIELD_STMT:
            return new Yield(makeExpr());
        case JJTRAISE_STMT:
            exprType tback = arity >= 3 ? makeExpr() : null;
            exprType inst = arity >= 2 ? makeExpr() : null;
            exprType type = arity >= 1 ? makeExpr() : null;
            return new Raise(type, inst, tback);
        case JJTGLOBAL_STMT:
            return new Global(makeIdentifiers());
        case JJTEXEC_STMT:
            exprType globals = arity >= 3 ? makeExpr() : null;
            exprType locals = arity >= 2 ? makeExpr() : null;
            value = makeExpr();
            return new Exec(value, locals, globals);
        case JJTASSERT_STMT:
            exprType msg = arity == 2 ? makeExpr() : null;
            test = makeExpr();
            return new Assert(test, msg);
        case JJTBEGIN_TRY_STMT:
            //we do that just to get the specials
            return new TryExcept(null, null, null);
        case JJTTRY_STMT:
            orelseSuite = null;
            if (peekNode() instanceof Suite) {
                arity--;
                arity--;
                
                orelseSuite = popSuiteAndSuiteType();
            }
            l = arity - 1;
            excepthandlerType[] handlers = new excepthandlerType[l];
            for (int i = l - 1; i >= 0; i--) {
                handlers[i] = (excepthandlerType) popNode();
            }
            s = (Suite)popNode();
            TryExcept tryExc = (TryExcept) popNode();
            tryExc.body = s.body;
            tryExc.handlers = handlers;
            tryExc.orelse = orelseSuite;
            addSpecials(s, tryExc);
            return tryExc;
        case JJTBEGIN_TRY_ELSE_STMT:
            //we do that just to get the specials
            return new suiteType(null);
        case JJTBEGIN_EXCEPT_CLAUSE:
        	return new excepthandlerType(null,null,null);
        case JJTEXCEPT_CLAUSE:
            s = (Suite) popNode();
            body = s.body;
            exprType excname = arity == 4 ? makeExpr() : null;
            if (excname != null){    
                ctx.setStore(excname);
            }
            type = arity >= 3 ? makeExpr() : null;
            excepthandlerType handler = (excepthandlerType) popNode(); 
        	handler.type = type;
        	handler.name = excname;
        	handler.body = body;
            addSpecials(s, handler);
            return handler;
        case JJTBEGIN_FINALLY_STMT:
            //we do that just to get the specials
            return new suiteType(null);
        case JJTTRYFINALLY_STMT:
            suiteType finalBody = popSuiteAndSuiteType();
            body = popSuite();
            //We have a try..except in the stack, but we will change it for a try..finally
            //This is because we recognize a try..except in the 'try:' token, but actually end up with a try..finally
            TryExcept tryExcept = (TryExcept) popNode();
            TryFinally tryFinally = new TryFinally(body, finalBody);
            addSpecialsAndClearOriginal(tryExcept, tryFinally);
            return tryFinally;
            
        case JJTOR_BOOLEAN:
            return new BoolOp(BoolOp.Or, makeExprs());
        case JJTAND_BOOLEAN:
            return new BoolOp(BoolOp.And, makeExprs());
        case JJTCOMPARISION:
            l = arity / 2;
            exprType[] comparators = new exprType[l];
            int[] ops = new int[l];
            for (int i = l-1; i >= 0; i--) {
                comparators[i] = makeExpr();
                SimpleNode op = (SimpleNode) stack.popNode();
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
            return new Compare(makeExpr(), ops, comparators);
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
            return new UnaryOp(UnaryOp.UAdd, makeExpr());
        case JJTNEG_1OP:
            return new UnaryOp(UnaryOp.USub, makeExpr());
        case JJTINVERT_1OP:
            return new UnaryOp(UnaryOp.Invert, makeExpr());
        case JJTNOT_1OP:
            return new UnaryOp(UnaryOp.Not, makeExpr());
        case JJTEXTRAKEYWORDVALUELIST:
            return new ExtraArgValue(makeExpr(), JJTEXTRAKEYWORDVALUELIST);
        case JJTEXTRAARGVALUELIST:
            return new ExtraArgValue(makeExpr(), JJTEXTRAARGVALUELIST);
        case JJTKEYWORD:
            value = makeExpr();
            nameTok = makeName(NameTok.KeywordName);
            return new keywordType(nameTok, value);
        case JJTTUPLE:
            if (stack.nodeArity() > 0 && peekNode() instanceof comprehensionType) {
                comprehensionType[] generators = new comprehensionType[arity-1];
                for (int i = arity-2; i >= 0; i--) {
                    generators[i] = (comprehensionType) popNode();
                }
                return new ListComp(makeExpr(), generators);
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
                throw new ParseException("Syntax error while detecting tuple.", lastPop);
            }
        case JJTLIST:
            if (stack.nodeArity() > 0 && peekNode() instanceof comprehensionType) {
                comprehensionType[] generators = new comprehensionType[arity-1];
                for (int i = arity-2; i >= 0; i--) {
                    generators[i] = (comprehensionType) popNode();
                }
                return new ListComp(makeExpr(), generators);
            }
            return new List(makeExprs(), List.Load);
        case JJTDICTIONARY:
            l = arity / 2;
            exprType[] keys = new exprType[l];
            exprType[] vals = new exprType[l];
            for (int i = l - 1; i >= 0; i--) {
                vals[i] = makeExpr();
                keys[i] = makeExpr();
            }
            return new Dict(keys, vals);
        case JJTSTR_1OP:
            return new Repr(makeExpr());
        case JJTSTRJOIN:
            Str str2 = (Str) popNode();
            Object o = popNode();
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
        case JJTLAMBDEF:
            test = makeExpr();
            arguments = makeArguments(arity - 1);
            Lambda lambda = new Lambda(arguments, test);
            if(arguments == null || arguments.args == null || arguments.args.length == 0){
                lambda.specialsBefore.add("lambda");
            }else{
                lambda.specialsBefore.add("lambda ");
            }
            return lambda;
        case JJTELLIPSES:
            return new Ellipsis();
        case JJTSLICE:
            SimpleNode[] arr = new SimpleNode[arity];
            for (int i = arity-1; i >= 0; i--) {
                arr[i] = popNode();
            }

            exprType[] values = new exprType[3];
            int k = 0;
            java.util.List<Object> specialsBefore = new ArrayList<Object>();
            java.util.List<Object> specialsAfter = new ArrayList<Object>();
            for (int j = 0; j < arity; j++) {
                if (arr[j].getId() == JJTCOLON){
                    specialsBefore.addAll(arr[j].specialsBefore);
                    specialsAfter.addAll(arr[j].specialsAfter);
                    arr[j].specialsBefore.clear(); //this nodes may be reused among parses, so, we have to erase the specials
                    arr[j].specialsAfter.clear();
                    k++;
                }else{
                    values[k] = (exprType) arr[j];
                    values[k].specialsBefore.addAll(specialsBefore);
                    values[k].specialsBefore.addAll(specialsAfter);
                    specialsBefore.clear();
                    specialsAfter.clear();
                }
            }
            SimpleNode sliceRet;
            if (k == 0) {
                sliceRet = new Index(values[0]);
            } else {
                sliceRet = new Slice(values[0], values[1], values[2]);
            }
            //this may happen if we have no values
            sliceRet.specialsBefore.addAll(specialsBefore);
            sliceRet.specialsAfter.addAll(specialsAfter);
            specialsBefore.clear();
            specialsAfter.clear();
            return sliceRet;
        case JJTSUBSCRIPTLIST:
            sliceType[] dims = new sliceType[arity];
            for (int i = arity - 1; i >= 0; i--) {
                SimpleNode sliceNode = popNode();
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
            exprType[] ifs = new exprType[arity-2];
            for (int i = arity-3; i >= 0; i--) {
                ifs[i] = makeExpr();
            }
            iter = makeExpr();
            target = makeExpr();
            ctx.setStore(target);
            return new Comprehension(target, iter, ifs);
        case JJTIMPORTFROM:
            aliasType[] aliases = makeAliases(arity - 1);
            return new ImportFrom(makeName(NameTok.ImportModule), aliases);
        case JJTIMPORT:
            return new Import(makeAliases());
    
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
                name0.specialsBefore = name.specialsBefore;
                name0.specialsAfter = name.specialsAfter;
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

    private suiteType popSuiteAndSuiteType() {
        Suite s = (Suite) popNode();
        suiteType orelseSuite = (suiteType) popNode();
        orelseSuite.body = s.body;
        addSpecialsAndClearOriginal(s, orelseSuite);
        return orelseSuite;
    }

    private void addSpecialsAndClearOriginal(SimpleNode from, SimpleNode to) {
    	addSpecials(from, to);
    	from.specialsBefore.clear();
    	from.specialsAfter.clear();
	}

	private void addSpecials(SimpleNode from, SimpleNode to) {
        to.specialsBefore.addAll(from.specialsBefore);
        to.specialsAfter.addAll(from.specialsAfter);
    }
    
    private void addSpecialsBeforeToAfter(SimpleNode from, SimpleNode to) {
        to.specialsAfter.addAll(from.specialsBefore);
        from.specialsBefore.clear();
    }
    private void addSpecialsBefore(SimpleNode from, SimpleNode to) {
        to.specialsBefore.addAll(from.specialsBefore);
        to.specialsBefore.addAll(from.specialsAfter);
    }

    /**
     * @param suite
     * @return
     */
    private stmtType[] getBodyAndSpecials() {
        Suite suite = (Suite)popNode();
        stmtType[] body;
        body = suite.body;
        body[0].specialsBefore.addAll(suite.specialsBefore);
        body[body.length-1].specialsAfter.addAll(suite.specialsAfter);
        return body;
    }

    
    SimpleNode makeDecorator(java.util.List nodes){
        exprType starargs = null;
        exprType kwargs = null;

        exprType func = null;
        ArrayList keywordsl = new ArrayList();
        ArrayList argsl = new ArrayList();
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            SimpleNode node = (SimpleNode) iter.next();
            
        
			if (node.getId() == JJTEXTRAKEYWORDVALUELIST) {
				final ExtraArgValue extraArg = (ExtraArgValue) node;
                kwargs = (extraArg).value;
                this.addSpecialsAndClearOriginal(extraArg, kwargs);
                extraArg.specialsBefore = kwargs.specialsBefore;
                extraArg.specialsAfter = kwargs.specialsAfter;
                
            } else if (node.getId() == JJTEXTRAARGVALUELIST) {
            	final ExtraArgValue extraArg = (ExtraArgValue) node;
                starargs = extraArg.value;
                this.addSpecialsAndClearOriginal(extraArg, starargs);
                extraArg.specialsBefore = starargs.specialsBefore;
                extraArg.specialsAfter = starargs.specialsAfter;
                
            } else if(node instanceof keywordType){
                //keyword
                keywordsl.add(node);
                
            } else if(isArg(node)){
                //default
                argsl.add(node);

            } else if(node instanceof comprehensionType){
                //list comp (2 nodes: comp type and the elt -- what does elt mean by the way?) 
                argsl.add( new ListComp((exprType)iter.next(), new comprehensionType[]{(comprehensionType)node}));
                
            } else if(node instanceof decoratorsType){
                func = (exprType) popNode();//the func is the last thing in the stack
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
        exprType value = makeExpr();
        exprType target = makeExpr();
        ctx.setAugStore(target);
        return new AugAssign(target, op, value);
    }

    private void dumpStack() {
        int n = stack.nodeArity();
        System.out.println("nodeArity:" + n);
        if (n > 0) {
            System.out.println("peek:" + stack.peekNode());
        }
    }

    SimpleNode peekNode() {
        return (SimpleNode) stack.peekNode();
    }

    SimpleNode popNode() {
        return (SimpleNode) stack.popNode();
    }

    BinOp makeBinOp(int op) {
        exprType right = makeExpr();
        exprType left = makeExpr();
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
    
    NameTok[] getVargAndKwarg(java.util.List args) throws Exception {
        NameTok varg = null;
        NameTok kwarg = null;
        for (Iterator iter = args.iterator(); iter.hasNext();) {
            SimpleNode node = (SimpleNode) iter.next();
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

            parameter.specialsBefore.addAll(node.specialsBefore);
            parameter.specialsAfter.addAll(node.specialsAfter);
            
            ctx.setStore(fpargs[i]);
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
        if (l > 0 && peekNode().getId() == JJTEXTRAKEYWORDLIST) {
        	ExtraArg node = (ExtraArg) popNode();
            kwarg = node.tok;
            l--;
            addSpecialsAndClearOriginal(node, kwarg);
        }
        if (l > 0 && peekNode().getId() == JJTEXTRAARGLIST) {
        	ExtraArg node = (ExtraArg) popNode();
            stararg = node.tok;
            l--;
            addSpecialsAndClearOriginal(node, stararg);
        }
        ArrayList list = new ArrayList();
        for (int i = l-1; i >= 0; i--) {
            list.add((DefaultArg) popNode());
        }
        Collections.reverse(list);//we get them in reverse order in the stack
        return makeArguments((DefaultArg[]) list.toArray(new DefaultArg[0]), stararg, kwarg);
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
        return "IdNode[" + PythonGrammarTreeConstants.jjtNodeName[id] + ", " +
                image + "]";
    }
}

class CtxVisitor extends Visitor {
    private int ctx;

    public CtxVisitor() { }

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
