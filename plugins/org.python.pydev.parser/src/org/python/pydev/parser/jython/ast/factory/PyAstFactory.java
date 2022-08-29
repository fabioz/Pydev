package org.python.pydev.parser.jython.ast.factory;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.keywordType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.FullRepIterable;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.FastStack;

public class PyAstFactory {

    NodeHelper nodeHelper;

    public PyAstFactory(AdapterPrefs adapterPrefs) {
        nodeHelper = new NodeHelper(adapterPrefs);
    }

    public static FunctionDef createFunctionDef(String name) {
        return createFunctionDef(new NameTok(name, NameTok.FunctionName));
    }

    public static FunctionDef createFunctionDef(NameTokType name) {
        return createFunctionDefFull(name, null, null, null, null, false);
    }

    public static FunctionDef createFunctionDefEmptyArrays(NameTokType nameTok, argumentsType args) {
        return createFunctionDefFull(nameTok, args, EMPTY_STMT_TYPE, EMPTY_DECORATORS_TYPE, null, false);
    }

    public static FunctionDef createFunctionDefFull(NameTokType name, argumentsType args, stmtType[] body,
            decoratorsType[] decs, exprType returns, boolean async) {
        return new FunctionDef(decs, name, args, returns, body, async);
    }

    public static final exprType[] EMPTY_EXPR_TYPE = new exprType[0];
    public static final keywordType[] EMPTY_KEYWORD_TYPE = new keywordType[0];
    public static final stmtType[] EMPTY_STMT_TYPE = new stmtType[0];
    public static final decoratorsType[] EMPTY_DECORATORS_TYPE = new decoratorsType[0];

    public argumentsType createEmptyArgumentsType() {
        exprType[] args = EMPTY_EXPR_TYPE;
        NameTokType vararg = null;
        NameTokType kwarg = null;
        exprType[] defaults = EMPTY_EXPR_TYPE;
        exprType[] kwonlyargs = EMPTY_EXPR_TYPE;
        exprType[] kw_defaults = EMPTY_EXPR_TYPE;
        exprType[] annotation = EMPTY_EXPR_TYPE;
        exprType varargannotation = null;
        exprType kwargannotation = null;
        exprType[] kwonlyargannotation = EMPTY_EXPR_TYPE;
        argumentsType argsType = new argumentsType(args,
                vararg,
                kwarg,
                defaults,
                kwonlyargs,
                kw_defaults,
                annotation,
                varargannotation,
                kwargannotation,
                kwonlyargannotation);
        return argsType;
    }

    public ClassDef createClassDef(String name) {
        exprType[] bases = EMPTY_EXPR_TYPE;
        stmtType[] body = EMPTY_STMT_TYPE;
        decoratorsType[] decs = null;
        keywordType[] keywords = EMPTY_KEYWORD_TYPE;
        exprType starargs = null;
        exprType kwargs = null;

        ClassDef def = new ClassDef(new NameTok(name, NameTok.ClassName), bases, body, decs, keywords, starargs,
                kwargs);
        return def;

    }

    public void setBaseClasses(ClassDef classDef, String... baseClasses) {
        ArrayList<exprType> bases = new ArrayList<exprType>();
        for (String s : baseClasses) {
            Name n = createName(s);
            bases.add(n);
        }
        classDef.bases = bases.toArray(new exprType[bases.size()]);
    }

    public Name createName(String s) {
        Name name = new Name(s, Name.Load, false);
        return name;
    }

    public Name createStoreName(String s) {
        Name name = new Name(s, Name.Store, false);
        return name;
    }

    public FunctionDef createSetterFunctionDef(String accessorName, String attributeName) {
        NameTok functionName = new NameTok(accessorName, NameTok.FunctionName);
        argumentsType args = createArguments(true, "value");
        stmtType[] body = createSetterBody(attributeName);

        return createFunctionDefFull(functionName, args, body, null, null, false);
    }

    public argumentsType createArguments(boolean addSelf, String... simpleParams) {
        List<exprType> params = new ArrayList<exprType>();

        if (addSelf) {
            params.add(new Name("self", Name.Param, true));
        }

        for (String s : simpleParams) {
            params.add(new Name(s, Name.Param, false));
        }

        return new argumentsType(params.toArray(new exprType[params.size()]), null, null, null, null, null, null, null,
                null, null);
    }

    private stmtType[] createSetterBody(String attributeName) {
        Name self = new Name("self", Name.Load, true);
        NameTok name = new NameTok(nodeHelper.getPrivateAttr(attributeName), NameTok.Attrib);
        Attribute attribute = new Attribute(self, name, Attribute.Store);

        Name value = new Name("value", Name.Load, false);
        Assign assign = new Assign(new exprType[] { attribute }, value, null);

        return new stmtType[] { assign };
    }

