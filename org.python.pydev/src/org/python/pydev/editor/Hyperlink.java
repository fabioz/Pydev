/*
 * Author: atotic
 * Created on Mar 31, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.model.ModelUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.ui.ColorCache;

/**
 * Hyperlink is a "Mouse highlight on ctrl".
 * The code here has been copied from JavaEditor::MouseClick.
 * I've just modified Python-specific code.
 */
public class Hyperlink implements KeyListener, MouseListener, MouseMoveListener,
	FocusListener, PaintListener, IDocumentListener, ITextInputListener {

	/** The session is active. */
	private boolean fActive;

	/** The currently active style range. */
	private IRegion fActiveRegion;
	/** The currently active style range as position. */
	private Position fRememberedPosition;
	/** The hand cursor. */
	private Cursor fCursor;
		
	/** The link color. */
	private Color fColor;
	/** The key modifier mask. */
	private int fKeyModifierMask;

	private ColorCache fColorCache;

	/************
	 * ALEKS ADDITIONS
	 */
	private ISourceViewer fSourceViewer;
	private PyEdit fEditor;
	private AbstractNode fClickedNode;
	
	public Hyperlink(ISourceViewer sourceViewer, PyEdit editor, ColorCache colorCache) {
		fSourceViewer = sourceViewer;
		fEditor = editor;
		fKeyModifierMask = SWT.CTRL;
		fColorCache = colorCache;
	}
	
	private ISourceViewer getSourceViewer() {
		return fSourceViewer;
	}	

	public void deactivate() {
		deactivate(false);
	}

	public void deactivate(boolean redrawAll) {
		if (!fActive)
			return;

		repairRepresentation(redrawAll);			
		fActive= false;
	}

	public void install() {

		ISourceViewer sourceViewer= getSourceViewer();
		if (sourceViewer == null)
			return;
				
		StyledText text= sourceViewer.getTextWidget();			
		if (text == null || text.isDisposed())
			return;
				
		updateColor(sourceViewer);

		sourceViewer.addTextInputListener(this);
			
		IDocument document= sourceViewer.getDocument();
		if (document != null)
			document.addDocumentListener(this);			

		text.addKeyListener(this);
		text.addMouseListener(this);
		text.addMouseMoveListener(this);
		text.addFocusListener(this);
		text.addPaintListener(this);
	}

	private int computeStateMask(String modifiers) {
		if (modifiers == null)
			return -1;
		
		if (modifiers.length() == 0)
			return SWT.NONE;

		int stateMask= 0;
		StringTokenizer modifierTokenizer= new StringTokenizer(modifiers, ",;.:+-* "); //$NON-NLS-1$
		while (modifierTokenizer.hasMoreTokens()) {
			int modifier= EditorUtility.findLocalizedModifier(modifierTokenizer.nextToken());
			if (modifier == 0 || (stateMask & modifier) == modifier)
				return -1;
			stateMask= stateMask | modifier;
		}
		return stateMask;
	}
		
	public void uninstall() {

		if (fColor != null) {
			fColor.dispose();
			fColor= null;
		}
			
		if (fCursor != null) {
			fCursor.dispose();
			fCursor= null;
		}
			
		ISourceViewer sourceViewer= getSourceViewer();
		if (sourceViewer == null)
			return;
				
		sourceViewer.removeTextInputListener(this);

		IDocument document= sourceViewer.getDocument();
		if (document != null)
			document.removeDocumentListener(this);
			
		StyledText text= sourceViewer.getTextWidget();
		if (text == null || text.isDisposed())
			return;
				
		text.removeKeyListener(this);
		text.removeMouseListener(this);
		text.removeMouseMoveListener(this);
		text.removeFocusListener(this);
		text.removePaintListener(this);
		}

	public void updateColor(ISourceViewer viewer) {
	
		StyledText text= viewer.getTextWidget();
		if (text == null || text.isDisposed())
			return;

		fColor = fColorCache.getNamedColor(PydevPrefs.HYPERLINK_COLOR);
	}

	/**
	 * Creates a color from the information stored in the given preference store.
	 * Returns <code>null</code> if there is no such information available.
	 */
	private Color createColor(IPreferenceStore store, String key, Display display) {
		
		RGB rgb= null;		
			
		if (store.contains(key)) {
				
			if (store.isDefault(key))
				rgb= PreferenceConverter.getDefaultColor(store, key);
			else
				rgb= PreferenceConverter.getColor(store, key);
			
			if (rgb != null)
				return new Color(display, rgb);
		}
			
		return null;
	}		
	
	private void repairRepresentation() {			
		repairRepresentation(false);
	}

	private void repairRepresentation(boolean redrawAll) {			

		if (fActiveRegion == null)
			return;
				
		ISourceViewer viewer= getSourceViewer();
		if (viewer != null) {
			resetCursor(viewer);

			int offset= fActiveRegion.getOffset();
			int length= fActiveRegion.getLength();

			// remove style
			if (!redrawAll && viewer instanceof ITextViewerExtension2)
				((ITextViewerExtension2) viewer).invalidateTextPresentation(offset, length);
			else
				viewer.invalidateTextPresentation();

			// remove underline				
			if (viewer instanceof ITextViewerExtension5) {
				ITextViewerExtension5 extension= (ITextViewerExtension5) viewer;
				offset= extension.modelOffset2WidgetOffset(offset);
			} else {
				offset -= viewer.getVisibleRegion().getOffset();
			}
				
			StyledText text= viewer.getTextWidget();
			try {
				text.redrawRange(offset, length, true);
			} catch (IllegalArgumentException x) {
				PydevPlugin.log(IStatus.ERROR, "Unexpected error in Hyperlink code", x);
			}
		}
			
		fActiveRegion= null;
	}

	// will eventually be replaced by a method provided by jdt.core		
	private IRegion selectWord(IDocument document, int anchor) {
		
		try {		
			int offset= anchor;
			char c;
	
			while (offset >= 0) {
				c= document.getChar(offset);
				if (!Character.isJavaIdentifierPart(c))
					break;
				--offset;
			}
	
			int start= offset;
	
			offset= anchor;
			int length= document.getLength();
	
			while (offset < length) {
				c= document.getChar(offset);
				if (!Character.isJavaIdentifierPart(c))
					break;
				++offset;
			}
				
			int end= offset;
				
			if (start == end)
				return new Region(start, 0);
			else
				return new Region(start + 1, end - start - 1);
				
		} catch (BadLocationException x) {
			return null;
		}
	}

	IRegion getCurrentTextRegion(ISourceViewer viewer) {

		int offset= getCurrentTextOffset(viewer);				
		if (offset == -1)
			return null;
		fClickedNode = ModelUtils.getElement(fEditor.getPythonModel(), 
									offset, viewer.getDocument(), AbstractNode.PROP_CLICKABLE);
		
		if (fClickedNode == null || 
			ModelUtils.findDefinition(fClickedNode).size() == 0)
			return null;

//		try {
//				
//			IJavaElement[] elements= null;
//			synchronized (input) {
//				elements= ((ICodeAssist) input).codeSelect(offset, 0);
//			}
//				
//			if (elements == null || elements.length == 0)
//				return null;
					
//			return selectWord(viewer.getDocument(), offset);
			return selectWord(viewer.getDocument(), offset);	
//		} catch (JavaModelException e) {
//			return null;	
//		}
	}

	private int getCurrentTextOffset(ISourceViewer viewer) {

		try {					
			StyledText text= viewer.getTextWidget();			
			if (text == null || text.isDisposed())
				return -1;

			Display display= text.getDisplay();				
			Point absolutePosition= display.getCursorLocation();
			Point relativePosition= text.toControl(absolutePosition);
				
			int widgetOffset= text.getOffsetAtLocation(relativePosition);
			if (viewer instanceof ITextViewerExtension5) {
				ITextViewerExtension5 extension= (ITextViewerExtension5) viewer;
				return extension.widgetOffset2ModelOffset(widgetOffset);
			} else {
				return widgetOffset + viewer.getVisibleRegion().getOffset();
			}

		} catch (IllegalArgumentException e) {
			return -1;
		}			
	}

	private void highlightRegion(ISourceViewer viewer, IRegion region) {

		if (region.equals(fActiveRegion))
			return;

		repairRepresentation();

		StyledText text= viewer.getTextWidget();
		if (text == null || text.isDisposed())
			return;

		// highlight region
		int offset= 0;
		int length= 0;
			
		if (viewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 extension= (ITextViewerExtension5) viewer;
			IRegion widgetRange= extension.modelRange2WidgetRange(region);
			if (widgetRange == null)
				return;
					
			offset= widgetRange.getOffset();
			length= widgetRange.getLength();
				
		} else {
			offset= region.getOffset() - viewer.getVisibleRegion().getOffset();
			length= region.getLength();
		}
			
		StyleRange oldStyleRange= text.getStyleRangeAtOffset(offset);
		Color foregroundColor= fColor;
		Color backgroundColor= oldStyleRange == null ? text.getBackground() : oldStyleRange.background;
		StyleRange styleRange= new StyleRange(offset, length, foregroundColor, backgroundColor);
		text.setStyleRange(styleRange);

		// underline
		text.redrawRange(offset, length, true);

		fActiveRegion= region;
	}

	private void activateCursor(ISourceViewer viewer) {
		StyledText text= viewer.getTextWidget();
		if (text == null || text.isDisposed())
			return;
		Display display= text.getDisplay();
		if (fCursor == null)
			fCursor= new Cursor(display, SWT.CURSOR_HAND);
		text.setCursor(fCursor);
	}
		
	private void resetCursor(ISourceViewer viewer) {
		StyledText text= viewer.getTextWidget();
		if (text != null && !text.isDisposed())
			text.setCursor(null);
						
		if (fCursor != null) {
			fCursor.dispose();
			fCursor= null;
		}
	}

	/*
	 * @see org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.KeyEvent)
	 */
	public void keyPressed(KeyEvent event) {

		if (fActive) {
			deactivate();
			return;	
		}

		if (event.keyCode != fKeyModifierMask) {
			deactivate();
			return;
		}
			
		fActive= true;

//		removed for #25871			
//
//		ISourceViewer viewer= getSourceViewer();
//		if (viewer == null)
//			return;
//			
//		IRegion region= getCurrentTextRegion(viewer);
//		if (region == null)
//			return;
//			
//		highlightRegion(viewer, region);
//		activateCursor(viewer);												
	}

	/*
	 * @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
	 */
	public void keyReleased(KeyEvent event) {
			
		if (!fActive)
			return;

		deactivate();				
	}

	/*
	 * @see org.eclipse.swt.events.MouseListener#mouseDoubleClick(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseDoubleClick(MouseEvent e) {}
	/*
	 * @see org.eclipse.swt.events.MouseListener#mouseDown(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseDown(MouseEvent event) {
			
		if (!fActive)
			return;
				
		if (event.stateMask != fKeyModifierMask) {
			deactivate();
			return;	
		}
			
		if (event.button != 1) {
			deactivate();
			return;	
		}			
	}

	/*
	 * @see org.eclipse.swt.events.MouseListener#mouseUp(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseUp(MouseEvent e) {

		if (!fActive)
			return;
				
		if (e.button != 1) {
			deactivate();
			return;
		}
			
		boolean wasActive= fCursor != null;
				
		deactivate();

		if (wasActive) {
			PyOpenAction action = (PyOpenAction)fEditor.getAction(PyEdit.ACTION_OPEN);
			ArrayList where = ModelUtils.findDefinition(fClickedNode);
			if (where.size() > 0)
				action.run((ItemPointer)where.get(0));
			else
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay().beep();
//			IAction action= getAction("OpenEditor");  //$NON-NLS-1$
//			if (action != null)
//				action.run();
		}
	}

	/*
	 * @see org.eclipse.swt.events.MouseMoveListener#mouseMove(org.eclipse.swt.events.MouseEvent)
	 */
	public void mouseMove(MouseEvent event) {
			
		if (event.widget instanceof Control && !((Control) event.widget).isFocusControl()) {
			deactivate();
			return;
		}
			
		if (!fActive) {
			if (event.stateMask != fKeyModifierMask)
				return;
			// modifier was already pressed
			fActive= true;
		}
	
		ISourceViewer viewer= getSourceViewer();
		if (viewer == null) {
			deactivate();
			return;
		}
				
		StyledText text= viewer.getTextWidget();
		if (text == null || text.isDisposed()) {
			deactivate();
			return;
		}
				
		if ((event.stateMask & SWT.BUTTON1) != 0 && text.getSelectionCount() != 0) {
			deactivate();
			return;
		}
		
		IRegion region= getCurrentTextRegion(viewer);
		if (region == null || region.getLength() == 0) {
			repairRepresentation();
			return;
		}
			
		highlightRegion(viewer, region);	
		activateCursor(viewer);												
	}

	/*
	 * @see org.eclipse.swt.events.FocusListener#focusGained(org.eclipse.swt.events.FocusEvent)
	 */
	public void focusGained(FocusEvent e) {}

	/*
	 * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
	 */
	public void focusLost(FocusEvent event) {
		deactivate();
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
	 */
	public void documentAboutToBeChanged(DocumentEvent event) {
		if (fActive && fActiveRegion != null) {
			fRememberedPosition= new Position(fActiveRegion.getOffset(), fActiveRegion.getLength());
			try {
				event.getDocument().addPosition(fRememberedPosition);
			} catch (BadLocationException x) {
				fRememberedPosition= null;
	}
		}
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
	 */
	public void documentChanged(DocumentEvent event) {
		if (fRememberedPosition != null && !fRememberedPosition.isDeleted()) {
			event.getDocument().removePosition(fRememberedPosition);
			fActiveRegion= new Region(fRememberedPosition.getOffset(), fRememberedPosition.getLength());
		}
		fRememberedPosition= null;

		ISourceViewer viewer= getSourceViewer();
		if (viewer != null) {
			StyledText widget= viewer.getTextWidget();
			if (widget != null && !widget.isDisposed()) {
				widget.getDisplay().asyncExec(new Runnable() {
					public void run() {
						deactivate();
					}
				});
			}
		}
	}

	/*
	 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentAboutToBeChanged(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
	 */
	public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
		if (oldInput == null)
			return;
		deactivate();
		oldInput.removeDocumentListener(this);
	}

	/*
	 * @see org.eclipse.jface.text.ITextInputListener#inputDocumentChanged(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.IDocument)
	 */
	public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
		if (newInput == null)
			return;
		newInput.addDocumentListener(this);
	}

	/*
	 * @see PaintListener#paintControl(PaintEvent)
	 */
	public void paintControl(PaintEvent event) {	
		if (fActiveRegion == null)
			return;
	
		ISourceViewer viewer= getSourceViewer();
		if (viewer == null)
			return;
				
		StyledText text= viewer.getTextWidget();
		if (text == null || text.isDisposed())
			return;
				
				
		int offset= 0;
		int length= 0;

		if (viewer instanceof ITextViewerExtension5) {
				
			ITextViewerExtension5 extension= (ITextViewerExtension5) viewer;
			IRegion widgetRange= extension.modelRange2WidgetRange(fActiveRegion);
			if (widgetRange == null)
				return;
					
			offset= widgetRange.getOffset();
			length= widgetRange.getLength();
				
		} else {
				
		IRegion region= viewer.getVisibleRegion();			
		if (!includes(region, fActiveRegion))
			return;		    

			offset= fActiveRegion.getOffset() - region.getOffset();
			length= fActiveRegion.getLength();
		}
			
		// support for bidi
		Point minLocation= getMinimumLocation(text, offset, length);
		Point maxLocation= getMaximumLocation(text, offset, length);
	
		int x1= minLocation.x;
		int x2= minLocation.x + maxLocation.x - minLocation.x - 1;
		int y= minLocation.y + text.getLineHeight() - 1;
			
		GC gc= event.gc;
		if (fColor != null && !fColor.isDisposed())
		gc.setForeground(fColor);
		gc.drawLine(x1, y, x2, y);
	}

	private boolean includes(IRegion region, IRegion position) {
		return
			position.getOffset() >= region.getOffset() &&
			position.getOffset() + position.getLength() <= region.getOffset() + region.getLength();
	}

	private Point getMinimumLocation(StyledText text, int offset, int length) {
		Point minLocation= new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
	
		for (int i= 0; i <= length; i++) {
			Point location= text.getLocationAtOffset(offset + i);
				
			if (location.x < minLocation.x)
				minLocation.x= location.x;			
			if (location.y < minLocation.y)
				minLocation.y= location.y;			
		}	
			
		return minLocation;
	}
	
	private Point getMaximumLocation(StyledText text, int offset, int length) {
		Point maxLocation= new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
	
		for (int i= 0; i <= length; i++) {
			Point location= text.getLocationAtOffset(offset + i);
				
			if (location.x > maxLocation.x)
				maxLocation.x= location.x;			
			if (location.y > maxLocation.y)
				maxLocation.y= location.y;			
		}	
			
		return maxLocation;
	}
}

class EditorUtility {
/**
 * Maps the localized modifier name to a code in the same
 * manner as #findModifier.
 * 
 * @return the SWT modifier bit, or <code>0</code> if no match was found
 *
 * @since 2.1.1
 */
public static int findLocalizedModifier(String token) {
	if (token == null)
		return 0;
		
	if (token.equalsIgnoreCase(Action.findModifierString(SWT.CTRL)))
		return SWT.CTRL;
	if (token.equalsIgnoreCase(Action.findModifierString(SWT.SHIFT)))
		return SWT.SHIFT;
	if (token.equalsIgnoreCase(Action.findModifierString(SWT.ALT)))
		return SWT.ALT;
	if (token.equalsIgnoreCase(Action.findModifierString(SWT.COMMAND)))
		return SWT.COMMAND;

	return 0;
}
}