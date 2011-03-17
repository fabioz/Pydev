/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.LineStartingScope;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.templates.PyDocumentTemplateContext;
import org.python.pydev.editor.codecompletion.templates.PyTemplateCompletionProcessor;
import org.python.pydev.editor.correctionassist.heuristics.AssistAssign;
import org.python.pydev.editor.templates.PyContextType;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.EndOffset;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;
import org.python.pydev.refactoring.core.base.RefactoringInfo;

public abstract class PyCreateClassOrMethod extends PyCreateAction{

    public abstract String getCreationStr();

    /**
     * When executed it'll create a proposal and apply it.
     */
    public void execute(RefactoringInfo refactoringInfo, int locationStrategy) {
        try {
            String creationStr = this.getCreationStr();
            final String asTitle = StringUtils.getWithFirstUpper(creationStr);
            
            PySelection pySelection = refactoringInfo.getPySelection();
            Tuple<String, Integer> currToken = pySelection.getCurrToken();
            String actTok = currToken.o1;
            List<String> parametersAfterCall = null;
            if(actTok.length() == 0){
                InputDialog dialog = new InputDialog(
                        PyAction.getShell(), 
                        asTitle+" name", 
                        "Please enter the name of the "+asTitle+" to be created.", 
                        "", 
                        new IInputValidator() {
                            
                            public String isValid(String newText) {
                                if(newText.length() == 0){
                                    return "The "+asTitle+" name may not be empty";
                                }
                                if(StringUtils.containsWhitespace(newText)){
                                    return "The "+asTitle+" name may not contain whitespaces.";
                                }
                                return null;
                            }
                        });
                if(dialog.open() != InputDialog.OK){
                    return;
                }
                actTok = dialog.getValue();
            }else{
                parametersAfterCall = pySelection.getParametersAfterCall(currToken.o2+actTok.length());

            }
            ICompletionProposal proposal = createProposal(refactoringInfo, actTok, locationStrategy, parametersAfterCall);
            if(proposal instanceof ICompletionProposalExtension2){
                ICompletionProposalExtension2 extension2 = (ICompletionProposalExtension2) proposal;
                extension2.apply(targetEditor.getPySourceViewer(), '\n', 0, 0);
            }else{
                proposal.apply(refactoringInfo.getDocument());
            }
            
        } catch (BadLocationException e) {
            Log.log(e);
        }
    }



    protected ICompletionProposal createProposal(PySelection pySelection, String source, Tuple<Integer, String> offsetAndIndent) {
        int lineOfOffset = pySelection.getLineOfOffset(offsetAndIndent.o1);
        if(lineOfOffset > 0){
            String line = pySelection.getLine(lineOfOffset);
            if(line.trim().length() > 0){
                source = "\n"+source;
            }
            if(lineOfOffset > 1){
                line = pySelection.getLine(lineOfOffset-1);
                if(line.trim().length() > 0){
                    source = "\n"+source;
                }
            }
        }
        
        String indent=offsetAndIndent.o2;
        if(targetEditor != null){
            String creationStr = getCreationStr();
            PyDocumentTemplateContext context = PyTemplateCompletionProcessor.createContext(new PyContextType(), 
                    targetEditor.getPySourceViewer(), new Region(offsetAndIndent.o1, 0), indent);
            
            Template template = new Template("Create "+creationStr, "Create "+creationStr, "", source, true);
            TemplateProposal templateProposal = new TemplateProposal(template, context, new Region(offsetAndIndent.o1, 0), null);
            return templateProposal;
            
        }else{
            //This should only happen in tests.
            source = StringUtils.indentTo(source, indent);
            return new CompletionProposal(source, offsetAndIndent.o1, 0, 0);
        }
    }

    

