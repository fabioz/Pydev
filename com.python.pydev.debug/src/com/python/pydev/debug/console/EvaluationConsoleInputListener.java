package com.python.pydev.debug.console;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.DebugUITools;
import org.python.pydev.debug.core.IConsoleInputListener;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.debug.model.remote.EvaluateExpressionCommand;

public class EvaluationConsoleInputListener implements IConsoleInputListener{

    private static final boolean DEBUG = false;
    private StringBuffer buf = new StringBuffer();
    
    public void newLineReceived(String lineReceived, AbstractDebugTarget target) {
        if(lineReceived.length() == 0){
            final String toEval = buf.toString();
            if(toEval.trim().length() > 0){
                IAdaptable context = DebugUITools.getDebugContext();
                if(DEBUG){
                    System.out.println("Evaluating:\n"+toEval);
                }
                if(context instanceof PyStackFrame){
                    target.getDebugger().postCommand(new EvaluateExpressionCommand(target.getDebugger(), toEval, 
                            ((PyStackFrame)context).getLocalsLocator().getPyDBLocation(), true));
                }
            }
            buf = new StringBuffer();
            
        }else{
            if(DEBUG){
                System.out.println("line: '"+lineReceived+"'");
            }
            buf.append(lineReceived);
            buf.append("@LINE@");
        }
    }

    public void pasteReceived(String text, AbstractDebugTarget target) {
        if(DEBUG){
            System.out.println("paste: '"+text+"'");
        }
        text = text.replaceAll("\n", "@LINE@").replaceAll("\r","");
        buf.append(text);
        buf.append("@LINE@");
    }

}