    public Call createCall(String call, String... params) {
        List<exprType> lst = createParamsList(params);
        return createCall(call, lst, new keywordType[0], null, null);
    }

    public List<exprType> createParamsList(String... params) {
        ArrayList<exprType> lst = new ArrayList<exprType>();
        for (String p : params) {
            lst.add(new Name(p, Name.Param, false));
        }
        return lst;
    }

    public Call createCall(String call, List<exprType> params, keywordType[] keywords, exprType starargs,
            exprType kwargs) {
        exprType[] array = params != null ? params.toArray(new Name[params.size()]) : new exprType[0];
        if (call.indexOf(".") != -1) {
            return new Call(createAttribute(call), array, keywords, starargs, kwargs);
        }
        return new Call(new Name(call, Name.Load, false), array, keywords, starargs, kwargs);
    }

    public Call createCall(exprType name, List<exprType> params, keywordType[] keywords, exprType starargs,
            exprType kwargs) {
        exprType[] array = params != null ? params.toArray(new exprType[0]) : new exprType[0];
        return new Call(name, array, keywords, starargs, kwargs);
    }

    public Attribute createAttribute(String attribute) {
        List<String> splitted = StringUtils.split(attribute, '.');
        if (splitted.size() <= 1) {
            throw new RuntimeException("Cannot create attribute without dot access.");
        }
        if (splitted.size() == 2) {
            return new Attribute(new Name(splitted.get(0), Name.Load, false), new NameTok(splitted.get(1),
                    NameTok.Attrib), Attribute.Load);
        }
        //>2
        return new Attribute(createAttribute(FullRepIterable.getWithoutLastPart(attribute)), new NameTok(
                splitted.get(splitted.size() - 1), NameTok.Attrib), Attribute.Load);

    }

    public Assign createAssign(exprType... targetsAndVal) {
        exprType[] targets = new exprType[targetsAndVal.length - 1];
        System.arraycopy(targetsAndVal, 0, targets, 0, targets.length);
        exprType value = targetsAndVal[targetsAndVal.length - 1];
        return new Assign(targets, value, null);
    }

    public void setBody(FunctionDef functionDef, Object... body) {
        functionDef.body = createStmtArray(body);
    }

    public void setBody(TryFinally tryFinally, Object... body) {
        tryFinally.body = createStmtArray(body);
    }

    public void setBody(TryExcept tryExcept, Object... body) {
        tryExcept.body = createStmtArray(body);
    }

    public void setFinally(TryFinally tryFinally, Object... body) {
        tryFinally.finalbody = new Suite(createStmtArray(body));
    }

    public void setBody(If ifNode, Object... body) {
        ifNode.body = createStmtArray(body);
    }

    public void setBody(ClassDef def, Object... body) {
        def.body = createStmtArray(body);
    }

    public void setBases(ClassDef classDef, Object... bases) {
        classDef.bases = createExprArray(bases);
    }

    public exprType[] createExprArray(Object... body) {
        ArrayList<exprType> newBases = new ArrayList<exprType>(body.length);
        for (Object b : body) {
            exprType expr = asExpr(b);
            if (expr != null) {
                newBases.add(expr);
            }
        }
        return newBases.toArray(new exprType[0]);
    }

    public exprType asExpr(Object node) {
        if (node instanceof exprType) {
            return (exprType) node;
        } else if (node instanceof Expr) {
            return ((Expr) node).value;
        } else if (node == null) {
            return null;
        } else {
            Log.log("Unhandled: " + node);
        }
        return null;
    }

    public stmtType[] createStmtArray(Object... body) {
        ArrayList<stmtType> newBody = new ArrayList<stmtType>();

        for (Object o : body) {
            stmtType asStmt = asStmt(o);
            if (asStmt != null) {
                newBody.add(asStmt);
            }
        }
        stmtType[] bodyArray = newBody.toArray(new stmtType[newBody.size()]);
        return bodyArray;
    }

    public stmtType asStmt(Object o) {
        if (o instanceof exprType) {
            return new Expr((exprType) o);
        } else if (o instanceof stmtType) {
            return (stmtType) o;
        } else if (o == null) {
            return null;
        } else {
            Log.log("Unhandled: " + o);
        }
        return null;
    }

    public Str createString(String string) {
        return new Str(string, Str.TripleSingle, false, false, false, false, null);
    }

    public Pass createPass() {
        return new Pass();
    }

    private static final RuntimeException stopVisitingException = new RuntimeException("stop visiting");

