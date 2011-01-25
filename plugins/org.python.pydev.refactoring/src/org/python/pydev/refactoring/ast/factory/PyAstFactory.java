package org.python.pydev.refactoring.ast.factory;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.keywordType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.refactoring.ast.adapters.AdapterPrefs;
import org.python.pydev.refactoring.ast.visitors.NodeHelper;

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
    
    public void setBaseClasses(ClassDef classDef, String ... baseClasses){
        ArrayList<exprType> bases = new ArrayList<exprType>();
        for(String s: baseClasses){
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

        if(addSelf){
            params.add(new Name("self", Name.Param, true));
        }

        for(String s:simpleParams){
            params.add(new Name(s, Name.Param, false));
        }

        return new argumentsType(params.toArray(new exprType[params.size()]), null, null, null, null, null, null, null, null, null);
    }

    private stmtType[] createSetterBody(String attributeName) {
        Name self = new Name("self", Name.Load, true);
        NameTok name = new NameTok(nodeHelper.getPrivateAttr(attributeName), NameTok.Attrib);
        Attribute attribute = new Attribute(self, name, Attribute.Store);

        Name value = new Name("value", Name.Load, false);
        Assign assign = new Assign(new exprType[] { attribute }, value);

        return new stmtType[] { assign };
    }

    public Call createCall(String call, String ... params) {
        if(call.indexOf(".") != -1){
            ArrayList<Name> lst = new ArrayList<Name>();
            for(String p:params){
                lst.add(new Name(p, Name.Param, false));
            }
            return new Call(createAttribute(call), lst.toArray(new Name[lst.size()]), null, null, null);
        }
        throw new RuntimeException("Unhandled.");
    }

    public Attribute createAttribute(String attribute) {
        List<String> splitted = StringUtils.split(attribute, '.');
        if(splitted.size() != 2){
            throw new RuntimeException("Only handling attributes with 1 dot for now.");
        }
        
        return new Attribute(
                new Name(splitted.get(0), Name.Load, false), 
                new NameTok(splitted.get(1), NameTok.Attrib),
                Attribute.Load);
    }

    public Assign createAssign(exprType ... targetsAndVal) {
        exprType[] targets = new exprType[targetsAndVal.length-1];
        System.arraycopy(targetsAndVal, 0, targets, 0, targets.length);
        exprType value = targetsAndVal[targetsAndVal.length-1];
        return new Assign(targets, value);
    }

    public void setBody(FunctionDef functionDef, Object ... body) {
        functionDef.body = createStmtArray(body);
    }
    
    public void setBody(ClassDef def, Object ... body) {
        def.body = createStmtArray(body);
    }

    private stmtType[] createStmtArray(Object... body) {
        ArrayList<stmtType> newBody = new ArrayList<stmtType>();
        
        for(Object o:body){
            if(o instanceof exprType){
                newBody.add(new Expr((exprType) o));
            }else if(o instanceof stmtType){
                newBody.add((stmtType) o);
            }else{
                throw new RuntimeException("Unhandled: "+o);
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



}
