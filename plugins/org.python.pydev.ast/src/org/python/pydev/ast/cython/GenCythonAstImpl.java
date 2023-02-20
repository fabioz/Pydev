package org.python.pydev.ast.cython;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.IDocumentExtension4;
import org.python.pydev.ast.codecompletion.shell.AbstractShell;
import org.python.pydev.ast.codecompletion.shell.CythonShell;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.json.eclipsesource.JsonArray;
import org.python.pydev.json.eclipsesource.JsonObject;
import org.python.pydev.json.eclipsesource.JsonValue;
import org.python.pydev.parser.PyParser.ParserInfo;
import org.python.pydev.parser.grammarcommon.CtxVisitor;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assert;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.AugAssign;
import org.python.pydev.parser.jython.ast.Await;
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
import org.python.pydev.parser.jython.ast.DictComp;
import org.python.pydev.parser.jython.ast.Exec;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.ExtSlice;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Global;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.IfExp;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Index;
import org.python.pydev.parser.jython.ast.Lambda;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.jython.ast.NonLocal;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Print;
import org.python.pydev.parser.jython.ast.Raise;
import org.python.pydev.parser.jython.ast.Repr;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.Set;
import org.python.pydev.parser.jython.ast.Slice;
import org.python.pydev.parser.jython.ast.Starred;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.UnaryOp;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.With;
import org.python.pydev.parser.jython.ast.WithItem;
import org.python.pydev.parser.jython.ast.WithItemType;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.comprehensionType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.excepthandlerType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.keywordType;
import org.python.pydev.parser.jython.ast.sliceType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.suiteType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.jython.ast.factory.PyAstFactory;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;
import org.python.pydev.shared_core.string.StringUtils;

public class GenCythonAstImpl {

    public static final NameTok FOUND_NAME_LIST = new NameTok("found_name_list", NameTok.ImportName);

    public static boolean IN_TESTS = false;
    private final ParserInfo parserInfo;

    public GenCythonAstImpl(ParserInfo parserInfo) {
        this.parserInfo = parserInfo;
    }

    private static class JsonToNodesBuilder {

        final List<stmtType> stmts = new ArrayList<stmtType>();
        final PyAstFactory astFactory;
        final CtxVisitor ctx = new CtxVisitor(null);

        public JsonToNodesBuilder(ParserInfo p) {
            astFactory = new PyAstFactory(new AdapterPrefs("\n", p));
        }

        public void addStatement(JsonValue jsonStmt) throws Exception {
            ISimpleNode node = createNode(jsonStmt);
            addToStmtsList(node, stmts);
        }

        private ISimpleNode createNode(JsonValue jsonValue) {
            if (jsonValue == null || (jsonValue.isString() && "None".equals(jsonValue.asString()))) {
                return null;
            }
            JsonObject asObject = jsonValue.asObject();
            JsonValue jsonNode = asObject.get("__node__");
            ISimpleNode node = null;
            if (jsonNode != null) {
                try {
                    switch (jsonNode.asString()) {
                        case "Int":
                            node = createInt(asObject);
                            break;

                        case "Float":
                            node = createFloat(asObject);
                            break;

                        case "Name":
                            node = createName(asObject);
                            break;

                        case "Attribute":
                            node = createAttribute(asObject);
                            break;

                        case "StatList":
                            node = createStatList(asObject);
                            break;

                        case "Bool":
                            node = createBool(asObject);
                            break;

                        case "Null":
                        case "None":
                            node = createNone(asObject);
                            break;

                        case "IfStat":
                            node = createIf(asObject);
                            break;

                        case "JoinedStr":
                            node = createFString(asObject);
                            break;

                        case "String":
                            node = createString(asObject);
                            break;

                        case "Tuple":
                            node = createTuple(asObject);
                            break;

                        case "AsTuple":
                            node = createAsTuple(asObject);
                            break;

                        case "IdentifierString":
                            node = createIdentifierString(asObject);
                            break;

                        case "Ampersand":
                            node = createAmpersand(asObject);
                            break;

                        case "Dict":
                            node = createDict(asObject);
                            break;

                        case "MergedDict":
                            node = createMergedDict(asObject);
                            break;

                        case "List":
                            node = createList(asObject);
                            break;

                        case "Set":
                            node = createSet(asObject);
                            break;

                        case "Unicode":
                        case "Char":
                            node = createUnicode(asObject);
                            break;

                        case "Bytes":
                            node = createBytes(asObject);
                            break;

                        case "PassStat":
                            node = createPass(asObject);
                            break;

                        case "ExprStat":
                            node = createExpr(asObject);
                            break;

                        case "SimpleCall":
                            node = createSimpleCall(asObject);
                            break;

                        case "GeneralCall":
                            node = createGeneralCall(asObject);
                            break;

                        case "GeneratorExpression":
                            node = createGeneratorExpression(asObject);
                            break;

                        case "Comprehension":
                            node = createComprehension(asObject);
                            break;

                        case "YieldExpr":
                            node = createYieldExpr(asObject);
                            break;

                        case "YieldFromExpr":
                            node = createYieldFromExpr(asObject);
                            break;

                        case "Def":
                            node = createFunctionDef(asObject);
                            break;

                        case "CFuncDef":
                            node = createCFunctionDef(asObject);
                            break;

                        case "SingleAssignment":
                            node = createSingleAssignment(asObject);
                            break;

                        case "CascadedAssignment":
                            node = createCascadedAssignment(asObject);
                            break;

                        case "ForFromStat":
                            node = createForFromStat(asObject);
                            break;

                        case "ForInStat":
                        case "AsyncForStat":
                            node = createFor(asObject);
                            break;

                        case "AwaitExpr":
                            node = createAwait(asObject);
                            break;

                        case "CascadedCmp":
                            node = createCascade(asObject);
                            break;

                        case "PyClassDef":
                            node = createClassDef(asObject);
                            break;

                        case "CClassDef":
                            node = createCClassDef(asObject);
                            break;

                        case "CppClass":
                            node = createCppClass(asObject);
                            break;

                        case "PrimaryCmp":
                            node = createCompare(asObject);
                            break;

                        case "BoolBinop":
                            node = createBoolOp(asObject);
                            break;

                        case "CVarDef":
                            node = createCVarDef(asObject);
                            break;

                        case "CondExpr":
                            node = createCondExpr(asObject);
                            break;

                        case "WhileStat":
                            node = createWhile(asObject);
                            break;

                        case "WithStat":
                            node = createWith(asObject);
                            break;

                        case "AssertStat":
                            node = createAssert(asObject);
                            break;

                        case "ReturnStat":
                            node = createReturn(asObject);
                            break;

                        case "CFuncDeclarator":
                            node = createCFuncDeclarator(asObject);
                            break;

                        case "CDefExtern":
                            node = createCDefExtern(asObject);
                            break;

                        case "CStructOrUnionDef":
                            node = createCStructOrUnionDef(asObject);
                            break;

                        case "CTypeDef":
                            node = createCtypeDef(asObject);
                            break;

                        case "SizeofType":
                            node = createSizeofType(asObject);
                            break;

                        case "SizeofVar":
                            node = createSizeofVar(asObject);
                            break;

                        case "CEnumDef":
                            node = createCEnumDef(asObject);
                            break;

                        case "InPlaceAssignment":
                            node = createAugAssign(asObject);
                            break;

                        case "Typecast":
                            node = createTypecast(asObject);
                            break;

                        case "Import":
                            node = createImport(asObject);
                            break;

                        case "FromImportStat":
                            node = createFromImport(asObject);
                            break;

                        case "Add":
                        case "Sub":
                        case "Mul":
                        case "Div":
                        case "MatMult":
                        case "IntBinop":
                        case "Pow":
                        case "Mod":
                            node = createBinOp(asObject);
                            break;

                        case "Tilde":
                        case "UnaryMinus":
                        case "UnaryPlus":
                            node = createUnaryOp(asObject);
                            break;

                        case "Not":
                            node = createUnaryOp(asObject, "not");
                            break;

                        case "ContinueStat":
                            node = createContinue(asObject);
                            break;

                        case "BreakStat":
                            node = createBreak(asObject);
                            break;

                        case "FromCImportStat":
                            node = createFromCImportStat(asObject);
                            break;

                        case "CImportStat":
                            node = createCImportStat(asObject);
                            break;

                        case "PrintStat":
                            node = createPrint(asObject);
                            break;

                        case "Global":
                            node = createGlobal(asObject, NameTok.GlobalName, new Global(null, null));
                            break;

                        case "Nonlocal":
                            node = createGlobal(asObject, NameTok.NonLocalName, new NonLocal(null, null));
                            break;

                        case "Index":
                            node = createIndex(asObject);
                            break;

                        case "Slice":
                            node = createSlice(asObject);
                            break;

                        case "GILStat":
                            node = createGILStat(asObject);
                            break;

                        case "NewExpr":
                            node = createNewExpr(asObject);
                            break;

                        case "TemplatedType":
                            node = createTemplatedType(asObject);
                            break;

                        case "DelStat":
                            node = createDel(asObject);
                            break;

                        case "Ellipsis":
                            node = createEllipsis(asObject);
                            break;

                        case "TryFinallyStat":
                            node = createTryFinally(asObject);
                            break;

                        case "TryExceptStat":
                            node = createTryExcept(asObject);
                            break;

                        case "ReraiseStat":
                            node = createReraise(asObject);
                            break;

                        case "RaiseStat":
                            node = createRaise(asObject);
                            break;

                        case "Property":
                            node = createProperty(asObject);
                            break;

                        case "FusedType":
                            node = createFusedType(asObject);
                            break;

                        case "CSimpleBaseType":
                            node = createCSimpleBaseType(asObject);
                            break;

                        case "SliceIndex":
                            node = createSliceIndex(asObject);
                            break;

                        case "ComprehensionAppend":
                            node = createComprehensionAppend(asObject);
                            break;

                        case "Lambda":
                            node = createLambda(asObject);
                            break;

                        case "ExecStat":
                            node = createExec(asObject);
                            break;

                        case "StarredUnpacking":
                            node = createStarredUnpacking(asObject);
                            break;

                        case "MergedSequence":
                            node = createMergedSequence(asObject);
                            break;

                        case "Imag":
                            node = createImag(asObject);
                            break;

                        case "Backquote":
                            node = createBackquote(asObject);
                            break;

                        case "DictComprehensionAppend":
                            node = createDictComprehensionAppend(asObject);
                            break;

                        case "CythonArray":
                            return null;

                        case "MemoryViewSliceType":
                            return null;

                        default:
                            String msg = "Don't know how to create statement from cython json: "
                                    + asObject.toPrettyString();
                            log(msg);
                            break;

                    }
                } catch (Exception e) {
                    log(e);
                }
            }
            return node;
        }

        private ISimpleNode createDictComprehensionAppend(JsonObject asObject) {
            return null; // not handled here (it's handled at the createGeneratorExpression).
        }

        private ISimpleNode createBackquote(JsonObject asObject) {
            Repr repr = new Repr(asExpr(createNode(asObject.get("arg"))));
            setLine(repr, asObject);
            return repr;
        }

