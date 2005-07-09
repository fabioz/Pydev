/*
 * @author: fabioz
 * Created: January 2004
 * License: Common Public License v1.0
 */
 
package org.python.pydev.editor.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;

/**
 * @author Fabio Zadrozny
 * 
 * Superclass of all our actions. Contains utility functions.
 * 
 * Subclasses should implement run(IAction action) method.
 */
public abstract class PyAction implements IEditorActionDelegate {

	// Always points to the current editor
	protected IEditorPart targetEditor;

	public void setEditor(IEditorPart targetEditor) {
		this.targetEditor = targetEditor;
	}
	
	/**
	 * This is an IEditorActionDelegate override
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		setEditor(targetEditor);
	}

	/**
	 * Activate action  (if we are getting text)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof TextSelection) {
			action.setEnabled(true);
			return;
		}
		action.setEnabled( targetEditor instanceof ITextEditor);
	}

	public static String getDelimiter(IDocument doc){
	    return getDelimiter(doc, 0);
	}
	
	/**
	 * This method returns the delimiter for the document
	 * @param doc
	 * @param startLineIndex
	 * @return  delimiter for the document (\n|\r\|r\n)
	 * @throws BadLocationException
	 */
	public static String getDelimiter(IDocument doc, int line){
		String endLineDelim;
        try {
			if (doc.getNumberOfLines() > 1){
			    endLineDelim = doc.getLineDelimiter(line);
		        if (endLineDelim == null) {
					endLineDelim = doc.getLegalLineDelimiters()[0];
				}
				return endLineDelim;
			}
        } catch (BadLocationException e) {
            PydevPlugin.log(e);
        }
		return System.getProperty("line.separator"); 
		
	}

	/**
	 * This function returns the text editor.
	 */
	protected ITextEditor getTextEditor() {
		if (targetEditor instanceof ITextEditor) {
			return (ITextEditor) targetEditor;
		} else {
			throw new RuntimeException("Expecting text editor. Found:"+targetEditor.getClass().getName());
		}
	}

	/**
	 * @return python editor.
	 */
	protected PyEdit getPyEdit() {
		if (targetEditor instanceof PyEdit) {
			return (PyEdit) targetEditor;
		} else {
			throw new RuntimeException("Expecting PyEdit editor. Found:"+targetEditor.getClass().getName());
		}
	}
	

	/**
	 * Helper for setting caret
	 * @param pos
	 * @throws BadLocationException
	 */
	protected void setCaretPosition(int pos) throws BadLocationException {
		getTextEditor().selectAndReveal(pos, 0);
	}

	/**
	 * Are we in the first char of the line with the offset passed?
	 * @param doc
	 * @param cursorOffset
	 */
	protected void isInFirstVisibleChar(IDocument doc, int cursorOffset) {
		try {
			IRegion region = doc.getLineInformationOfOffset(cursorOffset);
			int offset = region.getOffset();
			String src = doc.get(offset, region.getLength());
			if ("".equals(src))
				return;
			int i = 0;
			while (i < src.length()) {
				if (!Character.isWhitespace(src.charAt(i))) {
					break;
				}
				i++;
			}
			setCaretPosition(offset + i - 1);
		} catch (BadLocationException e) {
			beep(e);
			return;
		}
	}


	/**
	 * Returns the position of the first non whitespace char in the current line.
	 * @param doc
	 * @param cursorOffset
	 * @return position of the first character of the line (returned as an absolute
	 * 		   offset)
	 * @throws BadLocationException
	 */
	public static int getFirstCharPosition(IDocument doc, int cursorOffset)
		throws BadLocationException {
        IRegion region;
		region = doc.getLineInformationOfOffset(cursorOffset);
		int offset = region.getOffset();
		return offset + getFirstCharRelativePosition(doc, cursorOffset);
	}
	
	

	/**
     * @param doc
     * @param cursorOffset
     * @return
     * @throws BadLocationException
     */
    public static int getFirstCharRelativePosition(IDocument doc, int cursorOffset) throws BadLocationException {
        IRegion region;
		region = doc.getLineInformationOfOffset(cursorOffset);
		return getFirstCharRelativePosition(doc, region);
    }

	/**
     * @param doc
     * @param cursorOffset
     * @return
     * @throws BadLocationException
     */
    public static int getFirstCharRelativeLinePosition(IDocument doc, int line) throws BadLocationException {
        IRegion region;
		region = doc.getLineInformation(line);
		return getFirstCharRelativePosition(doc, region);
    }

    /**
     * @param doc
     * @param region
     * @return
     * @throws BadLocationException
     */
    public static int getFirstCharRelativePosition(IDocument doc, IRegion region) throws BadLocationException {
        int offset = region.getOffset();
		String src = doc.get(offset, region.getLength());

		return getFirstCharPosition(src);
    }

