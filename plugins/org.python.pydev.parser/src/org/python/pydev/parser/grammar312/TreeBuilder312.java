package org.python.pydev.parser.grammar312;

import java.util.ArrayList;
import java.util.Collections;

import org.python.pydev.core.log.Log;
import org.python.pydev.parser.grammarcommon.AbstractTreeBuilder;
import org.python.pydev.parser.grammarcommon.ComprehensionCollection;
import org.python.pydev.parser.grammarcommon.Decorators;
import org.python.pydev.parser.grammarcommon.DefaultArg;
import org.python.pydev.parser.grammarcommon.ExtraArg;
import org.python.pydev.parser.grammarcommon.ExtraArgValue;
import org.python.pydev.parser.grammarcommon.FuncDefReturnAnn;
import org.python.pydev.parser.grammarcommon.ITreeBuilder;
import org.python.pydev.parser.grammarcommon.ITreeConstants;
import org.python.pydev.parser.grammarcommon.JJTPythonGrammarState;
import org.python.pydev.parser.grammarcommon.JfpDef;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assert;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Await;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Ellipsis;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Global;
import org.python.pydev.parser.jython.ast.IfExp;
import org.python.pydev.parser.jython.ast.Index;
import org.python.pydev.parser.jython.ast.Lambda;
import org.python.pydev.parser.jython.ast.List;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Match;
import org.python.pydev.parser.jython.ast.MatchAs;
import org.python.pydev.parser.jython.ast.MatchClass;
import org.python.pydev.parser.jython.ast.MatchKeyVal;
import org.python.pydev.parser.jython.ast.MatchMapping;
import org.python.pydev.parser.jython.ast.MatchOr;
import org.python.pydev.parser.jython.ast.MatchSequence;
import org.python.pydev.parser.jython.ast.MatchValue;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NamedExpr;
import org.python.pydev.parser.jython.ast.NonLocal;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Raise;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.Set;
import org.python.pydev.parser.jython.ast.Slice;
import org.python.pydev.parser.jython.ast.Starred;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.enclosingType;
import org.python.pydev.parser.jython.ast.excepthandlerType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.keywordType;
import org.python.pydev.parser.jython.ast.match_caseType;
import org.python.pydev.parser.jython.ast.patternType;
import org.python.pydev.parser.jython.ast.sliceType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.suiteType;

public final class TreeBuilder312 extends AbstractTreeBuilder implements ITreeBuilder, ITreeConstants {

    public TreeBuilder312(JJTPythonGrammarState stack) {
        super(stack);
        this.ctx = new org.python.pydev.parser.grammarcommon.CtxVisitor30(stack);
    }