        private exprType asExpr(ISimpleNode node) {
            if (node instanceof MergedDict) {
                MergedDict mergedDict = (MergedDict) node;
                node = convertMergedDictToDict(mergedDict);
            }
            return astFactory.asExpr(node);
        }

        private ISimpleNode createMergedSequence(JsonObject asObject) {
            JsonValue type = asObject.get("type");
            ISimpleNode node = null;
            if (type != null && type.isString()) {
                String asString = type.asString();
                if (asString.startsWith("set")) {
                    node = new Set(null);

                } else if (asString.startsWith("tuple")) {
                    node = new Tuple(null, Tuple.Load, false);

                } else if (asString.startsWith("list")) {
                    node = new org.python.pydev.parser.jython.ast.List(null, Tuple.Load);

                } else {
                    log("Don't know how to deal with type: " + asString + " in: " + asObject.toPrettyString());
                    return null;
                }
            }

            List<JsonValue> bodyAsList = getBodyAsList(asObject.get("args"));
            List<exprType> elts = new ArrayList<>();

            for (JsonValue jsonValue : bodyAsList) {
                ISimpleNode n = createNode(jsonValue);
                if (n != null) {
                    if (n.getClass() == node.getClass()) {
                        exprType[] extractElts = NodeUtils.extractElts(n);
                        for (exprType exprType : extractElts) {
                            elts.add(exprType);
                        }
                    } else {
                        Starred starred = new Starred(asExpr(n), Starred.Load);
                        setLine(starred, jsonValue.asObject());
                        elts.add(starred);
                    }
                }
            }
            if (node instanceof Set) {
                ((Set) node).elts = elts.toArray(new exprType[0]);

            } else if (node instanceof Tuple) {
                ((Tuple) node).elts = elts.toArray(new exprType[0]);

            } else if (node instanceof org.python.pydev.parser.jython.ast.List) {
                ((org.python.pydev.parser.jython.ast.List) node).elts = elts.toArray(new exprType[0]);

            }

            return node;
        }

        private ISimpleNode createExec(JsonObject asObject) {
            exprType locals = null;
            exprType globals = null;
            exprType body = null;

            JsonValue args = asObject.get("args");
            if (args != null && args.isArray()) {
                JsonArray asArray = args.asArray();
                if (asArray.size() > 0) {
                    body = asExpr(createNode(asArray.get(0)));
                }
                if (asArray.size() > 1) {
                    globals = asExpr(createNode(asArray.get(1)));
                }
                if (asArray.size() > 2) {
                    locals = asExpr(createNode(asArray.get(2)));
                }
            }

            Exec exec = new Exec(body, globals, locals);
            setLine(exec, asObject);
            return exec;
        }

        private ISimpleNode createStarredUnpacking(JsonObject asObject) {
            exprType value = asExpr(createNode(asObject.get("target")));
            Starred starred = new Starred(value, Starred.Load);
            setLine(starred, asObject);
            return starred;
        }

        private ISimpleNode createComprehensionAppend(JsonObject asObject) {
            return createNode(asObject.get("expr"));
        }

        private ISimpleNode createProperty(JsonObject asObject) {
            Name name = createName(asObject);
            try {
                ctx.setStore(name);
            } catch (Exception e) {
            }
            setLine(name, asObject);
            Assign assign = astFactory.createAssign(name, createNone(asObject));
            setLine(assign, asObject);

            return assign;
        }

        private ISimpleNode createRaise(JsonObject asObject) {
            Raise raise = new Raise(null, null, null, null);
            JsonValue excType = asObject.get("exc_type");
            if (excType != null && excType.isObject()) {
                raise.type = asExpr(createNode(excType));
            }
            JsonValue cause = asObject.get("cause");
            if (cause != null && cause.isObject()) {
                raise.cause = asExpr(createNode(cause));
            }
            setLine(raise, asObject);
            return raise;
        }

        private ISimpleNode createReraise(JsonObject asObject) {
            Raise raise = new Raise(null, null, null, null);
            setLine(raise, asObject);
            return raise;
        }

        private ISimpleNode createTryExcept(JsonObject asObject) {
            TryExcept tryExcept = new TryExcept(null, null, null);
            astFactory.setBody(tryExcept, extractStmts(asObject, "body").toArray());

            JsonValue exceptClauses = asObject.get("except_clauses");
            List<excepthandlerType> lst = new ArrayList<>();
            if (exceptClauses != null && exceptClauses.isArray()) {
                JsonArray asArray = exceptClauses.asArray();
                for (JsonValue jsonValue : asArray) {
                    excepthandlerType exceptClause = createExceptClause(jsonValue.asObject());
                    lst.add(exceptClause);
                }
            }
            tryExcept.handlers = lst.toArray(new excepthandlerType[0]);

            // Cython issue: it seems that it never parser a try..except..else (although it's in its AST).
            // JsonValue elseClause = asObject.get("else_clause");
            // if(elseClause != null && elseClause.isObject()) {
            //
            // }

            setLine(tryExcept, asObject);
            return tryExcept;
        }

        private excepthandlerType createExceptClause(JsonObject asObject) {
            stmtType[] body = extractStmts(asObject, "body").toArray(new stmtType[0]);
            exprType name = null;
            exprType type = null;

            JsonValue target = asObject.get("target");
            if (target != null && target.isObject()) {
                name = asExpr(createNode(target));
                try {
                    ctx.setStore(name);
                } catch (Exception e) {
                }
            }

            JsonValue pattern = asObject.get("pattern");
            if (pattern != null && pattern.isArray()) {
                List<exprType> lst = new ArrayList<>();
                JsonArray asArray = pattern.asArray();

                for (JsonValue jsonValue : asArray) {
                    exprType asExpr = asExpr(createNode(jsonValue));
                    lst.add(asExpr);
                }

                if (lst.size() == 1) {
                    type = lst.get(0);
                } else {
                    type = new Tuple(lst.toArray(new exprType[0]), Tuple.Load, false);
                    setLine(type, asArray.get(0).asObject());
                }
            }
            excepthandlerType excepthandlerType = new excepthandlerType(type, name, body, false);
            setLine(excepthandlerType, asObject);
            return excepthandlerType;
        }

        private ISimpleNode createTryFinally(JsonObject asObject) {
            TryFinally tryFinally = new TryFinally(null, null);

            astFactory.setBody(tryFinally, extractStmts(asObject, "body").toArray());
            astFactory.setFinally(tryFinally, extractStmts(asObject, "finally_clause").toArray());
            setLine(tryFinally, asObject);

            return tryFinally;
        }

        private ISimpleNode createEllipsis(JsonObject asObject) {
            Name name = new Name("...", Name.Load, true);
            setLine(name, asObject);
            return name;
        }

        private ISimpleNode createDel(JsonObject asObject) {
            List<exprType> targets = new ArrayList<exprType>();
            JsonValue args = asObject.get("args");
            if (args != null) {
                if (args.isArray()) {
                    JsonArray arr = args.asArray();
                    for (JsonValue v : arr) {
                        ISimpleNode n = createNode(v);
                        if (n != null) {
                            targets.add(asExpr(n));
                        }
                    }
                }
            }
            Delete delete = new Delete(targets.toArray(new exprType[0]));
            try {
                ctx.setDelete(delete.targets);
            } catch (Exception e) {
            }
            setLine(delete, asObject);
            return delete;
        }

        private ISimpleNode createTemplatedType(JsonObject asObject) {
            Name name = createNameFromBaseType(asObject);
            return name;
        }

        private ISimpleNode createNewExpr(JsonObject asObject) {
            JsonValue jsonValue = asObject.get("cppclass");
            if (jsonValue != null && jsonValue.isObject()) {
                ISimpleNode node = createNode(jsonValue);
                return node;
            }
            return null;
        }

        private Slice createSlice(JsonObject asObject) {
            JsonValue start = asObject.get("start");
            JsonValue stop = asObject.get("stop");
            JsonValue step = asObject.get("step");

            exprType startNode = null;
            exprType stopNode = null;
            exprType stepNode = null;

            if (start != null && start.isObject() && !isNone(start.asObject())) {
                startNode = asExpr(createNode(start));
            }
            if (stop != null && stop.isObject() && !isNone(stop.asObject())) {
                stopNode = asExpr(createNode(stop));
            }
            if (step != null && step.isObject() && !isNone(step.asObject())) {
                stepNode = asExpr(createNode(step));
            }

            Slice slice = new Slice(startNode, stopNode, stepNode);
            setLine(slice, asObject);
            return slice;
        }

        private ISimpleNode createSliceIndex(JsonObject asObject) {
            JsonValue base = asObject.get("base");
            if (base != null && base.isObject()) {
                ISimpleNode subscript = createNode(base);

                Slice slice = createSlice(asObject);

                Subscript s = new Subscript(asExpr(subscript), slice,
                        Subscript.Load);
                setLine(s, asObject);
                return s;

            }
            return null;
        }

        private boolean isNone(JsonObject asObject) {
            JsonValue node = asObject.get("__node__");
            if (node != null && node.isString() && node.asString().equals("None")) {
                return true;
            }
            return false;
        }

        private ISimpleNode createIndex(JsonObject asObject) {
            JsonValue base = asObject.get("base");
            if (base != null && base.isObject()) {
                ISimpleNode subscript = createNode(base);
                JsonValue index = asObject.get("index");
                if (index != null && index.isObject()) {
                    JsonValue indexNodeType = index.asObject().get("__node__");
                    ISimpleNode slice;
                    if (indexNodeType != null && indexNodeType.isString() && indexNodeType.asString().equals("Tuple")) {
                        slice = createExtSliceFromTuple(index.asObject());

                    } else {
                        slice = createNode(index);
                    }
                    if (slice != null) {
                        if (!(slice instanceof sliceType)) {
                            Index idx = new Index(asExpr(slice));
                            setLine(idx, index.asObject());
                            slice = idx;
                        }
                        Subscript s = new Subscript(asExpr(subscript), (sliceType) slice,
                                Subscript.Load);
                        setLine(s, asObject);
                        return s;
                    }
                }
            }
            return null;
        }

        private ISimpleNode createExtSliceFromTuple(JsonObject asObject) {
            JsonValue jsonValue = asObject.get("args");
            ArrayList<JsonValue> bodyAsList = getBodyAsList(jsonValue);
            List<sliceType> lst = new ArrayList<>();
            for (JsonValue v : bodyAsList) {
                ISimpleNode bodyNode = createNode(v);
                if (bodyNode instanceof sliceType) {
                    lst.add((sliceType) bodyNode);
                } else {
                    Index idx = new Index(asExpr(bodyNode));
                    setLine(idx, v.asObject());
                    lst.add(idx);
                }
            }

            ExtSlice extSlice = new ExtSlice(lst.toArray(new sliceType[0]));
            setLine(extSlice, asObject);
            return extSlice;
        }

