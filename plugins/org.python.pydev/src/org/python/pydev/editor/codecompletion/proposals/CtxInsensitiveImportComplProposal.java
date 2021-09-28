/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 16/09/2005
 */
package org.python.pydev.editor.codecompletion.proposals;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.python.pydev.ast.codecompletion.IPyCompletionProposal2;
import org.python.pydev.ast.codecompletion.PriorityLRU;
import org.python.pydev.ast.codecompletion.PyCodeCompletionPreferences;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPySourceViewer;
import org.python.pydev.core.autoedit.DefaultIndentPrefs;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PyDevUiPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.ui.importsconf.ImportsPreferencesPage;

import com.python.pydev.analysis.refactoring.quick_fixes.AddTokenAndImportStatement;

/**
 * This is the proposal that should be used to do a completion that can have a related import.
 *
 * @author Fabio
 */
public class CtxInsensitiveImportComplProposal extends AbstractPyCompletionProposalExtension2 implements
        ICompletionProposalExtension, IPyCompletionProposal2 {

    public static boolean addApplyTipOnAdditionalInfo = true;

    /**
     * If empty, act as a regular completion
     */
    public String realImportRep;

    /**
     * This is the indentation string that should be used
     */
    public String indentString;

    /**
     * Determines if the import was added or if only the completion was applied.
     */
    private int importLen = 0;

    /**
     * Offset forced to be returned (only valid if >= 0)
     */
    private int newForcedOffset = -1;

    /**
     * Indicates if the completion was applied with a trigger char that should be considered
     * (meaning that the resulting position should be summed with 1)
     */
    private boolean appliedWithTrigger = false;

    /**
     * If the import should be added locally or globally.
     */
    private boolean addLocalImport = false;

    /**
     * Can be used to override the preferences in tests.
     */
    public Boolean addLocalImportsOnTopOfFunc = null;

    private int infoTypeForImage;

    public int getInfoTypeForImage() {
        // See IInfo constants.
        return infoTypeForImage;
    }

    public boolean getAddLocalImportsOnTopOfMethod() {
        if (SharedCorePlugin.inTestMode()) {
            if (addLocalImportsOnTopOfFunc != null) {
                return addLocalImportsOnTopOfFunc;
            }
            // In tests the default is true.
            return true;
        }
        return PyCodeCompletionPreferences.getPutLocalImportsOnTopOfMethod();
    }

    public CtxInsensitiveImportComplProposal(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, int infoTypeForImage, String displayString, IContextInformation contextInformation,
            String additionalProposalInfo, int priority, String realImportRep, ICompareContext compareContext) {

        super(replacementString, replacementOffset, replacementLength, cursorPosition, null, displayString,
                contextInformation, additionalProposalInfo, priority, IPyCompletionProposal.ON_APPLY_DEFAULT, "",
                compareContext);
        this.infoTypeForImage = infoTypeForImage;
        this.realImportRep = realImportRep;

        Integer lruPriority = PriorityLRU.getPriority(realImportRep);
        if (lruPriority != null) {
            this.priority = lruPriority;
        }
    }

    @Override
    public Image getImage() {
        if (fImage == null) {
            fImage = AnalysisImages.getImageForAutoImportTypeInfo(infoTypeForImage);
        }
        return ImageCache.asImage(fImage);
    }

    public void setAddLocalImport(boolean b) {
        this.addLocalImport = b;
    }

    public boolean getMakeLocalWhenShiftApplied() {
        return true;
    }

    private static final String MSG = ""
            + "Enter: apply completion.\n"
            + "  + Ctrl: replace current word (no Pop-up focus).\n"
            + "  + Shift: do local import (requires Pop-up focus).\n";

    @Override
    public String getAdditionalProposalInfo() {
        String original = super.getAdditionalProposalInfo();
        if (addApplyTipOnAdditionalInfo && getMakeLocalWhenShiftApplied()) {
            if (original == null || original.length() == 0) {
                return MSG;
            } else {
                return original + "\n\n" + MSG;
            }
        }
        return original;
    }

    /**
     * This is the apply that should actually be called!
     */
    @Override
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        IDocument document = viewer.getDocument();
        IAdaptable projectAdaptable;
        if (viewer instanceof IPySourceViewer) {
            IPySourceViewer pySourceViewer = (IPySourceViewer) viewer;
            IPyEdit pyEdit = pySourceViewer.getEdit();
            this.indentString = pyEdit.getIndentPrefs().getIndentationString();
            projectAdaptable = pyEdit;
        } else {
            //happens on compare editor
            this.indentString = new DefaultIndentPrefs(null).getIndentationString();
            projectAdaptable = null;
        }
        //If the completion is applied with shift pressed, do a local import. Note that the user is only actually
        //able to do that if the popup menu is focused (i.e.: request completion and do a tab to focus it, instead
        //of having the focus on the editor and just pressing up/down).
        if ((stateMask & SWT.SHIFT) != 0) {
            this.setAddLocalImport(true);
        }
        apply(document, trigger, stateMask, offset, projectAdaptable);
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
        apply(document, trigger, stateMask, offset, null);
    }

    protected void apply(IDocument document, char trigger, int stateMask, int offset, IAdaptable projectAdaptable) {
        PriorityLRU.appliedCompletion(realImportRep);
        if (this.indentString == null) {
            throw new RuntimeException("Indent string not set (not called with a PyEdit as viewer?)");
        }

        if (!triggerCharAppliesCurrentCompletion(trigger, document, offset)) {
            newForcedOffset = offset + 1; //+1 because that's the len of the trigger
            return;
        }

        final int maxCols;
        if (SharedCorePlugin.inTestMode()) {
            maxCols = 80;
        } else {
            IPreferenceStore chainedPrefStore = PyDevUiPrefs.getChainedPrefStore();
            maxCols = chainedPrefStore
                    .getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
        }

        AddTokenAndImportStatement.ComputedInfo computedInfo = new AddTokenAndImportStatement.ComputedInfo(
                realImportRep, fReplacementOffset, fLen, indentString,
                fReplacementString, appliedWithTrigger, importLen, document);
        new AddTokenAndImportStatement(document, trigger, offset, addLocalImport, getAddLocalImportsOnTopOfMethod(),
                ImportsPreferencesPage.getGroupImports(projectAdaptable), maxCols).createTextEdit(computedInfo);
        this.fReplacementString = computedInfo.fReplacementString;
        this.appliedWithTrigger = computedInfo.appliedWithTrigger;
        this.importLen = computedInfo.importLen;
        for (ReplaceEdit edit : computedInfo.replaceEdit) {
            try {
                edit.apply(document);
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    @Override
    public Point getSelection(IDocument document) {
        if (newForcedOffset >= 0) {
            return new Point(newForcedOffset, 0);
        }

        int pos = fReplacementOffset + fReplacementString.length() + importLen;
        if (appliedWithTrigger) {
            pos += 1;
        }

        return new Point(pos, 0);
    }

    @Override
    public final String getInternalDisplayStringRepresentation() {
        return fReplacementString;
    }

    /**
     * If another proposal with the same name exists, this method will be called to determine if
     * both completions should coexist or if one of them should be removed.
     */
    @Override
    public int getOverrideBehavior(ICompletionProposalHandle curr) {
        if (curr instanceof CtxInsensitiveImportComplProposal) {
            if (curr.getDisplayString().equals(getDisplayString())) {
                return IPyCompletionProposal.BEHAVIOR_IS_OVERRIDEN;
            } else {
                return IPyCompletionProposal.BEHAVIOR_COEXISTS;
            }
        } else {
            return IPyCompletionProposal.BEHAVIOR_IS_OVERRIDEN;
        }
    }

}