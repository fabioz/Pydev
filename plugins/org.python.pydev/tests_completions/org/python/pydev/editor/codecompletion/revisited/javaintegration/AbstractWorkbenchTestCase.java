package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.simpleassist.SimpleAssistProcessor;
import org.python.pydev.editorinput.PyOpenEditor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;
import org.python.pydev.ui.interpreters.JythonInterpreterManager;
import org.python.pydev.ui.interpreters.PythonInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * This is a base class for doing test cases that require the workbench to be 'alive' and that want to test the integration
 * with jython/java.
 *
 * @author Fabio
 */
public class AbstractWorkbenchTestCase extends TestCase{
    
    /**
     * This is the module that's opened in a PyEdit editor.
     */
    protected static IFile mod1;
    
    /**
     * This is the editor where the file-contents are opened.
     */
    protected static PyEdit editor;

    /**
     * Init file under pack1.pack2.__init__.py
     */
    protected static IFile initFile;


    /**
     * Create a project with the structure:
     * 
     * /pydev_unit_test_project
     *     junit.jar   <-- set in pythonpath
     *     /src        <-- set in pythonpath
     *         /pack1
     *             __init__.py
     *             /pack2
     *                 __init__.py
     *                 mod1.py 
     *     
     * /java_unit_test_project
     *     /src        <-- set in classpath
     *         JavaDefault.java (default package)
     *         /javamod1/JavaClass.java
     *             /javamod2/JavaClass.java
     *             
     * Note: the initialization of the structure will happen only once and will be re-used in all the tests after it.
     */
    @Override
    protected void setUp() throws Exception {
        closeWelcomeView();

        String mod1Contents = "import java.lang.Class\njava.lang.Class";
        if(editor == null){
            InterpreterInfo.configurePathsCallback = new ICallback<Boolean, Tuple<List<String>, List<String>>>(){
                public Boolean call(Tuple<List<String>, List<String>> arg) {
                    return Boolean.TRUE;
                }
            };
            PydevPlugin.setJythonInterpreterManager(new JythonInterpreterManager(PydevPlugin.getDefault().getPluginPreferences()));
            PydevPlugin.setPythonInterpreterManager(new PythonInterpreterManager(PydevPlugin.getDefault().getPluginPreferences()));
            
            
            ProjectModulesManager.IN_TESTS = true;
            NullProgressMonitor monitor = new NullProgressMonitor();
            
            createJythonInterpreterManager(monitor);
            createPythonInterpreterManager(monitor);
            
            IProject project = createProject(monitor, "pydev_unit_test_project");
            IJavaProject javaProject = configureAsJavaProject(createProject(monitor, "java_unit_test_project"), monitor);
            setProjectReference(monitor, project, javaProject);
            
            createJunitJar(monitor, project);
            
            IFolder sourceFolder = createSourceFolder(monitor, project);
            
            initFile = createPackageStructure(sourceFolder, "pack1.pack2", monitor);
            
            mod1 = initFile.getParent().getFile(new Path("mod1.py"));
            
            //OK, structure created, now, let's open mod1.py with a PyEdit so that the tests can begin...
            
            //create the contents and open the editor
            mod1.create(new ByteArrayInputStream(mod1Contents.getBytes()), true, monitor);
            
            
            PythonNature nature = PythonNature.getPythonNature(project);
            
            //Let's give it some time to run the jobs that restore the nature
            long finishAt = System.currentTimeMillis()+5000; //5 secs is the max time
            
            Display display = Display.getCurrent();
            if(display == null){
                display = Display.getDefault();
            }
            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()){
                    display.sleep();
                }
                if(finishAt<System.currentTimeMillis()){
                    break;
                }
                if(nature != null){
                    if(nature.getAstManager() != null){
                        break;
                    }
                }
            }

            
            assertTrue(nature != null);
            assertTrue(nature.getAstManager() != null);
            
            
            editor = (PyEdit) PyOpenEditor.doOpenEditor(mod1);
        }else{
            setFileContents(mod1Contents);//just make sure that the contents of mod1 are correct.
        }
    }

    

    /**
     * Prints the display strings for the passed proposals.
     */
    protected void printProps(ICompletionProposal[] props) {
        System.out.println("START Printing proposals -----------------------------");
        for (ICompletionProposal prop : props) {
            System.out.println(prop.getDisplayString());
        }
        System.out.println("END Printing proposals -----------------------------");
    }
    
    
    
    protected void goToIdleLoopUntilCondition(final ICallback<Boolean, Object> callback, 
            final ICallback<String, Object> errorMessageCallback, boolean failIfNotSatisfied ) {
        goToIdleLoopUntilCondition(callback, 10000L, errorMessageCallback, failIfNotSatisfied);
    }
    
    protected void goToIdleLoopUntilCondition(final ICallback<Boolean, Object> callback, 
            final ICallback<String, Object> errorMessageCallback ) {
        goToIdleLoopUntilCondition(callback, 10000L, errorMessageCallback, true);
    }
    
    /**
     * @see #goToIdleLoopUntilCondition(ICallback, long)
     */
    protected void goToIdleLoopUntilCondition(final ICallback<Boolean, Object> callback) {
        goToIdleLoopUntilCondition(callback, 10000L, null, true);//default with 10 secs (more than enough for any action to be executed)
    }

    /**
     * 
     * @param callback a callback that'll receive null as a parameter and should return true if the condition seeked was
     * reached and false otherwise.
     * @param deltaToElapse the number of seconds that can be elapsed until the function returns if the condition
     * has not been satisfied.
     * 
     * @throws AssertionError if the condition was not satisfied in the available amount of time
     */
    protected void goToIdleLoopUntilCondition(final ICallback<Boolean, Object> callback, long deltaToElapse, 
            final ICallback<String, Object> errorMessageCallback, boolean failIfNotSatisfied) {
        //make the delta the absolute time
        deltaToElapse = System.currentTimeMillis() + deltaToElapse;
        Display display = Display.getCurrent();
        if(display == null){
            display = Display.getDefault();
        }
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        while (!shell.isDisposed()) {
            display.readAndDispatch();
            synchronized(this){
                try {
                    this.wait(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            if(callback.call(null)){
                return;
            }
            if(deltaToElapse<System.currentTimeMillis()){
                break;
            }
        }
        if(!failIfNotSatisfied){
            return;
        }
        if(errorMessageCallback != null){
            fail("The condition requested was not satisfied in the available amount of time:\n"+
                    errorMessageCallback.call(null));
        }
        fail("The condition requested was not satisfied in the available amount of time");
    }

    
    protected void goToManual() {
        goToManual(-1);
    }
    
    protected void goToManual(long millis) {
        goToManual(millis, null);
    }
    /**
     * Goes to 'manual' mode to allow the interaction with the opened eclipse instance.
     */
    protected void goToManual(long millis, ICallback<Boolean, Object> condition) {
        long finishAt = System.currentTimeMillis()+millis;
        
        System.out.println("going to manual...");
        Display display = Display.getCurrent();
        if(display == null){
            display = Display.getDefault();
        }
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()){
                display.sleep();
            }
            if(millis > 0 && finishAt<System.currentTimeMillis()){
                break;
            }
            if(condition != null && condition.call(null)){
                break;
            }
        }
        System.out.println("finishing...");
    }
    
    /**
     * Creates a package structure below a source folder.
     */
    protected IFile createPackageStructure(IContainer sourceFolder, String packageName, IProgressMonitor monitor) throws CoreException {
        IFile lastFile = null;
        if(sourceFolder == null){
            return null;
        }
        IContainer parent = sourceFolder;
        String[] packageParts = StringUtils.dotSplit(packageName);
        for (String packagePart : packageParts) {
            IFolder folder = parent.getFolder(new Path(packagePart));
            if(!folder.exists()){
                folder.create(true, true, monitor);
            }
            parent = folder;
            IFile file = parent.getFile(new Path("__init__"+FileTypesPreferencesPage.getDefaultDottedPythonExtension()));
            if(!file.exists()){
                file.create(new ByteArrayInputStream(new byte[0]), true, monitor);
            }
            lastFile = file;
        }
        
        
        return lastFile;
    }
    
    

    /**
     * Sets the contents of the mod1.py -- which has the PyEdit opened.
     */
    protected void setFileContents(String mod1Contents) throws CoreException {
        NullProgressMonitor monitor = new NullProgressMonitor();
        mod1.setContents(new ByteArrayInputStream(mod1Contents.getBytes()), 0, monitor);
        mod1.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    }


    /**
     * Sets the referenced projects for project as being the javaProject passed.
     */
    protected void setProjectReference(IProgressMonitor monitor, IProject project, IJavaProject javaProject) throws CoreException {
        IProjectDescription description = project.getDescription();
        description.setReferencedProjects(new IProject[]{javaProject.getProject()});
        project.setDescription(description, monitor);
    }

    


    /**
     * Adds the java nature to a given project
     * @return the java project (nature) that has been set. 
     */
    protected IJavaProject configureAsJavaProject(IProject project, IProgressMonitor monitor) throws CoreException {
        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();
        String[] newNatures = new String[natures.length + 1];
        System.arraycopy(natures, 0, newNatures, 0, natures.length);
        newNatures[natures.length] = JavaCore.NATURE_ID;
        description.setNatureIds(newNatures);
        project.setDescription(description, monitor);
        
        
        IFolder srcFolder = project.getFolder(new Path("src"));
        srcFolder.create(false, true, monitor);
        
        IJavaProject javaProject = JavaCore.create(project);
        
        javaProject.setRawClasspath(new IClasspathEntry[]{JavaCore.newSourceEntry(srcFolder.getFullPath())}, monitor);
        
        Set<IClasspathEntry> entries = new HashSet<IClasspathEntry>();
        entries.addAll(Arrays.asList(javaProject.getRawClasspath()));
        entries.add(JavaRuntime.getDefaultJREContainerEntry());
        javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), monitor);
        
        //create src/javamod1/javamod2
        IFolder javaMod1Folder = srcFolder.getFolder("javamod1");
        javaMod1Folder.create(true, true, monitor);
        
        IFolder javaMod2Folder = javaMod1Folder.getFolder("javamod2");
        javaMod2Folder.create(true, true, monitor);
        
        
        //create src/JavaDefault.java
        IFile javaClassFile = srcFolder.getFile("JavaDefault.java");
        
        String javaClassContents = 
            "public class JavaDefault {\n"+ //default package        
            "   private int testJavaDefault(String[] args) {\n"+        
            "       return 0;\n"+        
            "   }\n"+        
            "}\n";
        
        javaClassFile.create(new ByteArrayInputStream(javaClassContents.getBytes()), true, monitor);
        
        
        //create src/javamod1/JavaClass.java
        javaClassFile = javaMod1Folder.getFile("JavaClass.java");
        
        javaClassContents = 
