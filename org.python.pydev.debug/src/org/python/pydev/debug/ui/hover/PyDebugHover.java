package org.python.pydev.debug.ui.hover;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.debug.ui.actions.EvalExpressionAction;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.hover.IPyHoverParticipant;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Gathers hover info during a debug session.
 * 
 * @author Fabio
 */
public class PyDebugHover implements IPyHoverParticipant{

    /**
     * Gets the value from the debugger for the currently hovered string.
     */
    public String getHoverText(IRegion hoverRegion, PySourceViewer s, PySelection ps, ITextSelection selection) {
        IAdaptable object = DebugUITools.getDebugContext();
        
        IDebugElement context = null;
        if (object instanceof IDebugElement) {
            context = (IDebugElement) object;
        } else if (object instanceof ILaunch) {
            context = ((ILaunch) object).getDebugTarget();
        }
        
        if(context != null){
            String act = null;
            ITextSelection textSelection = (ITextSelection) selection;
            int mouseOffset = ps.getAbsoluteCursorOffset();
            
            int offset = textSelection.getOffset();
            int len = textSelection.getLength();
            if(len > 0 && mouseOffset >= offset && offset+len >= mouseOffset){
                try {
                    act = ps.getDoc().get(offset, len);
                } catch (BadLocationException e) {
                    //that's Ok... we were not able to get the actual selection here
                    PydevPlugin.log(e);
                }
            }
            if(act == null || act.trim().length() == 0){
                String[] activationTokenAndQual = ps.getActivationTokenAndQual(true);
                act = activationTokenAndQual[0]+activationTokenAndQual[1];
            }
            
            
            //OK, we're in a debug context...
            IWatchExpression watchExpression = EvalExpressionAction.createWatchExpression(act);
            watchExpression.evaluate();
            EvalExpressionAction.waitForExrpessionEvaluation(watchExpression);
            IValue value = watchExpression.getValue();
            if(value != null){
                try {
                    return value.getValueString();
                } catch (DebugException e) {
                    PydevPlugin.log(e);
                }
            }
        }
        
        return null;
    }

}
