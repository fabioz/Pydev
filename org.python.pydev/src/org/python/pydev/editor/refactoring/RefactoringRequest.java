/**
 * 
 */
package org.python.pydev.editor.refactoring;

import java.io.File;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction.Operation;

public class RefactoringRequest{
	public File file;
	public IDocument doc;
	public PySelection ps;
	public String name;
	public Operation operation;
	public IPythonNature nature;
	public PyEdit pyEdit;

	public RefactoringRequest() {
		
	}

	/**
	 * If the file is passed, we also set the document automatically
	 * @param f the file correspondent to this request
	 */
	public RefactoringRequest(File f) {
		this.file = f;
		this.doc = new Document(REF.getFileContents(f));
	}
	

	public String resolveModule(){
		if (file != null){
			return nature.resolveModule(file);
		}else{
			return null;
		}
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


}