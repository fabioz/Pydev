/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 18, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.docutils.ImportHandle;
import org.python.pydev.core.docutils.PyImportsHandling;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.ImportHandle.ImportHandleInfo;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.ui.importsconf.ImportsPreferencesPage;

/**
 * @author Fabio Zadrozny
 */
public class PyOrganizeImports extends PyAction{

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @SuppressWarnings("unchecked")
    public void run(IAction action) {
        try 
        {
        	if(!canModifyEditor()){
        		return;
        	}

            PyEdit pyEdit = getPyEdit();
            
            PySelection ps = new PySelection(pyEdit);
            String endLineDelim = ps.getEndLineDelim();
            final IDocument doc = ps.getDoc();
            DocumentRewriteSession session = null;
            
            try {
                if (ps.getStartLineIndex() == ps.getEndLineIndex()) {
                    //let's see if someone wants to make a better implementation in another plugin...
                    List<IOrganizeImports> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_ORGANIZE_IMPORTS);
                    
                    for (IOrganizeImports organizeImports : participants) {
                        if(!organizeImports.beforePerformArrangeImports(ps, pyEdit)){
                            return;
                        }
                    }
                    
                    session = startWrite(doc);
                    
                    performArrangeImports(doc, endLineDelim, pyEdit.getIndentPrefs().getIndentationString());
                    
                    for (IOrganizeImports organizeImports : participants) {
                        organizeImports.afterPerformArrangeImports(ps, pyEdit);
                    }
                } else {
                    session = startWrite(doc);
                    performSimpleSort(doc, endLineDelim, ps.getStartLineIndex(), ps.getEndLineIndex());
                }
            } finally {
                if(session != null){
                    endWrite(doc, session);
                }
            }
        } 
        catch ( Exception e ) 
        {
            Log.log(e);
            beep ( e );
        }        
    }

    /**
     * Stop a rewrite session
     */
    private void endWrite(IDocument doc, DocumentRewriteSession session) {
        if(doc instanceof IDocumentExtension4){
            IDocumentExtension4 d = (IDocumentExtension4) doc;
            d.stopRewriteSession(session);
        }
    }

    /**
     * Starts a rewrite session (keep things in a single undo/redo)
     */
    private DocumentRewriteSession startWrite(IDocument doc) {
        if(doc instanceof IDocumentExtension4){
            IDocumentExtension4 d = (IDocumentExtension4) doc;
            return d.startRewriteSession(DocumentRewriteSessionType.UNRESTRICTED);
        }
        return null;
    }

    /**
     * Actually does the action in the document.
     * 
     * @param doc
     * @param endLineDelim
     */
    public static void performArrangeImports(IDocument doc, String endLineDelim, String indentStr){
        List<Tuple3<Integer, String, ImportHandle>> list = new ArrayList<Tuple3<Integer, String, ImportHandle>>();
        //Gather imports in a structure we can work on.
        PyImportsHandling pyImportsHandling = new PyImportsHandling(doc);
        int firstImport = -1;
        for(ImportHandle imp:pyImportsHandling){
            list.add( new Tuple3<Integer, String, ImportHandle>(imp.startFoundLine, imp.importFound, imp) );
            
            if(firstImport == -1){
                firstImport = imp.startFoundLine;
            }
        }

        
        //check if we had any import
        if(firstImport == -1){
            return;
        }
        
        //sort in inverse order (for removal of the string of the document).
        Collections.sort(list, new Comparator<Tuple3<Integer, String, ImportHandle>>() {

            public int compare(Tuple3<Integer, String, ImportHandle> o1, Tuple3<Integer, String, ImportHandle> o2) {
                return o2.o1.compareTo(o1.o1);
            }
        });
        //ok, now we have to delete all lines with imports.
        for (Iterator<Tuple3<Integer, String, ImportHandle>> iter = list.iterator(); iter.hasNext();) {
            Tuple3<Integer, String, ImportHandle> element = iter.next();
            String s = element.o2;
            int i = PySelection.countLineBreaks(s);
            while (i >= 0) {
                PySelection.deleteLine(doc, (element.o1).intValue());
                i--;
            }
        }
        Collections.sort(list, new Comparator<Tuple3<Integer, String, ImportHandle>>() {

            public int compare(Tuple3<Integer, String, ImportHandle> o1, Tuple3<Integer, String, ImportHandle> o2) {
                //When it's __future__, it has to appear before the others.
                List<ImportHandleInfo> info1 = o1.o3.getImportInfo();
                List<ImportHandleInfo> info2 = o2.o3.getImportInfo();
                boolean isFuture1 = getIsFuture(info1);
                boolean isFuture2 = getIsFuture(info2);
                if(isFuture1 && !isFuture2){
                    return -1;
                }
                if(!isFuture1 && isFuture2){
                    return 1;
                }
                return o1.o2.compareTo(o2.o2);
            }

            private boolean getIsFuture(List<ImportHandleInfo> info1){
                String from1 = null;
                if(info1.size() > 0){
                    from1 = info1.get(0).getFromImportStr();
                }
                boolean isFuture  = from1 != null && from1.equals("__future__");
                return isFuture;
            }
        });
        
        firstImport--; //add line after the the specified
        
        //now, re-add the imports
        FastStringBuffer all = new FastStringBuffer();
        
        
        if(!ImportsPreferencesPage.getGroupImports()){
            //no grouping
            for (Iterator<Tuple3<Integer, String, ImportHandle>> iter = list.iterator(); iter.hasNext();) {
                Tuple3<Integer, String, ImportHandle> element = iter.next();
                all.append(element.o2);
                all.append(endLineDelim);
            }
        }else{ //we have to group the imports!
            
            //import from to the imports that should be grouped given its 'from'
            TreeMap<String, List<ImportHandleInfo>> importsWithFrom = new TreeMap<String, List<ImportHandleInfo>>();
            List<ImportHandleInfo> importsWithoutFrom = new ArrayList<ImportHandleInfo>();
            
            fillImportStructures(list, importsWithFrom, importsWithoutFrom);
            
            //preferences for multiline imports
            boolean multilineImports = ImportsPreferencesPage.getMultilineImports();
            int maxCols = getMaxCols(multilineImports);
            
            //preferences for how to break imports
            boolean breakWithParenthesis = getBreakImportsWithParenthesis();
            

            Set<Entry<String, List<ImportHandleInfo>>> entrySet = importsWithFrom.entrySet();
            FastStringBuffer lastFromXXXImportWritten = new FastStringBuffer();
            FastStringBuffer line = new FastStringBuffer();
            
            for (Entry<String, List<ImportHandleInfo>> entry : entrySet) {
                
                //first, reorganize them in the order to be written (the ones with comments after the ones without)
                ArrayList<Tuple<String, String>> importsAndComments = new ArrayList<Tuple<String, String>>();
                ArrayList<Tuple<String, String>> importsAndNoComments = new ArrayList<Tuple<String, String>>();
                
                fillImportFromInfo(entry, importsAndComments, importsAndNoComments);
                
                
                //ok, it's all filled, let's start rewriting it!
                boolean firstInLine = true;
                line.clear();
                boolean addedParenForLine = false;
                
                //ok, write all the ones with comments after the ones without any comments (each one with comment
                //will be written as a new import)
                importsAndNoComments.addAll(importsAndComments);
                for(int i=0;i<importsAndNoComments.size();i++){
                    
                    Tuple<String, String> tuple = importsAndNoComments.get(i);
                    
                    if(firstInLine){
                        lastFromXXXImportWritten.clear();
                        lastFromXXXImportWritten.append("from ");
                        lastFromXXXImportWritten.append(entry.getKey());
                        lastFromXXXImportWritten.append(" import ");
                        line.append(lastFromXXXImportWritten);
                    }else{
                        line.append(", ");
                    }
                    
                    if(multilineImports){
                        if(line.length() + tuple.o1.length() + tuple.o2.length() > maxCols){
                            //we have to make the wrapping
                            if(breakWithParenthesis){
                                if(!addedParenForLine){
                                    line.insert(lastFromXXXImportWritten.length(), '(');
                                    addedParenForLine = true;
                                }
                                line.append(endLineDelim);
                                line.append(indentStr);
                            }else{
                                line.append('\\');
                                line.append(endLineDelim);
                                line.append(indentStr);
                            }
                            all.append(line);
                            line.clear();
                        }
                    }
                    
                    line.append(tuple.o1);
                    
                    if(addedParenForLine && i == importsAndNoComments.size()){
                        addedParenForLine = false;
                        line.append(')');
                    }
                    
                    firstInLine = false;
                    
                    if(tuple.o2.length() > 0){
                        if(addedParenForLine){
                            addedParenForLine = false;
                            line.append(')');
                        }
                        line.append(' ');
                        line.append(tuple.o2);
                        line.append(endLineDelim);
                        all.append(line);
                        line.clear();
                        firstInLine = true;
                    }
                }
                
                
                if(!firstInLine){
                    if(addedParenForLine){
                        addedParenForLine = false;
                        line.append(')');
                    }
                    line.append(endLineDelim);
                    all.append(line);
                    line.clear();
                }
            }
            
            writeImportsWithoutFrom(endLineDelim, all, importsWithoutFrom);
            
        }
        
        
        PySelection.addLine(doc, endLineDelim, all.toString(), firstImport);
    }

    
    /**
     * Write the imports that don't have a 'from' in the beggining (regular imports)
     */
    private static void writeImportsWithoutFrom(String endLineDelim, FastStringBuffer all,
            List<ImportHandleInfo> importsWithoutFrom) {
        //now, write the regular imports (no wrapping or tabbing here)
        for(ImportHandleInfo info:importsWithoutFrom){
            
            List<String> importedStr = info.getImportedStr();
            List<String> commentsForImports = info.getCommentsForImports();
            for(int i=0;i<importedStr.size();i++){
                all.append("import ");
                String importedString = importedStr.get(i);
                String comment = commentsForImports.get(i);
                all.append(importedString);
                if(comment.length() > 0){
                    all.append(' ');
                    all.append(comment);
                }
                all.append(endLineDelim);
            }
        }
    }

    /**
     * Fills the lists passed based on the entry set, so that imports that have comments are contained in a list
     * and imports without comments in another.
     */
    private static void fillImportFromInfo(Entry<String, List<ImportHandleInfo>> entry,
            ArrayList<Tuple<String, String>> importsAndComments, ArrayList<Tuple<String, String>> importsAndNoComments) {
        for (ImportHandleInfo v : entry.getValue()) {
            List<String> importedStr = v.getImportedStr();
            List<String> commentsForImports = v.getCommentsForImports();
            for(int i=0;i<importedStr.size();i++){
                String importedString = importedStr.get(i).trim();
                String comment = commentsForImports.get(i).trim();
                boolean isWildImport = importedString.equals("*");
                if(isWildImport){
                    importsAndComments.clear();
                    importsAndNoComments.clear();
                }
                if(comment.length() > 0){
                    importsAndComments.add(new Tuple<String, String>(importedString, comment));
                }else{
                    importsAndNoComments.add(new Tuple<String, String>(importedString, comment));
                }
                if(isWildImport){
                    return;
                }
            }
        }
    }

    /**
     * @return true if the imports should be split with parenthesis (instead of escaping)
     */
    private static boolean getBreakImportsWithParenthesis() {
        String breakIportMode = ImportsPreferencesPage.getBreakIportMode();
        boolean breakWithParenthesis = true;
        if(!breakIportMode.equals(ImportsPreferencesPage.BREAK_IMPORTS_MODE_PARENTHESIS)){
            breakWithParenthesis = false;
        }
        return breakWithParenthesis;
    }

    /**
     * @return the maximum number of columns that may be available in a line.
     */
    private static int getMaxCols(boolean multilineImports) {
        int maxCols = 80;
        if(multilineImports){
            if(PydevPlugin.getDefault() != null){
                IPreferenceStore chainedPrefStore = PydevPrefs.getChainedPrefStore();
                maxCols = chainedPrefStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
            }
        }else{
            maxCols = Integer.MAX_VALUE;
        }
        return maxCols;
    }

    /**
     * Fills the import structure passed, so that the imports from will be grouped by the 'from' part and the regular
     * imports will be in a separate list.
     */
    private static void fillImportStructures(List<Tuple3<Integer, String, ImportHandle>> list,
            TreeMap<String, List<ImportHandleInfo>> importsWithFrom, List<ImportHandleInfo> importsWithoutFrom) {
        //fill the info
        for (Iterator<Tuple3<Integer, String, ImportHandle>> iter = list.iterator(); iter.hasNext();) {
            Tuple3<Integer, String, ImportHandle> element = iter.next();
            
            List<ImportHandleInfo> importInfo = element.o3.getImportInfo();
            for (ImportHandleInfo importHandleInfo : importInfo) {
                String fromImportStr = importHandleInfo.getFromImportStr();
                if(fromImportStr == null){
                    importsWithoutFrom.add(importHandleInfo);
                }else{
                    List<ImportHandleInfo> lst = importsWithFrom.get(fromImportStr);
                    if(lst == null){
                        lst = new ArrayList<ImportHandleInfo>();
                        importsWithFrom.put(fromImportStr, lst);
                    }
                    lst.add(importHandleInfo);
                }
            }
        }
    }

    /**
     * Performs a simple sort without taking into account the actual contents of the selection (aside from lines
     * ending with '\' which are considered as a single line).
     * 
     * @param doc the document to be sorted
     * @param endLineDelim the delimiter to be used
     * @param startLine the first line where the sort should happen
     * @param endLine the last line where the sort should happen
     */
    public static void performSimpleSort(IDocument doc, String endLineDelim, int startLine, int endLine) {
        try {
            ArrayList<String> list = new ArrayList<String>();
            
            StringBuffer lastLine = null;
            for (int i = startLine; i <= endLine; i++) {
                
                String line = PySelection.getLine(doc, i);
                
                if(lastLine != null){
                    int len = lastLine.length();
                    if(len > 0 && lastLine.charAt(len-1) == '\\'){
                        lastLine.append(endLineDelim);
                        lastLine.append(line);
                    }else{
                        list.add(lastLine.toString());
                        lastLine = new StringBuffer(line);
                    }
                }else{
                    lastLine = new StringBuffer(line);
                }
            }
            
            if(lastLine != null){
                list.add(lastLine.toString());
            }
            
            Collections.sort(list);
            StringBuffer all = new StringBuffer();
            for (Iterator iter = list.iterator(); iter.hasNext();) {
                String element = (String) iter.next();
                all.append(element);
                if(iter.hasNext())
                    all.append(endLineDelim);
            }
        
            int length = doc.getLineInformation(endLine).getLength();
            int endOffset = doc.getLineInformation(endLine).getOffset()+length;
            int startOffset = doc.getLineInformation(startLine).getOffset();
            
            doc.replace(startOffset, endOffset-startOffset, all.toString());
            
        } catch (BadLocationException e) {
            Log.log(e);
        }

    }
}
