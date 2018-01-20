/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
///*
// * Created on Apr 12, 2005
// *
// * @author Fabio Zadrozny
// */
//package org.python.pydev.editor.codecompletion.revisited;
//
//import org.eclipse.core.runtime.IPath;
//import org.eclipse.core.runtime.Path;
//import org.eclipse.jface.text.BadLocationException;
//import org.eclipse.jface.text.Document;
//import org.eclipse.jface.text.IDocument;
//import org.eclipse.jface.text.IRegion;
//import org.eclipse.jface.text.contentassist.IContextInformation;
//import org.eclipse.swt.graphics.Image;
//import org.eclipse.swt.graphics.Point;
//import org.eclipse.ui.IEditorPart;
//import org.python.pydev.core.REF;
//import org.python.pydev.editor.PyEdit;
//import org.python.pydev.editor.codecompletion.PyCompletionProposal;
//import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
//import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
//import org.python.pydev.editorinput.PyOpenEditor;
//
///**
// * This class is a proposal that should be applied in a module and not in the original doc.
// * 
// * @author Fabio Zadrozny
// */
//public class SourceModuleProposal extends PyCompletionProposal {
//
//    public final SourceModule module;
//    public PyEdit edit;
//    public IDocument doc;
//    public Definition definition;
//    
//    public static final int ADD_TO_DEFAULT = -1;
//    public static final int ADD_TO_LAST_LINE_BEFORE_MAIN = 0;
//    public static final int ADD_TO_LAST_CLASS_LINE = 1;
//    public int addTo = ADD_TO_LAST_LINE_BEFORE_MAIN;
//    
//    public SourceModuleProposal(
//            String replacementString, 
//            int replacementOffset, 
//            int replacementLength, 
//            int cursorPosition, 
//            Image image, 
//            String displayString,
//            IContextInformation contextInformation, 
//            String additionalProposalInfo, 
//            SourceModule s,
//            int priority) {
//        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString, contextInformation, additionalProposalInfo, priority);
//        this.module = s;
//    }
//
//    /**
//     * @see org.python.pydev.editor.codecompletion.PyCompletionProposal#apply(org.eclipse.jface.text.IDocument)
//     */
//    public void apply(IDocument dummy) {
//        //OK, module can really be another or could be same...
//        
//        if(doc == null){ //doc can be preset
//            
//            //if not preset, let's find it.
//            if(module.getFile() == null){ //its same
//                doc = dummy;
//                
//            }else{//another
//                IPath path = new Path(REF.getFileAbsolutePath(module.getFile()));
//                IEditorPart part = PyOpenEditor.doOpenEditor(path);
//        
//                if(part instanceof PyEdit){
//                    edit = (PyEdit) part;
//                    doc = edit.getDocumentProvider().getDocument(edit.getEditorInput());
//                }else{
//                    String contents = FileUtils.getFileContents(module.getFile());
//                    doc = new Document(contents);
//                }
//            }        
//        }
//        
//        fReplacementOffset = getReplacementOffset();
//        super.apply(doc);
//    }
//
//    /**
//     * @return
//     * 
//     */
//    public int getReplacementOffset() {
//        //Replacement
//        int i = -1;
//        if(addTo == ADD_TO_LAST_LINE_BEFORE_MAIN){
//            i = module.findIfMain()-2;
//        }else if(addTo == ADD_TO_LAST_CLASS_LINE){
//            i = module.findAstEnd(definition.ast)-2;
//        }
//
//        if(i < 0){
//            i = doc.getNumberOfLines();
//        }
//        try {
//            IRegion lineInformation = doc.getLineInformation(i);
//            return lineInformation.getOffset()+lineInformation.getLength();
//        } catch (BadLocationException e) {
//            e.printStackTrace();
//        }
//
//        //return original...
//        return fReplacementOffset; 
//    }
//
//    /**
//     * @see org.python.pydev.editor.codecompletion.PyCompletionProposal#getSelection(org.eclipse.jface.text.IDocument)
//     */
//    public Point getSelection(IDocument dummy) {
//        Point sel = super.getSelection(doc);
//        if(module.getFile() == null){ //its same
//            return sel;
//        }
//
//        edit.setSelection(sel.x, sel.y);
//        return null;
//    }
//
//    /**
//     * @return
//     * 
//     */
//    public String getReplacementStr() {
//        return fReplacementString;
//    }
//    
//}