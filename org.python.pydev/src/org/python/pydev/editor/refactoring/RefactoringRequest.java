/**
 * 
 */
package org.python.pydev.editor.refactoring;

import java.io.File;

import org.eclipse.jface.text.IDocument;
import org.python.parser.SimpleNode;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction.Operation;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
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
	 * The new name in a refactoring (may be null if not applicable)
	 */
	public String name;

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
	private String moduleName;


	/**
	 * If the file is passed, we also set the document automatically
	 * @param f the file correspondent to this request
	 */
	public RefactoringRequest(File f, PySelection selection, PythonNature n) {
		this(f, selection.getDoc(), selection, null, null, n, null); 
	}

	public RefactoringRequest(File f, IDocument doc, PySelection ps2, String name2, Operation operation2, IPythonNature nature2, PyEdit pyEdit2) {
		this.file = f;
		this.doc = doc;
		this.ps = ps2;
		this.name = name2;
		this.operation = operation2;
		this.nature = nature2;
		this.pyEdit = pyEdit2;
		if(f != null){
			this.moduleName = resolveModule();
		}
	}

	/**
	 * @return the module name or null if it is not possible to determine the module name
	 */
	public String resolveModule(){
		if(moduleName == null){
			if (file != null){
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
	 * @return the module for the document
	 */
	public IModule getModule() {
		if(module == null){
			module= AbstractModule.createModuleFromDoc(
				   resolveModule(), file, doc, 
				   nature, getBeginLine());
		}
		return module;
	}
	
	/**
	 * @return the ast for the current module
	 */
    public SimpleNode getAST() {
    	IModule mod = getModule();
    	if(mod instanceof SourceModule){
    		((SourceModule)mod).getAst();
    	}
        return null;
    }

    /**
     * @return the token and the full qualifier.
     */
	public String[] getTokenAndQual() {
		return PyCodeCompletion.getActivationTokenAndQual(doc, ps.getAbsoluteCursorOffset(), true);
	}


}