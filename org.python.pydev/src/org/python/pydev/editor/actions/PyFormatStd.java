/*
 * Created on Feb 22, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.plugin.PyCodeFormatterPage;

/**
 * @author Fabio Zadrozny
 */
public class PyFormatStd extends PyAction{

    public static class FormatStd{
        public boolean spaceAfterComma;
        public boolean parametersWithSpace;
    }
    
    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
		try 
		{
			PySelection ps = new PySelection ( getTextEditor ( ), false );
		    String endLineDelim = ps.endLineDelim;
			IDocument doc = ps.doc;
			
			if(ps.textSelection.getLength() == 0){
			    performFormatAll(doc);
			}else{
			    performFormatSelection(doc, ps.startLineIndex, ps.endLineIndex);
			}
			TextSelection sel = new TextSelection(doc, doc.getLineOffset(ps.startLineIndex), 0);
			getTextEditor().getSelectionProvider().setSelection(sel);
		} 
		catch ( Exception e ) 
		{
			beep ( e );
		}		
    }

    /**
     * @param doc
     * @param endLineDelim
     * @param startLineIndex
     * @param endLineIndex
     */
    public void performFormatSelection(IDocument doc, int startLineIndex, int endLineIndex) {
        try {
            IRegion start = doc.getLineInformation(startLineIndex);
            IRegion end = doc.getLineInformation(endLineIndex);
        
            int iStart = start.getOffset();
            int iEnd = end.getOffset()+end.getLength();
            
	        String d = doc.get(iStart, iEnd-iStart);
	        FormatStd formatStd = getFormat();
	        String formatted = formatStr(d, formatStd);
	        
	        doc.replace(iStart, iEnd-iStart, formatted);
	        
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private FormatStd getFormat(){
        FormatStd formatStd = new FormatStd();
        formatStd.parametersWithSpace = PyCodeFormatterPage.useSpaceForParentesis();
        formatStd.spaceAfterComma = PyCodeFormatterPage.useSpaceAfterComma();
        return formatStd;
    }
    
    /**
     * @param doc
     * @param endLineDelim
     */
    public void performFormatAll(IDocument doc) {
        String d = doc.get();
        FormatStd formatStd = getFormat();
        String formatted = formatStr(d, formatStd);
        doc.set(formatted);
    }


    /**
     * This method formats a string given some standard.
     * 
     * @param str
     * @param std
     * @return
     */
    public static String formatStr(String str, FormatStd std ){
        char[] cs = str.toCharArray();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < cs.length; i++) {
            char c = cs[i];
            
            if(c == ','){
		        i = formatForComma(std, cs, buf, i);
		        
            }else if(c == '('){
		        
                i = formatForPar(cs, i, std, buf);
		        
                
            }else{
                buf.append(c);
            }
        }
        return buf.toString();
    }

    /**
     * @param cs
     * @param i
     */
    private static int formatForPar(char[] cs, int i, FormatStd std, StringBuffer buf) {
        char c = ' ';
        StringBuffer locBuf = new StringBuffer();
        
        //check that, otherwise, we get 2 spaces (so we don't break the rule... and treat the exception as exception)
        if(cs[i+1] == ')'){
            buf.append( "( )" ); 
            return i+1;
        }
        
        int j = i+1;
        while(j < cs.length && (c = cs[j]) != ')'){
            
            j++;
            
            if( c == '('){ //open another par.
                j = formatForPar(cs, j-1, std, locBuf)+1;
            
            }else{

                locBuf.append(c);
            }
        }
        
        if(c == ')'){
        
	        if(std.parametersWithSpace){
	            buf.append( "( " );
	            buf.append( formatStr(locBuf.toString().trim(), std) );
	            buf.append( " )" );
	        }else{
	            buf.append( "(" );
	            buf.append( formatStr(locBuf.toString().trim(), std) );
	            buf.append( ")" );
	        }
	        return j;
        }else{
            return i;
        }
    }

    /**
     * @param std
     * @param cs
     * @param buf
     * @param i
     * @return
     */
    private static int formatForComma(FormatStd std, char[] cs, StringBuffer buf, int i) {
        char c;
        while(i < cs.length && (c = cs[i+1]) == ' '){
            i++;
        }
        
        if(std.spaceAfterComma){
            buf.append(", ");
        }else{
            buf.append(',');
        }
        return i;
    }
}
