/**
 * 
 */
package org.python.pydev.editor.refactoring;

import java.io.File;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction.Operation;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * This class encapsulates all the info needed in order to do a refactoring
 */
public class RefactoringRequest{
	
	/**
	 * The file associated with the editor where the refactoring is being requested
	 */
	public File file;
	
	/**
	 * The document used in the refactoring 
	 */
	public IDocument doc;
	
	/**
	 * The current selection when the refactoring was requested
	 */
	public PySelection ps;
	

	/**
	 * The operation that does the refactoring. Used to give feedback to the user 
	 */
	public Operation operation;
	
	/**
	 * The nature used 
	 */
	public IPythonNature nature;
	
	/**
	 * The python editor. May be null (especially on tests)
	 */
	public PyEdit pyEdit;
	
	/**
	 * The module for the passed document
	 */
    private IModule module;
    
    /**
     * The module name (may be null)
     */
	public String moduleName;
    
    /**
     * Information acquired during the refactoring process -- see class for more info.
     */
    public DuringProcessInfo duringProcessInfo;
    
    /**
     * may be set to false if we are not interested in searching the additional info for definitions 
     */
    public boolean findDefinitionInAdditionalInfo = true;
    
    /**
     * may be set to true if we only care about the local scope
     */
    public boolean findReferencesOnlyOnLocalScope = false;
    
    /**
     * This class contains information that is acquired during the refactoring process (such as the initial or final
     * name of what we are renaming, etc).
     */
    public static class DuringProcessInfo{
        /**
         * The new name in a refactoring (may be null if not applicable)
         */
        public String name;
        public String initialName;
        public int initialOffset;
        
    }

    /**
     * Default constructor... the user is responsible for filling the needed information
     * later.
     */
    public RefactoringRequest() {
    }
    
	/**
	 * If the file is passed, we also set the document automatically
	 * @param f the file correspondent to this request
	 */
	public RefactoringRequest(File f, PySelection selection, PythonNature n) {
		this(f, selection.getDoc(), selection, null, n, null); 
	}

	public RefactoringRequest(File f, IDocument doc, PySelection ps2, Operation operation2, IPythonNature nature2, PyEdit pyEdit2) {
        this.duringProcessInfo = new DuringProcessInfo();
		this.file = f;
		this.doc = doc;
		this.ps = ps2;
		this.operation = operation2;
		this.nature = nature2;
		this.pyEdit = pyEdit2;
		if(f != null){
			this.moduleName = resolveModule();
		}
	}

    public synchronized void communicateWork(String desc) {
        if(operation != null){
            operation.monitor.setTaskName(desc);
            operation.monitor.worked(1);
            
            if(operation.monitor.isCanceled()){
                throw new CancelledException();
            }
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
	
    /**
     * @return
     */
    public int getEndCol() {
        return ps.getAbsoluteCursorOffset() + ps.getSelLength() - ps.getEndLine().getOffset();
    }

    /**
     * @return
     */
    public int getEndLine() {
        return ps.getEndLineIndex() + 1;
    }

    /**
     * @return
     */
    public int getBeginCol() {
        return ps.getAbsoluteCursorOffset() - ps.getStartLine().getOffset();
    }

    /**
     * @return
     */
    public int getBeginLine() {
        return ps.getStartLineIndex() + 1;
    }

	public int getOffset() {
		return ps.getAbsoluteCursorOffset();
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
    				   resolveModule(), file, doc, 
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

    public void fillInitialNameAndOffset(){
        try {
            Tuple<String, Integer> currToken = ps.getCurrToken();
            duringProcessInfo.initialName = currToken.o1;
            duringProcessInfo.initialOffset = currToken.o2;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}