    /**
     * @param src
     * @return
     */
    public static int getFirstCharPosition(String src) {
        int i = 0;
		boolean breaked = false;
		while (i < src.length()) {
		    if (   Character.isWhitespace(src.charAt(i)) == false && src.charAt(i) != '\t'  ) {
		        i++;
			    breaked = true;
				break;
			}
		    i++;
		}
		if (!breaked){
		    i++;
		}
		return (i - 1);
    }

    /**
	 * Returns the position of the last non whitespace char in the current line.
	 * @param doc
	 * @param cursorOffset
	 * @return position of the last character of the line (returned as an absolute
	 * 		   offset)
	 * 
	 * @throws BadLocationException
	 */
	protected int getLastCharPosition(IDocument doc, int cursorOffset)
		throws BadLocationException {
		IRegion region;
		region = doc.getLineInformationOfOffset(cursorOffset);
		int offset = region.getOffset();
		String src = doc.get(offset, region.getLength());

		int i = src.length();
		boolean breaked = false;
		while (i > 0 ) {
		    i--;
		    //we have to break if we find a character that is not a whitespace or a tab.
			if (   Character.isWhitespace(src.charAt(i)) == false && src.charAt(i) != '\t'  ) {
			    breaked = true;
				break;
			}
		}
		if (!breaked){
		    i--;
		}
		return (offset + i);
	}

	/**
	 * Goes to first char of the line.
	 * @param doc
	 * @param cursorOffset
	 */
	protected void gotoFirstChar(IDocument doc, int cursorOffset) {
		try {
			IRegion region = doc.getLineInformationOfOffset(cursorOffset);
			int offset = region.getOffset();
			setCaretPosition(offset);
		} catch (BadLocationException e) {
			beep(e);
		}
	}

	/**
	 * Goes to the first visible char.
	 * @param doc
	 * @param cursorOffset
	 */
	protected void gotoFirstVisibleChar(IDocument doc, int cursorOffset) {
		try {
			setCaretPosition(getFirstCharPosition(doc, cursorOffset));
		} catch (BadLocationException e) {
			beep(e);
		}
	}

	/**
	 * Goes to the first visible char.
	 * @param doc
	 * @param cursorOffset
	 */
	protected boolean isAtFirstVisibleChar(IDocument doc, int cursorOffset) {
		try {
			return getFirstCharPosition(doc, cursorOffset) == cursorOffset;
		} catch (BadLocationException e) {
			return false;
		}
	}

	//================================================================
	// HELPER FOR DEBBUGING... 
	//================================================================

	/*
	 * Beep...humm... yeah....beep....ehehheheh
	 */
	protected static void beep(Exception e) {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay().beep();
		e.printStackTrace();
	}

	protected void print(Object o) {
		System.out.println(o);
	}

	protected void print(boolean b) {
		System.out.println(b);
	}
    protected void print(int i) {
		System.out.println(i);
	}

    /**
     * 
     */
    public static String getLineWithoutComments(String sel) {
        return sel.replaceAll("#.*", "");
    }
    
    /**
     * 
     */
    public static String getLineWithoutComments(PySelection ps) {
        return getLineWithoutComments(ps.getCursorLineContents());
    }

    /**
     * @param ps
     * @return
     */
    public static String getInsideParentesisTok(String sel) {
        sel = getLineWithoutComments(sel);
        
        int beg = sel.indexOf('(')+1;
        int end = sel.indexOf(')');
        return sel.substring(beg, end);
    }
    
    /**
     * @param ps
     * @return
     */
    public static String getInsideParentesisTok(PySelection ps) {
        String cursorLineContents = ps.getCursorLineContents();
        if(cursorLineContents.indexOf("(") != -1 && cursorLineContents.indexOf(")") != -1) 
            return getInsideParentesisTok(cursorLineContents);
        else
            return "";
    }
    
