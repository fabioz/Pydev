/*
 * Created on Mar 27, 2006
 */
package org.python.pydev.editor.actions;


import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.ui.texteditor.IStatusField;
import org.eclipse.ui.texteditor.IStatusFieldExtension;
import org.python.pydev.editor.PyEdit;

/**
 * This action is supposed to be subclassed. It provides means to attach a listener to the editor and grab the input until
 * it is disabled (such as the incremental find: ctrl+j)
 * 
 * Reference implementation: 
 * 
 * class IncrementalFindTarget

 * @author Fabio
 */
public class OfflineActionTarget implements VerifyKeyListener, MouseListener, FocusListener, ISelectionChangedListener, ITextListener {


    /** The string representing rendered tab */
    private final static String TAB= "<TAB>";
    /** The text viewer to operate on */
    private final ITextViewer fTextViewer;
    /** The status line manager for output */
    private final IStatusLineManager fStatusLine;
    /** The current find string */
    private StringBuffer fFindString= new StringBuffer();
    /** A flag indicating listeners are installed. */
    private boolean fInstalled;
    /**
     * The find status field.
     */
    private IStatusField fStatusField;
    /**
     * Tells whether the status field implements
     * <code>IStatusFieldExtension</code>.
     * @see IStatusFieldExtension
     */
    private boolean fIsStatusFieldExtension;
	private PyEdit fEdit;



    /**
     * Creates an instance of an incremental find target.
     * @param viewer the text viewer to operate on
     * @param manager the status line manager for output
     */
    public OfflineActionTarget(ITextViewer viewer, IStatusLineManager manager, PyEdit edit) {
        Assert.isNotNull(viewer);
        Assert.isNotNull(manager);
        fTextViewer= viewer;
        fStatusLine= manager;
        fEdit = edit;
    }

    public void beginSession() {

        // Workaround since some accelerators get handled directly by the OS
        if (fInstalled) {
            updateStatus();
            return;
        }

        fFindString.setLength(0);
        install();
        updateStatus();
    }

    public void endSession() {
        // will uninstall itself
    }

    /**
     * Installs this target. I.e. adds all required listeners.
     */
    private void install() {

        if (fInstalled)
            return;

        StyledText text= fTextViewer.getTextWidget();
        if (text == null)
            return;

        text.addMouseListener(this);
        text.addFocusListener(this);
        fTextViewer.addTextListener(this);

        ISelectionProvider selectionProvider= fTextViewer.getSelectionProvider();
        if (selectionProvider != null)
            selectionProvider.addSelectionChangedListener(this);

        if (fTextViewer instanceof ITextViewerExtension)
            ((ITextViewerExtension) fTextViewer).prependVerifyKeyListener(this);
        else
            text.addVerifyKeyListener(this);

        fInstalled= true;
    }

    /**
     * Uninstalls itself. I.e. removes all listeners installed in <code>install</code>.
     */
    private void uninstall() {

        fTextViewer.removeTextListener(this);

        ISelectionProvider selectionProvider= fTextViewer.getSelectionProvider();
        if (selectionProvider != null)
            selectionProvider.removeSelectionChangedListener(this);

        StyledText text= fTextViewer.getTextWidget();
        if (text != null) {
            text.removeMouseListener(this);
            text.removeFocusListener(this);
        }

        if (fTextViewer instanceof ITextViewerExtension) {
            ((ITextViewerExtension) fTextViewer).removeVerifyKeyListener(this);

        } else {
            if (text != null)
                text.removeVerifyKeyListener(this);
        }

        fInstalled= false;
    }

    /**
     * Updates the status line.
     */
    private void updateStatus() {
        if (!fInstalled)
            return;

        String string= fFindString.toString();

        statusMessage(string);
//        statusError("Error");
    }

    /*
     * @see VerifyKeyListener#verifyKey(VerifyEvent)
     */
    public void verifyKey(VerifyEvent event) {

        if (!event.doit)
            return;

        if (event.character == 0) {

            switch (event.keyCode) {

            // ALT, CTRL, ARROW_LEFT, ARROW_RIGHT == leave
            case SWT.ARROW_LEFT:
            case SWT.ARROW_RIGHT:
            case SWT.HOME:
            case SWT.END:
            case SWT.PAGE_DOWN:
            case SWT.PAGE_UP:
            case SWT.ARROW_DOWN:
            case SWT.ARROW_UP:
                leave();
                break;

            }

        // event.character != 0
        } else {

			switch (event.character) {

            // ESC = quit
            case 0x1B:
            	leave();
            	event.doit= false;
            	break;
            	
            //CR = exec and quit
            case 0x0D:
            	boolean executed = doExec();
                event.doit= false;
                if(!executed){
                	return; //we don't want to update the status
                }
                break;

            // backspace    and delete
            case 0x08:
            case 0x7F:
            	removeLastCharSearch();
                event.doit= false;
                break;

            default:
                if (event.stateMask == 0 || event.stateMask == SWT.SHIFT || event.stateMask == (SWT.ALT | SWT.CTRL)) { // SWT.ALT | SWT.CTRL covers AltGr (see bug 43049)
                    event.doit= false;
                    if(addCharSearch(event.character)){
                        //ok, triggered some automatic action (does not need enter)
                        executed = doExec();
                        if(!executed){
                            return; //we don't want to update the status
                        }
                        
                    }
                }
                break;
            }
        }
        updateStatus();
    }

