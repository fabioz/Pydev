/*
 * Created on May 16, 2006
 */
package com.python.pydev.analysis.scopeanalysis;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;

import com.python.pydev.analysis.messages.AbstractMessage;
import com.python.pydev.analysis.visitors.Found;
import com.python.pydev.analysis.visitors.GenAndTok;
import com.python.pydev.analysis.visitors.ScopeItems;

/**
 * This class is used to discover the occurrences of some token having its scope as something important.
 */
public class ScopeAnalyzerVisitor extends AbstractScopeAnalyzerVisitor{

    private String nameToFind;
	private PySelection ps;
	private List<Found> foundOccurrences = new ArrayList<Found>();

	public ScopeAnalyzerVisitor(IPythonNature nature, String moduleName, IModule current,  
            IDocument document, IProgressMonitor monitor, PySelection ps) {
        super(nature, moduleName, current, document, monitor);
        
        try {
			Tuple<String, Integer> currToken = ps.getCurrToken();
			nameToFind = currToken.o1;
			
			this.ps = ps;
		} catch (BadLocationException e) {
			Log.log(e);
		}
    }

    @Override
    protected void onAddUndefinedMessage(IToken token) {
    }

    @Override
    protected void onAddUndefinedVarInImportMessage(IToken foundTok) {
    }

    @Override
    protected void onLastScope(ScopeItems m) {
    }

    @Override
    public void onAddUnusedMessage(Found found) {
    }

    @Override
    public void onAddReimportMessage(Found newFound) {
    }

    @Override
    public void onAddUnresolvedImport(IToken token) {
    }

    @Override
    public void onAddDuplicatedSignature(SourceToken token, String name) {
    }

    @Override
    public void onAddNoSelf(SourceToken token, Object[] objects) {
    }

	@Override
	protected void onAfterStartScope(int newScopeType, SimpleNode node) {
	}

	@Override
	protected void onBeforeEndScope(SimpleNode node) {
	}

	@Override
	protected void onAfterVisitAssign(Assign node) {
	}

    @Override
    protected void onAfterEndScope(SimpleNode node, ScopeItems m) {
		System.out.println("Finishing: " + m);
		Found found = m.get(this.nameToFind);
		List<GenAndTok> all = found.getAll();
		for (GenAndTok tok : all) {
			int startLine = AbstractMessage.getStartLine(tok.generator, this.document)-1;
			int endLine = AbstractMessage.getEndLine(tok.generator, this.document)-1;

			int startCol = AbstractMessage.getStartCol(tok.generator, this.document, tok.generator.getRepresentation())-1;
			int endCol = AbstractMessage.getEndCol(tok.generator, this.document, tok.generator.getRepresentation())-1;

			int absoluteCursorOffset = ps.getAbsoluteCursorOffset();
			try {
				IRegion region = document.getLineInformationOfOffset(absoluteCursorOffset);
				int currLine = document.getLineOfOffset(absoluteCursorOffset);
				int currCol = absoluteCursorOffset - region.getOffset();
				
				if(currLine >= startLine && currLine <= endLine && currCol >= startCol && currCol <= endCol){
					//ok, it's a valid occurrence, so, let's add it.
					foundOccurrences.add(found);
					break;
				}
			} catch (Exception e) {
				Log.log(e);
			}
		}
		System.out.println("Found: " + found);
		
    }
    
	public List<Found> getOccurrences() {
		endScope(null); //finish the last scope
		
		return new ArrayList<Found>(foundOccurrences);
	}
	
}
