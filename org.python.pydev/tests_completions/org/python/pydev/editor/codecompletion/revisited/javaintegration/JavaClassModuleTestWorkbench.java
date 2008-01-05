package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.simpleassist.SimpleAssistProcessor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * Test that needs to run in the workbench to request a code-completion together with the java integration.
 * 
 * @author Fabio
 */
public class JavaClassModuleTestWorkbench extends TestCase {

    @Override
    protected void setUp() throws Exception {
        InterpreterInfo.configurePathsCallback = new ICallback<Boolean, Tuple<List<String>, List<String>>>(){
            public Boolean call(Tuple<List<String>, List<String>> arg) {
                return Boolean.TRUE;
            }
        };
    }

    
    /**
     * Test the java module creation.
     * 
     * Create a project with the structure:
     * 
     * /pydev_unit_test_project
     *     junit.jar
     *     /pack1/pack2/mod1.py
     */
    public void testJavaClassModule() throws Exception {
        ProjectModulesManager.IN_TESTS = true;
        NullProgressMonitor monitor = new NullProgressMonitor();
        
        createJythonInterpreterManager(monitor);
        
        IProject project = createProject(monitor);
        
        createJunitJar(monitor, project);
        
        IFolder sourceFolder = createSourceFolder(monitor, project);
        
        IFile initFile = createPackageStructure(sourceFolder, "pack1.pack2", monitor);
        
        IFile mod1 = initFile.getParent().getFile(new Path("mod1.py"));
        //OK, structure created, now, let's try to set some contents in mod1.py and see if the completions
        //are correctly requested.
        
        
        //case 1: try it with the rt.jar classes
        String mod1Contents = "import java.lang.Class\njava.lang.Class.";
        mod1.create(new ByteArrayInputStream(mod1Contents.getBytes()), true, monitor);

        PyEdit editor = (PyEdit) PydevPlugin.doOpenEditor(mod1, true);
        ICompletionProposal[] props = requestProposals(mod1Contents, editor);
        
        CodeCompletionTestsBase.assertContains("getDeclaredFields()", props);
        CodeCompletionTestsBase.assertContains("getPrimitiveClass(string)", props);
        
        
        //case 2: try with jar added to the project pythonpath
        mod1Contents = "import junit.framework.Assert\njunit.framework.Assert.";
        mod1.setContents(new ByteArrayInputStream(mod1Contents.getBytes()), 0, monitor);
        
        props = requestProposals(mod1Contents, editor);
        CodeCompletionTestsBase.assertContains("assertNotNull(obj)", props);
        CodeCompletionTestsBase.assertContains("assertEquals(obj, obj)", props);
        
        
        //case 3: try with referenced java project
        //TODO: Finish!
        
//        goToManual();
    }

    
    /**
     * Creates a source folder and configures the project to use it and the junit.jar
     */
    private IFolder createSourceFolder(NullProgressMonitor monitor, IProject project) throws CoreException {
        IFolder sourceFolder = project.getFolder(new Path("src"));
        sourceFolder.create(true, true, monitor);
        PythonNature.addNature(project, monitor, PythonNature.JYTHON_VERSION_2_1, "/pydev_unit_test_project/src|/pydev_unit_test_project/junit.jar");
        return sourceFolder;
    }


    /**
     * Creates the jython interpreter manager with the default jython jar location.
     */
    private void createJythonInterpreterManager(NullProgressMonitor monitor) {
        IInterpreterManager iMan = PydevPlugin.getJythonInterpreterManager(true);
        iMan.addInterpreter(TestDependent.JYTHON_JAR_LOCATION, monitor);
        iMan.restorePythopathFor(TestDependent.JYTHON_JAR_LOCATION, monitor);
        iMan.setPersistedString(iMan.getStringToPersist(new String[]{TestDependent.JYTHON_JAR_LOCATION}));
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
    protected IProject createProject(NullProgressMonitor monitor) throws CoreException {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("pydev_unit_test_project");
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
     * Prints the display strings for the passed proposals.
     */
    protected void printProps(ICompletionProposal[] props) {
        System.out.println("START Printing proposals -----------------------------");
        for (ICompletionProposal prop : props) {
            System.out.println(prop.getDisplayString());
        }
        System.out.println("END Printing proposals -----------------------------");
    }
    
    
    
    /**
     * Goes to 'manual' mode to allow the interaction with the opened eclipse instance.
     */
    protected void goToManual() {
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
        String[] packageParts = FullRepIterable.dotSplit(packageName);
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

}
