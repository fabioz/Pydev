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
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Name;

import com.python.pydev.analysis.messages.AbstractMessage;
import com.python.pydev.analysis.visitors.Found;
import com.python.pydev.analysis.visitors.GenAndTok;
import com.python.pydev.analysis.visitors.ScopeItems;

/**
 * This class is used to discover the occurrences of some token having its scope as something important.
 */
public class ScopeAnalyzerVisitor extends AbstractScopeAnalyzerVisitor{

    private String completeNameToFind="";
    private String nameToFind="";
	private PySelection ps;
	private List<Found> foundOccurrences = new ArrayList<Found>();
	private boolean finished = false;

	public ScopeAnalyzerVisitor(IPythonNature nature, String moduleName, IModule current,  
            IDocument document, IProgressMonitor monitor, PySelection ps) {
        super(nature, moduleName, current, document, monitor);
        
        try {
			Tuple<String, Integer> currToken = ps.getCurrToken();
			nameToFind = currToken.o1;
			
			String[] tokenAndQual = ps.getActivationTokenAndQual(true);
			completeNameToFind = tokenAndQual[0]+tokenAndQual[1];
			
		} catch (BadLocationException e) {
			Log.log(e);
		}
		this.ps = ps;
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
		Found found = m.get(this.completeNameToFind);
		if(found == null){
			return;
		}
		List<GenAndTok> all = found.getAll();
		int absoluteCursorOffset = ps.getAbsoluteCursorOffset();
		try {
			IRegion region = document.getLineInformationOfOffset(absoluteCursorOffset);
			int currLine = document.getLineOfOffset(absoluteCursorOffset);
			int currCol = absoluteCursorOffset - region.getOffset();
			
			for (GenAndTok gen : all) {
				for (IToken tok2 : gen.getAllTokens()) {
					if(checkToken(found, currLine, currCol, tok2)){
						return; //ok, found it
					}
				}
			}
		} catch (Exception e) {
			Log.log(e);
		}
		
    }

	private boolean checkToken(Found found, int currLine, int currCol, IToken generator) {
		int startLine = AbstractMessage.getStartLine(generator, this.document)-1;
		int endLine = AbstractMessage.getEndLine(generator, this.document, false)-1;
		
		int startCol = AbstractMessage.getStartCol(generator, this.document, generator.getRepresentation())-1;
		int endCol = AbstractMessage.getEndCol(generator, this.document, generator.getRepresentation(), false)-1;
		if(currLine >= startLine && currLine <= endLine && currCol >= startCol && currCol <= endCol){
			//ok, it's a valid occurrence, so, let's add it.
			foundOccurrences.add(found);
			return true;
		}
		return false;
	}
    
	public List<Found> getFoundOccurrences() {
		if(!finished){
			finished = true;
			endScope(null); //finish the last scope
		}
		
		return new ArrayList<Found>(foundOccurrences);
	}

	/**
	 * We get the occurrences as tokens for the name we're looking for. Note that the complete name (may be a dotted name)
	 * we're looking for may not be equal to the 'partial' name.
	 * 
	 * This can happen when we're looking for some import such as os.path, and are looking just for the 'path' part.
	 * So, when this happens, the return is analyzed and only returns names as the one we're looking for (with
	 * the correct line and col positions). 
	 */
	public List<IToken> getTokenOccurrences() {
		ArrayList<IToken> complete = getCompleteTokenOccurrences();
		ArrayList<IToken> ret = new ArrayList<IToken>();
		
		for (IToken token : complete) {
			//if it is different, we have to make partial names
			String representation = token.getRepresentation();
			if(nameToFind.equals(representation)){
				ret.add(token);
				continue;
			}
			
			Name nameAst = new Name(nameToFind, Name.Store);
			String[] strings = representation.split("\\.");
			
			int plus = 0;
			for (String string : strings) {
				if(string.equals(nameToFind)){
					break;
				}
				plus += string.length()+1; //len + dot
			}
			nameAst.beginColumn = AbstractMessage.getStartCol(token, ps.getDoc())+plus;
			nameAst.beginLine = AbstractMessage.getStartLine(token, ps.getDoc());
			ret.add(AbstractVisitor.makeToken(nameAst, moduleName));
		}
		
		return ret;
	}

	/**
	 * @return all the occurrences found in a 'complete' way (dotted name).
	 */
	private ArrayList<IToken> getCompleteTokenOccurrences() {
		List<Found> foundOccurrences2 = getFoundOccurrences();
		ArrayList<IToken> ret = new ArrayList<IToken>();
		for (Found found : foundOccurrences2) {
			List<GenAndTok> all = found.getAll();
			for (GenAndTok tok : all) {
				ret.add(tok.generator);
				ret.addAll(tok.references);
			}
		}
		return ret;
	}
	
	
	
}
