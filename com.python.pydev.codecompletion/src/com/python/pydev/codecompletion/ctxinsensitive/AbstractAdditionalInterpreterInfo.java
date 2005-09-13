/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.pydev.core.REF;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.interpreters.IInterpreterManager;

import sun.misc.BASE64Decoder;

import com.python.pydev.codecompletion.CodecompletionPlugin;


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
public abstract class AbstractAdditionalInterpreterInfo {

    private static final boolean DEBUG_ADDITIONAL_INFO = false;

    /**
     * This is the place where the information is actually stored
     */
    private List<IInfo> additionalInfo;
    
    

    public AbstractAdditionalInterpreterInfo(){
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

    /**
     * Adds a class or a function to the definition
     * 
     * @param classOrFunc the class or function we want to add
     * @param moduleDeclared the module where it is declared
     */
    public void addClassOrFunc(SimpleNode classOrFunc, String moduleDeclared) {
        if(classOrFunc instanceof ClassDef){
            addClass((ClassDef) classOrFunc, moduleDeclared);
        }else{
            addMethod((FunctionDef) classOrFunc, moduleDeclared);
        }
    }

    /**
     * Adds information for a source module
     * @param m the module we want to add to the info
     */
    public void addSourceModuleInfo(SourceModule m) {
        addAstInfo(m.getAst(), m.getName());
    }

    
    /**
     * Add info from a generated ast
     * @param node the ast root
     */
    public void addAstInfo(SimpleNode node, String moduleName) {
        try {
            EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
            node.accept(visitor);
            Iterator<ASTEntry> classesAndMethods = visitor.getClassesAndMethodsIterator();

            while (classesAndMethods.hasNext()) {
                SimpleNode classOrFunc = classesAndMethods.next().node;
                addClassOrFunc(classOrFunc, moduleName);
            }
        } catch (Exception e) {
            PydevPlugin.log(e);
        }

    }

    /**
     * Removes all the info associated with a given module
     * @param moduleName the name of the module we want to remove info from
     */
    public void removeInfoFromModule(String moduleName) {
        for (Iterator<IInfo> it = additionalInfo.iterator(); it.hasNext(); ) {
            IInfo info = it.next();
            if(info != null && info.getDeclaringModuleName() != null){
                if(info.getDeclaringModuleName().equals(moduleName)){
                    it.remove();
                }
            }
        }
    }

    /**
     * @param qualifier the tokens returned have to start with the given qualifier
     * @return a list of info, all starting with the given qualifier
     */
    public List<IInfo> getTokensStartingWith(String qualifier) {
        ArrayList<IInfo> toks = new ArrayList<IInfo>();
        String lowerCaseQual = qualifier.toLowerCase();
        if(additionalInfo != null){
            for (IInfo info : additionalInfo) {
                if(info.getName().toLowerCase().startsWith(lowerCaseQual)){
                    toks.add(info);
                }
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
     * this can be used to save the file
     */
    public void save() {
        saveTo(getPersistingLocation());
    }

    /**
     * @return the location where we can persist this info.
     */
    protected abstract String getPersistingLocation();


    /**
     * save the information contained for the given manager
     */
    public static void saveAdditionalSystemInfo(IInterpreterManager manager) {
        AbstractAdditionalInterpreterInfo info = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(manager);
        info.save();
    }

    /**
     * @return the path to the folder we want to keep things on
     */
    protected static String getPersistingFolder() {
        IPath stateLocation = CodecompletionPlugin.getDefault().getStateLocation();
        String osString = stateLocation.toOSString();
        if(osString.length() > 0){
            char c = osString.charAt(osString.length() -1);
            if(c != '\\' && c != '/'){
                osString += '/';
            }
        }
        return osString;
    }
    

    private void saveTo(String pathToSave) {
        if(DEBUG_ADDITIONAL_INFO){
            System.out.println("Saving info to file (size = "+additionalInfo.size()+") "+pathToSave);
        }
        REF.writeToFile(additionalInfo.toArray(new IInfo[0]), new File(pathToSave));
    }

    /**
     * actually does the load
     * @return true if it was successfully loaded and false otherwise
     */
    protected boolean load() {
        File file = new File(getPersistingLocation());
        if(file.exists() && file.isFile()){
            try {
                List<IInfo> additionalInfo = new ArrayList<IInfo> ( Arrays.asList((IInfo[])IOUtils.readFromFile(file)));
                this.additionalInfo = additionalInfo;
                setAsDefaultInfo();
                return true;
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }
        return false;
    }

    /**
     * this method should be overriden so that the info sets itself as the default info given the info it holds
     * (e.g. default for a project, default for python interpreter, etc.)
     */
    protected abstract void setAsDefaultInfo();

    
}

class IOUtils {

    /**
     * @param astOutputFile
     * @return
     */
    public static Object readFromFile(File astOutputFile) {
        try {
            InputStream input = new FileInputStream(astOutputFile);
            ObjectInputStream in = new ObjectInputStream(input);
            Object o = in.readObject();
            in.close();
            input.close();
            return o;
        } catch (Exception e) {
            Log.log(e);
            return null;
        }
    }

}