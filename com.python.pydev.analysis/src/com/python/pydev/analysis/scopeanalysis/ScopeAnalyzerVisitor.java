/*
 * Created on May 16, 2006
 */
package com.python.pydev.analysis.scopeanalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.analysis.messages.AbstractMessage;
import com.python.pydev.analysis.visitors.Found;
import com.python.pydev.analysis.visitors.GenAndTok;
import com.python.pydev.analysis.visitors.ScopeItems;
import com.python.pydev.analysis.visitors.ImportChecker.ImportInfo;

/**
 * This class is used to discover the occurrences of some token having its scope as something important.
 */
public class ScopeAnalyzerVisitor extends AbstractScopeAnalyzerVisitor{

	private String completeNameToFind="";
    private String nameToFind="";
    
    /**
     * List of tuple with: 
     * the token found
     * the delta to the column that the token we're looking for was found
     * the entry that is the parent of this found
     */
	private List<Tuple3<Found, Integer, ASTEntry>> foundOccurrences = new ArrayList<Tuple3<Found, Integer, ASTEntry>>();
	private FastStack<ASTEntry> parents; //initialized on demand
	
	/**
	 * Keeps the variables that are really undefined (we keep them here if there's still a chance that
	 * what we're looking for is an undefined variable and all in the same scope should also be marked
	 * as that same undefined).
	 */
	private List<Found> undefinedFound = new ArrayList<Found>();

	/**
	 * This is the key in the importsFoundFromModuleName for tokens that were not resolved.
	 */
	private static final String UNRESOLVED_MOD_NAME = "__UNRESOLVED__MOD__NAME__!";
	
	/**
	 * It contains import artificially generated, such as module names in ImportFrom
	 * E.g.: from os.path import xxx will generate an import for 'os' and an import for 'path'
	 * artificially, just to make matches
	 */
	private Map<String, List<Tuple3<Found, Integer, ASTEntry>>> importsFoundFromModuleName = new HashMap<String, List<Tuple3<Found, Integer, ASTEntry>>>();
	/**
	 * Same as the importsFoundFromModuleName, but works on the imports that actually become tokens
	 * in the namespace.
	 */
	private Map<String, List<Tuple3<Found, Integer, ASTEntry>>> importsFound = new HashMap<String, List<Tuple3<Found, Integer, ASTEntry>>>();
	
	/**
	 * This one is not null only if it is the name we're looking for in the exact position (even if it does
	 * not have a definition).
	 */
	private Found hitAsUndefined = null;
	
	private boolean finished = false;
    private int currLine;
    private int currCol;

    /**
     * Constructor when we have a PySelection object
     * @throws BadLocationException
     */
	public ScopeAnalyzerVisitor(IPythonNature nature, String moduleName, IModule current,  
	        IDocument document, IProgressMonitor monitor, PySelection ps) throws BadLocationException {
        this(nature, moduleName, current, document, monitor, ps.getCurrToken().o1, 
                ps.getAbsoluteCursorOffset(), ps.getActivationTokenAndQual(true));
        
    }
    
    /**
     * Base constructor (when a PySelection is not available)
     * @throws BadLocationException 
     */
	public ScopeAnalyzerVisitor(IPythonNature nature, String moduleName, IModule current,  
            IDocument document, IProgressMonitor monitor, String pNameToFind, int absoluteCursorOffset,
            String[] tokenAndQual) throws BadLocationException {
        
        super(nature, moduleName, current, document, monitor);
        IRegion region = document.getLineInformationOfOffset(absoluteCursorOffset);
        currLine = document.getLineOfOffset(absoluteCursorOffset);
        currCol = absoluteCursorOffset - region.getOffset();

		nameToFind = pNameToFind;
		completeNameToFind = tokenAndQual[0]+tokenAndQual[1];
    }

