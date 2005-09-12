/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;


/**
 * This class contains additional information on an interpreter, so that we are able to make code-completion in
 * a context-insensitive way (and make additionally auto-import).
 * 
 * The information that is needed for that is the following:
 * 
 * - Classes that are available in the global context
 * - Methods that are available in the global context
 * 
 * We must access this information very fast, so the underlying structure has to take that into consideration.
 * 
 * It should not 'eat' too much memory because it should be all in memory at all times
 * 
 * It should also be easy to query it. 
 *      Some query situations include: 
 *          - which classes have the method xxx and yyy?
 *          - which methods and classes start with xxx?
 *          - is there any class or method with the name xxx?
 *      
 * The information must be persisted for reuse (and persisting and restoring it should be fast).
 * 
 * We need to store information for any interpreter, be it python, jython...
 * 
 * For creating and keeping this information up-to-date, we have to know when:
 * - the interpreter used changes (the InterpreterInfo should be passed after the change)
 * - some file changes (pydev_builder)
 * 
 * @author Fabio
 */
public class AdditionalInterpreterInfo {

    private List<IInfo> additionalInfo;

    public AdditionalInterpreterInfo(){
        additionalInfo = new ArrayList<IInfo>();
    }
    
    /**
     * adds a method to the definition
     */
    public void addMethod(FunctionDef def, String moduleDeclared) {
        FuncInfo info2 = FuncInfo.fromFunctionDef(def, moduleDeclared);
        additionalInfo.add(info2);
    }
    
    /**
     * Adds a class to the definition
     */
    public void addClass(ClassDef def, String moduleDeclared) {
        ClassInfo info = ClassInfo.fromClassDef(def, moduleDeclared);
        additionalInfo.add(info);
    }

    public void addClassOrFunc(SimpleNode classOrFunc, String moduleDeclared) {
        if(classOrFunc instanceof ClassDef){
            addClass((ClassDef) classOrFunc, moduleDeclared);
        }else{
            addMethod((FunctionDef) classOrFunc, moduleDeclared);
        }
    }

    public void removeInfoFromModule(String moduleName) {
        for (Iterator<IInfo> it = additionalInfo.iterator(); it.hasNext(); ) {
            IInfo info = it.next();
            if(info.getDeclaringModuleName().equals(moduleName)){
                it.remove();
            }
        }
    }

    /**
     * @param qualifier the tokens returned have to start with the given qualifier
     * @return a list of info, all starting with the given qualifier
     */
    public List<IInfo> getTokensStartingWith(String qualifier) {
        ArrayList<IInfo> toks = new ArrayList<IInfo>();
        for (IInfo info : additionalInfo) {
            if(info.getName().startsWith(qualifier)){
                toks.add(info);
            }
        }
        return toks;
    }
    
    /**
     * @return all the tokens that are in this info
     */
    public Collection<IInfo> getAllTokens(){
        return additionalInfo;
    }

    /**
     * holds nature info
     */
    public static Map<String, AdditionalInterpreterInfo> additionalNatureInfo = new HashMap<String, AdditionalInterpreterInfo>();

    /**
     * holds system info
     */
    public static AdditionalInterpreterInfo additionalSystemInfo;
    
    /**
     * @return the additional info for the system
     */
    public static AdditionalInterpreterInfo getAdditionalSystemInfo() {
        if(additionalSystemInfo == null){
            additionalSystemInfo = new AdditionalInterpreterInfo();
        }
        return additionalSystemInfo;
    }


    
    
}