    public static List getInsideParentesisToks(IDocument doc, int offset){
        List l = new ArrayList();
        try {
            int lineOfOffset = doc.getLineOfOffset(offset);
            IRegion lineInformation = doc.getLineInformation(lineOfOffset);
            String sel = doc.get(lineInformation.getOffset(), lineInformation.getLength());
            l = getInsideParentesisToks(sel);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return l;
    }

    
    
    /**
     * @param l
     * @param sel
     */
    public static List getInsideParentesisToks(String sel, boolean addSelf) {
        List l = new ArrayList();
        String insideParentesisTok = getInsideParentesisTok(sel);
        
        StringTokenizer tokenizer = new StringTokenizer(insideParentesisTok, ",");
        while(tokenizer.hasMoreTokens()){
            String tok = tokenizer.nextToken();
            String trimmed = tok.split("=")[0].trim();
            if(!addSelf && trimmed.equals("self")){
                //don't add self...
            }else{
                l.add(trimmed);
            }
        }
        return l;
    }
    
    /**
     * @param l
     * @param sel
     */
    public static List getInsideParentesisToks(String sel) {
        return getInsideParentesisToks(sel, true);
    }

    /**
     * @param document
     * @param offset
     * @param string
     * @return
     */
    public static boolean lineContains(IDocument document, int offset, String tok) {
        try {
            IRegion lineInformation = getRegionOfOffset(document, offset);
            return regionContains(document, tok, lineInformation);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @param document
     * @param tok
     * @param lineInformation
     * @return
     * @throws BadLocationException
     */
    private static boolean regionContains(IDocument document, String tok, IRegion lineInformation) throws BadLocationException {
        String line = document.get(lineInformation.getOffset(), lineInformation.getLength());
        return line.indexOf(tok) != -1;
    }

    /**
     * @param document
     * @param offset
     * @return
     * @throws BadLocationException
     */
    private static IRegion getRegionOfOffset(IDocument document, int offset) throws BadLocationException {
        int lineOfOffset = document.getLineOfOffset(offset);
        IRegion lineInformation = document.getLineInformation(lineOfOffset);
        return lineInformation;
    }

    /**
     * @param document
     * @param offset
     * @param string
     * @return
     */
    public static boolean nextLineContains(IDocument document, int offset, String tok) {
        try {
            int lineOfOffset = document.getLineOfOffset(offset);
            IRegion lineInformation = document.getLineInformation(lineOfOffset+1);
            return regionContains(document, tok, lineInformation);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @param selection
     * @return
     */
    public static String getIndentationFromLine(String selection) {
        int firstCharPosition = getFirstCharPosition(selection);
        return selection.substring(0, firstCharPosition);
    }

    /**
     * @param c
     * @param string
     */
    public static boolean containsOnlyWhitespaces(String string) {
        for (int i = 0; i < string.length(); i++) {
            if(Character.isWhitespace(string.charAt(i)) == false){
                return false;
            }
        }
        return true;
    }
    
    /**
     * @param c
     * @param string
     */
    public static boolean containsOnly(char c, String string) {
        for (int i = 0; i < string.length(); i++) {
            if(string.charAt(i) != c){
                return false;
            }
        }
        return true;
    }

    /**
     * @param c
     * @param line
     */
    public static int countChars(char c, String line) {
        int ret = 0;
        for (int i = 0; i < line.length(); i++) {
            if(line.charAt(i) == c){
                ret += 1;
            }
        }
        return ret;
    }

    /**
     * @param ps
     * @return string with the token or empty token if not found.
     */
    public static String getBeforeParentesisTok(PySelection ps) {
        String string = getLineWithoutComments(ps);
    
        int i;
    
        String callName = "";
        //get parentesis position and go backwards
        if ((i = string.indexOf("(")) != -1) {
            callName = "";
    
            for (int j = i-1; j >= 0 && PyAction.stillInTok(string, j); j--) {
                callName = string.charAt(j) + callName;
            }
            
        }
        return callName;
    }

    public static String lowerChar(String s, int pos){
        char[] ds = s.toCharArray(); 
        ds[pos] = (""+ds[pos]).toLowerCase().charAt(0);
        return new String(ds);
    }

    /**
     * @param string
     * @param j
     * @return
     */
    public static boolean stillInTok(String string, int j) {
        char c = string.charAt(j);
    
        return c != '\n' && c != '\r' && c != ' ' && c != '.' && c != '(' && c != ')' && c != ',' && c != ']' && c != '[' && c != '#';
    }

    /**
     * 
     * @return indentation string (always recreated) 
     */
    public static String getStaticIndentationString() {
        try {
            int tabWidth = PydevPrefs.getPreferences().getInt(PydevPrefs.TAB_WIDTH);
            boolean useSpaces = PydevPrefs.getPreferences().getBoolean(PydevPrefs.SUBSTITUTE_TABS);
            boolean forceTabs = false;
            String identString;

            if (useSpaces && !forceTabs)
                identString = PyAction.createStaticSpaceString(tabWidth, tabWidth);
            else
                identString = "\t";
            return identString;
        } catch (Exception e) {
            
            PydevPlugin.log(e);
            return "    "; //default
        }
    }

    public static String createStaticSpaceString(int width, int tabWidth) {
        StringBuffer b = new StringBuffer(width);
        while (tabWidth-- > 0)
            b.append(" ");
        return b.toString();
    }
}
