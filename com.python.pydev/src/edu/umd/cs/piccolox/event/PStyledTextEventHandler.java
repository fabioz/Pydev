/*
 * Copyright (c) 2002-@year@, University of Maryland
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of the University of Maryland nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Piccolo was written at the Human-Computer Interaction Laboratory www.cs.umd.edu/hcil by Jesse Grosjean
 * under the supervision of Ben Bederson. The Piccolo website is www.cs.umd.edu/hcil/piccolo.
 */
 package edu.umd.cs.piccolox.event;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolox.nodes.PStyledText;

/**
 * @author Lance Good
 */
public class PStyledTextEventHandler extends PBasicInputEventHandler {

	protected PCanvas canvas;

	protected JTextComponent editor;

	protected DocumentListener docListener;

	protected PStyledText editedText;

	/**
	 * Basic constructor for PStyledTextEventHandler
	 */
	public PStyledTextEventHandler(PCanvas canvas) {
		super();

		this.canvas = canvas;
		initEditor(createDefaultEditor());
	}

	/**
	 * Constructor for PStyledTextEventHandler that allows an editor to be specified
	 */
	public PStyledTextEventHandler(PCanvas canvas, JTextComponent editor) {
		super();

		this.canvas = canvas;
		initEditor(editor);
	}

	protected void initEditor(JTextComponent newEditor) {
		editor = newEditor;
		
		canvas.setLayout(null);
		canvas.add(editor);
		editor.setVisible(false);
		
		docListener = createDocumentListener();
	}

	protected JTextComponent createDefaultEditor() {
		JTextPane tComp = new JTextPane() {

			/**
			 * Set some rendering hints - if we don't then the rendering can be inconsistent.  Also,
			 * Swing doesn't work correctly with fractional metrics.
			 */
			public void paint(Graphics g) {
				Graphics2D g2 = (Graphics2D)g;
				
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);				
				
				super.paint(g);
			}
			
			/**
			 * If the standard scroll rect to visible is on, then you can get weird behaviors if the
			 * canvas is put in a scrollpane.
			 */
			public void scrollRectToVisible() {
			}
		};	
		tComp.setBorder(new CompoundBorder(new LineBorder(Color.black),new EmptyBorder(3,3,3,3)));
		return tComp;
	}

	protected DocumentListener createDocumentListener() {
		return new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
				reshapeEditorLater();
			}	
			
			public void insertUpdate(DocumentEvent e) {
				reshapeEditorLater();
			}
			
			public void changedUpdate(DocumentEvent e) {
				reshapeEditorLater();
			}			
		};		
	}

	public PStyledText createText() {
		PStyledText newText = new PStyledText();
		
		Document doc = editor.getUI().getEditorKit(editor).createDefaultDocument();
		if (doc instanceof StyledDocument) {
			if (!doc.getDefaultRootElement().getAttributes().isDefined(StyleConstants.FontFamily)
				|| !doc.getDefaultRootElement().getAttributes().isDefined(StyleConstants.FontSize)) {

				Font eFont = editor.getFont();
				SimpleAttributeSet sas = new SimpleAttributeSet();
				sas.addAttribute(StyleConstants.FontFamily, eFont.getFamily());
				sas.addAttribute(StyleConstants.FontSize, new Integer(eFont.getSize()));

				((StyledDocument) doc).setParagraphAttributes(0, doc.getLength(), sas, false);
			}
		}
		newText.setDocument(doc);
		
		return newText;
	}

	public void mousePressed(PInputEvent inputEvent) {
		PNode pickedNode = inputEvent.getPickedNode();
				
		stopEditing();
		
		if (pickedNode instanceof PStyledText) {
			startEditing(inputEvent,(PStyledText)pickedNode);
		}
		else if (pickedNode instanceof PCamera) {
			PStyledText newText = createText();
			Insets pInsets = newText.getInsets();
			canvas.getLayer().addChild(newText);
			newText.translate(inputEvent.getPosition().getX()-pInsets.left,inputEvent.getPosition().getY()-pInsets.top);
			startEditing(inputEvent, newText);
		}
	}	
	
	public void startEditing(PInputEvent event, PStyledText text) {
		// Get the node's top right hand corner
		Insets pInsets = text.getInsets();
		Point2D nodePt = new Point2D.Double(text.getX()+pInsets.left,text.getY()+pInsets.top);
		text.localToGlobal(nodePt);
		event.getTopCamera().viewToLocal(nodePt);

		// Update the editor to edit the specified node
		editor.setDocument(text.getDocument());
		editor.setVisible(true);

		Insets bInsets = editor.getBorder().getBorderInsets(editor);
		editor.setLocation((int)nodePt.getX()-bInsets.left,(int)nodePt.getY()-bInsets.top);
		reshapeEditorLater();

		dispatchEventToEditor(event);
		canvas.repaint();

		text.setEditing(true);
		text.getDocument().addDocumentListener(docListener);
		editedText = text;					
	}
	
	public void stopEditing() {
		if (editedText != null) {
			editedText.getDocument().removeDocumentListener(docListener);
			editedText.setEditing(false);

			if (editedText.getDocument().getLength() == 0) {
				editedText.removeFromParent();		
			}
			else {
				editedText.syncWithDocument();
			}

			editor.setVisible(false);
			canvas.repaint();
		
			editedText = null;
		}
	}

	public void dispatchEventToEditor(final PInputEvent e) {
		// We have to nest the mouse press in two invoke laters so that it is 
		// fired so that the component has been completely validated at the new size
		// and the mouse event has the correct offset
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() { 	
						MouseEvent me =
							new MouseEvent(
								editor,
								MouseEvent.MOUSE_PRESSED,
								e.getWhen(),
								e.getModifiers() | InputEvent.BUTTON1_MASK,
								(int) (e.getCanvasPosition().getX() - editor.getX()),
								(int) (e.getCanvasPosition().getY() - editor.getY()),
								1,
								false);
						editor.dispatchEvent(me);
					}
				});
			}
		});
	}

	
	public void reshapeEditor() {
		if (editedText != null) {
			// Update the size to fit the new document - note that it is a 2 stage process
			Dimension prefSize = editor.getPreferredSize();
	
			Insets pInsets = editedText.getInsets();
			Insets jInsets = editor.getInsets();
			
			int width = (editedText.getConstrainWidthToTextWidth()) ? (int)prefSize.getWidth() : (int)(editedText.getWidth()-pInsets.left-pInsets.right+jInsets.left+jInsets.right+3.0);
			prefSize.setSize(width,prefSize.getHeight());
			editor.setSize(prefSize);

			prefSize = editor.getPreferredSize();
			int height = (editedText.getConstrainHeightToTextHeight()) ? (int)prefSize.getHeight() : (int)(editedText.getHeight()-pInsets.top-pInsets.bottom+jInsets.top+jInsets.bottom+3.0);			
			prefSize.setSize(width,height); 		
			editor.setSize(prefSize);
		}
	}

	/**
	 * Sometimes we need to invoke this later because the document events seem to get fired
	 * before the text is actually incorporated into the document
	 */
	protected void reshapeEditorLater() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				reshapeEditor();
			}
		}); 			
	}
	
}
