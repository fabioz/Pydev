package org.python.pydev.parser.jython.ast.factory;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.keywordType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.FastStack;

public class PyAstFactory {

    NodeHelper nodeHelper;

    public PyAstFactory(AdapterPrefs adapterPrefs) {
        nodeHelper = new NodeHelper(adapterPrefs);
    }

    public FunctionDef createFunctionDef(String name) {
        FunctionDef functionDef = new FunctionDef(new NameTok(name, NameTok.FunctionName), null, null, null, null);
        return functionDef;
    }

    public ClassDef createClassDef(String name) {
        exprType[] bases = null;
        stmtType[] body = null;
        decoratorsType[] decs = null;
        keywordType[] keywords = null;
        exprType starargs = null;
        exprType kwargs = null;

        ClassDef def = new ClassDef(new NameTok(name, NameTok.ClassName), bases, body, decs, keywords, starargs, kwargs);
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

    public FunctionDef createSetterFunctionDef(String accessorName, String attributeName) {
        NameTok functionName = new NameTok(accessorName, NameTok.FunctionName);
        argumentsType args = createArguments(true, "value");
        stmtType[] body = createSetterBody(attributeName);

        return new FunctionDef(functionName, args, body, null, null);
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
        Assign assign = new Assign(new exprType[] { attribute }, value);

        return new stmtType[] { assign };
    }

    public Call createCall(String call, String... params) {
        List<exprType> lst = createParamsList(params);
        return createCall(call, lst, null, null, null);
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
        return new Assign(targets, value);
    }

    public void setBody(FunctionDef functionDef, Object... body) {
        functionDef.body = createStmtArray(body);
    }

    public void setBody(ClassDef def, Object... body) {
        def.body = createStmtArray(body);
    }

    private stmtType[] createStmtArray(Object... body) {
        ArrayList<stmtType> newBody = new ArrayList<stmtType>();

        for (Object o : body) {
            if (o instanceof exprType) {
                newBody.add(new Expr((exprType) o));
            } else if (o instanceof stmtType) {
                newBody.add((stmtType) o);
            } else {
                throw new RuntimeException("Unhandled: " + o);
            }
        }
        stmtType[] bodyArray = newBody.toArray(new stmtType[newBody.size()]);
        return bodyArray;
    }

    public Str createString(String string) {
        return new Str(string, Str.TripleSingle, false, false, false);
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

}