    /**
     * @param functionDef the function for the override body
     * @param currentClassName
     */
    public stmtType createOverrideBody(FunctionDef functionDef, String parentClassName, String currentClassName) {
        //create a copy because we do not want to retain the original line/col and we may change the originals here.
        final boolean[] addReturn = new boolean[] { false };
        if (functionDef.returns != null) {
            String rep = NodeUtils.getRepresentationString(functionDef.returns);
            addReturn[0] = !("None".equals(rep));
        } else {
            VisitorBase visitor = new VisitorBase() {

                @Override
                public Object visitClassDef(ClassDef node) throws Exception {
                    return null;
                }

                @Override
                public Object visitFunctionDef(FunctionDef node) throws Exception {
                    return null; //don't visit internal scopes.
                }

                @Override
                protected Object unhandled_node(SimpleNode node) throws Exception {
                    if (node instanceof Return) {
                        addReturn[0] = true;
                        throw stopVisitingException;
                    }
                    return null;
                }

                @Override
                public void traverse(SimpleNode node) throws Exception {
                    node.traverse(this);
                }
            };
            try {
                visitor.traverse(functionDef);
            } catch (Exception e) {
                if (e != stopVisitingException) {
                    Log.log(e);
                }
            }
        }

        boolean isClassMethod = false;
        if (functionDef.decs != null) {
            for (decoratorsType dec : functionDef.decs) {
                String rep = NodeUtils.getRepresentationString(dec.func);
                if ("classmethod".equals(rep)) {
                    isClassMethod = true;
                    break;
                }
            }
        }

        argumentsType args = functionDef.args.createCopy(false);
        List<exprType> params = new ArrayList<exprType>();
        for (exprType expr : args.args) { //note: self should be there already!
            params.add(expr);
        }

        exprType starargs = args.vararg != null ? new Name(((NameTok) args.vararg).id, Name.Load, false) : null;
        exprType kwargs = args.kwarg != null ? new Name(((NameTok) args.kwarg).id, Name.Load, false) : null;
        List<keywordType> keywords = new ArrayList<keywordType>();
        if (args.defaults != null) {
            int diff = args.args.length - args.defaults.length;

            FastStack<Integer> removePositions = new FastStack<Integer>(args.defaults.length);
            for (int i = 0; i < args.defaults.length; i++) {
                exprType expr = args.defaults[i];
                if (expr != null) {
                    exprType name = params.get(i + diff);
                    if (name instanceof Name) {
                        removePositions.push(i + diff); //it's removed backwards, that's why it's a stack
                        keywords.add(new keywordType(new NameTok(((Name) name).id, NameTok.KeywordName), name, false));
                    } else {
                        Log.log("Expected: " + name + " to be a Name instance.");
                    }
                }
            }
            while (removePositions.size() > 0) {
                Integer pop = removePositions.pop();
                params.remove((int) pop);
            }
        }
        Call call;
        if (isClassMethod && params.size() > 0) {
            //We need to use the super() construct
            //Something as:
            //Expr[value=
            //    Call[func=
            //        Attribute[value=
            //            Call[func=Name[id=super, ctx=Load, reserved=false], args=[Name[id=Current, ctx=Load, reserved=false], Name[id=cls, ctx=Load, reserved=false]], keywords=[], starargs=null, kwargs=null],
            //        attr=NameTok[id=test, ctx=Attrib], ctx=Load],
            //    args=[], keywords=[], starargs=null, kwargs=null]
            //]

            exprType firstParam = params.remove(0);

            Call innerCall = createCall("super", currentClassName, NodeUtils.getRepresentationString(firstParam));
            Attribute attr = new Attribute(innerCall, new NameTok(NodeUtils.getRepresentationString(functionDef),
                    NameTok.Attrib), Attribute.Load);
            call = new Call(attr, params.toArray(new Name[params.size()]), keywords.toArray(new keywordType[keywords
                    .size()]), starargs, kwargs);

        } else {
            call = createCall(parentClassName + "." + NodeUtils.getRepresentationString(functionDef), params,
                    keywords.toArray(new keywordType[keywords.size()]), starargs, kwargs);
        }
        if (addReturn[0]) {
            return new Return(call);
        } else {
            return new Expr(call);
        }
    }

    public Module createModule(List<stmtType> body) {
        return new Module(body.toArray(new stmtType[body.size()]));
    }

    public Name createNone() {
        Name node = createName("None");
        node.reserved = true;
        return node;
    }

    public Name createFalse() {
        Name node = createName("False");
        node.reserved = true;
        return node;
    }

    public Name createTrue() {
        Name node = createName("True");
        node.reserved = true;
        return node;
    }

    public decoratorsType createEmptyDecoratorsType() {
        exprType[] args = EMPTY_EXPR_TYPE;
        boolean isCall = false;
        exprType kwargs = null;
        exprType starargs = null;
        keywordType[] keywords = EMPTY_KEYWORD_TYPE;
        exprType func = null;
        return new decoratorsType(func, args, keywords, starargs, kwargs, isCall);
    }

}
