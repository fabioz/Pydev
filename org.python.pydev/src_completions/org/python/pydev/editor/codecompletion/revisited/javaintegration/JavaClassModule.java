package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.AbstractJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.core.FindInfo;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledToken;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModuleForZip;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.plugin.PydevPlugin;

/**
 * This is the module wrapper around java files.
 * 
 * Don't know how to make the completions for java correctly... check: 
 * http://www.eclipse.org/newsportal/article.php?id=68521&group=eclipse.platform#68521
 * 
 * @author Fabio
 */
public class JavaClassModule extends AbstractModule {

    public static final boolean DEBUG_JARS = false;
    
    private static final IToken[] EMPTY_ITOKEN = new IToken[0];

    public static HashMap<String, String> replacementMap = new HashMap<String, String>();
    
    static{
        replacementMap.put("object", "obj");
        replacementMap.put("class", "class_");
    }
    
    private CompiledToken[] tokens;

    private File file;

    @Override
    public File getFile() {
        return file;
    }

    public JavaClassModule(EmptyModuleForZip emptyModuleForZip) {
        super(emptyModuleForZip.getName());
        this.file = emptyModuleForZip.f;
        
        //that's because if the JavaPlugin is not initialized, we'll have errors because it will try to create the
        //image descriptor registry from a non-display owner when making the completions (and in this way, we'll 
        //guarantee that its cache is already created).
        try{
            JavaPlugin.getImageDescriptorRegistry();
        }catch(Throwable e){
            Display.getDefault().syncExec(new Runnable(){

                public void run() {
                    try{
                        JavaPlugin.getImageDescriptorRegistry();
                    }catch(Throwable e){
                        //ignore it at this point
                    }
                }
            });
        }
        
        if(DEBUG_JARS){
            System.out.println("Created JavaClassModule: "+name);
        }
        this.tokens = createTokens(name);
        
    }

    /**
     * This method will create the tokens for a given package.
     */
    private CompiledToken[] createTokens(String packagePlusactTok) {
        ArrayList<CompiledToken> lst = new ArrayList<CompiledToken>();
        IClasspathEntry entries[] = new IClasspathEntry[] { JavaCore.newLibraryEntry(Path.fromOSString(this.file.getAbsolutePath()), null, null, true) };

        WorkingCopyOwner workingCopyOwner = new WorkingCopyOwner() {};

        try {
            ICompilationUnit unit = workingCopyOwner.newWorkingCopy(name, entries, new NullProgressMonitor());
            
            String contents = "class CompletionClass {\n" + 
                              "    void main(){\n" + 
                              "        new %s().}}" + "";
            
            contents = StringUtils.format(contents, packagePlusactTok);
            unit.getBuffer().setContents(contents);
            CompletionProposalCollector collector = new CompletionProposalCollector(unit);

            unit.codeComplete(contents.length() - 2, collector);
            IJavaCompletionProposal[] javaCompletionProposals = collector.getJavaCompletionProposals();

            for (IJavaCompletionProposal javaCompletionProposal : javaCompletionProposals) {
                if (javaCompletionProposal instanceof AbstractJavaCompletionProposal) {
                    AbstractJavaCompletionProposal prop = (AbstractJavaCompletionProposal) javaCompletionProposal;
                    IJavaElement javaElement = prop.getJavaElement();
                    String args = "";
                    if(javaElement instanceof IMethod){
                        StringBuffer tempArgs = new StringBuffer("()");
                        IMethod method = (IMethod) javaElement;
                        for(String param:method.getParameterTypes()){
                            if(tempArgs.length() > 2){
                                tempArgs.insert(1, ", ");
                            }
                            
                            //now, let's make the parameter 'pretty'
                            String lastPart = FullRepIterable.getLastPart(param);
                            if(lastPart.length() > 0){
                                lastPart = PyAction.lowerChar(lastPart, 0);
                                if(lastPart.charAt(lastPart.length()-1) == ';'){
                                    lastPart=lastPart.substring(0, lastPart.length()-1);
                                }
                            }
                            
                            //we may have to replace it for some other word
                            String replacement = replacementMap.get(lastPart);
                            if(replacement != null){
                                lastPart = replacement;
                            }
                            tempArgs.insert(1, lastPart);
                        }
                        args = tempArgs.toString();
                    }
                    if(DEBUG_JARS){
                        System.out.println("Element: "+javaElement);
                    }
                    lst.add(new CompiledToken(javaElement.getElementName(), javaCompletionProposal.getAdditionalProposalInfo(), args, this.name, IToken.TYPE_BUILTIN));
                } else {
                    System.err.println("Not treated: " + javaCompletionProposal.getClass());
                }
            }
        } catch (JavaModelException e) {
            PydevPlugin.log(e);
        }
        
        return lst.toArray(new CompiledToken[lst.size()]);
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
        return this.tokens;
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
        String act = name+"."+state.getActivationToken();
        return createTokens(act);
    }

    @Override
    public boolean isInDirectGlobalTokens(String tok) {
        if(this.tokens != null){
            return binaryHasObject(this.tokens, new CompiledToken(tok, "", "", "", 0));
        }
        return false;
    }

    @Override
    public boolean isInGlobalTokens(String tok, IPythonNature nature) {
        if(tok.indexOf('.') == -1){
            return isInDirectGlobalTokens(tok);
        }else{
            System.err.println("Still no treated isInDirectGlobalTokens with dotted string:"+tok);
            return false;
        }
    }
    
    /**
     * Gotten from Arrays.binarySearch (but returning boolean if key was found or not).
     * 
     * It also works directly with CompiledToken because we want a custom compare (from the representation)
     */
    private static boolean binaryHasObject(CompiledToken[] a, CompiledToken key) {
        int low = 0;
        int high = a.length - 1;

        while (low <= high) {
            int mid = (low + high) >> 1;
            CompiledToken midVal = (CompiledToken) a[mid];
            int cmp = midVal.getRepresentation().compareTo(key.getRepresentation());

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return true; // key found
        }
        return false; // key not found.
    }


    /**
     * @param findInfo 
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#findDefinition(java.lang.String, int, int)
     */
    public Definition[] findDefinition(ICompletionState state, int line, int col, IPythonNature nature, List<FindInfo> findInfo)
            throws Exception {
        return new Definition[0];
    }

}
