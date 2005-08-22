/*
 * Created on Feb 18, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.docutils.WordUtils;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Fabio Zadrozny
 */
public class PyOrganizeImports extends PyAction{

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
		try 
		{
			PySelection ps = new PySelection ( getTextEditor ( ));
		    String endLineDelim = ps.getEndLineDelim();
			IDocument doc = ps.getDoc();
			
			if(ps.getStartLineIndex() == ps.getEndLineIndex()){
			    performArrangeImports(doc, endLineDelim);
			}else{
			    performSimpleSort(doc, endLineDelim, ps.getStartLineIndex(), ps.getEndLineIndex());
			}
		} 
		catch ( Exception e ) 
		{
			beep ( e );
		}		
    }

    /**
     * Actually does the action in the document.
     * 
     * @param doc
     * @param endLineDelim
     */
    public static void performArrangeImports(IDocument doc, String endLineDelim){
		ArrayList list = new ArrayList();
		int lines = doc.getNumberOfLines();
		
		int firstImport = -1;
		boolean inComment = false;
		for (int i = 0; i < lines; i++) {
		    String str = PySelection.getLine(doc, i);
		    
		    if(str.indexOf("'''")!=-1){
		        String string = str.substring(str.indexOf("'''")+3);
		        if(string.indexOf("'''") == -1){
			        inComment = !inComment;
			        continue;
		        }
		    }
		    
		    if(inComment == false && (str.startsWith("import ") || str.startsWith("from "))){
                int iToAdd = i;
                if(WordUtils.endsWith(str, '\\')){
                    i++;
                    while(i < lines && WordUtils.endsWith(str, '\\')){
                        //we have to get all until there are no more back-slashes
                        String str1 = PySelection.getLine(doc, i);
                        str += PyAction.getDelimiter(doc);
                        str += str1;
                        i++;
                    }
                }
		        list.add( new Object[]{new Integer(iToAdd), str} );
		        
		        if(firstImport == -1){
		            firstImport = iToAdd;
		        }
		    }
        }
		
		//check if we had any import
		if(firstImport == -1){
		    return;
		}
		
		//sort in inverse order
		Collections.sort(list, new Comparator(){

            public int compare(Object o1, Object o2) {
                Object[] c1 = (Object[])o1;
                Object[] c2 = (Object[])o2;
                Integer i1 = (Integer) c1[0];
                Integer i2 = (Integer) c2[0];
                return i2.compareTo(i1);
            }
		});

		//ok, now we have to delete all lines with imports.
		for (Iterator iter = list.iterator(); iter.hasNext();) {
		    Object[] element = (Object[]) iter.next();
            String s = (String) element[1];
            int i = PyAction.countLineBreaks(s);
            while(i >= 0){
                PySelection.deleteLine(doc, ((Integer)element[0]).intValue());
                i--;
            }
        }
		
		Collections.sort(list, new Comparator(){

            public int compare(Object o1, Object o2) {
                Object[] c1 = (Object[])o1;
                Object[] c2 = (Object[])o2;
                String s1 = (String) c1[1];
                String s2 = (String) c2[1];
                return s1.compareTo(s2);
            }
		});
		
        firstImport--; //add line after the the specified
        StringBuffer all = new StringBuffer();
		for (Iterator iter = list.iterator(); iter.hasNext();) {
		    Object[] element = (Object[]) iter.next();
		    all.append((String) element[1]);
		    all.append(endLineDelim);
        }
	    PySelection.addLine(doc, endLineDelim, all.toString(), firstImport);
    }

    /**
     * 
     * @param doc
     * @param endLineDelim
     * @param startLine
     * @param endLine
     */
    public static void performSimpleSort(IDocument doc, String endLineDelim, int startLine, int endLine) {
        try {
	        ArrayList list = new ArrayList();
	        for (int i = startLine; i <= endLine; i++) {
			    list.add( PySelection.getLine(doc, i) );
	        }
	        
	        Collections.sort(list);
	        StringBuffer all = new StringBuffer();
			for (Iterator iter = list.iterator(); iter.hasNext();) {
			    String element = (String) iter.next();
			    all.append(element);
			    if(iter.hasNext())
			        all.append(endLineDelim);
			}
		
            int length = doc.getLineInformation(endLine).getLength();
            int endOffset = doc.getLineInformation(endLine).getOffset()+length;
            int startOffset = doc.getLineInformation(startLine).getOffset();
            
            doc.replace(startOffset, endOffset-startOffset, all.toString());
            
        } catch (BadLocationException e) {
            PydevPlugin.log(e);
        }

    }
}