        private ISimpleNode createGlobal(JsonObject asObject, int flag, SimpleNode node) {
            List<NameTok> lst = new ArrayList<>();
            JsonValue names = asObject.get("names");
            if (names != null && names.isArray()) {
                JsonArray arr = names.asArray();
                for (JsonValue v : arr) {
                    if (v.isString()) {
                        NameTok n = new NameTok(v.asString(), flag);
                        lst.add(n);
                        setLine(n, asObject);
                    }
                }

                if (node instanceof Global) {
                    Global global = (Global) node;
                    global.names = lst.toArray(new NameTokType[0]);
                } else if (node instanceof NonLocal) {
                    NonLocal global = (NonLocal) node;
                    global.names = lst.toArray(new NameTokType[0]);

                }
            }
            setLine(node, asObject);
            return node;
        }

        private Continue createContinue(JsonObject asObject) {
            Continue c = new Continue();
            setLine(c, asObject);
            return c;
        }

        private Break createBreak(JsonObject asObject) {
            Break b = new Break();
            setLine(b, asObject);
            return b;
        }

        private ISimpleNode createFromImport(JsonObject asObject) {
            JsonValue moduleValue = asObject.get("module");
            if (moduleValue != null && moduleValue.isObject()) {
                ISimpleNode module = createNode(moduleValue.asObject());
                if (module instanceof Import) {
                    Import imp = (Import) module;
                    NameTok moduleName = (NameTok) imp.names[0].name;
                    moduleName.ctx = NameTok.ImportModule;
                    ImportFrom importFrom = new ImportFrom(moduleName, null, 0);
                    setLine(importFrom, asObject);

                    JsonValue items = asObject.get("items");
                    if (items.isArray()) {
                        List<aliasType> lst = new ArrayList<>();
                        JsonArray asArray = items.asArray();
                        for (JsonValue v : asArray) {
                            if (v.isArray()) {
                                JsonArray arr2 = v.asArray();
                                if (arr2.size() == 2) {
                                    JsonValue v0 = arr2.get(0);
                                    JsonValue v2 = arr2.get(1);
                                    NameTok nameTok = new NameTok(v0.asString(), NameTok.ImportName);
                                    setLine(nameTok, v2.asObject());
                                    NameTok n2 = createNameTok(v2.asObject(), NameTok.ImportName);
                                    if (n2 != null && n2.id != null && n2.id.equals(nameTok.id)) {
                                        n2 = null;
                                    }
                                    if (!nameTok.id.equals("*")) {
                                        lst.add(new aliasType(nameTok, n2));
                                    }
                                }
                            }
                        }
                        importFrom.names = lst.toArray(new aliasType[0]);
                    }

                    JsonValue level = moduleValue.asObject().get("level");
                    if (level != null && level.isString()) {
                        try {
                            importFrom.level = Integer.parseInt(level.asString());
                        } catch (NumberFormatException e) {
                        }
                    }
                    return importFrom;
                }
            }
            return null;
        }

        private ISimpleNode createPrint(JsonObject asObject) {
            JsonValue jsonValue = asObject.get("arg_tuple");
            ISimpleNode args = createNode(jsonValue);
            if (args instanceof Tuple) {
                Tuple tuple = (Tuple) args;
                JsonValue appendNewLine = asObject.get("append_newline");
                boolean nl = appendNewLine != null && appendNewLine.isString()
                        && appendNewLine.asString().equals("True");
                Print node = new Print(null, tuple.elts, nl);
                setLine(node, asObject);
                return node;
            }
            return null;
        }

        private ISimpleNode createFromCImportStat(JsonObject asObject) {
            // We don't follow cimports, so, just create an assign to None.
            JsonValue jsonValue = asObject.get("imported_names");
            NodeList nodeList = new NodeList();
            if (jsonValue != null && jsonValue.isArray()) {
                JsonArray asArray = jsonValue.asArray();
                for (JsonValue v : asArray) {
                    if (v.isString()) {
                        String s = v.asString();
                        Name name = new Name(s, Name.Store, false);
                        setLine(name, asObject);
                        Assign assign = astFactory.createAssign(name, createNone(asObject));
                        setLine(assign, asObject);
                        nodeList.nodes.add(assign);

                    } else if (v.isArray()) {
                        for (JsonValue x : v.asArray()) {
                            if (x.isString()) {
                                String s = x.asString();
                                Name name = new Name(s, Name.Store, false);
                                setLine(name, asObject);
                                Assign assign = astFactory.createAssign(name, createNone(asObject));
                                setLine(assign, asObject);
                                nodeList.nodes.add(assign);
                            }
                        }
                    }
                }
            }

            return nodeList;
        }

        private ISimpleNode createCImportStat(JsonObject asObject) {
            // We don't follow cimports, so, just create an assign to None.
            JsonValue jsonValue = asObject.get("module_name");
            JsonValue asName = asObject.get("as_name");
            if (asName != null && asName.isString() && !asName.asString().equals("None")) {
                jsonValue = asName;
            }

            if (jsonValue != null && jsonValue.isString()) {
                Name name = new Name(jsonValue.asString(), Name.Store, false);
                setLine(name, asObject);
                Assign assign = astFactory.createAssign(name, createNone(asObject));
                setLine(assign, asObject);
                return assign;
            }
            return null;
        }

        private ISimpleNode createImport(JsonObject asObject) {
            JsonValue jsonValue = asObject.get("module_name");
            if (jsonValue != null && jsonValue.isObject()) {
                List<aliasType> alias = new ArrayList<aliasType>();
                JsonValue nameList = asObject.get("name_list");
                NameTok foundNameList;
                if (nameList == null || (nameList.isString() && nameList.asString().equals("None"))) {
                    foundNameList = null;
                } else {
                    foundNameList = FOUND_NAME_LIST;

                }
                NameTokType name = createNameTok(jsonValue.asObject(), NameTok.ImportName);
                alias.add(new aliasType(name, foundNameList));
                Import imp = new Import(alias.toArray(new aliasType[0]));

                return imp;
            }
            return null;
        }

        private ISimpleNode createTypecast(JsonObject asObject) {
            Name name = createNameFromBaseType(asObject);
            if (name != null) {
                JsonValue jsonValue = asObject.get("operand");
                if (jsonValue.isObject()) {
                    ISimpleNode operand = createNode(jsonValue.asObject());
                    if (operand != null) {
                        List<exprType> params = new ArrayList<exprType>();
                        exprType expr = asExpr(operand);
                        if (expr != null) {
                            params.add(expr);
                        }
                        Call call = astFactory.createCall(name, params, new keywordType[0], null, null);
                        setLine(call, asObject);
                        return call;
                    }
                }
            }
            return null;
        }

        private org.python.pydev.parser.jython.ast.Tuple createTuple(JsonObject asObject) throws Exception {
            boolean endsWithComma = false;
            List<exprType> extract = extractExprs(asObject, "args");
            org.python.pydev.parser.jython.ast.Tuple tup = new org.python.pydev.parser.jython.ast.Tuple(
                    astFactory.createExprArray(extract.toArray()),
                    org.python.pydev.parser.jython.ast.Tuple.Load,
                    endsWithComma);
            setLine(tup, asObject);
            return tup;
        }

        private org.python.pydev.parser.jython.ast.Tuple createAsTuple(JsonObject asObject) throws Exception {
            boolean endsWithComma = false;
            List<exprType> extract = extractExprs(asObject, "arg");
            org.python.pydev.parser.jython.ast.Tuple tup = new org.python.pydev.parser.jython.ast.Tuple(
                    astFactory.createExprArray(extract.toArray()),
                    org.python.pydev.parser.jython.ast.Tuple.Load,
                    endsWithComma);
            setLine(tup, asObject);
            return tup;
        }

        private org.python.pydev.parser.jython.ast.List createList(JsonObject asObject) throws Exception {
            List<exprType> extract = extractExprs(asObject, "args");
            org.python.pydev.parser.jython.ast.List tup = new org.python.pydev.parser.jython.ast.List(
                    astFactory.createExprArray(extract.toArray()),
                    org.python.pydev.parser.jython.ast.List.Load);
            setLine(tup, asObject);
            return tup;
        }

        private Set createSet(JsonObject asObject) throws Exception {
            List<exprType> extract = extractExprs(asObject, "args");
            Set tup = new Set(astFactory.createExprArray(extract.toArray()));
            setLine(tup, asObject);
            return tup;
        }

        private ISimpleNode createMergedDict(JsonObject asObject) throws Exception {
            List<JsonValue> bodyAsList = getBodyAsList(asObject.get("keyword_args"));
            MergedDict dct = new MergedDict();
            for (JsonValue jsonValue : bodyAsList) {
                ISimpleNode n = createNode(jsonValue);
                if (n != null) {
                    dct.nodes.add(n);
                }
            }
            return dct;
        }

        private Dict createDict(JsonObject asObject) throws Exception {
            List<JsonValue> bodyAsList = getBodyAsList(asObject.get("key_value_pairs"));
            List<exprType> keys = new ArrayList<>();
            List<exprType> values = new ArrayList<>();
            for (JsonValue jsonValue : bodyAsList) {
                JsonObject itemAsObj = jsonValue.asObject();
                ISimpleNode key = createNode(itemAsObj.get("key"));
                ISimpleNode val = createNode(itemAsObj.get("value"));
                keys.add(asExpr(key));
                values.add(asExpr(val));
            }
            Dict tup = new Dict(keys.toArray(new exprType[0]), values.toArray(new exprType[0]));
            setLine(tup, asObject);
            return tup;
        }

        private SimpleNode createString(JsonObject asObject) {
            boolean raw = false;
            boolean unicode = false;
            boolean binary = false;
            boolean fstring = false;
            int type = Str.SingleSingle;
            String s = "";

            JsonValue value = asObject.get("unicode_value");
            if (value != null && value.isString()) {
                s = value.asString();
            }

            Str str = new Str(s, type, unicode, raw, binary, fstring, null);
            setLine(str, asObject);
            return str;
        }

        private SimpleNode createFString(JsonObject asObject) throws Exception {
            boolean raw = false;
            boolean unicode = true;
            boolean binary = false;
            boolean fstring = false;
            int type = Str.SingleSingle;
            String s = "";

            List<ISimpleNode> stmts = new ArrayList<>();

            JsonValue values = asObject.get("values");
            if (values != null && values.isArray()) {
                for (JsonValue v : values.asArray()) {
                    if (v.isObject()) {
                        JsonObject o = v.asObject();
                        JsonValue n = o.get("__node__");
                        if (n != null && n.isString() && n.asString().equals("FormattedValue")) {
                            ISimpleNode node = createNode(o.get("value"));
                            addToNodesList(node, stmts);
                        } else {
                            ISimpleNode node = createNode(o);
                            if (node != null) {
                                stmts.add(node);
                            }
                        }
                    }
                }
            }

            Str str = new Str(s, type, unicode, raw, binary, fstring,
                    stmts.size() == 0 ? null : astFactory.createStmtArray(stmts.toArray()));
            setLine(str, asObject);
            return str;
        }

        private void addToStmtsList(ISimpleNode node, List<stmtType> stmts) {
            if (node != null) {
                if (node instanceof NodeList) {
                    NodeList nodeList = (NodeList) node;
                    for (ISimpleNode n : nodeList.nodes) {
                        addToStmtsList(n, stmts);
                    }
                } else {
                    stmts.add(astFactory.asStmt(node));
                }
            }
        }

