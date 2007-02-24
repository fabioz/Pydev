/*
 * Created on Sep 23, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.IPyCodeCompletion;
import org.python.pydev.editor.correctionassist.docstrings.AssistDocString;
import org.python.pydev.editor.correctionassist.heuristics.AssistAssign;
import org.python.pydev.editor.correctionassist.heuristics.AssistImport;
import org.python.pydev.editor.correctionassist.heuristics.AssistTry;
import org.python.pydev.editor.correctionassist.heuristics.IAssistProps;
import org.python.pydev.plugin.PydevPlugin;

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
 * 	    self.newMethod()<- create new method on class C  (with params if needed)
 * 						<- assign result to new local variable 
 * 						<- assign result to new field 
 * 
 * 		a = A()
 * 		a.newMethod()   <- create new method on class A 
 * 						<- assign result to new local variable 
 * 						<- assign result to new field
 * 
 * 		param.b() <- don't show anything.
 * 
 * 		self.a1 = A() 
 * 		self.a1.newMethod() <- create new method on class A (difficult part is discovering class)
 * 							<- assign result to new local variable 
 * 							<- assign result to new field
 * 
 * 		def m(self): 
 * 			self.a1.newMethod() <- create new method on class A 
 * 								<- assign result to new local variable 
 * 								<- assign result to new field
 * 
 * 			import compiler	<- move import to global context
 * 			NewClass() <- Create class NewClass (Depends on new class wizard)
 *
 * 	   a() <-- make this a new method in this class 
 *                       																				 
 * @author Fabio Zadrozny
 */
public class PythonCorrectionProcessor implements IQuickAssistProcessor {

    private PyEdit edit;

    private ImageCache imageCache;

    private static Map<String, IAssistProps> additionalAssists = new HashMap<String, IAssistProps>();

    public static boolean hasAdditionalAssist(String id) {
        synchronized (additionalAssists) {
            return additionalAssists.containsKey(id);
        }
    }

    public static void addAdditionalAssist(String id, IAssistProps assist) {
        synchronized (additionalAssists) {
            additionalAssists.put(id, assist);
        }
    }

    public static void removeAdditionalAssist(String id, IAssistProps assist) {
        synchronized (additionalAssists) {
            additionalAssists.remove(id);
        }
    }

    /**
     * @param edit
     */
    public PythonCorrectionProcessor(PyEdit edit) {
        this.edit = edit;
        this.imageCache = new ImageCache(PydevPlugin.getDefault().getBundle().getEntry("/"));
    }

    public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
        return true;
    }

    public boolean canFix(Annotation annotation) {
        return false;
    }

    @SuppressWarnings("unchecked")
    public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext invocationContext) {
        int offset = invocationContext.getOffset();
        PySelection ps = new PySelection(edit);

        List<ICompletionProposal> results = new ArrayList<ICompletionProposal>();
        String sel = PyAction.getLineWithoutComments(ps);

        List<IAssistProps> assists = new ArrayList<IAssistProps>();
        synchronized (PythonCorrectionProcessor.additionalAssists) {
            for (IAssistProps prop : additionalAssists.values()) {
                assists.add(prop);
            }
        }

        assists.add(new AssistTry());
        assists.add(new AssistImport());
        assists.add(new AssistDocString());
        assists.add(new AssistAssign());

        assists.addAll(ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_CTRL_1));

        for (IAssistProps assist : assists) {
            try {
                if (assist.isValid(ps, sel, edit, offset)) {
                    try {
                        results.addAll(assist.getProps(ps, imageCache, edit.getEditorFile(), edit.getPythonNature(), edit, offset));
                    } catch (BadLocationException e) {
                        PydevPlugin.log(e);
                    }
                }
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }

        Collections.sort(results, IPyCodeCompletion.PROPOSAL_COMPARATOR);

        return (ICompletionProposal[]) results.toArray(new ICompletionProposal[0]);
    }

    public String getErrorMessage() {
        return null;
    }

}