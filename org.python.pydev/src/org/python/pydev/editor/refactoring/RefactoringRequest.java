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
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.PyParser;
import org.python.pydev.plugin.nature.PythonNature;

public class RefactoringRequest{
	public File file;
	public IDocument doc;
	public PySelection ps;
	public String name;
	public Operation operation;
	public IPythonNature nature;
	public PyEdit pyEdit;
    private SimpleNode ast;
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
			this.moduleName = nature.resolveModule(f);
		}
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

	public int getOffset() {
		return ps.getAbsoluteCursorOffset();
	}

	public IModule getModule() {
		return new SourceModule(moduleName, file, getAST());
	}
	
    public SimpleNode getAST() {
        if(this.ast == null){
            PyParser.ParserInfo info = new PyParser.ParserInfo(doc, true, nature);
            info.tryReparse = false;
            Object[] parse = PyParser.reparseDocument(info);
            if(parse[0] == null){
                throw new RuntimeException("Unable to get the ast.");
            }
            this.ast = (SimpleNode) parse[0];
        }
        return this.ast;
    }


}