        private void addToExprsList(ISimpleNode node, List<exprType> exprs) {
            if (node != null) {
                if (node instanceof NodeList) {
                    NodeList nodeList = (NodeList) node;
                    for (ISimpleNode n : nodeList.nodes) {
                        addToExprsList(n, exprs);
                    }
                } else {
                    exprs.add(asExpr(node));
                }
            }
        }

        private void addToNodesList(ISimpleNode node, List<ISimpleNode> lst) {
            if (node != null) {
                if (node instanceof NodeList) {
                    NodeList nodeList = (NodeList) node;
                    for (ISimpleNode n : nodeList.nodes) {
                        addToNodesList(n, lst);
                    }
                } else {
                    lst.add(node);
                }
            }
        }

        private SimpleNode createUnicode(JsonObject asObject) {
            boolean raw = false;
            boolean unicode = true;
            boolean binary = false;
            boolean fstring = false;
            int type = Str.SingleSingle;
            String s = "";

            JsonValue value = asObject.get("value");
            if (value != null && value.isString()) {
                s = value.asString();
            }

            Str str = new Str(s, type, unicode, raw, binary, fstring, null);
            setLine(str, asObject);
            return str;
        }

        private SimpleNode createBytes(JsonObject asObject) {
            boolean raw = false;
            boolean unicode = false;
            boolean binary = true;
            boolean fstring = false;
            int type = Str.SingleSingle;
            String s = "";

            JsonValue value = asObject.get("value");
            if (value != null && value.isString()) {
                s = value.asString();
            }

            Str str = new Str(s, type, unicode, raw, binary, fstring, null);
            setLine(str, asObject);
            return str;
        }

        private SimpleNode createComprehension(final JsonObject asObject) throws Exception {
            return createGeneratorExpression(asObject);
        }

        private SimpleNode createGeneratorExpression(final JsonObject asObject) throws Exception {
            JsonValue loop = asObject.get("loop");
            SimpleNode node = null;
            if (loop != null && loop.isObject()) {
                ISimpleNode loopNode = createNode(loop);
                if (loopNode instanceof For) {
                    For for1 = (For) loopNode;
                    exprType iter = for1.iter;
                    suiteType orelse = for1.orelse;
                    exprType target = for1.target;
                    stmtType[] body = for1.body;
                    Comprehension generator = new Comprehension(target, iter, new exprType[0]);
                    exprType exprToUse = null;
                    if (body.length == 1) {
                        stmtType stmtType = body[0];
                        if (stmtType instanceof If) {
                            If if1 = (If) stmtType;
                            if (if1.body != null && if1.body.length == 1) {
                                stmtType = if1.body[0];
                                generator.ifs = new exprType[] { if1.test };
                            }
                        }
                        if (stmtType instanceof Expr) {
                            Expr expr = (Expr) stmtType;
                            exprToUse = expr.value;
                            if (expr.value instanceof Yield) {
                                Yield yield = (Yield) expr.value;
                                exprToUse = yield.value;
                            }
                        }
                    }

                    JsonValue type = asObject.get("type");
                    if (type != null && type.isString()) {
                        if (type.asString().startsWith("dict")) {
                            JsonValue appendValue = asObject.get("append");
                            if (appendValue != null && appendValue.isObject()) {
                                JsonObject appendAsObject = appendValue.asObject();
                                node = new DictComp(
                                        asExpr(createNode(appendAsObject.get("key_expr"))),
                                        asExpr(createNode(appendAsObject.get("value_expr"))),
                                        new comprehensionType[] { generator });
                            } else {
                                node = new DictComp(null, null, new comprehensionType[] { generator });
                                log("Expected append to be an object.");
                            }

                        } else if (type.asString().startsWith("list")) {
                            node = new ListComp(exprToUse, new comprehensionType[] { generator },
                                    ListComp.ListCtx);

                        } else if (type.asString().startsWith("tuple")) {
                            node = new ListComp(exprToUse, new comprehensionType[] { generator },
                                    ListComp.TupleCtx);

                        } else {
                            node = new ListComp(exprToUse, new comprehensionType[] { generator },
                                    ListComp.EmptyCtx);

                        }
                    } else {
                        node = new ListComp(exprToUse, new comprehensionType[] { generator },
                                ListComp.EmptyCtx);

                    }
                }
            }
            return node;
        }

        private SimpleNode createGeneralCall(final JsonObject asObject) throws Exception {
            final JsonValue funcJsonValue = asObject.get("function");
            if (funcJsonValue != null && funcJsonValue.isObject()) {
                final JsonObject funcAsObject = funcJsonValue.asObject();
                final Name name = createName(funcAsObject);
                if (name != null) {
                    final JsonValue jsonValueArgs = asObject.get("positional_args");
                    final JsonValue jsonKeywordArgs = asObject.get("keyword_args");

                    List<exprType> params = new ArrayList<>();
                    List<keywordType> keywords = new ArrayList<>();
                    exprType starargs = null;
                    exprType kwargs = null;

                    if (jsonValueArgs != null) {
                        if (jsonValueArgs.isArray()) {
                            for (JsonValue v : jsonValueArgs.asArray()) {
                                ISimpleNode n = createNode(v);
                                if (n != null) {
                                    params.add(asExpr(n));
                                }
                            }
                        } else if (jsonValueArgs.isObject()) {
                            // Star args
                            JsonValue node = jsonValueArgs.asObject().get("__node__");
                            if (node != null && node.isString() && node.asString().equals("AsTuple")) {
                                starargs = createStarArgsFromAsTuple(jsonValueArgs);

                            } else if (node != null && node.isString() && node.asString().equals("Add")) {
                                // regular args and star args in an Add (kind of weird...)
                                JsonValue op1 = jsonValueArgs.asObject().get("operand1");
                                JsonValue op2 = jsonValueArgs.asObject().get("operand2");
                                starargs = createStarArgsFromAsTuple(op2);
                                createParamsFromTuple(op1, params);

                            } else {
                                // Regular tuple in positional args
                                createParamsFromTuple(jsonValueArgs, params);
                            }
                        }
                    }

                    if (jsonKeywordArgs != null && jsonKeywordArgs.isObject()) {
                        ISimpleNode dictNode = createNode(jsonKeywordArgs);
                        if (dictNode instanceof Dict) {
                            Dict dict = (Dict) dictNode;
                            boolean afterstarargs = starargs == null;
                            fillKeywordsFromDict(keywords, dict, afterstarargs);

                        } else if (dictNode instanceof MergedDict) {
                            MergedDict mergedDict = (MergedDict) dictNode;
                            for (ISimpleNode node : mergedDict.nodes) {
                                if (node instanceof Dict) {
                                    Dict dict = (Dict) node;
                                    boolean afterstarargs = starargs == null;
                                    fillKeywordsFromDict(keywords, dict, afterstarargs);

                                } else if (node instanceof exprType) {
                                    kwargs = (exprType) node;

                                } else if (node instanceof MergedDict) {
                                    kwargs = convertMergedDictToDict((MergedDict) node);

                                } else {
                                    log("Don't know how to deal with merged dict entry: " + node);
                                    return null;
                                }
                            }

                        } else if (dictNode instanceof exprType) {
                            kwargs = (exprType) dictNode;
                        }
                    }

                    Call call = astFactory.createCall(name, params, keywords.toArray(new keywordType[0]), starargs,
                            kwargs);
                    setLine(call, asObject);
                    return call;
                }
            }
            return null;
        }

        private exprType convertMergedDictToDict(MergedDict node) {
            List<ISimpleNode> nodes = node.nodes;
            List<exprType> keys = new ArrayList<>();
            List<exprType> values = new ArrayList<>();
            for (ISimpleNode iSimpleNode : nodes) {
                if (iSimpleNode instanceof Dict) {
                    Dict dict = (Dict) iSimpleNode;
                    if (dict.keys != null) {
                        for (exprType k : dict.keys) {
                            keys.add(k);
                        }
                    }
                    if (dict.values != null) {
                        for (exprType v : dict.values) {
                            keys.add(v);
                        }
                    }
                } else if (iSimpleNode instanceof exprType) {
                    keys.add((exprType) iSimpleNode);
                }
            }
            return new Dict(keys.toArray(new exprType[0]), values.toArray(new exprType[0]));
        }

        private void fillKeywordsFromDict(List<keywordType> keywords, Dict dict, boolean afterstarargs) {
            for (int i = 0; i < dict.keys.length; i++) {
                exprType key = dict.keys[i];
                if (key instanceof Name) {
                    Name nameKey = (Name) key;
                    NameTok nameTok = new NameTok(nameKey.id, NameTok.KeywordName);
                    nameTok.beginColumn = key.beginColumn;
                    nameTok.beginLine = key.beginLine;
                    exprType value = dict.values[i];
                    keywords.add(new keywordType(nameTok, value, afterstarargs));
                }
            }
        }

        private void createParamsFromTuple(final JsonValue jsonValueArgs, List<exprType> params) {
            ISimpleNode asTupleNode = createNode(jsonValueArgs);
            if (asTupleNode instanceof Tuple) {
                Tuple tuple = (Tuple) asTupleNode;
                for (exprType e : tuple.elts) {
                    params.add(e);
                }
            }
        }

        private exprType createStarArgsFromAsTuple(final JsonValue jsonValueArgs) {
            exprType starargs = null;
            ISimpleNode asTupleNode = createNode(jsonValueArgs);
            if (asTupleNode instanceof Tuple) {
                Tuple tuple = (Tuple) asTupleNode;
                if (tuple.elts.length > 0) {
                    starargs = tuple.elts[0];
                }
            }
            return starargs;
        }

        private SimpleNode createSimpleCall(final JsonObject asObject) throws Exception {
            final JsonValue funcJsonValue = asObject.get("function");
            if (funcJsonValue != null && funcJsonValue.isObject()) {
                final JsonObject funcAsObject = funcJsonValue.asObject();
                exprType expr = asExpr(createNode(funcAsObject));
                if (expr != null) {
                    final JsonValue jsonValueArgs = asObject.get("args");

                    List<exprType> params = new ArrayList<>();
                    List<keywordType> keywords = new ArrayList<>();
                    exprType starargs = null;
                    exprType kwargs = null;

                    if (jsonValueArgs != null && jsonValueArgs.isArray()) {
                        for (JsonValue v : jsonValueArgs.asArray()) {
                            ISimpleNode n = createNode(v);
                            if (n != null) {
                                params.add(asExpr(n));
                            }
                        }
                    }

                    Call call = astFactory.createCall(expr, params, keywords.toArray(new keywordType[0]), starargs,
                            kwargs);
                    setLine(call, asObject);
                    return call;
                }
            }
            return null;
        }

