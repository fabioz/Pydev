/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.scopeanalysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.FastStack;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;
import org.python.pydev.shared_core.structure.Tuple4;

import com.python.pydev.analysis.messages.AbstractMessage;
import com.python.pydev.analysis.visitors.Found;
import com.python.pydev.analysis.visitors.GenAndTok;
import com.python.pydev.analysis.visitors.ScopeItems;

/**
 * This is almost the same as the ScopeAnalyzer, but it won't find imports that may be related
 * nor imports that are generated from the module part of an ImportFrom
 */
public class ScopeAnalyzerVisitorWithoutImports extends AbstractScopeAnalyzerVisitor {

    public final static String FOUND_ADDITIONAL_INFO_IN_AST_ENTRY = "FOUND_ADDITIONAL_INFO_IN_AST_ENTRY";
    protected String completeNameToFind = "";
    protected String nameToFind = "";

    /**
     * List of tuple with: 
     * 
     * - the token found
     * 
     * - the delta to the column that the token we're looking for was found (delta from the currCol)
     * negative values mean that it is undefined
     * 
     * - the entry that is the parent of this found
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
    public ScopeAnalyzerVisitorWithoutImports(IPythonNature nature, String moduleName, IModule current,
            IProgressMonitor monitor, PySelection ps) throws BadLocationException {
        this(nature, moduleName, current, ps.getDoc(), monitor, ps.getCurrToken().o1, ps.getAbsoluteCursorOffset(), ps
                .getActivationTokenAndQual(true));

    }

    /**
     * Base constructor (after data from the PySelection is gotten)
     * @throws BadLocationException 
     */
    protected ScopeAnalyzerVisitorWithoutImports(IPythonNature nature, String moduleName, IModule current,
            IDocument document, IProgressMonitor monitor, String pNameToFind, int absoluteCursorOffset,
            String[] tokenAndQual) throws BadLocationException {

        super(nature, moduleName, current, document, monitor);
        if (document != null) {
            IRegion region = document.getLineInformationOfOffset(absoluteCursorOffset);
            currLine = document.getLineOfOffset(absoluteCursorOffset);
            currCol = absoluteCursorOffset - region.getOffset();
        }

        nameToFind = pNameToFind;
        completeNameToFind = tokenAndQual[0] + tokenAndQual[1];
    }

    @Override
    protected void onLastScope(ScopeItems m) {
        //not found
        for (Found found : probablyNotDefined) {
            ASTEntry parent = peekParent();
            if (checkFound(found, parent) == null) {
                //ok, it was actually not found, so, after marking it as an occurrence, we have to check all 
                //the others that have the same representation in its scope.
                String rep = found.getSingle().generator.getRepresentation();
                if (FullRepIterable.containsPart(rep, nameToFind)) {
                    undefinedFound.add(found);
                }
            } else {
                hitAsUndefined = found;
            }
        }
    }

    @Override
    public void onAddUnusedMessage(SimpleNode node, Found found) {
    }

    @Override
    public void onAddReimportMessage(Found newFound) {
    }

    @Override
    public void onAddUnresolvedImport(IToken token) {
    }

    @Override
    protected void onAfterAddToNamesToIgnore(ScopeItems currScopeItems, Tuple<IToken, Found> tup) {
        if (tup.o1 instanceof SourceToken) {
            checkFound(tup.o2, peekParent());
        }
    }

