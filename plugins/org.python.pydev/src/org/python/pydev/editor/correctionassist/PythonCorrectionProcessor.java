/**
 * Copyright (c) 2005-2016 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 23, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.internal.texteditor.spelling.NoCompletionsProposal;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingCorrectionProcessor;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.IPySyntaxHighlightingAndCodeCompletionEditor;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.ProposalsComparator;
import org.python.pydev.editor.correctionassist.docstrings.AssistDocString;
import org.python.pydev.editor.correctionassist.heuristics.AssistAssign;
import org.python.pydev.editor.correctionassist.heuristics.AssistImport;
import org.python.pydev.editor.correctionassist.heuristics.AssistPercentToFormat;
import org.python.pydev.editor.correctionassist.heuristics.AssistSurroundWith;
import org.python.pydev.editor.correctionassist.heuristics.IAssistProps;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.ImageCache;

/**
 * This class should be used to give context help
 *
 * Help depending on context (Ctrl+1):
 *
 * class A: pass
 *
 * class C:
 *
 * def __init__(self, param):
 *         self.newMethod()<- create new method on class C  (with params if needed)
 *                         <- assign result to new local variable
 *                         <- assign result to new field
 *
 *         a = A()
 *         a.newMethod()   <- create new method on class A
 *                         <- assign result to new local variable
 *                         <- assign result to new field
 *
 *         param.b() <- don't show anything.
 *
 *         self.a1 = A()
 *         self.a1.newMethod() <- create new method on class A (difficult part is discovering class)
 *                             <- assign result to new local variable
 *                             <- assign result to new field
 *
 *         def m(self):
 *             self.a1.newMethod() <- create new method on class A
 *                                 <- assign result to new local variable
 *                                 <- assign result to new field
 *
 *             import compiler    <- move import to global context
 *             NewClass() <- Create class NewClass (Depends on new class wizard)
 *
 *        a() <-- make this a new method in this class
 *
 * @author Fabio Zadrozny
 */
public class PythonCorrectionProcessor implements IQuickAssistProcessor {

    private IPySyntaxHighlightingAndCodeCompletionEditor edit;

    /**
     * Contains additional assists (used from the jython scripting: pyedit_assign_params_to_attributes.py to add new assists)
     */
    private static Map<String, IAssistProps> additionalAssists = new HashMap<String, IAssistProps>();

    /**
     * Checks if some assist with the given id is already added.
     *
     * @param id the id of the assist
     * @return true if it's already added and false otherwise
     */
    public static boolean hasAdditionalAssist(String id) {
        synchronized (additionalAssists) {
            return additionalAssists.containsKey(id);
        }
    }

    /**
     * Adds some additional assist to Ctrl+1 (used from the scripting engine)
     *
     * @param id the id of the assist
     * @param assist the assist to be added
     */
    public static void addAdditionalAssist(String id, IAssistProps assist) {
        synchronized (additionalAssists) {
            additionalAssists.put(id, assist);
        }
    }

    /**
     * Removes some additional assist from Ctrl+1
     *
     * @param id id of the assist to be removed
     */
    public static void removeAdditionalAssist(String id) {
        synchronized (additionalAssists) {
            additionalAssists.remove(id);
        }
    }

    /**
     * @param edit
     */
    public PythonCorrectionProcessor(IPySyntaxHighlightingAndCodeCompletionEditor edit) {
        this.edit = edit;
    }

    @Override
    public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
        return true;
    }

    @Override
    public boolean canFix(Annotation annotation) {
        return false;
    }

    @Override
    public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext invocationContext) {
        int offset = invocationContext.getOffset();
        PySelection base = edit.createPySelection();
        if (!(this.edit instanceof PyEdit) || base == null) {
            return new ICompletionProposal[0];
        }
        PyEdit editor = (PyEdit) this.edit;

        List<ICompletionProposal> results = new ArrayList<ICompletionProposal>();
        String sel = PyAction.getLineWithoutComments(base);

        List<IAssistProps> assists = new ArrayList<IAssistProps>();
        synchronized (PythonCorrectionProcessor.additionalAssists) {
            for (IAssistProps prop : additionalAssists.values()) {
                assists.add(prop);
            }
        }

        assists.add(new AssistSurroundWith());
        assists.add(new AssistImport());
        assists.add(new AssistDocString());
        assists.add(new AssistAssign());
        assists.add(new AssistPercentToFormat());

        assists.addAll(ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_CTRL_1));
        ImageCache imageCache = PydevPlugin.getImageCache();
        File editorFile = edit.getEditorFile();
        IPythonNature pythonNature = null;
        try {
            pythonNature = edit.getPythonNature();
        } catch (MisconfigurationException e1) {
            Log.log(e1);
        }

        for (IAssistProps assist : assists) {
            //Always create a new for each assist, as any given assist may change it.
            PySelection ps = new PySelection(base);
            try {
                if (assist.isValid(ps, sel, editor, offset)) {
                    try {
                        results.addAll(assist.getProps(ps, imageCache, editorFile, pythonNature, editor, offset));
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }

        Collections.sort(results, new ProposalsComparator("", new ProposalsComparator.CompareContext(pythonNature)));

        try {
            //handling spelling... (we only want to show spelling fixes if a spell problem annotation is found at the current location).
            //we'll only show some spelling proposal if there's some spelling problem (so, we don't have to check the preferences at this place,
            //as no annotations on spelling will be here if the spelling is not enabled).
            ICompletionProposal[] spellProps = null;

            IAnnotationModel annotationModel = editor.getPySourceViewer().getAnnotationModel();
            Iterator<Annotation> it = annotationModel.getAnnotationIterator();
            while (it.hasNext()) {
                Annotation annotation = it.next();
                if (annotation instanceof SpellingAnnotation) {
                    SpellingAnnotation spellingAnnotation = (SpellingAnnotation) annotation;
                    SpellingProblem spellingProblem = spellingAnnotation.getSpellingProblem();

                    int problemOffset = spellingProblem.getOffset();
                    int problemLen = spellingProblem.getLength();
                    if (problemOffset <= offset && problemOffset + problemLen >= offset) {
                        SpellingCorrectionProcessor spellingCorrectionProcessor = new SpellingCorrectionProcessor();
                        spellProps = spellingCorrectionProcessor.computeQuickAssistProposals(invocationContext);
                        break;
                    }
                }
            }

            if (spellProps == null || (spellProps.length == 1 && spellProps[0] instanceof NoCompletionsProposal)) {
                //no proposals from the spelling
                return results.toArray(new ICompletionProposal[results.size()]);
            }

            //ok, add the spell problems and return...
            ICompletionProposal[] ret = results.toArray(new ICompletionProposal[results.size()
                    + spellProps.length]);
            System.arraycopy(spellProps, 0, ret, results.size(), spellProps.length);
            return ret;
        } catch (Throwable e) {
            if (e instanceof ClassNotFoundException || e instanceof LinkageError || e instanceof NoSuchMethodException
                    || e instanceof NoSuchMethodError || e instanceof NoClassDefFoundError) {
                //Eclipse 3.2 support
                return results.toArray(new ICompletionProposal[results.size()]);
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

}