    /**
     * @return the offset and the indent to be used.
     */
    protected Tuple<Integer, String> getLocationOffset(
            int locationStrategy, PySelection pySelection, ModuleAdapter moduleAdapter, IClassDefAdapter targetClass) {
        Assert.isNotNull(targetClass);
        
        int offset;
        IOffsetStrategy strategy;
        try {
            switch (locationStrategy) {
            case LOCATION_STRATEGY_BEFORE_CURRENT:
                String currentLine = pySelection.getLine();
                int firstCharPosition = PySelection.getFirstCharPosition(currentLine);

                LineStartingScope scopeStart = pySelection.getPreviousLineThatStartsScope(
                        PySelection.CLASS_AND_FUNC_TOKENS, false, firstCharPosition);

                if(scopeStart == null){
                    PydevPlugin.log("Could not get proper scope to create code inside class.");
                    ClassDef astNode = targetClass.getASTNode();
                    if(astNode.body.length > 0){
                        offset = NodeUtils.getLineEnd(astNode.body[astNode.body.length-1]);
                        
                    }else{
                        offset = NodeUtils.getLineEnd(astNode);
                    }
                }else{
                    int iLineStartingScope = scopeStart.iLineStartingScope;
                    String line = pySelection.getLine(iLineStartingScope);
                    
                    if(pySelection.matchesFunctionLine(line) || pySelection.matchesClassLine(line)){
                        //check for decorators...
                        if(iLineStartingScope > 0){
                            int i = iLineStartingScope-1;
                            while(pySelection.getLine(i).trim().startsWith("@")){
                                iLineStartingScope = i;
                                i--;
                            }
                        }
                    }
                    offset = pySelection.getLineOffset(iLineStartingScope);
                }
                
                break;
                
            case LOCATION_STRATEGY_END:
                strategy = new EndOffset(
                        targetClass, pySelection.getDoc(), moduleAdapter.getAdapterPrefs());
                offset = strategy.getOffset();
                
                break;
                
            default:
                throw new AssertionError("Unknown location strategy: "+locationStrategy);
            }
            int nodeBodyIndent = targetClass.getNodeBodyIndent();
            return new Tuple<Integer, String>(offset, new FastStringBuffer(nodeBodyIndent).appendN(' ', nodeBodyIndent).toString());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    
    protected Tuple<Integer, String> getLocationOffset(int locationStrategy, PySelection pySelection, ModuleAdapter moduleAdapter) throws AssertionError {
        int offset;
        switch (locationStrategy) {
            case LOCATION_STRATEGY_BEFORE_CURRENT:
                int lastNodeFirstLineBefore = moduleAdapter.getLastNodeFirstLineBefore(pySelection.getCursorLine()+1);
                lastNodeFirstLineBefore = lastNodeFirstLineBefore-1; // From AST line to doc line
                
                int line = lastNodeFirstLineBefore;
                if(line > 0){
                    try {
                        String trimmed = pySelection.getLine(line).trim();
                        if(trimmed.startsWith("class") || trimmed.startsWith("def")){
                            //if we'll add to a class or def line, let's see if there are comments just above it 
                            //(in this case, we'll go backwards in the file until the block comment ends)
                            //i.e.:
                            //#======================
                            //# Existing
                            //#======================
                            //class Existing(object): <-- currently at this line
                            //    passMyClass()
                            int curr = line;
                            while(curr >= 0){
                                line = curr;
                                if(curr -1 < 0){
                                    break;
                                }
                                if(!pySelection.getLine(curr-1).trim().startsWith("#")){
                                    break;
                                }
                                
                                curr--;
                            }
                        }
                    } catch (Exception e) {
                        PydevPlugin.log(e);
                    }
                }
                
                offset = pySelection.getLineOffset(line);
                break;
                
            case LOCATION_STRATEGY_END:
                offset = pySelection.getEndOfDocummentOffset();
                
                break;
    
            default:
                throw new AssertionError("Unknown location strategy: "+locationStrategy);
        }
        return new Tuple<Integer, String>(offset, "");
    }

    

    protected FastStringBuffer createParametersList(List<String> parametersAfterCall) {
        FastStringBuffer params = new FastStringBuffer(parametersAfterCall.size()*10);
        AssistAssign assistAssign = new AssistAssign();
        for(int i=0;i<parametersAfterCall.size();i++){
            String param = parametersAfterCall.get(i).trim();
            if(params.length() > 0){
                params.append(", ");
            }
            String tok;
            if(StringUtils.isPythonIdentifier(param)){
                tok = param;
            }else{
                tok = assistAssign.getTokToAssign(param);
                if(tok == null || tok.length() == 0){
                    tok = "param"+i;
                }
            }
            boolean addTag = !(i == 0 && (tok.equals("cls") || tok.equals("self")));
            if(addTag){
                params.append("${");
            }
            params.append(tok);
            if(addTag){
                params.append("}");
            }
        }
        return params;
    }
}