        public ISimpleNode createAugAssign(JsonObject asObject) {
            AugAssign node = null;
            JsonValue op1 = asObject.get("lhs");
            JsonValue op2 = asObject.get("rhs");
            JsonValue operator = asObject.get("operator");
            if (op1 != null && op1.isObject() && op2 != null && op2.isObject() && operator != null
                    && operator.isString()) {
                ISimpleNode left = createNode(op1);
                ISimpleNode right = createNode(op2);
                int op = 0;
                switch (operator.asString()) {
                    case "+":
                        op = AugAssign.Add;
                        break;
                    case "-":
                        op = AugAssign.Sub;
                        break;
                    case "*":
                        op = AugAssign.Mult;
                        break;
                    case "/":
                        op = AugAssign.Div;
                        break;
                    case "@":
                        op = AugAssign.Dot;
                        break;
                    case "&":
                        op = AugAssign.BitAnd;
                        break;
                    case "|":
                        op = AugAssign.BitOr;
                        break;
                    case "^":
                        op = AugAssign.BitXor;
                        break;
                    case "%":
                        op = AugAssign.Mod;
                        break;

                }
                exprType leftAsExpr = asExpr(left);
                try {
                    ctx.setAugStore(leftAsExpr);
                } catch (Exception e) {
                    Log.log(e);
                }
                node = new AugAssign(leftAsExpr, op, asExpr(right));
                setLine(node, asObject);
            }
            return node;
        }

        public ISimpleNode createUnaryOp(JsonObject asObject) {
            JsonValue operator = asObject.get("operator");
            return createUnaryOp(asObject, operator.asString());
        }

        public ISimpleNode createUnaryOp(JsonObject asObject, String operator) {
            JsonValue operand = asObject.get("operand");
            int op = 0;
            switch (operator) {
                case "~":
                    op = UnaryOp.Invert;
                    break;
                case "-":
                    op = UnaryOp.USub;
                    break;
                case "+":
                    op = UnaryOp.UAdd;
                    break;
                case "not":
                    op = UnaryOp.Not;
                    break;
            }
            UnaryOp node = new UnaryOp(op, asExpr(createNode(operand)));
            setLine(node, asObject);
            return node;
        }

        public ISimpleNode createBinOp(JsonObject asObject) {
            BinOp node = null;
            JsonValue op1 = asObject.get("operand1");
            JsonValue op2 = asObject.get("operand2");
            JsonValue operator = asObject.get("operator");
            if (op1 != null && op1.isObject() && op2 != null && op2.isObject() && operator != null
                    && operator.isString()) {
                ISimpleNode left = createNode(op1);
                ISimpleNode right = createNode(op2);
                int op = 0;
                switch (operator.asString()) {
                    case "+":
                        op = BinOp.Add;
                        break;
                    case "-":
                        op = BinOp.Sub;
                        break;
                    case "*":
                        op = BinOp.Mult;
                        break;
                    case "/":
                        op = BinOp.Div;
                        break;
                    case "@":
                        op = BinOp.Dot;
                        break;
                    case "&":
                        op = BinOp.BitAnd;
                        break;
                    case "|":
                        op = BinOp.BitOr;
                        break;
                    case "^":
                        op = BinOp.BitXor;
                        break;
                    case "**":
                        op = BinOp.Pow;
                        break;
                    case "%":
                        op = BinOp.Mod;
                        break;
                    default:
                        break;

                }
                node = new BinOp(asExpr(left), op, asExpr(right));
                setLine(node, asObject);
            }
            return node;
        }

        private BoolOp createBoolOp(JsonObject asObject) {

            BoolOp node = null;
            JsonValue op1 = asObject.get("operand1");
            JsonValue op2 = asObject.get("operand2");
            JsonValue operator = asObject.get("operator");
            if (op1 != null && op1.isObject() && op2 != null && op2.isObject() && operator != null
                    && operator.isString()) {
                ISimpleNode left = createNode(op1);
                ISimpleNode right = createNode(op2);
                int op = 0;
                switch (operator.asString()) {
                    case "or":
                        op = BoolOp.Or;
                        break;
                    case "and":
                        op = BoolOp.And;
                        break;

                }
                node = new BoolOp(op, new exprType[] { asExpr(left), asExpr(right) });
                setLine(node, asObject);
            }
            return node;
        }

        private Compare createCompare(JsonObject asObject) {

            Compare node = null;
            JsonValue op1 = asObject.get("operand1");
            JsonValue op2 = asObject.get("operand2");
            JsonValue operator = asObject.get("operator");
            if (op1 != null && op1.isObject() && op2 != null && op2.isObject() && operator != null
                    && operator.isString()) {
                ISimpleNode left = createNode(op1);

                List<exprType> right = new ArrayList<>();
                right.add(asExpr(createNode(op2)));

                List<Integer> operators = new ArrayList<Integer>();
                operators.add(opToCompareOp(operator.asString()));

                JsonValue cascadeValue = asObject.get("cascade");
                if (cascadeValue != null && cascadeValue.isObject()) {
                    CascadeNode cascadeNode = (CascadeNode) createNode(cascadeValue);
                    while (cascadeNode != null) {
                        operators.add(opToCompareOp(cascadeNode.operator));
                        right.add(asExpr(cascadeNode.operand));
                        cascadeNode = cascadeNode.cascade;
                    }
                }
                node = new Compare(asExpr(left), operators.stream().mapToInt(i -> i).toArray(),
                        right.toArray(new exprType[0]));
                setLine(node, asObject);
            }
            return node;
        }

        private int opToCompareOp(String opStr) {
            int op = 0;
            switch (opStr) {
                case ">":
                    op = Compare.Gt;
                    break;
                case "==":
                    op = Compare.Eq;
                    break;
                case "!=":
                    op = Compare.NotEq;
                    break;
                case "<":
                    op = Compare.Lt;
                    break;
                case ">=":
                    op = Compare.GtE;
                    break;
                case "<=":
                    op = Compare.LtE;
                    break;
                case "in":
                    op = Compare.In;
                    break;
                case "not_in":
                    op = Compare.NotIn;
                    break;
                case "is":
                    op = Compare.Is;
                    break;
                case "is_not":
                    op = Compare.IsNot;
                    break;

            }
            return op;
        }

        private static class CascadeNode implements ISimpleNode {

            public final String operator;
            public final ISimpleNode operand;
            public final CascadeNode cascade;

            public CascadeNode(String operator, ISimpleNode operand, CascadeNode cascade) {
                this.operator = operator;
                this.operand = operand;
                this.cascade = cascade;
            }

        }

        private ISimpleNode createCascade(JsonObject asObject) {
            JsonValue operator = asObject.get("operator");
            JsonValue operand = asObject.get("operand2");
            JsonValue cascade = asObject.get("cascade");
            return new CascadeNode(operator.asString(), createNode(operand), (CascadeNode) createNode(cascade));
        }

        private exprType createExpr(JsonObject asObject) throws Exception {
            try {
                ISimpleNode node = createNode(asObject.get("expr"));
                exprType expr = asExpr(node);
                setLine(expr, asObject);
                return expr;
            } catch (Exception e) {
                log(e);
                return null;
            }
        }

        private SimpleNode createAwait(JsonObject asObject) {
            try {
                exprType node = asExpr(createNode(asObject.get("arg")));
                Await await = new Await(node);
                setLine(await, asObject);
                return await;
            } catch (Exception e) {
                log(e);
                return null;
            }
        }

        private Pass createPass(JsonObject asObject) {
            Pass pass = astFactory.createPass();
            setLine(pass, asObject);
            return pass;
        }

        private void setBases(JsonObject asObject, ClassDef classDef) {
            setBases(asObject, classDef, "bases");
        }

        private void setBases(JsonObject asObject, ClassDef classDef, String basesName) {
            List<exprType> bases = extractExprs(asObject, "bases");
            if (bases.size() == 1 && bases.get(0) instanceof org.python.pydev.parser.jython.ast.Tuple) {
                org.python.pydev.parser.jython.ast.Tuple tuple = (org.python.pydev.parser.jython.ast.Tuple) bases
                        .get(0);
                astFactory.setBases(classDef, (Object[]) tuple.elts);

            } else {
                astFactory.setBases(classDef, bases.toArray());
            }
        }

        private ClassDef createCEnumDef(JsonObject asObject) throws Exception {
            final JsonValue value = asObject.get("name");
            if (value != null && value.isString()) {
                ClassDef classDef = astFactory.createClassDef(value.asString());
                setLine(classDef, asObject);
                classDef.name.beginLine = classDef.beginLine;
                classDef.name.beginColumn = classDef.beginColumn + 7;

                List<Assign> nodes = new ArrayList<>();
                JsonValue items = asObject.get("items");
                if (items != null && items.isArray()) {
                    JsonArray asArray = items.asArray();
                    for (JsonValue item : asArray) {
                        if (item.isObject()) {
                            Name name = createName(item.asObject());
                            if (name != null) {
                                ctx.setStore(name);
                                Num num = new Num(new Integer(0), Num.Int, "0");
                                Assign assign = astFactory.createAssign(name, num);
                                setLine(assign, item.asObject());
                                setLine(num, item.asObject());
                                nodes.add(assign);
                            }
                        }
                    }
                }
                astFactory.setBody(classDef, nodes.toArray());

                classDef.decs = createDecorators(asObject);
                return classDef;
            }
            return null;
        }

        private ClassDef createClassDef(JsonObject asObject) throws Exception {
            final JsonValue value = asObject.get("name");
            if (value != null && value.isString()) {
                ClassDef classDef = astFactory.createClassDef(value.asString());
                setLine(classDef, asObject);
                classDef.name.beginLine = classDef.beginLine;
                classDef.name.beginColumn = classDef.beginColumn + 6;

                setBases(asObject, classDef);

                astFactory.setBody(classDef, extractStmts(asObject, "body").toArray());

                classDef.decs = createDecorators(asObject);
                return classDef;
            }
            return null;
        }

        private ISimpleNode createSizeofVar(JsonObject asObject) throws Exception {
            Call call = astFactory.createCall("sizeof");
            setLine(call.func, asObject);
            setLine(call, asObject);
            return call;
        }

        private ISimpleNode createSizeofType(JsonObject asObject) {
            Call call = astFactory.createCall("sizeof");
            setLine(call.func, asObject);
            setLine(call, asObject);
            return call;
        }

        private ISimpleNode createFusedType(JsonObject asObject) {
            Name name = createName(asObject);
            try {
                ctx.setStore(name);
            } catch (Exception e) {
            }
            JsonValue jsonValue = asObject.get("types");
            if (jsonValue != null && jsonValue.isArray()) {
                JsonValue val = jsonValue.asArray().get(0);
                ISimpleNode node = createNode(val);
                if (node == null) {
                    node = astFactory.createNone();
                }
                Assign assign = astFactory.createAssign(name, asExpr(node));
                setLine(assign, asObject);
                return assign;
            }
            return null;
        }

        private ISimpleNode createCSimpleBaseType(JsonObject asObject) {
            return createName(asObject);
        }

        private ISimpleNode createCtypeDef(JsonObject asObject) {
            final JsonValue declarator = getDeclarator(asObject);
            if (declarator.isObject()) {
                Name name = createName(declarator.asObject());
                if (name != null) {
                    try {
                        ctx.setStore(name);
                    } catch (Exception e) {
                    }
                    Name nameNode = createNameFromBaseType(asObject);
                    if (nameNode == null) {
                        nameNode = astFactory.createNone();
                    }
                    Assign assign = astFactory.createAssign(name, nameNode);
                    setLine(assign, asObject);
                    return assign;
                }
            }
            return null;
        }