    /**
     * @return
     */
    private boolean doExec() {
        final boolean executed = fEdit.onOfflineAction(fFindString.toString(), this);
        if(executed){
        	leave();
        }
        return executed;
    }


    /**
     * Is called from the outside on a backspace action (because it will eat the backspace, we have to make this
     * 'little' hack)
     */
    public void removeLastCharSearchAndUpdateStatus() {
    	removeLastCharSearch();
    	updateStatus();
    }
    
    private void removeLastCharSearch() {
    	final int len = fFindString.length();
    	if(len > 0){
    		fFindString.deleteCharAt(len-1);
    	}
	}

	/**
     * Adds the given character to the search string and repeats the search with the last parameters.
     *
     * @param c the character to append to the search pattern
     * @return <code>true</code> if the action triggered some 'automatic' action
     */
    private boolean addCharSearch(char c) {
        fFindString.append(c);
        return fEdit.activatesAutomaticallyOn(fFindString.toString());
    }

    /**
     * Leaves this incremental search session.
     */
    private void leave() {
        statusClear();
        uninstall();
    }

    /*
     * @see ITextListener#textChanged(TextEvent)
     */
    public void textChanged(TextEvent event) {
        if (event.getDocumentEvent() != null)
            leave();
    }

    /*
     * @see MouseListener##mouseDoubleClick(MouseEvent)
     */
    public void mouseDoubleClick(MouseEvent e) {
        leave();
    }

    /*
     * @see MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
     */
    public void mouseDown(MouseEvent e) {
        leave();
    }

    /*
     * @see MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
     */
    public void mouseUp(MouseEvent e) {
        leave();
    }

    /*
     * @see FocusListener#focusGained(org.eclipse.swt.events.FocusEvent)
     */
    public void focusGained(FocusEvent e) {
        leave();
    }

    /*
     * @see FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
     */
    public void focusLost(FocusEvent e) {
        leave();
    }

    /**
     * Sets the given string as status message, clears the status error message.
     * @param string the status message
     */
    private void statusMessage(String string) {
        if (fStatusField != null) {
            if (fIsStatusFieldExtension) {
                ((IStatusFieldExtension)fStatusField).setErrorText(null);
                fStatusField.setText(escapeTabs(string));
                ((IStatusFieldExtension)fStatusField).setVisible(true);
                fStatusLine.update(true);
            } else {
                fStatusLine.setErrorMessage(null);
                fStatusField.setText(escapeTabs(string));
            }
        } else {
            fStatusLine.setErrorMessage(null);
            fStatusLine.setMessage(escapeTabs(string));
        }
    }

    /**
     * Sets the status error message, clears the status message.
     * @param string the status error message
     */
    public void statusError(String string) {
        if (fStatusField != null) {
            if (fIsStatusFieldExtension) {
                ((IStatusFieldExtension)fStatusField).setErrorText(escapeTabs(string));
                fStatusField.setText(""); //$NON-NLS-1$
                ((IStatusFieldExtension)fStatusField).setVisible(true);
                fStatusLine.update(true);
            } else {
                fStatusLine.setErrorMessage(escapeTabs(string));
                fStatusField.setText(""); //$NON-NLS-1$
            }
        } else {
            fStatusLine.setErrorMessage(escapeTabs(string));
            fStatusLine.setMessage(null);
        }
    }

    /**
     * Clears the status message and the status error message.
     */
    public void statusClear() {
        if (fStatusField != null) {
            if (fIsStatusFieldExtension) {
                fStatusField.setText(""); //$NON-NLS-1$
                ((IStatusFieldExtension)fStatusField).setErrorText(null);
                ((IStatusFieldExtension)fStatusField).setVisible(false);
                fStatusLine.update(true);
            } else {
                fStatusField.setText(""); //$NON-NLS-1$
                fStatusLine.setErrorMessage(null);
            }
        } else {
            fStatusLine.setErrorMessage(null);
            fStatusLine.setMessage(null);
        }
    }

    /**
     * Translates all tab characters into a proper status line presentation.
     * @param string the string in which to translate the tabs
     * @return the given string with all tab characters replace with a proper status line presentation
     */
    private String escapeTabs(String string) {
        StringBuffer buffer= new StringBuffer();

        int begin= 0;
        int end= string.indexOf('\t', begin);

        while (end >= 0) {
            buffer.append(string.substring(begin, end));
            buffer.append(TAB);
            begin= end + 1;
            end= string.indexOf('\t', begin);
        }
        buffer.append(string.substring(begin));

        return buffer.toString();
    }

    /*
     * @see ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
     */
    public void selectionChanged(SelectionChangedEvent e) {
        //System.out.println("selection changed:"+e);
    }

    /**
     * Sets the find status field for this incremental find target.
     *
     * @param statusField the status field
     */
    void setStatusField(IStatusField statusField) {
        fStatusField= statusField;
        fIsStatusFieldExtension= fStatusField instanceof IStatusFieldExtension;
    }


	public boolean isInstalled() {
		return fInstalled;
	}
}
