package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.CompletionProposal;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.core.FindInfo;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.revisited.jython.ExtractTextFromHtml;
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
    
    private static final CompiledToken[] EMPTY_ITOKEN = new CompiledToken[0];

    public static HashMap<String, String> replacementMap = new HashMap<String, String>();
    
    static{
        replacementMap.put("object", "obj");
        replacementMap.put("class", "class_");
    }
    
    private CompiledToken[] tokens;

    private File file;
    
    /**
     * If true, this represents a .class file in a zip, otherwise, it's a module representation.
     */
    private boolean isFileInZip;

    @Override
    public File getFile() {
        return file;
    }

    private static HashMap<String, IClasspathEntry[]> classpathEntries = new HashMap<String, IClasspathEntry[]>();
    
    private synchronized IClasspathEntry[] getClasspathEntries() {
        return getClasspathEntries(this.file.getAbsolutePath());
    }
    
    /**
     * @return the classpath entries used in this class
     */
    private static synchronized IClasspathEntry[] getClasspathEntries(String path) {
        IClasspathEntry[] entry = classpathEntries.get(path);
        if(entry == null){
            entry = new IClasspathEntry[] { JavaCore.newLibraryEntry(Path.fromOSString(path), null, null, true) };
            classpathEntries.put(path, entry);
        }
        return entry;
    }


    public JavaClassModule(EmptyModuleForZip emptyModuleForZip) {
        super(emptyModuleForZip.getName());
        this.file = emptyModuleForZip.f;
        this.isFileInZip = emptyModuleForZip.isFile;
        
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
        if(isFileInZip){
            this.tokens = createTokens(name);
        }else{
            this.tokens = EMPTY_ITOKEN;
        }
        
    }

    /**
     * This method will create the tokens for a given package.
     */
    private CompiledToken[] createTokens(String packagePlusactTok) {
        ArrayList<CompiledToken> lst = new ArrayList<CompiledToken>();

        try {
            
            //TODO: if we don't want to depend on jdt inner classes, we should create a org.eclipse.jdt.core.CompletionRequestor
            //(it's not currently done because its API is not as easy to handle).
            //we should be able to check the CompletionProposalCollector to see how we can transform the info we want...
            //also, making that change, it should be faster, because we won't need to 1st create a java proposal to then
            //create a pydev token (it would be a single step to transform it from a Completion Proposal to an IToken).
            
            IJavaCompletionProposal[] javaCompletionProposals = getJavaCompletionProposals(packagePlusactTok, null);

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
//                    String additionalProposalInfo = ExtractTextFromHtml.getText(javaCompletionProposal.getAdditionalProposalInfo());
                    
                    //TODO: We should do that only when it's actually requested
                    //so that we don't keep the images in memory (they take lots of memory because they're created
                    //at runtime -- and are not fixed as those in pydev)
                    //Image image = javaCompletionProposal.getImage();
                    //so, we should keep in memory the info needed to pass to CompletionProposalLabelProvider
                    //to create it only when needed.
                    
                    lst.add(new CompiledToken(javaElement.getElementName(), "", args, this.name, 
                            getType(javaElement.getElementType())));

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
     * Stores the mapping from the java type to the IToken type
     */
    private static HashMap<Integer, Integer> typesMapping = new HashMap<Integer, Integer>();
    static{
        typesMapping.put(IJavaElement.CLASS_FILE, IToken.TYPE_CLASS);
        typesMapping.put(IJavaElement.COMPILATION_UNIT, IToken.TYPE_CLASS);
        typesMapping.put(IJavaElement.PACKAGE_DECLARATION, IToken.TYPE_PACKAGE);
        typesMapping.put(IJavaElement.PACKAGE_FRAGMENT, IToken.TYPE_PACKAGE);
        typesMapping.put(IJavaElement.PACKAGE_FRAGMENT_ROOT, IToken.TYPE_PACKAGE);
    }

    /**
     * @param elementType the java element type we're interested in
     * @return the IToken type which is correspondent to the java type
     */
    private int getType(int elementType) {
        Integer found = typesMapping.get(elementType);
        if(found != null){
            return found;
        }
        return IToken.TYPE_ATTR;
    }

    /**
     * @param completeClassDesc the name of the class from where we should get the tokens. E.g. java.lang.Class, javax.swing.JFrame
     * @param filterCompletionName 
     * @return a list of completion proposals gotten from that class.
     * @throws JavaModelException
     */
    private IJavaCompletionProposal[] getJavaCompletionProposals(String completeClassDesc, final String filterCompletionName) throws JavaModelException {
        IClasspathEntry entries[] = getClasspathEntries();
        ICompilationUnit unit = new WorkingCopyOwner(){}.newWorkingCopy(name, entries, new NullProgressMonitor());
        String contents = "class CompletionClass {void main(){new %s().}}";
        contents = StringUtils.format(contents, completeClassDesc);
        unit.getBuffer().setContents(contents);
        CompletionProposalCollector collector = new CompletionProposalCollector(unit){
            @Override
            public IJavaCompletionProposal createJavaCompletionProposal(CompletionProposal proposal) {
                IJavaCompletionProposal javaCompletionProposal = super.createJavaCompletionProposal(proposal);
                if(filterCompletionName == null){
                    return javaCompletionProposal;
                }
                if (javaCompletionProposal instanceof AbstractJavaCompletionProposal) {
                    AbstractJavaCompletionProposal prop = (AbstractJavaCompletionProposal) javaCompletionProposal;
                    IJavaElement javaElement = prop.getJavaElement();
                    if(javaElement != null){
                        if(javaElement.getElementName().equals(filterCompletionName)){
                            return javaCompletionProposal;
                        }
                    }
                }
                return null;
            }
        };

        unit.codeComplete(contents.length() - 2, collector);
        IJavaCompletionProposal[] javaCompletionProposals = collector.getJavaCompletionProposals();
        return javaCompletionProposals;
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
        
        //try to see if that's a java class from a package... to do that, we must go iterating through the name found
        //to check if we're able to find modules with that name. If a module with that name is found, that means that 
        //we actually have a java class. 
        String[] splitted = FullRepIterable.dotSplit(state.getActivationToken());
        StringBuffer modNameBuf = new StringBuffer(this.getName());
        IModule validModule = null;
        IModule module = null;
        int i=0; //so that we know what will result in the tok
        for(;i<splitted.length; i++){
            String s = splitted[i];
            modNameBuf.append(".");
            modNameBuf.append(s);
            module = nature.getAstManager().getModule(modNameBuf.toString(), nature, true, false);
            if(module != null){
                validModule = module;
            }else{
                break;
            }
        }
        
        
        StringBuffer pathInJavaClass = new StringBuffer();
        if(validModule == null){
            validModule = this;
            pathInJavaClass = new StringBuffer(state.getActivationToken());
        }else{
            //After having found a valid java class, we must also check which was the resulting token within that class 
            //to check if it's some method or something alike (that should be easy after having the class and the path
            //to the method we want to find within it).
            if(!(validModule instanceof JavaClassModule)){
                throw new RuntimeException("The module found from a java class module was found as another kind: "+validModule.getClass());
            }
            for(int j=i; j<splitted.length;j++){
                if(j!=i){
                    pathInJavaClass.append(".");
                }
                pathInJavaClass.append(splitted[j]);
            }
        }
        
        JavaClassModule javaClassModule = (JavaClassModule) validModule;
        
        //ok, now, if there is no path, the definition is the java class itself.
        if(pathInJavaClass.length() == 0){
            
            IClasspathEntry entries[] = getClasspathEntries();
            ICompilationUnit unit = new WorkingCopyOwner(){}.newWorkingCopy(name, entries, new NullProgressMonitor());
            String contents = "import %s.;";
            contents = StringUtils.format(contents, FullRepIterable.getWithoutLastPart(javaClassModule.getName()));
            unit.getBuffer().setContents(contents);
            
            final String lookingForClass = FullRepIterable.getLastPart(javaClassModule.getName());
            CompletionProposalCollector collector = new CompletionProposalCollector(unit){
                @Override
                public IJavaCompletionProposal createJavaCompletionProposal(CompletionProposal proposal) {
                    IJavaCompletionProposal javaCompletionProposal = super.createJavaCompletionProposal(proposal);
                    if (javaCompletionProposal instanceof AbstractJavaCompletionProposal) {
                        AbstractJavaCompletionProposal prop = (AbstractJavaCompletionProposal) javaCompletionProposal;
                        IJavaElement javaElement = prop.getJavaElement();
                        if(javaElement != null){
                            if(javaElement.getElementName().equals(lookingForClass)){
                                return javaCompletionProposal;
                            }
                        }
                    }
                    return null;
                }
            };

            unit.codeComplete(contents.length() - 1, collector);
            IJavaCompletionProposal[] javaCompletionProposals = collector.getJavaCompletionProposals();

            for (IJavaCompletionProposal javaCompletionProposal : javaCompletionProposals) {
                if (javaCompletionProposal instanceof AbstractJavaCompletionProposal) {
                    AbstractJavaCompletionProposal prop = (AbstractJavaCompletionProposal) javaCompletionProposal;
                    IJavaElement javaElement = prop.getJavaElement();
                    if(javaElement != null){
                        if(javaElement.getElementName().equals(lookingForClass)){
                            return new Definition[]{new JavaDefinition("", javaClassModule, javaElement)};
                        }
                    }
                }
            }
            
            
        }else{
            String pathInClassStr = pathInJavaClass.toString();
            //ok, it's not the class directly, so, we have to check what it actually is.
            IJavaCompletionProposal[] javaCompletionProposals = getJavaCompletionProposals(javaClassModule.getName(), pathInClassStr);

            for (IJavaCompletionProposal javaCompletionProposal : javaCompletionProposals) {
                if (javaCompletionProposal instanceof AbstractJavaCompletionProposal) {
                    AbstractJavaCompletionProposal prop = (AbstractJavaCompletionProposal) javaCompletionProposal;
                    IJavaElement javaElement = prop.getJavaElement();
                    if(javaElement != null){
                        if(javaElement.getElementName().equals(pathInClassStr)){
                            return new Definition[]{new JavaDefinition(pathInClassStr, javaClassModule, javaElement)};
                        }
                    }
                }
            }
        }
        return new Definition[0];
    }

}
