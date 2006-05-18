/*
 * Created on May 16, 2006
 */
package com.python.pydev.analysis.scopeanalysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;

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
	private List<Tuple3<Found, Integer, ASTEntry>> foundOccurrences = new ArrayList<Tuple3<Found, Integer, ASTEntry>>();
	private Stack<ASTEntry> parents; //initialized on demand
	
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
		if(parents == null){
			parents = new Stack<ASTEntry>();
		}
		if(node == null){
			return;
		}
		if(parents.size() == 0){
			parents.push(new ASTEntry(null, node));
		}else{
			parents.add(new ASTEntry(parents.peek(), node));
		}
	}

	@Override
	protected void onBeforeEndScope(SimpleNode node) {
	}

	@Override
	protected void onAfterVisitAssign(Assign node) {
	}
	
	private void checkFinished() {
		if(!finished){
			finished = true;
			endScope(null); //finish the last scope
		}
	}

    @Override
    protected void onAfterEndScope(SimpleNode node, ScopeItems m) {
    	ASTEntry parent = null;
    	if(node != null){
    		parent = parents.pop();
    	}
    	
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
					if(checkToken(found, currLine, currCol, tok2, parent)){
						return; //ok, found it
					}
				}
			}
		} catch (Exception e) {
			Log.log(e);
		}
    }

	private boolean checkToken(Found found, int currLine, int currCol, IToken generator, ASTEntry parent) {
		int startLine = AbstractMessage.getStartLine(generator, this.document)-1;
		int endLine = AbstractMessage.getEndLine(generator, this.document, false)-1;
		
		int startCol = AbstractMessage.getStartCol(generator, this.document, generator.getRepresentation())-1;
		int endCol = AbstractMessage.getEndCol(generator, this.document, generator.getRepresentation(), false)-1;
		if(currLine >= startLine && currLine <= endLine && currCol >= startCol && currCol <= endCol){
			//ok, it's a valid occurrence, so, let's add it.
			foundOccurrences.add(new Tuple3<Found, Integer, ASTEntry>(found, currCol-startCol, parent));
			return true;
		}
		return false;
	}
    

	public List<IToken> getTokenOccurrences() {
		List<IToken> ret = new ArrayList<IToken>();
		
		List<ASTEntry> entryOccurrences = getEntryOccurrences();
		for (ASTEntry entry : entryOccurrences) {
			ret.add(AbstractVisitor.makeToken(entry.node, moduleName));
		}
		return ret;
	}
	
	/**
	 * We get the occurrences as tokens for the name we're looking for. Note that the complete name (may be a dotted name)
	 * we're looking for may not be equal to the 'partial' name.
	 * 
	 * This can happen when we're looking for some import such as os.path, and are looking just for the 'path' part.
	 * So, when this happens, the return is analyzed and only returns names as the one we're looking for (with
	 * the correct line and col positions). 
	 */
	public List<ASTEntry> getEntryOccurrences() {
		checkFinished();
		
		ArrayList<Tuple3<IToken, Integer, ASTEntry>> complete = getCompleteTokenOccurrences();
		ArrayList<ASTEntry> ret = new ArrayList<ASTEntry>();
		
		for (Tuple3<IToken, Integer, ASTEntry> tup: complete) {
			IToken token = tup.o1;
			
			//if it is different, we have to make partial names
			SimpleNode ast = ((SourceToken)tup.o1).getAst();
			String representation = NodeUtils.getFullRepresentationString(ast);
			
			if(nameToFind.equals(representation)){
				ret.add(new ASTEntry(tup.o3, ast));
				continue;
			}
			
			Name nameAst = new Name(nameToFind, Name.Store);
			String[] strings = representation.split("\\.");
			
			int plus = 0;
			for (String string : strings) {
				if(string.equals(nameToFind) && (plus + nameToFind.length() >= tup.o2) ){
					break;
				}
				plus += string.length()+1; //len + dot
			}
			nameAst.beginColumn = AbstractMessage.getStartCol(token, ps.getDoc())+plus;
			nameAst.beginLine = AbstractMessage.getStartLine(token, ps.getDoc());
			ret.add(new ASTEntry(tup.o3, nameAst));
		}
		
		return ret;
	}

	/**
	 * @return all the occurrences found in a 'complete' way (dotted name).
	 */
	private ArrayList<Tuple3<IToken, Integer, ASTEntry>> getCompleteTokenOccurrences() {
		//that's because we don't want duplicates
		Set<Tuple<IToken, Integer>> f = new HashSet<Tuple<IToken, Integer>>();
		
		ArrayList<Tuple3<IToken, Integer, ASTEntry>> ret = new ArrayList<Tuple3<IToken, Integer, ASTEntry>>();
		
		for (Tuple3<Found, Integer, ASTEntry> found : foundOccurrences) {
			
			List<GenAndTok> all = found.o1.getAll();
			
			for (GenAndTok tok : all) {
				
				Tuple<IToken, Integer> tup = new Tuple<IToken, Integer>(tok.generator, found.o2);
				Tuple3<IToken, Integer, ASTEntry> tup3 = new Tuple3<IToken, Integer, ASTEntry>(tok.generator, found.o2, found.o3);
				
				if(!f.contains(tup)){
					f.add(tup);
					ret.add(tup3);
				}
				
				for (IToken t: tok.references){
					tup = new Tuple<IToken, Integer>(t, found.o2);
					tup3 = new Tuple3<IToken, Integer, ASTEntry>(t, found.o2, found.o3);
					if(!f.contains(tup)){
						f.add(tup);
						ret.add(tup3);
					}
				}
			}
		}
		return ret;
	}
	
	
	
}