        private ISimpleNode createCStructOrUnionDef(JsonObject asObject) {
            final JsonValue value = asObject.get("name");
            if (value != null && value.isString()) {
                ClassDef classDef = astFactory.createClassDef(value.asString());
                setLine(classDef, asObject);
                classDef.name.beginLine = classDef.beginLine;
                classDef.name.beginColumn = classDef.beginColumn + 6;

                astFactory.setBody(classDef, extractStmts(asObject, "attributes").toArray());
                return classDef;
            }
            return null;
        }

        private ClassDef createCppClass(JsonObject asObject) throws Exception {
            final JsonValue value = asObject.get("name");
            if (value != null && value.isString()) {
                ClassDef classDef = astFactory.createClassDef(value.asString());
                setLine(classDef, asObject);
                classDef.name.beginLine = classDef.beginLine;
                classDef.name.beginColumn = classDef.beginColumn + 6; // cdef class X

                setBases(asObject, classDef, "base_classes");

                astFactory.setBody(classDef, extractStmts(asObject, "attributes").toArray());

                classDef.decs = createDecorators(asObject);
                return classDef;
            }
            return null;
        }

        private ClassDef createCClassDef(JsonObject asObject) throws Exception {
            final JsonValue value = asObject.get("class_name");
            if (value != null && value.isString()) {
                ClassDef classDef = astFactory.createClassDef(value.asString());
                setLine(classDef, asObject);
                classDef.name.beginLine = classDef.beginLine;
                classDef.name.beginColumn = classDef.beginColumn + 6; // cdef class X

                setBases(asObject, classDef);

                astFactory.setBody(classDef, extractStmts(asObject, "body").toArray());

                classDef.decs = createDecorators(asObject);
                return classDef;
            }
            return null;
        }

        private ISimpleNode createLambda(JsonObject asObject) {
            Lambda lambda = new Lambda(null, null);
            lambda.args = createArgs(asObject);
            lambda.body = asExpr(createNode(asObject.get("result_expr")));
            setLine(lambda, asObject);
            return lambda;
        }

        private FunctionDef createFunctionDef(JsonObject asObject) throws Exception {
            final JsonValue value = asObject.get("name");
            if (value != null && value.isString()) {
                FunctionDef funcDef = astFactory.createFunctionDef(value.asString());
                setLine(funcDef, asObject);
                funcDef.name.beginLine = funcDef.beginLine;
                funcDef.name.beginColumn = funcDef.beginColumn + 4;

                funcDef.args = createArgs(asObject);
                funcDef.decs = createDecorators(asObject);

                JsonValue isAsyncDef = asObject.get("is_async_def");
                if (isAsyncDef != null && isAsyncDef.asString().equals("True")) {
                    funcDef.async = true;
                }
                astFactory.setBody(funcDef, extractStmts(asObject, "body").toArray());
                return funcDef;
            }
            return null;
        }

        private FunctionDef createCFuncDeclarator(JsonObject declarator) throws Exception {
            FunctionDef funcDef = null;
            JsonValue baseDeclarator = declarator.asObject().get("base");
            if (baseDeclarator != null) {
                JsonValue name = baseDeclarator.asObject().get("name");
                if (name != null) {
                    NameTokType nameTok = createNameTok(baseDeclarator.asObject(), NameTok.FunctionName);
                    funcDef = astFactory.createFunctionDef(nameTok);
                    setLine(funcDef, declarator);
                    funcDef.args = createArgs(declarator.asObject());
                }
            }
            return funcDef;
        }

        private FunctionDef createCFunctionDef(JsonObject asObject) throws Exception {
            final JsonValue declarator = getDeclarator(asObject);
            if (declarator != null && declarator.isObject()) {
                FunctionDef funcDef = createCFuncDeclarator(declarator.asObject());
                if (funcDef != null) {
                    funcDef.decs = createDecorators(asObject);
                    setLine(funcDef, asObject);
                    setLine(funcDef.name, asObject);
                    astFactory.setBody(funcDef, extractStmts(asObject, "body").toArray());
                }
                return funcDef;
            }
            return null;
        }

        public List<stmtType> extractStmts(JsonObject asObject, String field) {
            JsonValue jsonValue = asObject.get(field);
            ArrayList<JsonValue> bodyAsList = getBodyAsList(jsonValue);
            List<stmtType> lst = new ArrayList<>();
            for (JsonValue v : bodyAsList) {
                ISimpleNode bodyNode = createNode(v);
                addToStmtsList(bodyNode, lst);
            }
            return lst;
        }

        public List<exprType> extractExprs(JsonObject asObject, String field) {
            JsonValue jsonValue = asObject.get(field);
            ArrayList<JsonValue> bodyAsList = getBodyAsList(jsonValue);
            List<exprType> lst = new ArrayList<>();
            for (JsonValue v : bodyAsList) {
                ISimpleNode bodyNode = createNode(v);
                addToExprsList(bodyNode, lst);
            }
            return lst;
        }

        private decoratorsType[] createDecorators(JsonObject asObject) throws Exception {
            List<decoratorsType> decs = new ArrayList<decoratorsType>();
            JsonValue jsonValue = asObject.get("decorators");
            if (jsonValue != null && jsonValue.isArray()) {
                for (JsonValue v : jsonValue.asArray()) {
                    decoratorsType dec = createDecorator(v);
                    if (dec != null) {
                        decs.add(dec);
                    }
                }
            }
            if (decs.size() == 0) {
                return null;
            }
            return decs.toArray(new decoratorsType[0]);
        }

        private decoratorsType createDecorator(JsonValue v) throws Exception {
            if (v != null && v.isObject()) {
                JsonObject decAsObject = v.asObject();
                JsonValue decJsonValue = decAsObject.get("decorator");
                if (decJsonValue != null && decJsonValue.isObject()) {
                    JsonObject asObject = decJsonValue.asObject();
                    ISimpleNode func = createNode(asObject);
                    decoratorsType decorator = astFactory.createEmptyDecoratorsType();

                    if (func instanceof Call) {
                        Call call = (Call) func;
                        decorator.func = call.func;
                        decorator.args = call.args;
                        decorator.keywords = call.keywords;
                        decorator.starargs = call.starargs;
                        decorator.kwargs = call.kwargs;
                        decorator.isCall = true;

                    } else if (func instanceof exprType) {
                        decorator.func = (exprType) func;

                    } else {
                        if (func != null) {
                            log("Don't know how to create decorator from: " + func);
                        }
                    }
                    return decorator;
                }
            }
            return null;
        }

        private argumentsType createArgs(JsonObject funcAsObject) {
            argumentsType arguments = astFactory.createEmptyArgumentsType();
            JsonValue args = funcAsObject.get("args");
            if (args != null) {
                try {
                    List<exprType> argsList = new ArrayList<exprType>();
                    List<exprType> defaultsList = new ArrayList<exprType>();
                    List<exprType> annotationsList = new ArrayList<exprType>();

                    List<exprType> kwOnlyArgsList = new ArrayList<exprType>();
                    List<exprType> kwOnlyArgsDefaultsList = new ArrayList<exprType>();
                    List<exprType> kwOnlyArgsAnnotationsList = new ArrayList<exprType>();

                    for (JsonValue a : args.asArray()) {
                        JsonObject asObject = a.asObject();
                        JsonValue declaratorValue = getDeclarator(asObject);
                        if (declaratorValue != null) {
                            Name nameNode = null;
                            JsonObject declaratorAsObj = declaratorValue.asObject();
                            JsonValue nameValue = declaratorAsObj.get("name");
                            if (nameValue != null && !nameValue.asString().isEmpty()) {
                                nameNode = createName(declaratorAsObj);
                            }
                            if (nameNode == null) {
                                nameNode = createNameFromBaseType(asObject);
                            }
                            if (nameNode == null) {
                                // def method((a, b)): ...
                                List<Name> names = createNamesListFromTupleBaseType(asObject.get("base_type"));
                                if (names != null && names.size() > 0) {
                                    for (Name n : names) {
                                        ctx.setParam(n);
                                        argsList.add(n);
                                        defaultsList.add(null);
                                        annotationsList.add(null);
                                    }
                                    continue;
                                }
                            }
                            if (nameNode == null) {
                                log("Unable to get arg name in: " + asObject.toPrettyString());
                                continue;
                            }
                            // The declarator col is at the end, set it to the start.
                            setLine(nameNode, asObject);

                            boolean isKwOnly = false;
                            JsonValue kwOnlyValue = asObject.get("kw_only");
                            if (kwOnlyValue != null && kwOnlyValue.isString()) {
                                if ("1".equals(kwOnlyValue.asString())) {
                                    isKwOnly = true;
                                }
                            }

                            if (isKwOnly) {
                                ctx.setKwOnlyParam(nameNode);
                                kwOnlyArgsList.add(nameNode);
                                kwOnlyArgsDefaultsList.add((exprType) createNode(asObject.get("default")));
                                kwOnlyArgsAnnotationsList.add((exprType) createNode(asObject.get("annotation")));
                            } else {
                                ctx.setParam(nameNode);
                                argsList.add(nameNode);
                                defaultsList.add((exprType) createNode(asObject.get("default")));
                                annotationsList.add((exprType) createNode(asObject.get("annotation")));
                            }

                        }
                    }
                    arguments.kwonlyargs = kwOnlyArgsList.toArray(new exprType[0]);
                    arguments.kwonlyargannotation = kwOnlyArgsAnnotationsList.toArray(new exprType[0]);
                    arguments.kw_defaults = kwOnlyArgsDefaultsList.toArray(new exprType[0]);

                    arguments.args = argsList.toArray(new exprType[0]);
                    arguments.defaults = defaultsList.toArray(new exprType[0]);
                    arguments.annotation = annotationsList.toArray(new exprType[0]);
                } catch (Exception e) {
                    log(e);
                }
            }

            JsonValue starArgValue = funcAsObject.get("star_arg");
            if (starArgValue != null && starArgValue.isObject()) {
                arguments.vararg = createNameTok(starArgValue.asObject(), NameTok.VarArg);
            }

            JsonValue kwArgValue = funcAsObject.get("starstar_arg");
            if (kwArgValue != null && kwArgValue.isObject()) {
                arguments.kwarg = createNameTok(kwArgValue.asObject(), NameTok.KwArg);
            }
            return arguments;
        }

        private List<Name> createNamesListFromTupleBaseType(JsonValue jsonValueBaseType) {
            List<Name> names = new ArrayList<>();
            if (jsonValueBaseType != null && jsonValueBaseType.isObject()) {
                JsonObject asObject = jsonValueBaseType.asObject();
                JsonValue node = asObject.get("__node__");
                if (node != null && node.isString() && node.asString().equals("CTupleBaseType")) {
                    JsonValue components = asObject.get("components");
                    if (components != null && components.isArray()) {
                        JsonArray asArray = components.asArray();
                        for (JsonValue jsonValue : asArray) {
                            if (jsonValue.isObject()) {
                                Name name = createNameFromBaseType(jsonValue.asObject());
                                if (name != null) {
                                    names.add(name);
                                }
                            }
                        }
                    }
                }
            }
            return names;
        }