	@Override
	public Object visitImportFrom(ImportFrom node) throws Exception {
	    Object ret = super.visitImportFrom(node);
        //the import from will generate the tokens that go into the module namespace, but still, it needs to
        //create tokens that will not be used in code-analysis, but will be used in matching tokens
        //regarding its module.
	    NameTok tokModName = (NameTok)node.module;
		for(String m: new FullRepIterable(tokModName.id)){
			if(m.indexOf(".") == -1){
				aliasType[] names = new aliasType[1];
				NameTok importNameTok = new NameTok(m, NameTok.ImportModule);
				
				importNameTok.beginLine = tokModName.beginLine;
				importNameTok.beginColumn = tokModName.beginColumn;
				
				names[0] = new aliasType(importNameTok, null);
				names[0].beginLine = tokModName.beginLine;
				names[0].beginColumn = tokModName.beginColumn;
				
				Import importTok = new Import(names);
				importTok.beginLine = tokModName.beginLine;
				importTok.beginColumn = tokModName.beginColumn;
				
				List<IToken> createdTokens = AbstractVisitor.makeImportToken(importTok, null, "", true);
				for (IToken token : createdTokens) {
					ImportInfo info = this.scope.importChecker.visitImportToken(token, false);
					Found found = new Found(token, token, scope.getCurrScopeId(), scope.getCurrScopeItems());
					found.importInfo = info;
					
					addFoundToImportsMap(found, importsFoundFromModuleName);
				}
			}
	    }
        return ret;
	}

	/**
	 * Used to add some Found that is related to an import to a 'global import register'.
	 * This is needed because, unlike other regular tokens, we want to find imports that are
	 * in diferent contexts as being in the same context.
	 * 
	 * @param found this is the Found that we want to add to the imports
	 * @param map this is the map that contains the imports Found occurrences (it has to be passed,
	 * as there is a map for the imports that are actually in the namespace and another for those
	 * that are 'artificially' generated).
	 */
	private void addFoundToImportsMap(Found found, Map<String, List<Tuple3<Found, Integer, ASTEntry>>> map) {
		ImportInfo info = found.importInfo;
		String modName = UNRESOLVED_MOD_NAME;
		if(info.mod != null){
			modName = info.mod.getName();
		}
		List<Tuple3<Found, Integer, ASTEntry>> prev = map.get(modName);
		if(prev == null){
			prev = new ArrayList<Tuple3<Found, Integer, ASTEntry>>();
			map.put(modName, prev);
		}
		prev.add(new Tuple3<Found, Integer, ASTEntry>(found, 0, peekParent()));
	}
	
	@Override
	public void onImportInfoSetOnFound(Found found) {
		super.onImportInfoSetOnFound(found);
		addFoundToImportsMap(found, importsFound);
	}