"package javamod1;\n"+        
"public class JavaClass {\n"+        
"   \n"+        
"   public static int JAVA_CLASS_CONSTANT = 1;\n"+        
"   \n"+        
"   public static void main(String[] args) {\n"+        
"       new JavaClass().testJavaClass(new int[0]);\n"+        
"   }\n"+        
"   private int testJavaClass(int[] args) {\n"+        
"       return 0;\n"+        
"   }\n"+        
"}\n";

        javaClassFile.create(new ByteArrayInputStream(javaClassContents.getBytes()), true, monitor);
        
        
        //create src/javamod1/javamod2/JavaClass2.java
        javaClassFile = javaMod2Folder.getFile("JavaClass2.java");
        
        javaClassContents = 
"package javamod1.javamod2;\n"+        
"public class JavaClass2 {\n"+        
"   \n"+        
"   public static int JAVA_CLASS_CONSTANT_2 = 1;\n"+        
"   \n"+        
"   public JavaClass2(int i){};\n"+        
"   \n"+        
"   public static void main(String[] args) {\n"+        
"       new JavaClass2(1).testJavaClass2(new int[0]);\n"+        
"   }\n"+        
"   private int testJavaClass2(int[] args) {\n"+        
"       return 0;\n"+        
"   }\n"+        
"}\n";

        javaClassFile.create(new ByteArrayInputStream(javaClassContents.getBytes()), true, monitor);

        
        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        return javaProject;
    }

    
    /**
     * Creates a source folder and configures the project to use it and the junit.jar
     */
    protected IFolder createSourceFolder(IProgressMonitor monitor, IProject project) throws CoreException {
        return createSourceFolder(monitor, project, true);
    }

    /**
     * Creates a source folder and configures the project to use it and the junit.jar
     * 
     * @param addNature if false, no nature will be initially added to the project (if true, the nature will be added)
     */
    protected IFolder createSourceFolder(IProgressMonitor monitor, IProject project, boolean addNature) throws CoreException {
        IFolder sourceFolder = project.getFolder(new Path("src"));
        if(!sourceFolder.exists()){
            sourceFolder.create(true, true, monitor);
        }
        if(addNature){
            PythonNature.addNature(project, monitor, PythonNature.JYTHON_VERSION_2_1, 
                    "/pydev_unit_test_project/src|/pydev_unit_test_project/junit.jar", null, null);
        }
        return sourceFolder;
    }


    /**
     * Creates the jython interpreter manager with the default jython jar location.
     */
    protected void createJythonInterpreterManager(NullProgressMonitor monitor) {
        IInterpreterManager iMan = PydevPlugin.getJythonInterpreterManager(true);
        IInterpreterInfo interpreterInfo = iMan.createInterpreterInfo(TestDependent.JYTHON_JAR_LOCATION, monitor);
        iMan.addInterpreterInfo(interpreterInfo);
        iMan.restorePythopathForAllInterpreters(monitor);
        iMan.setPersistedString(iMan.getStringToPersist(new IInterpreterInfo[]{interpreterInfo}));
        iMan.saveInterpretersInfoModulesManager();
    }

    /**
     * Creates the python interpreter manager with the default jython jar location.
     */
    protected void createPythonInterpreterManager(NullProgressMonitor monitor) {
        IInterpreterManager iMan = PydevPlugin.getPythonInterpreterManager(true);
        IInterpreterInfo interpreterInfo = iMan.createInterpreterInfo(TestDependent.PYTHON_EXE, monitor);
        iMan.addInterpreterInfo(interpreterInfo);
        iMan.restorePythopathForAllInterpreters(monitor);
        iMan.setPersistedString(iMan.getStringToPersist(new IInterpreterInfo[]{interpreterInfo}));
        iMan.saveInterpretersInfoModulesManager();
    }
    

    /**
     * Creates a junit.jar file in the project.
     */
    protected void createJunitJar(NullProgressMonitor monitor, IProject project) throws CoreException {
        String junitJarLocatioon = project.getLocation().toOSString()+"/junit.jar";
        File junitJarFile = new File(junitJarLocatioon);
        if(!junitJarFile.exists()){
            REF.copyFile(TestDependent.TEST_PYDEV_PLUGIN_LOC+"tests_completions/org/python/pydev/editor/codecompletion/revisited/javaintegration/junit.jar", 
                    junitJarLocatioon);
        }
        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
    }


    /**
     * Creates a pydev_unit_test_project to be used in the tests
     */
    protected IProject createProject(IProgressMonitor monitor, String projectName) throws CoreException {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if(project.exists()){
            project.delete(true, monitor);
        }
        project.create(monitor);
        project.open(monitor);
        return project;
    }


    /**
     * Requests proposals in the last location of the given editor.
     */
    protected ICompletionProposal[] requestProposals(String mod1Contents, PyEdit editor) {
        IContentAssistant contentAssistant = editor.getEditConfiguration().getContentAssistant(editor.getPySourceViewer());
        SimpleAssistProcessor processor = (SimpleAssistProcessor) contentAssistant.getContentAssistProcessor(IDocument.DEFAULT_CONTENT_TYPE);
        processor.doCycle(); //we want to show the default completions in this case (not the simple ones)
        ICompletionProposal[] props = processor.computeCompletionProposals(editor.getPySourceViewer(), mod1Contents.length());
        return props;
    }


    /**
     * Closes the welcome view (if being shown)
     */
    public void closeWelcomeView() {
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IViewReference[] viewReferences = workbenchWindow.getActivePage().getViewReferences();
        for(IViewReference ref:viewReferences){
            if(ref.getPartName().equals("Welcome")){
                workbenchWindow.getActivePage().hideView(ref);
            }
        }
    }

}
