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
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.docutils.ImportHandle;
import org.python.pydev.core.docutils.PyImportsHandling;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Fabio Zadrozny
 */
public class PyOrganizeImports extends PyAction{

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @SuppressWarnings("unchecked")
	public void run(IAction action) {
		try 
		{
			PySelection ps = new PySelection ( getTextEditor ( ));
		    String endLineDelim = ps.getEndLineDelim();
			final IDocument doc = ps.getDoc();
			DocumentRewriteSession session = startWrite(doc);
			
			try {
				if (ps.getStartLineIndex() == ps.getEndLineIndex()) {
					//let's see if someone wants to make a better implementation in another plugin...
					List<IOrganizeImports> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_ORGANIZE_IMPORTS);
					if (participants.size() == 1) {
						PyEdit pyEdit = getPyEdit();
						participants.get(0).performArrangeImports(ps, pyEdit);
					} else {
						if (participants.size() > 1) {
							//let's issue a warning... this extension can only have 1 plugin implementing it
							PydevPlugin.log("The organize imports has more than one plugin with this extension point, therefore, the default is being used.");
						}
						performArrangeImports(doc, endLineDelim);
					}
				} else {
					performSimpleSort(doc, endLineDelim, ps.getStartLineIndex(), ps.getEndLineIndex());
				}
			} finally {
				endWrite(doc, session);
			}
		} 
		catch ( Exception e ) 
		{
            PydevPlugin.log(e);
			beep ( e );
		}		
    }

	private void endWrite(IDocument doc, DocumentRewriteSession session) {
		if(doc instanceof IDocumentExtension4){
			IDocumentExtension4 d = (IDocumentExtension4) doc;
			d.stopRewriteSession(session);
		}
	}

	private DocumentRewriteSession startWrite(IDocument doc) {
		if(doc instanceof IDocumentExtension4){
			IDocumentExtension4 d = (IDocumentExtension4) doc;
			return d.startRewriteSession(DocumentRewriteSessionType.UNRESTRICTED);
		}
		return null;
	}

    /**
     * Actually does the action in the document.
     * 
     * @param doc
     * @param endLineDelim
     */
    @SuppressWarnings("unchecked")
	public static void performArrangeImports(IDocument doc, String endLineDelim){
		ArrayList list = new ArrayList();
		
		//Gather imports in a structure we can work on.
		PyImportsHandling pyImportsHandling = new PyImportsHandling(doc);
		int firstImport = -1;
		for(ImportHandle imp:pyImportsHandling){
            list.add( new Object[]{imp.startFoundLine, imp.importFound} );
            
            if(firstImport == -1){
                firstImport = imp.startFoundLine;
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
            int i = PySelection.countLineBreaks(s);
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
    @SuppressWarnings("unchecked")
	public static void performSimpleSort(IDocument doc, String endLineDelim, int startLine, int endLine) {
        try {
	        ArrayList<String> list = new ArrayList<String>();
	        
	        StringBuffer lastLine = null;
	        for (int i = startLine; i <= endLine; i++) {
	            
	            String line = PySelection.getLine(doc, i);
                
	            if(lastLine != null){
	                int len = lastLine.length();
                    if(len > 0 && lastLine.charAt(len-1) == '\\'){
                        lastLine.append(endLineDelim);
                        lastLine.append(line);
                    }else{
                        list.add(lastLine.toString());
                        lastLine = new StringBuffer(line);
                    }
	            }else{
	                lastLine = new StringBuffer(line);
	            }
	        }
	        
	        if(lastLine != null){
	            list.add(lastLine.toString());
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