    @Override
    protected void onLastScope(ScopeItems m) {
		//not found
        for(Found found: probablyNotDefined){
        	ASTEntry parent = peekParent();
        	if(checkFound(found, parent) == null){
        		//ok, it was actually not found, so, after marking it as an occurrence, we have to check all 
        		//the others that have the same representation in its scope.
        		String rep = found.getSingle().generator.getRepresentation();
        		if(FullRepIterable.containsPart(rep, nameToFind)){
        			undefinedFound.add(found);
        		}
        	}else{
        		hitAsUndefined = found;
        	}
        }
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
    protected void onAfterAddToNamesToIgnore(ScopeItems currScopeItems, Tuple<IToken, Found> tup) {
    	if(tup.o1 instanceof SourceToken){
			checkFound(tup.o2, peekParent());
    	}
    }
    
    @Override
    protected boolean doCheckIsInNamesToIgnore(String rep, IToken token) {
    	org.python.pydev.core.Tuple<IToken, Found> found = scope.isInNamesToIgnore(rep);
    	if(found != null){
    		found.o2.getSingle().references.add(token);
            checkToken(found.o2, token, peekParent());
    	}
    	return found != null;
    }
    

    @Override
    protected void onFoundUnresolvedImportPart(IToken token, String rep, Found foundAs) {
        onAddUndefinedMessage(token, foundAs);
    }
    
    @Override
    protected void onAddUndefinedVarInImportMessage(IToken token, Found foundAs) {
        onAddUndefinedMessage(token, foundAs);
    }

    
    @Override
    protected void onAddUndefinedMessage(IToken token, Found found) {
    	ASTEntry parent = peekParent();
    	if(checkFound(found, parent) == null){
    		//ok, it was actually not found, so, after marking it as an occurrence, we have to check all 
    		//the others that have the same representation in its scope.
    		if(token.getRepresentation().equals(nameToFind)){
    			undefinedFound.add(found);
    		}
    	}else{
    		hitAsUndefined = found;
    	}
    }


    /**
     * If the 'parents' stack is higher than 0, peek it (may return null)
     */
	private ASTEntry peekParent() {
		ASTEntry parent = null;
    	if(parents.size() > 0){
    		parent = parents.peek();
    	}
		return parent;
	}

	/**
	 * If the 'parents' stack is higher than 0, pop it (may return null)
	 */
	private ASTEntry popParent(SimpleNode node) {
		ASTEntry parent = null;
    	if(node != null){
    		parent = parents.pop();
    	}
		return parent;
	}
    
	/**
	 * When we start the scope, we have to put an entry in the parents.
	 */
	@Override
	protected void onAfterStartScope(int newScopeType, SimpleNode node) {
		if(parents == null){
			parents = new FastStack<ASTEntry>();
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
    
	/**
	 * If it is still not finished we'll have to finish it (end the last scope).
	 */
	private void checkFinished() {
		if(!finished){
			finished = true;
			endScope(null); //finish the last scope
		}
	}

    @Override
    protected void onAfterEndScope(SimpleNode node, ScopeItems m) {
        if(hitAsUndefined == null){
            for (String rep : new FullRepIterable(this.completeNameToFind, true)){
                Found found = m.get(rep);
                if(found != null){
                    if(checkFound(node, found) != null){
                        return;
                    }
                }
                
            }
            
        }else{ //(hitAsUndefined != null)
            
            String foundRep = hitAsUndefined.getSingle().generator.getRepresentation();
            
            if(foundRep.indexOf('.') == -1 || FullRepIterable.containsPart(foundRep,nameToFind)){
                //now, there's a catch here, if we found it as an attribute,
                //we cannot get the locals
                for(Found f :this.undefinedFound){
                	if(f.getSingle().generator.getRepresentation().startsWith(foundRep)){
                        if (foundOccurrences.size() == 1){
	                        Tuple3<Found, Integer, ASTEntry> hit = foundOccurrences.get(0);
	                        Tuple3<Found, Integer, ASTEntry> foundOccurrence = new Tuple3<Found, Integer, ASTEntry>(f, hit.o2, hit.o3);
	                        addFoundOccurrence(foundOccurrence);
                        }
                	}
                }
            }
    	}
    	
    }

    private Found checkFound(SimpleNode node, Found found) {
    	ASTEntry parent = popParent(node);
		return checkFound(found, parent);
    }

    
	private Found checkFound(Found found, ASTEntry parent) {
		if(found == null){
			return null;
		}
		List<GenAndTok> all = found.getAll();
		
		try {
			for (GenAndTok gen : all) {
				for (IToken tok2 : gen.getAllTokens()) {
					if(checkToken(found, tok2, parent)){
						return found; //ok, found it
					}
				}
			}
		} catch (Exception e) {
			Log.log(e);
		}
		return null;
	}

	private boolean checkToken(Found found, IToken generator, ASTEntry parent) {
		int startLine = AbstractMessage.getStartLine(generator, this.document)-1;
		int endLine = AbstractMessage.getEndLine(generator, this.document, false)-1;
		
		int startCol = AbstractMessage.getStartCol(generator, this.document, generator.getRepresentation(), true)-1;
		int endCol = AbstractMessage.getEndCol(generator, this.document, generator.getRepresentation(), false)-1;
		if(currLine >= startLine && currLine <= endLine && currCol >= startCol && currCol <= endCol){
			Tuple3<Found, Integer, ASTEntry> foundOccurrence = new Tuple3<Found, Integer, ASTEntry>(found, currCol-startCol, parent);
			//ok, it's a valid occurrence, so, let's add it.
			addFoundOccurrence(foundOccurrence);
			return true;
		}
		return false;
	}

	/**
	 * Used to add an occurrence to the found occurrences.
	 * @param foundOccurrence
	 */
	private void addFoundOccurrence(Tuple3<Found, Integer, ASTEntry> foundOccurrence) {
		foundOccurrences.add(foundOccurrence);
	}
    

	/**
	 * @return all the token occurrences
	 */
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
        Set<Tuple3<String, Integer, Integer>> s = new HashSet<Tuple3<String, Integer, Integer>>(); 
		
		ArrayList<Tuple3<IToken, Integer, ASTEntry>> complete = getCompleteTokenOccurrences();
		ArrayList<ASTEntry> ret = new ArrayList<ASTEntry>();
		
		for (Tuple3<IToken, Integer, ASTEntry> tup: complete) {
			IToken token = tup.o1;
			if(!(token instanceof SourceToken)){ // we want only the source tokens for this module
				continue;
			}
			
			//if it is different, we have to make partial names
			SourceToken sourceToken = (SourceToken)tup.o1;
			SimpleNode ast = (sourceToken).getAst();
			
			String representation = null;
			
			if(ast instanceof ImportFrom){
				ImportFrom f = (ImportFrom) ast;
				//f.names may be empty if it is a wild import
				for (aliasType t : f.names){
					NameTok importName = NodeUtils.getNameForAlias(t);
					String importRep = NodeUtils.getFullRepresentationString(importName);
					
					if(importRep.equals(nameToFind)){
						ast = importName;
						representation = importRep;
						break;
					}
					
				}
				
			}else if(ast instanceof Import){
				representation = NodeUtils.getFullRepresentationString(ast);
				Import f = (Import) ast;
				NameTok importName = NodeUtils.getNameForRep(f.names, representation);
				if(importName != null){
					ast = importName;
				}
				
				
			}else{
				representation = NodeUtils.getFullRepresentationString(ast);
			}
			
			if(representation == null){
				continue; //can happen on wild imports
			}
			if(nameToFind.equals(representation)){
				ret.add(new ASTEntry(tup.o3, ast));
				continue;
			}
            if(!FullRepIterable.containsPart(representation, nameToFind)){
                continue;
            }
			
			Name nameAst = new Name(nameToFind, Name.Store);
			String[] strings = FullRepIterable.dotSplit(representation);
			
			int plus = 0;
			for (String string : strings) {
				if(string.equals(nameToFind) && (plus + nameToFind.length() >= tup.o2) ){
					break;
				}
				plus += string.length()+1; //len + dot
			}
			nameAst.beginColumn = AbstractMessage.getStartCol(token, document)+plus;
			nameAst.beginLine = AbstractMessage.getStartLine(token, document);
            Tuple3<String, Integer, Integer> t = new Tuple3<String, Integer, Integer>(nameToFind, nameAst.beginColumn, nameAst.beginLine);
            if (!s.contains(t)){
                s.add(t);
                ret.add(new ASTEntry(tup.o3, nameAst));
            }
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
			getImportEntries(found, ret, f);
			
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

	/**
	 * This method finds entries for found tokens that are the same import, but that may still not be there
	 * because they are either in some other scope or are in the module part of an ImportFrom
	 */
	private void getImportEntries(Tuple3<Found, Integer, ASTEntry> found, ArrayList<Tuple3<IToken, Integer, ASTEntry>> ret, Set<Tuple<IToken, Integer>> f) {
		if(found.o1.isImport()){
			//now, as it is an import, we have to check if there are more matching imports found
			String key = UNRESOLVED_MOD_NAME;
			if(found.o1.importInfo.mod != null){
				key = found.o1.importInfo.mod.getName();
			}
			List<Tuple3<Found, Integer, ASTEntry>> unresolved = importsFound.get(key);
			List<Tuple3<Found, Integer, ASTEntry>> fromModule = importsFoundFromModuleName.get(key);
			
			if(fromModule != null){
				for (Tuple3<Found, Integer, ASTEntry> foundInFromModule : fromModule) {
					IToken generator = foundInFromModule.o1.getSingle().generator;
					Tuple<IToken, Integer> tup = new Tuple<IToken, Integer>(generator, foundInFromModule.o2);
					Tuple3<IToken, Integer, ASTEntry> tup3 = new Tuple3<IToken, Integer, ASTEntry>(generator, foundInFromModule.o2, foundInFromModule.o3);
					
					if(!f.contains(tup)){
						f.add(tup);
						ret.add(tup3);
					}
				}
			}
		}
	}
	
	
	
}