        private Name createNameFromBaseType(JsonObject asObject) {
            Name nameNode = null;
            JsonValue baseType = asObject.get("base_type");
            if (baseType != null && baseType.isObject()) {
                nameNode = createName(baseType.asObject());
                if (nameNode == null) {
                    JsonValue baseTypeNode = baseType.asObject().get("base_type_node");
                    if (baseTypeNode != null && baseTypeNode.isObject()) {
                        nameNode = createName(baseTypeNode.asObject());
                    }
                }
            } else {
                JsonValue baseTypeNode = asObject.get("base_type_node");
                if (baseTypeNode != null && baseTypeNode.isObject()) {
                    nameNode = createName(baseTypeNode.asObject());
                }

            }

            if (nameNode == null && baseType.isObject()) {
                JsonValue jsonValue = baseType.asObject().get("base_type");
                if (jsonValue != null && jsonValue.isObject()) {
                    return createNameFromBaseType(baseType.asObject());
                }
            }
            return nameNode;
        }

        private ArrayList<JsonValue> getBodyAsList(JsonValue jsonValue) {
            ArrayList<JsonValue> lst = new ArrayList<JsonValue>();
            if (jsonValue == null) {
                return lst;
            }

            if (jsonValue.isArray()) {
                for (JsonValue v : jsonValue.asArray()) {
                    lst.add(v);
                }
                return lst;
            }

            if (jsonValue.isString() && jsonValue.asString().equals("None")) {
                return lst;
            }
            JsonObject asObject = jsonValue.asObject();
            final JsonValue nodeType = asObject.get("__node__");
            if (nodeType != null) {
                if ("StatList".equals(nodeType.asString())) {
                    JsonValue stats = asObject.get("stats");
                    JsonArray asArray = stats.asArray();
                    for (JsonValue v : asArray) {
                        lst.add(v);
                    }
                } else {
                    // If it has a single element it may not be in a list.
                    lst.add(jsonValue);
                }
            }
            return lst;
        }

        private SimpleNode createForFromStat(JsonObject asObject) throws Exception {
            exprType target = null;
            exprType iter = null;
            stmtType[] body = null;
            suiteType orelse = null;

            JsonValue jsonTarget = asObject.get("target");
            if (jsonTarget != null) {
                target = asExpr(createNode(jsonTarget));
                ctx.setStore(target);
            }

            // Note: we don't provide an iterator for it.

            JsonValue jsonElse = asObject.get("else_clause");
            if (jsonElse != null && jsonElse.isObject()) {
                orelse = createSuite(jsonElse.asObject());
            }

            body = extractStmts(asObject, "body").toArray(new stmtType[0]);

            For node = new For(target, iter, body, orelse, false);
            setLine(node, asObject);
            return node;
        }

        private SimpleNode createFor(JsonObject asObject) throws Exception {
            exprType target = null;
            exprType iter = null;
            stmtType[] body = null;
            suiteType orelse = null;
            JsonValue isAsyncNode = asObject.get("is_async");
            boolean async = isAsyncNode != null && "True".equals(isAsyncNode.asString());

            JsonValue jsonTarget = asObject.get("target");
            if (jsonTarget != null) {
                target = asExpr(createNode(jsonTarget));
                ctx.setStore(target);
            }
            JsonValue jsonIter = asObject.get("iterator");
            if (jsonIter != null) {
                JsonValue jsonValue = jsonIter.asObject().get("sequence");
                iter = asExpr(createNode(jsonValue));
            }
            JsonValue jsonElse = asObject.get("else_clause");
            if (jsonElse != null && jsonElse.isObject()) {
                orelse = createSuite(jsonElse.asObject());
            }

            body = extractStmts(asObject, "body").toArray(new stmtType[0]);

            For node = new For(target, iter, body, orelse, async);
            setLine(node, asObject);
            return node;
        }

        private suiteType createSuite(JsonObject asObject) {
            JsonValue jsonNode = asObject.get("__node__");
            Suite suite = null;
            if (jsonNode != null && jsonNode.asString().equals("StatList")) {
                List<stmtType> extract = extractStmts(asObject, "stats");
                suite = new Suite(astFactory.createStmtArray(extract.toArray()));
                setLine(suite, asObject);

            } else {
                ISimpleNode node = createNode(asObject);
                if (node != null) {
                    ArrayList<stmtType> lst = new ArrayList<>();
                    addToStmtsList(node, lst);
                    suite = new Suite(lst.toArray(new stmtType[0]));
                }
            }
            return suite;
        }

        private SimpleNode createCascadedAssignment(JsonObject asObject) throws Exception {
            SimpleNode node = null;
            JsonValue lhsList = asObject.get("lhs_list");
            JsonValue rhs = asObject.get("rhs");
            if (lhsList != null && lhsList.isArray() && rhs != null && rhs.isObject()) {
                JsonArray asArray = lhsList.asArray();
                List<exprType> targets = new ArrayList<>(asArray.size());

                for (JsonValue lhs : asArray) {
                    ISimpleNode left = createNode(lhs);
                    ctx.setStore((SimpleNode) left);
                    targets.add(asExpr(left));
                }

                ISimpleNode right = createNode(rhs);
                node = new Assign(targets.toArray(new exprType[0]), asExpr(right), null);
                setLine(node, asObject);
            }
            return node;
        }

        private SimpleNode createSingleAssignment(JsonObject asObject) throws Exception {
            SimpleNode node = null;
            JsonValue lhs = asObject.get("lhs");
            JsonValue rhs = asObject.get("rhs");
            if (lhs != null && lhs.isObject() && rhs != null && rhs.isObject()) {
                ISimpleNode left = createNode(lhs);
                ctx.setStore((SimpleNode) left);

                ISimpleNode right = createNode(rhs);

                if (right instanceof Import) {
                    // i.e.: in cython it assigns an import to a name.
                    node = (Import) right;
                    setLine(node, asObject);

                    if (left instanceof Name) {
                        Name leftName = (Name) left;
                        aliasType[] names = ((Import) right).names;
                        aliasType aliasType = names[0];

                        if (aliasType.asname != null) {
                            aliasType.asname = new NameTok(leftName.id, NameTok.ImportName);
                            aliasType.asname.beginColumn = leftName.beginColumn;
                            aliasType.asname.beginLine = leftName.beginLine;
                        }

                    }
                } else {
                    node = astFactory.createAssign(asExpr(left), asExpr(right));
                    setLine(node, asObject);
                }
            }
            return node;
        }

        private static class NodeList implements ISimpleNode {
            public List<ISimpleNode> nodes = new ArrayList<ISimpleNode>();
        }

        private static class MergedDict implements ISimpleNode {
            public List<ISimpleNode> nodes = new ArrayList<ISimpleNode>();
        }

        private JsonValue getDeclarator(JsonObject asObject) {
            JsonValue jsonValue = asObject.get("declarator");
            if (jsonValue.isObject()) {
                return getRemovingPtrDeclarator(jsonValue.asObject());
            }
            return jsonValue;
        }

        private JsonValue getRemovingPtrDeclarator(JsonObject asObject) {
            JsonValue jsonValue2 = asObject.get("__node__");
            if (jsonValue2.isString()) {
                if ("CPtrDeclarator".equals(jsonValue2.asString())) {
                    JsonValue base = asObject.get("base");
                    if (base != null && base.isObject()) {
                        return getRemovingPtrDeclarator(base.asObject());
                    }
                }
            }
            return asObject;
        }

        private ISimpleNode createCDefExtern(JsonObject asObject) throws Exception {
            NodeList nodeList = new NodeList();
            List<stmtType> extractStmts = extractStmts(asObject, "body");
            for (stmtType stmtType : extractStmts) {
                nodeList.nodes.add(stmtType);
            }
            return nodeList;
        }

        private ISimpleNode createCVarDef(JsonObject asObject) throws Exception {
            NodeList nodeList = new NodeList();
            JsonValue declarators = asObject.get("declarators");
            if (declarators != null && declarators.isArray()) {
                for (JsonValue d : declarators.asArray()) {
                    JsonValue declaratorWithoutPtr = getRemovingPtrDeclarator(d.asObject());
                    if (declaratorWithoutPtr.isObject()) {
                        JsonObject declaratorAsObject = declaratorWithoutPtr.asObject();
                        JsonValue nodeValue = declaratorAsObject.get("__node__");
                        if (nodeValue != null && nodeValue.isString()
                                && nodeValue.asString().equals("CFuncDeclarator")) {
                            nodeList.nodes.add(createNode(declaratorAsObject));

                        } else {
                            Name left = createName(declaratorAsObject.asObject());
                            exprType right = null;
                            if (left != null) {
                                JsonValue defaultJsonValue = declaratorAsObject.get("default");
                                if (defaultJsonValue == null
                                        || (defaultJsonValue.isString()
                                                && defaultJsonValue.asString().equals("None"))) {
                                    right = astFactory.createNone();
                                    setLine(right, declaratorAsObject);
                                } else {
                                    right = asExpr(createNode(defaultJsonValue));
                                }
                                ctx.setStore(left);
                                if (right == null) {
                                    right = astFactory.createNone();
                                    setLine(right, declaratorAsObject);
                                }
                                Assign node = astFactory.createAssign(left, right);
                                setLine(node, declaratorAsObject);
                                nodeList.nodes.add(node);
                            } else {
                                return null;
                                // log("Could not create name from: " + d.toPrettyString());
                            }
                        }

                    }
                }
            }
            return nodeList;
        }

        private With createGILStat(JsonObject asObject) {
            Name nogil = astFactory.createName("nogil");
            setLine(nogil, asObject);

            stmtType[] body = extractStmts(asObject, "body").toArray(new stmtType[0]);
            suiteType bodySuite = new Suite(body);
            setLine(bodySuite, asObject);

            WithItemType[] withItem = new WithItem[] { new WithItem(nogil, null) };
            setLine(withItem[0], asObject);

            boolean async = false;
            With node = new With(withItem, bodySuite, async);
            setLine(node, asObject);
            return node;
        }

        private With createWith(JsonObject asObject) {
            exprType test = asExpr(createNode(asObject.get("manager")));
            exprType target = asExpr(createNode(asObject.get("target")));

            try {
                ctx.setStore(target);
            } catch (Exception e) {
            }
            stmtType[] body = extractStmts(asObject, "body").toArray(new stmtType[0]);

            suiteType bodySuite = new Suite(body);
            setLine(bodySuite, asObject);

            WithItemType[] withItem = new WithItem[] { new WithItem(test, target) };
            setLine(withItem[0], asObject);

            boolean async = false;
            With node = new With(withItem, bodySuite, async);
            setLine(node, asObject);
            return node;
        }