    @Override
    protected Tuple<IToken, Found> findInNamesToIgnore(String rep, IToken token) {
        org.python.pydev.shared_core.structure.Tuple<IToken, Found> found = scope.findInNamesToIgnore(rep);
        if (found != null) {
            found.o2.getSingle().references.add(token);
            checkToken(found.o2, token, peekParent());
        }
        return found;
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
    protected void onAddAssignmentToBuiltinMessage(IToken token, String representation) {
    }

    @Override
    protected void onAddToProbablyNotDefined(IToken token, Found foundForProbablyNotDefined) {
        super.onAddToProbablyNotDefined(token, foundForProbablyNotDefined);
        onAddUndefinedMessage(token, foundForProbablyNotDefined);
    }

    @Override
    protected void onNotDefinedFoundLater(Found foundInProbablyNotDefined, Found laterFound) {
        super.onNotDefinedFoundLater(foundInProbablyNotDefined, laterFound);
        if (hitAsUndefined == foundInProbablyNotDefined) {
            //we have a 'late' match as a foundInProbablyNotDefined, so, remove the hit as undefined
            hitAsUndefined = null;
        }
        //and add those generators -- task: Scope Analysis should be able to get all the class references
        Tuple3<Found, Integer, ASTEntry> tup = new Tuple3<Found, Integer, ASTEntry>(laterFound, -1, peekParent());
        laterFound.addGeneratorsFromFound(foundInProbablyNotDefined);
        addFoundOccurrence(tup);
    }

    @Override
    protected void onAddUndefinedMessage(IToken token, Found found) {
        ASTEntry parent = peekParent();
        if (checkFound(found, parent) == null) {
            //ok, it was actually not found, so, after marking it as an occurrence, we have to check all 
            //the others that have the same representation in its scope.
            if (token.getRepresentation().equals(nameToFind)) {
                undefinedFound.add(found);
            }
        } else {
            hitAsUndefined = found;
        }
    }

    /**
     * Will peek the parent if the node is not null (otherwise will return null)
     */
    protected ASTEntry popParent(SimpleNode node) {
        return parents.pop();
    }

    /**
     * If the 'parents' stack is higher than 0, peek it (may return null)
     */
    protected ASTEntry peekParent() {
        ASTEntry parent = null;
        if (parents.size() > 0) {
            parent = parents.peek();
        }
        return parent;
    }

    /**
     * When we start the scope, we have to put an entry in the parents.
     */
    @Override
    protected void onAfterStartScope(int newScopeType, SimpleNode node) {
        if (parents == null) {
            parents = new FastStack<ASTEntry>(10);
        }
        if (parents.size() == 0) {
            parents.push(new ASTEntry(null, node));
        } else {
            parents.push(new ASTEntry(parents.peek(), node));
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
    protected void checkFinished() {
        if (!finished) {
            finished = true;
            endScope(null); //finish the last scope
        }
    }

    @Override
    protected void onAfterEndScope(SimpleNode node, ScopeItems m) {
        ASTEntry parent = popParent(node);
        if (hitAsUndefined == null) {
            for (String rep : new FullRepIterable(this.completeNameToFind, true)) {
                List<Found> foundItems = m.getAll(rep);
                for (Found found : foundItems) {
                    if (checkFound(found, parent) != null) {
                        return;
                    }
                }

            }

        } else { //(hitAsUndefined != null)

            String foundRep = hitAsUndefined.getSingle().generator.getRepresentation();

            if (foundRep.indexOf('.') == -1 || FullRepIterable.containsPart(foundRep, nameToFind)) {
                //now, there's a catch here, if we found it as an attribute,
                //we cannot get the locals
                for (Found f : this.undefinedFound) {
                    if (f.getSingle().generator.getRepresentation().startsWith(foundRep)) {
                        if (foundOccurrences.size() == 1) {
                            Tuple3<Found, Integer, ASTEntry> hit = foundOccurrences.get(0);
                            Tuple3<Found, Integer, ASTEntry> foundOccurrence = new Tuple3<Found, Integer, ASTEntry>(f,
                                    hit.o2, hit.o3);
                            addFoundOccurrence(foundOccurrence);
                        }
                    }
                }
            }
        }

    }

    /**
     * Checks to see if the given found is actually a match to the current position.
     * @return the same Found passed on the parameter if it is a match (and null otherwise)
     */
    protected Found checkFound(Found found, ASTEntry parent) {
        if (found == null) {
            return null;
        }
        List<GenAndTok> all = found.getAll();

        try {
            for (GenAndTok gen : all) {
                for (IToken tok2 : gen.getAllTokens()) {
                    if (checkToken(found, tok2, parent)) {
                        return found; //ok, found it
                    }
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }

    protected boolean checkToken(Found found, IToken generator, ASTEntry parent) {
        int startLine = AbstractMessage.getStartLine(generator, this.document) - 1;
        int endLine = AbstractMessage.getEndLine(generator, this.document, false) - 1;

        String rep = generator.getRepresentation();
        int startCol = AbstractMessage.getStartCol(generator, this.document, rep, true) - 1;
        int endCol = AbstractMessage.getEndCol(generator, this.document, rep, false) - 1;
        if (currLine >= startLine && currLine <= endLine && currCol >= startCol && currCol <= endCol) {
            int colDelta = 0;
            if (currLine == startLine || currLine == endLine) {
                colDelta = currCol - startCol;
            }
            Tuple3<Found, Integer, ASTEntry> foundOccurrence = new Tuple3<Found, Integer, ASTEntry>(found, colDelta,
                    parent);
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

        ArrayList<Tuple4<IToken, Integer, ASTEntry, Found>> complete = getCompleteTokenOccurrences();
        ArrayList<ASTEntry> ret = new ArrayList<ASTEntry>();

        for (Tuple4<IToken, Integer, ASTEntry, Found> tup : complete) {
            IToken token = tup.o1;
            if (!(token instanceof SourceToken)) { // we want only the source tokens for this module
                continue;
            }

            //if it is different, we have to make partial names
            SourceToken sourceToken = (SourceToken) tup.o1;
            SimpleNode ast = (sourceToken).getAst();

            String representation = null;

            if (ast instanceof ImportFrom) {
                ImportFrom f = (ImportFrom) ast;
                //f.names may be empty if it is a wild import
                for (aliasType t : f.names) {
                    NameTok importName = NodeUtils.getNameForAlias(t);
                    String importRep = NodeUtils.getFullRepresentationString(importName);

                    if (importRep.equals(nameToFind)) {
                        ast = importName;
                        representation = importRep;
                        break;
                    }

                }

            } else if (ast instanceof Import) {
                representation = NodeUtils.getFullRepresentationString(ast);
                Import f = (Import) ast;
                NameTok importName = NodeUtils.getNameForRep(f.names, representation);
                if (importName != null) {
                    ast = importName;
                }

            } else {
                representation = NodeUtils.getFullRepresentationString(ast);
            }

            if (representation == null) {
                //do nothing
                //can happen on wild imports

            } else if (nameToFind.equals(representation)) {
                if (ast instanceof Attribute) {
                    //it can happen, as we won't go up to the part of the actual call (if there's one).
                    ast = NodeUtils.getAttributeParts((Attribute) ast).get(0);
                    ASTEntry entry = new ASTEntry(tup.o3, ast);
                    entry.setAdditionalInfo(FOUND_ADDITIONAL_INFO_IN_AST_ENTRY, tup.o4);
                    ret.add(entry);
                } else {
                    ASTEntry entry = new ASTEntry(tup.o3, ast);
                    entry.setAdditionalInfo(FOUND_ADDITIONAL_INFO_IN_AST_ENTRY, tup.o4);
                    ret.add(entry);
                }

            } else if (FullRepIterable.containsPart(representation, nameToFind)) {

                Name nameAst = new Name(nameToFind, Name.Store, false);
                List<String> strings = StringUtils.dotSplit(representation);

                int plus = 0;
                for (String string : strings) {
                    if (string.equals(nameToFind) && (plus + nameToFind.length() >= tup.o2)) {
                        break;
                    }
                    plus += string.length() + 1; //len + dot
                }
                nameAst.beginColumn = AbstractMessage.getStartCol(token, document) + plus;
                nameAst.beginLine = AbstractMessage.getStartLine(token, document);
                Tuple3<String, Integer, Integer> t = new Tuple3<String, Integer, Integer>(nameToFind,
                        nameAst.beginColumn, nameAst.beginLine);
                if (!s.contains(t)) {
                    s.add(t);
                    ASTEntry entry = new ASTEntry(tup.o3, nameAst);
                    entry.setAdditionalInfo(FOUND_ADDITIONAL_INFO_IN_AST_ENTRY, tup.o4);
                    ret.add(entry);
                }
            }
        }

        return ret;
    }

    /**
     * @return all the occurrences found in a 'complete' way (dotted name).
     * The ASTEtries are decorated with the Found here...
     */
    protected ArrayList<Tuple4<IToken, Integer, ASTEntry, Found>> getCompleteTokenOccurrences() {
        //that's because we don't want duplicates
        Set<IToken> f = new HashSet<IToken>();
        ArrayList<Tuple4<IToken, Integer, ASTEntry, Found>> ret = new ArrayList<Tuple4<IToken, Integer, ASTEntry, Found>>();

        for (Tuple3<Found, Integer, ASTEntry> found : foundOccurrences) {
            List<GenAndTok> all = found.o1.getAll();

            for (GenAndTok tok : all) {

                Tuple4<IToken, Integer, ASTEntry, Found> tup4 = new Tuple4<IToken, Integer, ASTEntry, Found>(
                        tok.generator, found.o2, found.o3, found.o1);

                if (!f.contains(tok.generator)) {
                    f.add(tok.generator);
                    ret.add(tup4);
                }

                for (IToken t : tok.references) {
                    tup4 = new Tuple4<IToken, Integer, ASTEntry, Found>(t, found.o2, found.o3, found.o1);
                    if (!f.contains(t)) {
                        f.add(t);
                        ret.add(tup4);
                    }
                }
            }

            onGetCompleteTokenOccurrences(found, f, ret);
        }
        return ret;
    }

    /**
     * To be overriden
     * @param ret 
     */
    protected void onGetCompleteTokenOccurrences(Tuple3<Found, Integer, ASTEntry> found, Set<IToken> f,
            ArrayList<Tuple4<IToken, Integer, ASTEntry, Found>> ret) {

    }

}
