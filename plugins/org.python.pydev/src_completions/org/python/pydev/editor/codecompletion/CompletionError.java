package org.python.pydev.editor.codecompletion;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

public class CompletionError implements ICompletionProposal, IPyCompletionProposal, ICompletionProposalExtension4{

    private Throwable error;
    
    public CompletionError(Throwable e) {
        this.error = e;
    }

    public void apply(IDocument document) {
    }

    public String getAdditionalProposalInfo() {
        return getErrorMessage();
    }

    public IContextInformation getContextInformation() {
        return null;
    }

    public String getDisplayString() {
        return getErrorMessage();
    }

    public Image getImage() {
        return PydevPlugin.getImageCache().get(UIConstants.ERROR);
    }

    public Point getSelection(IDocument document) {
        return null;
    }

    public int getPriority() {
        return -1;
    }

    public boolean isAutoInsertable() {
        return false;
    }

    public String getErrorMessage() {
        String message = error.getMessage();
        if(message == null){
            //NullPointerException
            if(error instanceof NullPointerException){
                error.printStackTrace();
                message = "NullPointerException";
            }else{
                message = "Null error message";
            }
        }
        
        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            error.printStackTrace(new PrintStream(out));
            String errorLocation = out.toString();
            StringTokenizer strTok = new StringTokenizer(errorLocation, "\r\n");
            
            //Get the 1st line that has line information in it...
            while(strTok.hasMoreTokens()){
                String tok = strTok.nextToken();
                if(tok.endsWith(")") && tok.startsWith("\tat ") && tok.indexOf(":") != -1 && tok.indexOf("(") != -1){
                    message+="\nAt: "+tok.trim();
                    break;
                }
            }
        }catch(Throwable e){
            //Ignore any error here...
        }
        
        return message;
    }

}
