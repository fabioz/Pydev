/**
 * 
 */
package org.python.pydev.editor.refactoring;

import java.io.File;
import java.util.Stack;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.structure.DecoratableObject;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.nature.SystemPythonNature;

/**
 * This class encapsulates all the info needed in order to do a refactoring
 * As we'd have a getter/setter without any side-effects, let's leave them all public...
 * 
 * It is a Decoratable Object, so that clients can add additional information to this
 * object at runtime.
 */
public class RefactoringRequest extends DecoratableObject{
	
	/**
	 * The file associated with the editor where the refactoring is being requested
	 */
	public File file;
	
	/**
	 * The current selection when the refactoring was requested
	 */
	public PySelection ps;
	
	/**
	 * The progress monitor to give feedback to the user (may be checked in another thread)
     * May be null
     * 
     * Note that this is the monitor for the initial request, but, clients may use it in othe
	 */
	private volatile Stack<IProgressMonitor> monitors = new Stack<IProgressMonitor>();
	
	/**
	 * The nature used 
	 */
	public IPythonNature nature;
	
	/**
	 * The python editor. May be null (especially on tests)
	 */
	public PyEdit pyEdit;
	
	/**
	 * The module for the passed document. Has a getter that caches the result here.
	 */
    private IModule module;
    
    /**
     * The module name (may be null)
     */
	public String moduleName;
	
	/**
	 * The new name in a refactoring (may be null if not applicable)
	 */
	public String inputName;
	
	/**
	 * The initial representation of the selected name
	 */
	public String initialName;
    
    /**
     * Default constructor... the user is responsible for filling the needed information
     * later.
     */
    public RefactoringRequest() {
    }
    
	/**
	 * If the file is passed, we also set the document automatically
	 * @param file the file correspondent to this request
	 */
	public RefactoringRequest(File file, PySelection selection, PythonNature nature) {
		this(file, selection, null, nature, null); 
	}

    /**
     * Assigns parameters to attributes (tries to resolve the module name and create a SystemPythonNature if the 
     * nature is not specified)
     */
	public RefactoringRequest(File file, PySelection ps, IProgressMonitor monitor, IPythonNature nature, PyEdit pyEdit) {
		this.file = file;
		this.ps = ps;
		this.pushMonitor(monitor);
        
		if(nature == null){
		    Tuple<SystemPythonNature,String> infoForFile = PydevPlugin.getInfoForFile(file);
		    if(infoForFile != null){
		        this.nature = infoForFile.o1;
                this.moduleName = infoForFile.o2;
		    }
		}else{
		    this.nature = nature;
		    if(file != null){
		        this.moduleName = resolveModule();
		    }
        }
        
		this.pyEdit = pyEdit;
	}

    /**
     * Used to make the work communication (also checks to see if it has been cancelled)
     * @param desc Some string to be shown in the progress
     */
    public synchronized void communicateWork(String desc) throws OperationCanceledException {
        if(getMonitor() != null){
            getMonitor().setTaskName(desc);
            getMonitor().worked(1);
            checkCancelled();
        }
    }
    
    /**
     * Checks if the process was cancelled (throws CancelledException in this case)
     */
    public void checkCancelled() throws OperationCanceledException {
        if(getMonitor().isCanceled()){
            throw new OperationCanceledException();
        }
    }


	/**
	 * @return the module name or null if it is not possible to determine the module name
	 */
	public String resolveModule(){
		if(moduleName == null){
			if (file != null && nature != null){
				moduleName = nature.resolveModule(file);
			}
		}
		return moduleName;
	}
	
    // Some shortcuts to the PySelection
	/**
	 * @return the initial column selected (starting at 0)
	 */
	public int getBeginCol() {
	    return ps.getAbsoluteCursorOffset() - ps.getStartLine().getOffset();
	}
    
    /**
     * @return the final column selected (starting at 0)
     */
    public int getEndCol() {
        return ps.getAbsoluteCursorOffset() + ps.getSelLength() - ps.getEndLine().getOffset();
    }

    /**
     * @return the last line selected (starting at 1)
     */
    public int getEndLine() {
        return ps.getEndLineIndex() + 1;
    }

    /**
     * @return the initial line selected (starting at 1)
     */
    public int getBeginLine() {
        return ps.getStartLineIndex() + 1;
    }


	/**
	 * @return the module for the document (may return the ast from the pyedit if it is available).
	 */
	public IModule getModule() {
		if(module == null){
            if(pyEdit != null){
                SimpleNode ast = pyEdit.getAST();
                if(ast != null){
                    module = AbstractModule.createModule(ast, file, resolveModule());
                }
            }
            
            if(module == null){
    			module= AbstractModule.createModuleFromDoc(
    				   resolveModule(), file, ps.getDoc(), 
    				   nature, getBeginLine());
            }
		}
		return module;
	}
	
	/**
	 * @return the ast for the current module
	 */
    public SimpleNode getAST() {
    	IModule mod = getModule();
    	if(mod instanceof SourceModule){
    		return ((SourceModule)mod).getAst();
    	}
        return null;
    }

    /**
     * Fills the initial name and initial offset from the PySelection
     */
    public void fillInitialNameAndOffset(){
        try {
            Tuple<String, Integer> currToken = ps.getCurrToken();
            initialName = currToken.o1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the current document where this refactoring request was asked
     */
    public IDocument getDoc() {
        return ps.getDoc();
    }

    /**
     * Sets the monitor to be used (because it may change depending on the current step we're in)
     * @param monitor the monitor to be used
     */
    public void pushMonitor(IProgressMonitor monitor){
        this.monitors.push(monitor);
    }
    
    /**
     * Removes and returns the current top-most progress monitor
     */
    public IProgressMonitor popMonitor(){
        return this.monitors.pop();
    }
    
    /**
     * @return the current progress monitor 
     */
    public IProgressMonitor getMonitor() {
        return this.monitors.peek();
    }

    /**
     * Creates a new SubMonitor (that will act on the current monitor)
     */
    public void createSubMonitor(int i) {
        IProgressMonitor monitor = getMonitor();
        if(monitor == null){
            monitor = new NullProgressMonitor();
        }
        pushMonitor(new SubProgressMonitor(monitor, 50));        
    }



}