    @Override
    public final SimpleNode onCloseNode(SimpleNode n, int arity) throws Exception {
        exprType value;
        exprType[] exprs;
        Suite orelseSuite;
        stmtType[] body;
        Suite suite;
        int l;
        exprType awaitExpr;
        exprType target;
        NameTok nameTok;
        argumentsType arguments;

        switch (n.getId()) {
            case JJTEXPR_STMT:
                value = (exprType) stack.popNode();
                if (arity > 1) {
                    exprs = makeExprs(arity - 1);
                    ctx.setStore(exprs);
                    return new Assign(exprs, value, null);
                } else {
                    return new Expr(value);
                }
            case JJTINDEX_OP:
                sliceType slice = (sliceType) stack.popNode();
                value = (exprType) stack.popNode();
                return new Subscript(value, slice, Subscript.Load);

            case JJTBEGIN_FOR_ELSE_STMT:
                return new Suite(null);
            case JJTBEGIN_ELSE_STMT:
                return new Suite(null);
            case JJTBEGIN_WHILE_STMT:
                return new While(null, null, null);
            case JJTWHILE_STMT:
                orelseSuite = null;
                if (stack.nodeArity() == 5) {
                    orelseSuite = popSuiteAndSuiteType();
                }

                body = popSuite();
                exprType test = (exprType) stack.popNode();
                While w = (While) stack.popNode();
                w.test = test;
                w.body = body;
                w.orelse = orelseSuite;
                return w;
            case JJTCALL_OP:
                exprType starargs = null;
                exprType kwargs = null;

                java.util.List<exprType> args = new ArrayList<exprType>();
                java.util.List<keywordType> keywords = new ArrayList<keywordType>();

                for (int i = arity - 2; i >= 0; i--) {
                    SimpleNode node = stack.popNode();
                    if (node instanceof keywordType) {
                        keywordType keyword = (keywordType) node;
                        keywords.add(0, keyword);
                        if (starargs == null) {
                            keyword.afterstarargs = true; //note that we get things backward in the stack
                        }

                    } else if (node.getId() == JJTEXTRAARGVALUELIST) {
                        ExtraArgValue nstarargs = (ExtraArgValue) node;
                        starargs = nstarargs.value;
                        this.addSpecialsAndClearOriginal(nstarargs, starargs);

                    } else if (node.getId() == JJTEXTRAKEYWORDVALUELIST) {
                        ExtraArgValue nkwargs = (ExtraArgValue) node;
                        kwargs = nkwargs.value;
                        this.addSpecialsAndClearOriginal(nkwargs, kwargs);

                    } else if (node instanceof ComprehensionCollection) {
                        //what can happen is something like print sum(x for x in y), where we have already passed x in the args, and then get 'for x in y'
                        args.add(
                                0,
                                new ListComp((exprType) stack.popNode(), ((ComprehensionCollection) node)
                                        .getGenerators(), ListComp.EmptyCtx));
                        i--; //popped node

                    } else {
                        args.add(0, (exprType) node);
                    }
                }

                exprType func = (exprType) stack.popNode();
                Call c = new Call(func, args.toArray(new exprType[args.size()]),
                        keywords.toArray(new keywordType[keywords.size()]), starargs, kwargs);
                addSpecialsAndClearOriginal(n, c);
                return c;
            case JJTFUNCDEF_RETURN_ANNOTTATION:
                SimpleNode funcdefReturn = stack.popNode();
                return new FuncDefReturnAnn(funcdefReturn);
            case JJTFUNCDEF:
                return closeFuncDef(arity, n);
            case JJTTFPDEF:
                Name tfpdefName = null;
                exprType typeDef = null;
                if (arity == 1) {
                    tfpdefName = (Name) stack.popNode();
                } else if (arity == 2) {
                    typeDef = (exprType) stack.popNode();
                    tfpdefName = (Name) stack.popNode();
                } else {
                    throw new RuntimeException("Unexpected arity: " + arity);
                }

                return new JfpDef(tfpdefName, typeDef);
            case JJTONLYKEYWORDARG2:
            case JJTDEFAULTARG2:
                DefaultArg defaultArg;
                JfpDef jfpDef;
                if (arity == 1) {
                    jfpDef = (JfpDef) stack.popNode();
                    defaultArg = new DefaultArg(jfpDef.nameNode, null, jfpDef.typeDef, n.getId());
                } else if (arity == 2) {
                    exprType defaultValue = (exprType) stack.popNode();
                    jfpDef = (JfpDef) stack.popNode();
                    defaultArg = new DefaultArg(jfpDef.nameNode, defaultValue, jfpDef.typeDef, n.getId());
                } else {
                    throw new RuntimeException("Unexpected arity: " + arity);
                }
                return defaultArg;
            case JJTONLYKEYWORDARG:
            case JJTDEFAULTARG:
                //no type definition in this case
                if (arity == 1) {
                    return new DefaultArg(((exprType) stack.popNode()), null, null, n.getId());
                }
                exprType parameter = (exprType) stack.popNode();
                return new DefaultArg((exprType) stack.popNode(), parameter, null, n.getId());
            case JJTEXTRAARGLIST:
                if (arity == 0) {
                    //nothing here (just '*')
                    return new ExtraArg(null, JJTEXTRAARGLIST, null);
                }
                return new ExtraArg(makeNameTok(NameTok.VarArg), JJTEXTRAARGLIST);
            case JJTEXTRAKEYWORDLIST:
                return new ExtraArg(makeNameTok(NameTok.KwArg), JJTEXTRAKEYWORDLIST);
            case JJTEXTRAARGLIST2: //with type declaration
                if (arity == 0) {
                    //nothing here (just '*')
                    return new ExtraArg(null, JJTEXTRAARGLIST, null);
                }
                jfpDef = (JfpDef) stack.popNode();
                NameTok jfpDefName = makeNameTok(NameTok.VarArg, jfpDef.nameNode);
                ExtraArg extra = new ExtraArg(jfpDefName, JJTEXTRAARGLIST, jfpDef.typeDef);
                return extra;
            case JJTEXTRAKEYWORDLIST2: //with type declaration
                jfpDef = (JfpDef) stack.popNode();
                return new ExtraArg(makeNameTok(NameTok.KwArg, jfpDef.nameNode), JJTEXTRAKEYWORDLIST, jfpDef.typeDef);
            case JJTDECORATED:
                if (stack.nodeArity() != 2) {
                    throw new RuntimeException("Expected 2 nodes at this context, found: " + arity);
                }
                SimpleNode def = stack.popNode();
                Decorators decorators = (Decorators) stack.popNode();
                if (def instanceof ClassDef) {
                    ClassDef classDef = (ClassDef) def;
                    classDef.decs = decorators.exp;
                } else {
                    FunctionDef fDef = (FunctionDef) def;
                    fDef.decs = decorators.exp;
                }
                return def;
            case JJTCLASSDEF:
                suite = (Suite) stack.popNode();
                body = suite.body;
                int nodeArity = stack.nodeArity() - 1;
                ArrayList<keywordType> classDefKeywords = new ArrayList<keywordType>();
                starargs = null;
                kwargs = null;

                int loopTo = nodeArity;
                for (int i = 0; i < loopTo; i++) {
                    SimpleNode node = stack.peekNode();
                    if (node instanceof keywordType) {
                        stack.popNode();
                        keywordType keyword = (keywordType) node;
                        classDefKeywords.add(keyword);
                        if (starargs == null) {
                            keyword.afterstarargs = true; //note that we get things backward in the stack
                        }
                        nodeArity--;
                    } else if (node instanceof ExtraArgValue) {
                        if (node.getId() == JJTEXTRAARGVALUELIST) {
                            ExtraArgValue nstarargs = (ExtraArgValue) stack.popNode();
                            starargs = nstarargs.value;
                            this.addSpecialsAndClearOriginal(nstarargs, starargs);
                            nodeArity--;
                        } else if (node.getId() == JJTEXTRAKEYWORDVALUELIST) {
                            ExtraArgValue nkwargs = (ExtraArgValue) stack.popNode();
                            kwargs = nkwargs.value;
                            this.addSpecialsAndClearOriginal(nkwargs, kwargs);
                            nodeArity--;
                        }
                    } else {
                        break;
                    }
                }
                if (classDefKeywords.size() > 1) {
                    Collections.reverse(classDefKeywords);
                }

                exprType[] bases = makeExprs(nodeArity);
                nameTok = makeNameTok(NameTok.ClassName);
                //decorator is always null at this point... it's decorated later on
                ClassDef classDef = new ClassDef(nameTok, bases, body, null,
                        classDefKeywords.toArray(new keywordType[classDefKeywords.size()]), starargs, kwargs);

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
                if (arity > 0) {
                    //we may have an empty yield, so, we have to check it before
                    yieldExpr = (exprType) stack.popNode();
                }
                return new Yield(yieldExpr, false);
            case JJTRAISE_STMT:
                exprType from = arity >= 2 ? ((exprType) stack.popNode()) : null;
                exprType type = arity >= 1 ? ((exprType) stack.popNode()) : null;
                return new Raise(type, null, null, from);
            case JJTGLOBAL_STMT:
                return new Global(makeIdentifiers(NameTok.GlobalName), null);
            case JJTNONLOCAL_STMT:
                return new NonLocal(makeIdentifiers(NameTok.NonLocalName), null);
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
                if (stack.peekNode() instanceof TryFinally) {
                    outer = (TryFinally) stack.popNode();
                    arity--;
                }
                orelseSuite = null;
                if (stack.peekNode() instanceof suiteType) {
                    orelseSuite = (Suite) stack.popNode();
                    arity--;
                }

                l = arity;
                excepthandlerType[] handlers = new excepthandlerType[l];
                for (int i = l - 1; i >= 0; i--) {
                    handlers[i] = (excepthandlerType) stack.popNode();
                }
                suite = (Suite) stack.popNode();
                TryExcept tryExc = (TryExcept) stack.popNode();
                if (outer != null) {
                    outer.beginLine = tryExc.beginLine;
                }
                tryExc.body = suite.body;
                tryExc.handlers = handlers;
                tryExc.orelse = orelseSuite;
                addSpecials(suite, tryExc);
                if (outer == null) {
                    return tryExc;
                } else {
                    if (outer.body != null) {
                        throw new RuntimeException("Error. Expecting null body to be filled on try..except..finally");
                    }
                    outer.body = new stmtType[] { tryExc };
                    return outer;
                }
            case JJTBEGIN_TRY_ELSE_STMT:
                //we do that just to get the specials
                return new Suite(null);
            case JJTBEGIN_EXCEPT_CLAUSE:
                return new excepthandlerType(null, null, null, false);
            case JJTBEGIN_EXCEPT_MULTIPLY_CLAUSE:
                return new excepthandlerType(null, null, null, true);
            case JJTEXCEPT_CLAUSE:
                suite = (Suite) stack.popNode();
                body = suite.body;
                exprType excname = arity == 4 ? ((exprType) stack.popNode()) : null;
                if (excname != null) {
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
                return new Suite(null);
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

            case JJTNAMEDEXPR_TEST:
                if (arity == 1) {
                    return stack.popNode();
                }
                value = (exprType) stack.popNode();
                target = (exprType) stack.popNode();
                ctx.setNamedStore(target);
                NamedExpr namedExpr = (NamedExpr) n;
                namedExpr.value = value;
                namedExpr.target = target;
                return n;

            case JJTWITH_STMT:
                return makeWithStmt(arity);
            case JJTWITH_ITEM:
                return makeWithItem(arity);
            case JJTEXTRAKEYWORDVALUELIST:
                return new ExtraArgValue(((exprType) stack.popNode()), JJTEXTRAKEYWORDVALUELIST);
            case JJTEXTRAARGVALUELIST:
                return new ExtraArgValue(((exprType) stack.popNode()), JJTEXTRAARGVALUELIST);
            case JJTARGUMENT:
                SimpleNode keyword = stack.popNode();
                if (keyword instanceof keywordType) {
                    nameTok = makeNameTok(NameTok.KeywordName);
                    ((keywordType) keyword).arg = nameTok;
                }
                return keyword;
            case JJTKEYWORD:
                value = (exprType) stack.popNode();
                return new keywordType(null, value, false);
            case JJTTUPLE:
                if (stack.nodeArity() > 0) {
                    SimpleNode peeked = stack.peekNode();
                    if (peeked instanceof ComprehensionCollection) {
                        ComprehensionCollection col = (ComprehensionCollection) stack.popNode();
                        return new ListComp(((exprType) stack.popNode()), col.getGenerators(), ListComp.TupleCtx);
                    }
                }
                return makeTuple(n);
            case JJTLIST:
                if (stack.nodeArity() > 0 && stack.peekNode() instanceof ComprehensionCollection) {
                    ComprehensionCollection col = (ComprehensionCollection) stack.popNode();
                    return new ListComp(((exprType) stack.popNode()), col.getGenerators(), ListComp.ListCtx);
                }
                return new List(makeExprs(), List.Load);
            case JJTSET:
                return new Set(null);
            case JJTDICTIONARY:
                return makeDictionaryOrSet(arity);
            //        case JJTSTR_1OP: #No more backticks in python 3.0
            //            return new Repr(((exprType) stack.popNode()));
            case JJTTEST:
                if (arity == 2) {
                    IfExp node = (IfExp) stack.popNode();
                    node.body = (exprType) stack.popNode();
                    return node;
                } else {
                    return stack.popNode();
                }
            case JJTIF_EXP:
                exprType ifExprOrelse = (exprType) stack.popNode();
                exprType ifExprTest = (exprType) stack.popNode();
                return new IfExp(ifExprTest, null, ifExprOrelse);
            case JJTLAMBDEF_NOCOND:
            case JJTLAMBDEF:
                test = (exprType) stack.popNode();
                arguments = makeArguments(arity - 1);
                Lambda lambda = new Lambda(arguments, test);
                //            if(arguments == null || arguments.args == null || arguments.args.length == 0){
                //                lambda.getSpecialsBefore().add("lambda");
                //            }else{
                //                lambda.getSpecialsBefore().add("lambda ");
                //            }
                return lambda;
            case JJTELLIPSIS:
                return new Ellipsis();

            case JJTELLIPSIS_AS_NAME:
                return new Name("...", Name.Load, true);

            case JJTSLICE:
                SimpleNode[] arr = new SimpleNode[arity];
                for (int i = arity - 1; i >= 0; i--) {
                    arr[i] = stack.popNode();
                }

                exprType[] values = new exprType[3];
                int k = 0;
                java.util.List<Object> specialsBefore = new ArrayList<Object>();
                java.util.List<Object> specialsAfter = new ArrayList<Object>();
                for (int j = 0; j < arity; j++) {
                    if (arr[j].getId() == JJTCOLON) {
                        if (arr[j].specialsBefore != null) {
                            specialsBefore.addAll(arr[j].specialsBefore);
                            arr[j].specialsBefore.clear(); //this nodes may be reused among parses, so, we have to erase the specials
                        }
                        if (arr[j].specialsAfter != null) {
                            specialsAfter.addAll(arr[j].specialsAfter);
                            arr[j].specialsAfter.clear();
                        }
                        k++;
                    } else {
                        values[k] = (exprType) arr[j];
                        if (specialsBefore.size() > 0) {
                            values[k].getSpecialsBefore().addAll(specialsBefore);
                            specialsBefore.clear();
                        }
                        if (specialsAfter.size() > 0) {
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
            case JJTCOMP_FOR:
                return makeCompFor(arity);

            case JJTIMPORTFROM:
                return makeImportFrom25Onwards(arity);

            case JJTAWAIT_ATOM_EXPR:
                awaitExpr = (exprType) stack.popNode();
                return new Await(awaitExpr);

            case JJTSTAR_EXPR:
                Starred starred = (Starred) n;
                starred.value = (exprType) this.stack.popNode();
                ctx.setCtx(starred, starred.ctx);
                return starred;

            case JJTANN_ASSIGN:
                return typedDeclaration(arity, stack, ctx);

            case JJTEVAL_INPUT:
                Expr expr = (Expr) n;

                if (arity != 1) {
                    Log.log("Expected arity to be == 1 here.");
                }
                for (int i = arity - 1; i >= 0; i--) {
                    SimpleNode popNode = stack.popNode();
                    try {
                        exprType node = (exprType) popNode;
                        expr = new Expr(node);
                    } catch (Exception e) {
                        Log.log("Expected expr. Found: " + popNode);
                    }
                }
                return expr;

            case JJTMATCH_STMT:
                if (arity > 1) {
                    match_caseType[] cases = new match_caseType[arity - 1];
                    for (int i = arity - 2; i >= 0; i--) {
                        cases[i] = (match_caseType) securePop(match_caseType.class);
                    }
                    exprType subject = (exprType) securePop(exprType.class);
                    return new Match(subject, cases);
                }
                addAndReportException(Match.class.getName());
                return new Match(getDefaultInvalidExpr(), new match_caseType[] { getDefaultInvalidMatchCase() });

            case JJTSUBJECT_EXPR:
                popSurplus(arity, 2);
                if (arity == 1) {
                    return stack.popNode();
                } else if (arity == 2) {
                    exprType[] elts = new exprType[arity];
                    for (int i = 0; i < arity; i++) {
                        exprType secureExpr = (exprType) securePop(exprType.class);
                        elts[i] = new Starred(secureExpr, Starred.Load);
                    }
                    return new Tuple(elts, Starred.Load, false);
                }
                addAndReportException(exprType.class.getName());
                return getDefaultInvalidExpr();

            case JJTCASE_BLOCK:
                if (arity > 1) {
                    suite = (Suite) securePop(Suite.class);
                    exprType guard = null;
                    if (arity > 2) {
                        guard = (exprType) securePopNullable(exprType.class);
                    }
                    patternType pattern = (patternType) securePop(patternType.class);
                    return new match_caseType(pattern, guard, suite.body);
                }
                addAndReportException(match_caseType.class.getName());
                return getDefaultInvalidMatchCase();

            case JJTPATTERN:
                popSurplus(arity, 2);
                if (arity == 1) {
                    SimpleNode popNode = stack.popNode();
                    if (popNode instanceof exprType) {
                        exprType exprNode = (exprType) popNode;
                        if (exprNode instanceof Name) {
                            Name name = (Name) exprNode;
                            name.ctx = Name.Store;
                        }
                        return new MatchValue((exprType) popNode);
                    }
                    return popNode;
                } else if (arity == 2) {
                    Name asname = (Name) securePop(Name.class);
                    asname.ctx = Name.Store;
                    patternType pattern = (patternType) securePop(patternType.class);
                    return new MatchAs(pattern, asname);
                }
                addAndReportException(patternType.class.getName());
                return getDefaultInvalidPattern();

            case JJTLITERAL_PATTERN:
                exprType exp = (exprType) securePop(exprType.class);
                if (exp instanceof Name) {
                    Name name = (Name) exp;
                    name.ctx = Name.Store;
                }
                return new MatchValue(exp);

            case JJTCLOSED_PATTERN:
                popSurplus(arity, 2);
                if (arity == 1) {
                    return securePop(patternType.class);
                } else if (arity == 2) {
                    MatchClass classPattern = (MatchClass) securePop(MatchClass.class);
                    SimpleNode cls = stack.popNode();
                    if (cls instanceof exprType) {
                        classPattern.cls = (exprType) cls;
                        return classPattern;
                    } else if (cls instanceof MatchValue) {
                        MatchValue matchValue = (MatchValue) cls;
                        ctx.setLoad(matchValue.value);
                        classPattern.cls = matchValue.value;
                        return classPattern;
                    }
                }
                addAndReportException(exprType.class.getName());
                return getDefaultInvalidExpr();

            case JJTKEY_VALUE_PATTERN:
                popSurplus(arity, 2);
                if (arity == 1) {
                    // This means we got into the double_star_pattern()
                    // and we just have a Name there. This is the '**rest'
                    Name doubleStarName = (Name) securePop(Name.class);
                    doubleStarName.ctx = Name.Store;
                    return doubleStarName;
                }

                if (arity == 2) {
                    patternType pattern = (patternType) securePop(patternType.class);
                    MatchValue arg = (MatchValue) securePop(MatchValue.class);

                    // We want the expr in the MatchValue. As we're removing the MatchValue itself
                    // we need to copy what it had before.
                    this.addSpecialsAndClearOriginal(arg, arg.value);

                    MatchKeyVal matchKeyword = new MatchKeyVal(arg.value, pattern);
                    return matchKeyword;
                }
                addAndReportException(exprType.class.getName());
                return getDefaultInvalidExpr();

            case JJTMAPPING_PATTERN:
                if (arity > 0) {
                    java.util.List<patternType> lst = new ArrayList<>();
                    Name rest = null;
                    for (int i = arity - 1; i >= 0; i--) {
                        SimpleNode found = stack.popNode();
                        if (found instanceof MatchKeyVal) {
                            lst.add((MatchKeyVal) found);
                        } else {
                            if (found instanceof Name) {
                                rest = (Name) found;
                            }
                        }
                    }
                    Collections.reverse(lst);
                    return new MatchMapping(lst.toArray(new patternType[0]), rest);
                }
                addAndReportException(MatchKeyVal.class.getName());
                return new MatchMapping(new patternType[0], null);

            case JJTLIST_PATTERN:
                return createMatchSequence(arity, enclosingType.LIST);

            case JJTTUPLE_PATTERN:
                return createMatchSequence(arity, enclosingType.TUPLE);

            case JJTATTR:
                if (arity == 1) {
                    if (isPeekedNodeWildcardPattern()) {
                        return new MatchValue(makeName(Name.Artificial));
                    }
                    return new MatchValue(makeName(Name.Store));
                } else if (arity >= 2) {
                    SimpleNode peekedNode = stack.peekNode();
                    if (peekedNode instanceof patternType) {
                        popSurplus(arity, 2);
                        patternType pattern = (patternType) securePop(patternType.class);
                        Name arg = makeName(Name.Artificial);
                        return new MatchKeyVal(arg, pattern);
                    } else {
                        Attribute attr = popMatchAttributeInMatch(arity);
                        return new MatchValue(attr);
                    }
                }
                addAndReportException(Attribute.class.getName());
                return getDefaultInvalidExpr();

            case JJTCLASS_PATTERN:
                if (arity > 0) {
                    patternType[] patterns = new patternType[arity];
                    for (int i = arity - 1; i >= 0; i--) {
                        patterns[i] = (patternType) securePop(patternType.class);
                    }
                    return new MatchClass(null, patterns);
                }
                // This is expected when the class_pattern has no given args.
                return new MatchClass(null, null);

            case JJTOR_PATTERN:
                if (arity == 1) {
                    return stack.popNode();
                } else if (arity > 1) {
                    patternType[] patterns = new patternType[arity];
                    for (int i = arity - 1; i >= 0; i--) {
                        SimpleNode popNode = stack.popNode();
                        if (popNode instanceof patternType) {
                            patterns[i] = (patternType) popNode;
                        } else if (popNode instanceof exprType) {
                            patterns[i] = new MatchValue((exprType) popNode);
                        } else {
                            patterns[i] = getDefaultInvalidPattern();
                        }
                    }
                    return new MatchOr(patterns);
                }
                addAndReportException(patternType.class.getName());
                return new MatchOr(new patternType[0]);

            case JJTSTAR_PATTERN:
                popSurplus(arity, 1);
                if (arity == 1) {
                    Name name = makeName(Name.Store);
                    return new Starred(name, name.ctx);
                }
                addAndReportException(SimpleNode.class.getName());
                return new Starred(getDefaultInvalidExpr(), 0);

            case JJTOPEN_SEQUENCE_PATTERN:
                if (arity == 1) {
                    return stack.popNode();
                } else if (arity > 1) {
                    patternType[] patterns = new patternType[arity];
                    for (int i = arity - 1; i >= 0; i--) {
                        SimpleNode popNode = stack.popNode();
                        if (popNode instanceof patternType) {
                            patterns[i] = (patternType) popNode;
                        } else if (popNode instanceof exprType) {
                            patterns[i] = new MatchValue((exprType) popNode);
                        } else {
                            patterns[i] = getDefaultInvalidPattern();
                        }
                    }
                    return new MatchSequence(patterns, 0);
                }
                addAndReportException(SimpleNode.class.getName());
                return new MatchSequence(new patternType[0], 0);

            default:
                Log.log(("Error at TreeBuilder: default not treated:" + n.getId()));
                return null;
        }
    }

    private boolean isPeekedNodeWildcardPattern() {
        SimpleNode peekedNode = stack.peekNode();
        if (peekedNode instanceof Name) {
            Name name = (Name) peekedNode;
            if ("_".equals(name.id) && name.ctx == Name.Load && name.reserved == false) {
                return true;
            }
        }
        return false;
    }

    private match_caseType getDefaultInvalidMatchCase() {
        return new match_caseType(getDefaultInvalidPattern(), null, getDefaultBody());
    }

    private stmtType[] getDefaultBody() {
        return new stmtType[] { new Pass() };
    }

    private patternType getDefaultInvalidPattern() {
        return new MatchValue(getDefaultInvalidExpr());
    }

    private exprType getDefaultInvalidExpr() {
        return new Name("$INVALID$", Name.Artificial, false);
    }

    private void popSurplus(int arity, int max) throws ParseException {
        if (arity > max) {
            String errorMessage = "Popping all surplus nodes. Expected arity: " + max + ". Actual: " + arity;
            SimpleNode firstNode = stack.popNode();
            stack.getGrammar().addAndReport(new ParseException(errorMessage, firstNode),
                    "Treated node arity greater than max expected.");
            for (int i = 0; i < (arity - 1) - max; i++) {
                stack.popNode();
            }
        }
    }

    private SimpleNode securePop(Class<? extends SimpleNode> cls) throws Exception {
        SimpleNode ret = null;
        SimpleNode popNode = stack.popNode();
        try {
            ret = cls.cast(popNode);
        } catch (ClassCastException e) {
            addAndReportException(cls.getName(), popNode);
            SimpleNode defaultNode = getDefaultInvalidNode(cls);
            ret = copyDefaultWithReference(defaultNode, popNode);
        }
        return ret;
    }

    private SimpleNode getDefaultInvalidNode(Class<? extends SimpleNode> cls) throws Exception {
        if (patternType.class.equals(cls)) {
            return getDefaultInvalidPattern();
        } else if (exprType.class.equals(cls)) {
            return getDefaultInvalidExpr();
        } else if (match_caseType.class.equals(cls)) {
            return getDefaultInvalidMatchCase();
        } else if (Suite.class.equals(cls)) {
            return new Suite(getDefaultBody());
        } else if (MatchClass.class.equals(cls)) {
            return new MatchClass(getDefaultInvalidExpr(), null);
        } else if (MatchKeyVal.class.equals(cls)) {
            return new MatchKeyVal(getDefaultInvalidExpr(), getDefaultInvalidPattern());
        }
        throw new Exception("Could not find any invalid default node for " + cls.getName());
    }

    private SimpleNode securePopNullable(Class<? extends SimpleNode> cls) throws ParseException {
        SimpleNode ret = null;
        SimpleNode popNode = stack.popNode();
        try {
            ret = cls.cast(popNode);
        } catch (Exception e) {
            addAndReportException(cls.getName(), popNode);
        }
        return ret;
    }

    private SimpleNode copyDefaultWithReference(SimpleNode defaultNode, SimpleNode referenceNode) {
        SimpleNode copy = defaultNode.createCopy();
        copy.beginLine = referenceNode.beginLine;
        copy.beginColumn = referenceNode.beginColumn;
        return copy;
    }

    private SimpleNode createMatchSequence(int arity, int enclosing) throws Exception {
        if (arity == 0) {
            return new MatchSequence(new patternType[0], enclosing);
        }
        if (arity == 1) {
            patternType pattern = null;
            SimpleNode peekedNode = stack.peekNode();
            if (peekedNode instanceof MatchSequence) {
                MatchSequence sequence = (MatchSequence) stack.popNode();
                sequence.enclosing = enclosing;
                return sequence;
            } else if (peekedNode instanceof exprType) {
                pattern = new MatchValue((exprType) stack.popNode());
            } else {
                pattern = (patternType) securePop(patternType.class);
            }
            return new MatchSequence(new patternType[] { pattern }, enclosing);
        }
        patternType[] patterns = new patternType[arity];
        for (int i = arity - 1; i >= 0; i--) {
            patterns[i] = (patternType) securePop(patternType.class);
        }
        return new MatchSequence(patterns, enclosing);
    }

}
