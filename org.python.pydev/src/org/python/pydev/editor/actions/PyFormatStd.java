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
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PyCodeFormatterPage;

/**
 * @author Fabio Zadrozny
 */
public class PyFormatStd extends PyAction implements IFormatter{

    public static class FormatStd{
        public boolean spaceAfterComma;
        public boolean parametersWithSpace;
    }
    
    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
		try {
            IFormatter participant = (IFormatter) ExtensionHelper.getParticipant(ExtensionHelper.PYDEV_FORMATTER);
            if(participant==null){
                participant = this;
            }
            PySelection ps = new PySelection ( getTextEditor ());
            IDocument doc = ps.getDoc();
            
            int startLine = ps.getStartLineIndex();
            if(ps.getTextSelection().getLength() == 0){
                participant.formatAll(doc, getPyEdit());
            }else{
                participant.formatSelection(doc, startLine, ps.getEndLineIndex(), getPyEdit(), ps);
            }
            
            if(startLine >= doc.getNumberOfLines()){
                startLine = doc.getNumberOfLines()-1;
            }
            TextSelection sel = new TextSelection(doc, doc.getLineOffset(startLine), 0);
            getTextEditor().getSelectionProvider().setSelection(sel);

		} catch ( Exception e ) {
			beep ( e );
		}		
    }

    public void formatSelection(IDocument doc, int startLine, int endLineIndex, PyEdit edit, PySelection ps){
        performFormatSelection(doc, startLine, endLineIndex);
    }
    
    public void formatAll(IDocument doc, PyEdit edit) {
        performFormatAll(doc);
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
            
            if(c == '\'' || c == '"'){ //ignore comments or multiline comments...
                i = ParsingUtils.eatLiterals(cs, buf, i);
                
            }else if(c == '#'){
                i = ParsingUtils.eatComments(cs, buf, i);
                
            }else if(c == ','){
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
        
        int j = i+1;
        while(j < cs.length && (c = cs[j]) != ')'){
            
            j++;
            
            if(c == '\'' || c == '"'){ //ignore comments or multiline comments...
                j = ParsingUtils.eatLiterals( cs, locBuf, j-1)+1;
                
            }else if(c == '#'){
                j = ParsingUtils.eatComments(cs, locBuf, j-1)+1;
                
            }else if( c == '('){ //open another par.
                j = formatForPar(cs, j-1, std, locBuf)+1;
            
            }else{

                locBuf.append(c);
            }
        }
        
        if(c == ')'){
            
            char c1;
            StringBuffer buf1 = new StringBuffer();
            
            if(locBuf.indexOf("\n") != -1){
	            for (int k = locBuf.length(); k > 0 && (c1 = locBuf.charAt(k-1))!= '\n'; k--) {
	                buf1.insert(0, c1);
	            }
            }
            
	        String formatStr = formatStr(trim(locBuf), std);
	        formatStr = trim(new StringBuffer(formatStr));
	        
	        String closing = ")";
	        if(buf1.length() > 0 && PyAction.containsOnlyWhitespaces(buf1.toString())){
	            formatStr += buf1.toString();
	        }else if(std.parametersWithSpace){
	            closing = " )";
	        }
	        
	        if(std.parametersWithSpace){
	            if(formatStr.length() == 0){
		            buf.append( "()" );
	                
	            }else{
		            buf.append( "( " );
		            buf.append( formatStr );
		            buf.append( closing );
	            }
	        }else{
	            buf.append( "(" );
	            buf.append( formatStr );
	            buf.append( closing );
	        }
	        return j;
        }else{
            return i;
        }
    }

    
    
    /**
     * We just want to trim whitespaces, not newlines!
     * @param locBuf
     * @return
     */
    private static String trim(StringBuffer locBuf) {
        while(locBuf.length() > 0 && locBuf.charAt(0) == ' '){
            locBuf.deleteCharAt(0);
        }
        while(locBuf.length() > 0 && locBuf.charAt(locBuf.length()-1) == ' '){
            locBuf.deleteCharAt(locBuf.length()-1);
        }
        return locBuf.toString();
    }

    /**
     * @param std
     * @param cs
     * @param buf
     * @param i
     * @return
     */
    private static int formatForComma(FormatStd std, char[] cs, StringBuffer buf, int i) {
        while(i < cs.length-1 && (cs[i+1]) == ' '){
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
