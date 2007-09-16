package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import java.io.File;
import java.util.List;

import org.python.pydev.core.FindInfo;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModuleForZip;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;

/**
 * This is the module wrapper around java files.
 * 
 * Don't know how to make the completions for java correctly... check: 
 * http://www.eclipse.org/newsportal/article.php?id=68521&group=eclipse.platform#68521
 * 
 * @author Fabio
 */
public class JavaClassModule extends AbstractModule{
    
    private static final IToken[] EMPTY_ITOKEN = new IToken[0];
    private File file;
    
    @Override
    public File getFile() {
        return file;
    }

    public JavaClassModule(EmptyModuleForZip emptyModuleForZip) {
        super(emptyModuleForZip.getName());
        //TODO: make it work with the java structure correctly.
//        JavaCore.create(project)
//        ResourceUtil.getFile(element)
//        JavaCore.createJarPackageFragmentRootFrom(file)
//        for(LibraryLocation lib:JavaRuntime.getDefaultVMInstall().getLibraryLocations()){
//            System.out.println(lib.getPackageRootPath());
//        }
    }

    /**
     * Compiled modules do not have imports to be seen
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getWildImportedModules()
     */
    public IToken[] getWildImportedModules() {
        return EMPTY_ITOKEN;
    }

    /**
     * Compiled modules do not have imports to be seen
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getTokenImportedModules()
     */
    public IToken[] getTokenImportedModules() {
        return EMPTY_ITOKEN;
    }

    /**
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getGlobalTokens()
     */
    public IToken[] getGlobalTokens() {
        return EMPTY_ITOKEN;
    }

    /**
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getDocString()
     */
    public String getDocString() {
        return "Java class module extension";
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#getGlobalTokens(java.lang.String)
     */
    public IToken[] getGlobalTokens(ICompletionState state, ICodeCompletionASTManager manager) {
        return EMPTY_ITOKEN;
    }
    
    @Override
    public boolean isInDirectGlobalTokens(String tok) {
        return false;
    }
    
    @Override
    public boolean isInGlobalTokens(String tok, IPythonNature nature) {
        return false;

    }

    /**
     * @param findInfo 
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#findDefinition(java.lang.String, int, int)
     */
    public Definition[] findDefinition(ICompletionState state, int line, int col, IPythonNature nature, List<FindInfo> findInfo) throws Exception {
        return new Definition[0];
    }

}
