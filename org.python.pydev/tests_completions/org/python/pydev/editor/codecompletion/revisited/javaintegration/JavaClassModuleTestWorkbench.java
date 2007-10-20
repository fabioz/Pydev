package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import java.io.ByteArrayInputStream;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
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
     * @throws Exception
     */
    public void testJavaClassModule() throws Exception {
        ProjectModulesManager.IN_TESTS = true;
        NullProgressMonitor monitor = new NullProgressMonitor();
        
        IInterpreterManager iMan = PydevPlugin.getJythonInterpreterManager(true);
        iMan.addInterpreter(TestDependent.JYTHON_JAR_LOCATION, monitor);
        iMan.restorePythopathFor(TestDependent.JYTHON_JAR_LOCATION, monitor);
        iMan.setPersistedString(iMan.getStringToPersist(new String[]{TestDependent.JYTHON_JAR_LOCATION}));
        
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("pydev_unit_test_project");
        if(project.exists()){
            project.delete(true, monitor);
        }
        project.create(monitor);
        project.open(monitor);
        IFolder sourceFolder = project.getFolder(new Path("src"));
        if(!sourceFolder.exists()){
            sourceFolder.create(true, true, monitor);
            PythonNature.addNature(project, monitor, PythonNature.JYTHON_VERSION_2_1, "src");
        }

        IFile initFile = createPackageStructure(sourceFolder, "pack1.pack2", monitor);
        IFile mod1 = initFile.getParent().getFile(new Path("mod1.py"));
        String mod1Contents = "import java.lang.Class\njava.lang.Class.";
        if(!mod1.exists()){
            mod1.create(new ByteArrayInputStream(mod1Contents.getBytes()), true, monitor);
        }

//        PyEdit editor = (PyEdit) PydevPlugin.doOpenEditor(mod1, true);
//        IContentAssistant contentAssistant = editor.getEditConfiguration().getContentAssistant(editor.getPySourceViewer());
//        IContentAssistProcessor processor = contentAssistant.getContentAssistProcessor(IDocument.DEFAULT_CONTENT_TYPE);
//        System.out.println("Request props");
//        ICompletionProposal[] props = processor.computeCompletionProposals(editor.getPySourceViewer(), mod1Contents.length()-1);
//        for (ICompletionProposal prop : props) {
//            System.out.println("Prop:"+prop.getDisplayString());
//        }
        
        goToManual();
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
            IFile file = parent.getFile(new Path("__init__"+PythonPathHelper.getDefaultDottedPythonExtension()));
            if(!file.exists()){
                file.create(new ByteArrayInputStream(new byte[0]), true, monitor);
            }
            lastFile = file;
        }
        
        
        return lastFile;
    }

}
