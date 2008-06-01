/*
 * Created on 16/09/2005
 */
package com.python.pydev.analysis;

import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.python.pydev.core.docutils.ImportHandle;
import org.python.pydev.core.docutils.ImportNotRecognizedException;
import org.python.pydev.core.docutils.PyImportsHandling;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.ImportHandle.ImportHandleInfo;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.PyCompletionProposalExtension2;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.importsconf.ImportsPreferencesPage;

/**
 * This is the proposal that should be used to do a completion that can have a related import. 
 * 
 * @author Fabio
 */
public class CtxInsensitiveImportComplProposal extends PyCompletionProposalExtension2{
    
    /**
     * If empty, act as a regular completion
     */
    public String realImportRep;
    
    /**
     * This is the indentation string that should be used
     */
    public String indentString;

    public CtxInsensitiveImportComplProposal(String replacementString, int replacementOffset, int replacementLength, 
            int cursorPosition, Image image, String displayString, IContextInformation contextInformation, 
            String additionalProposalInfo, int priority, 
            String realImportRep) {
        
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority, ON_APPLY_DEFAULT, "");
        this.realImportRep = realImportRep;
    }
    
    /**
     * This is the apply that should actually be called!
     */
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        IDocument document = viewer.getDocument();
        if(viewer instanceof PySourceViewer){
            PySourceViewer pySourceViewer = (PySourceViewer) viewer;
            PyEdit pyEdit = pySourceViewer.getEdit();
            this.indentString = pyEdit.getIndentPrefs().getIndentationString();
            
        }
        apply(document, trigger, stateMask, offset);
    }
    
    /**
     * Note: This apply is not directly called (it's called through 
     * {@link CtxInsensitiveImportComplProposal#apply(ITextViewer, char, int, int)})
     * 
     * This is the point where the completion is written. It has to be written and if some import is also available
     * it should be inserted at this point.
     * 
     * We have to be careful to only add an import if that's really needed (e.g.: there's no other import that
     * equals the import that should be added).
     * 
     * Also, we have to check if this import should actually be grouped with another import that already exists.
     * (and it could be a multi-line import)
     */
    public void apply(IDocument document, char trigger, int stateMask, int offset) {
        if(this.indentString == null){
            throw new RuntimeException("Indent string not set (not called with a PyEdit as viewer?)");
        }

        
        try {
            PySelection selection = new PySelection(document);
            int lineToAddImport=-1;
            ImportHandleInfo groupInto=null;
            ImportHandleInfo realImportHandleInfo = null;
            
            boolean groupImports = ImportsPreferencesPage.getGroupImports();
            
            if (realImportRep.length() > 0){
                if(groupImports){
                    try {
                        realImportHandleInfo = new ImportHandleInfo(realImportRep);
                        PyImportsHandling importsHandling = new PyImportsHandling(document);
                        for(ImportHandle handle:importsHandling){
                            if(handle.contains(realImportHandleInfo)){
                                lineToAddImport = -2; //signal that there's no need to find a line available to add the import
                                break;
                                
                            }else if(groupInto == null && realImportHandleInfo.getFromImportStr() != null){
                                List<ImportHandleInfo> handleImportInfo = handle.getImportInfo();
                                
                                for (ImportHandleInfo importHandleInfo : handleImportInfo) {
                                    
                                    if(realImportHandleInfo.getFromImportStr().equals(importHandleInfo.getFromImportStr())){
                                        List<String> commentsForImports = importHandleInfo.getCommentsForImports();
                                        if(commentsForImports.size() > 0 && commentsForImports.get(commentsForImports.size()-1).length() == 0){
                                            groupInto = importHandleInfo;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (ImportNotRecognizedException e1) {
                        PydevPlugin.log(e1);//that should not happen at this point
                    }
                }
                
                
                if(lineToAddImport == -1){
                    lineToAddImport = selection.getLineAvailableForImport();
                }
            }else{
                lineToAddImport = -1;
            }
            String delimiter = PyAction.getDelimiter(document);
            
            
            
            //first do the completion
            int dif = offset - fReplacementOffset;
            document.replace(offset-dif, dif+this.fLen, fReplacementString);
            

            if(groupInto != null && realImportHandleInfo != null){
                //let's try to group it
                int maxCols = 80;
                if(PydevPlugin.getDefault() != null){
                    IPreferenceStore chainedPrefStore = PydevPlugin.getChainedPrefStore();
                    maxCols = chainedPrefStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
                }
                

                int endLine = groupInto.getEndLine();
                IRegion lineInformation = document.getLineInformation(endLine);
                String strToAdd = ", "+realImportHandleInfo.getImportedStr().get(0);
                
                String line = PySelection.getLine(document, endLine);
                if(line.length() + strToAdd.length() > maxCols){
                    if(line.indexOf('#') == -1){
                        //no comments: just add it in the next line
                        int len = line.length();
                        if(line.trim().endsWith(")")){
                            len = line.indexOf(")");
                            strToAdd = ","+delimiter+indentString+realImportHandleInfo.getImportedStr().get(0);
                        }else{
                            strToAdd = ",\\"+delimiter+indentString+realImportHandleInfo.getImportedStr().get(0);
                        }
                        
                        int end = lineInformation.getOffset()+len;
                        document.replace(end, 0, strToAdd);
                        return;
                        
                    }
                    
                }else{
                    //regular addition (it won't pass the number of columns expected).
                    line = PySelection.getLineWithoutCommentsOrLiterals(line);
                    int len = line.length();
                    if(line.trim().endsWith(")")){
                        len = line.indexOf(")");
                    }
                    
                    int end = lineInformation.getOffset()+len;
                    document.replace(end, 0, strToAdd);
                    return;
                }                
            }
            
            //if we got here, it hasn't been added in a grouped way, so, let's add it in a new import
            if(lineToAddImport >=0 && lineToAddImport <= document.getNumberOfLines()){
                IRegion lineInformation = document.getLineInformation(lineToAddImport);
                document.replace(lineInformation.getOffset(), 0, realImportRep+delimiter);
            }

            
        } catch (BadLocationException x) {
            PydevPlugin.log(x);
        }
    }    
    
    
    @Override
    public Point getSelection(IDocument document) {
        int importLen = 0;
        if(realImportRep.length() > 0){
            importLen = realImportRep.length()+PyAction.getDelimiter(document).length();
        }
        return new Point(fReplacementOffset+fReplacementString.length()+importLen, 0 );
    }
    
    public String getInternalDisplayStringRepresentation() {
        return fReplacementString;
    }


    /**
     * If another proposal with the same name exists, this method will be called to determine if 
     * both completions should coexist or if one of them should be removed.  
     */
    @Override
    public int getOverrideBehavior(ICompletionProposal curr) {
        if(curr instanceof CtxInsensitiveImportComplProposal){
            if(curr.getDisplayString().equals(getDisplayString())){
                return BEHAVIOR_IS_OVERRIDEN;
            }else{
                return BEHAVIOR_COEXISTS;
            }
        }else{
            return BEHAVIOR_IS_OVERRIDEN;
        }
    }
}