        private While createWhile(JsonObject asObject) {
            exprType test = asExpr(createNode(asObject.get("condition")));
            stmtType[] body = extractStmts(asObject, "body").toArray(new stmtType[0]);
            suiteType orelse = null;

            List<stmtType> extractStmts = extractStmts(asObject, "else_clause");
            if (extractStmts.size() > 0) {
                orelse = new Suite(extractStmts.toArray(new stmtType[0]));
            }

            While whileStmt = new While(test, body, orelse);
            setLine(whileStmt, asObject);
            return whileStmt;
        }

        private Assert createAssert(JsonObject asObject) {
            exprType cond = asExpr(createNode(asObject.get("cond")));
            exprType value = asExpr(createNode(asObject.get("value")));

            Assert assertStmt = new Assert(cond, value);
            setLine(assertStmt, asObject);
            return assertStmt;
        }

        private Return createReturn(JsonObject asObject) {
            exprType value = asExpr(createNode(asObject.get("value")));

            Return returnStmt = new Return(value);
            setLine(returnStmt, asObject);
            return returnStmt;
        }

        private IfExp createCondExpr(JsonObject asObject) {
            exprType test = asExpr(createNode(asObject.get("test")));
            exprType body = asExpr(createNode(asObject.get("true_val")));
            exprType orelse = asExpr(createNode(asObject.get("false_val")));

            IfExp ifNode = new IfExp(test, body, orelse);
            setLine(ifNode, asObject);
            return ifNode;
        }

        private If createIf(JsonObject asObject) {
            JsonValue ifClauses = asObject.get("if_clauses");
            If ifNode = null;
            If lastIfNode = null;
            if (ifClauses != null && ifClauses.isArray()) {
                for (JsonValue v : ifClauses.asArray()) {
                    JsonObject ifValueAsObject = v.asObject();
                    JsonValue conditionNodeValue = ifValueAsObject.get("condition");
                    if (conditionNodeValue != null) {
                        ISimpleNode conditionNode = createNode(conditionNodeValue);
                        suiteType orelse = null;
                        stmtType[] body = null;
                        If ifNodeTemp = new If(asExpr(conditionNode), body, orelse);
                        astFactory.setBody(ifNodeTemp, extractStmts(ifValueAsObject, "body").toArray());
                        setLine(ifNodeTemp, ifValueAsObject);

                        if (ifNode == null) {
                            ifNode = ifNodeTemp;
                        } else if (lastIfNode != null) {
                            lastIfNode.orelse = new Suite(new stmtType[] { ifNodeTemp });
                        }
                        lastIfNode = ifNodeTemp;
                    }
                }

                if (lastIfNode != null) {
                    JsonValue jsonValue = asObject.get("else_clause");
                    if (jsonValue != null && jsonValue.isObject()) {
                        ISimpleNode elseClause = createNode(jsonValue);
                        List<stmtType> elseStmts = new ArrayList<>();
                        addToStmtsList(elseClause, elseStmts);
                        lastIfNode.orelse = new Suite(elseStmts.toArray(new stmtType[0]));
                    }
                }
            }
            return ifNode;
        }

        private Name createNone(JsonObject asObject) {
            Name node = astFactory.createNone();
            setLine(node, asObject);
            return node;
        }

        private SimpleNode createBool(JsonObject asObject) {
            JsonValue value = asObject.get("value");
            if (value != null && value.isString()) {
                Name node = astFactory.createName(value.asString());
                node.reserved = true;
                setLine(node, asObject);
                return node;
            }
            log("Unable to create bool with info: " + asObject);
            return null;
        }

        private NameTok createNameTok(JsonObject asObject, int ctx) {
            NameTok node = null;
            JsonValue value;
            value = asObject.get("name");
            if (value != null) {
                node = new NameTok(value.asString(), ctx);
                setLine(node, asObject);
            } else {
                JsonValue nodeValue = asObject.get("__node__");
                if (nodeValue.isString() && "IdentifierString".equals(nodeValue.asString())) {
                    value = asObject.get("value");
                    if (value != null) {
                        node = new NameTok(value.asString(), ctx);
                        setLine(node, asObject);
                    }
                }
            }
            return node;
        }

        private Attribute createAttribute(JsonObject asObject) {
            JsonValue attribute = asObject.get("attribute");
            if (attribute.isString()) {
                String attr = attribute.asString();
                JsonValue obj = asObject.get("obj");
                if (obj.isObject()) {
                    ISimpleNode objNode = createNode(obj.asObject());

                    NameTok attribName = new NameTok(attr, NameTok.Attrib);
                    setLine(attribName, asObject);
                    // The column for cython starts at the dot, so, we need to update
                    // it for the real pos.
                    attribName.beginColumn += 1;

                    Attribute attributeNode = new Attribute(
                            asExpr(objNode), attribName, Attribute.Load);
                    setLine(attributeNode, asObject);
                    return attributeNode;
                }
            }
            return null;
        }

        private NodeList createStatList(JsonObject asObject) {
            JsonValue stats = asObject.get("stats");
            if (stats != null && stats.isArray()) {
                JsonArray asArray = stats.asArray();
                NodeList nodeList = new NodeList();
                for (JsonValue v : asArray) {
                    ISimpleNode n = createNode(v);
                    nodeList.nodes.add(n);
                }
                return nodeList;
            }
            return null;
        }

        private Name createName(JsonObject asObject) {
            Name node = null;
            JsonValue value;
            value = asObject.get("name");
            if (value != null) {
                node = astFactory.createName(value.asString());
                setLine(node, asObject);
            }
            return node;
        }

        private Name createIdentifierString(JsonObject asObject) {
            Name node = null;
            JsonValue value;
            value = asObject.get("value");
            if (value != null) {
                node = astFactory.createName(value.asString());
                setLine(node, asObject);
            }
            return node;
        }

        private ISimpleNode createAmpersand(JsonObject asObject) {
            Name node = null;
            JsonValue value;
            value = asObject.get("operand");
            if (value != null && value.isObject()) {
                return createNode(value.asObject());
            }
            return node;
        }

        private Yield createYieldFromExpr(JsonObject asObject) throws Exception {
            Yield yield = createYieldExpr(asObject);
            yield.yield_from = true;
            return yield;
        }

        private Yield createYieldExpr(JsonObject asObject) throws Exception {
            Yield node = null;
            JsonValue value = asObject.get("arg");
            if (value != null) {
                node = new Yield(asExpr(createNode(value)), false);
                setLine(node, asObject);
            }
            return node;
        }

        private SimpleNode createInt(JsonObject asObject) {
            JsonValue value = asObject.get("value");
            if (value != null) {
                String asString = value.asString();
                Num node;
                try {
                    node = new Num(new java.math.BigInteger(asString), Num.Int, asString);
                } catch (Exception e) {
                    node = new Num(asString, Num.Int, asString);
                }
                setLine(node, asObject);
                return node;
            }
            return null;
        }

        private ISimpleNode createImag(JsonObject asObject) {
            JsonValue value = asObject.get("value");
            if (value != null) {
                String asString = value.asString();
                Num node;
                try {
                    node = new Num(Double.valueOf(asString), Num.Comp, asString);
                } catch (NumberFormatException e) {
                    // i.e.: could be nan.
                    node = new Num(asString, Num.Comp, asString);
                }
                setLine(node, asObject);
                return node;
            }
            return null;
        }

        private SimpleNode createFloat(JsonObject asObject) {
            JsonValue value = asObject.get("value");
            if (value != null) {
                String asString = value.asString();
                Num node;
                try {
                    node = new Num(Double.valueOf(asString), Num.Float, asString);
                } catch (NumberFormatException e) {
                    // i.e.: could be nan.
                    node = new Num(asString, Num.Float, asString);
                }
                setLine(node, asObject);
                return node;
            }
            return null;
        }

        private void setLine(SimpleNode node, JsonObject asObject) {
            if (node != null) {
                JsonValue line = asObject.get("line");
                if (line != null) {
                    node.beginLine = line.asInt();
                }
                JsonValue col = asObject.get("col");
                if (col != null) {
                    node.beginColumn = col.asInt() + 1;
                }
            }
        }

        public ISimpleNode createModule() {
            return astFactory.createModule(stmts);
        }

    }

    private ParseOutput jsonToParseOutput(ParserInfo p, String cythonJson, long modifiedTime) {
        JsonValue json = JsonValue.readFrom(cythonJson);
        JsonObject asObject = json.asObject();

        JsonValue errors = asObject.get("errors");

        ParseException exc = null;
        for (JsonValue v : errors.asArray()) {
            JsonObject objError = v.asObject();
            JsonValue lineValue = objError.get("line");
            JsonValue colValue = objError.get("col");
            JsonValue messageValue = objError.get("message_only");
            exc = new ParseException(messageValue.asString(), lineValue.asInt(), colValue.asInt());
        }

        JsonValue ast = asObject.get("ast");
        if (ast == null || !ast.isObject()) {
            ParseOutput parseOutput = new ParseOutput(null, exc, modifiedTime);
            parseOutput.isCython = true;
            return parseOutput;
        } else {
            JsonValue body = ast.asObject().get("stats");
            if (body != null && body.isArray()) {
                // System.out.println(body.toPrettyString());
                JsonToNodesBuilder builder = new JsonToNodesBuilder(p);
                JsonArray asArray = body.asArray();
                Iterator<JsonValue> iterator = asArray.iterator();
                while (iterator.hasNext()) {
                    JsonValue node = iterator.next();
                    try {
                        builder.addStatement(node);
                    } catch (Exception e) {
                        log("Error converting cython json to ast: " + node, e);
                    }
                }
                ISimpleNode mod = builder.createModule();
                ParseOutput parseOutput = new ParseOutput(mod, null, modifiedTime);
                parseOutput.isCython = true;
                return parseOutput;
            }
        }
        return null;
    }

    public static class StopOnLogException extends RuntimeException {

        public StopOnLogException(String s) {
            super(s);
        }

        public StopOnLogException(Exception e) {
            super(e);
        }

        public StopOnLogException(String s, Exception e) {
            super(s, e);
        }

        private static final long serialVersionUID = 1L;

    }

    private static void log(String s) {
        if (IN_TESTS) {
            Log.log(s);
            throw new StopOnLogException(s);
        }
    }

    private static void log(Exception e) {
        if (IN_TESTS) {
            Log.log(e);
            throw new StopOnLogException(e);
        }
    }

    private static void log(String s, Exception e) {
        if (IN_TESTS) {
            Log.log(s, e);
            throw new StopOnLogException(s, e);
        }
    }

    public ParseOutput genCythonAst() {
        long modifiedTime = ((IDocumentExtension4) parserInfo.document).getModificationStamp();
        String cythonJson = genCythonJson();
        return jsonToParseOutput(parserInfo, cythonJson, modifiedTime);
    }

    public String genCythonJson() {
        try {
            SystemPythonNature nature = new SystemPythonNature(InterpreterManagersAPI.getPythonInterpreterManager());
            CythonShell serverShell = (CythonShell) AbstractShell.getServerShell(nature,
                    CompletionProposalFactory.get().getCythonShellId());
            String contents = parserInfo.document.get();
            return serverShell.convertToJsonAst(StringUtils.replaceNewLines(contents, "\n"));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
