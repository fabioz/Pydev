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
package edu.umd.cs.piccolo.nodes;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * <b>PText</b> is a multi-line text node. The text will flow to base
 * on the width of the node's bounds.
 * <P>
 * @version 1.1
 * @author Jesse Grosjean
 */
public class PText extends PNode {
	
	/** 
	 * The property name that identifies a change of this node's text (see
	 * {@link #getText getText}). Both old and new value will be set in any
	 * property change event. 
	 */
	public static final String PROPERTY_TEXT = "text";
    public static final int PROPERTY_CODE_TEXT = 1 << 19;
	
	/** 
	 * The property name that identifies a change of this node's font (see
	 * {@link #getFont getFont}).  Both old and new value will be set in any
	 * property change event.
	 */
	public static final String PROPERTY_FONT = "font";
    public static final int PROPERTY_CODE_FONT = 1 << 20;

	public static Font DEFAULT_FONT = new Font("Helvetica", Font.PLAIN, 12);
	public static double DEFAULT_GREEK_THRESHOLD = 5.5;
	
	private String text;
	private Paint textPaint;
	private Font font;
	protected double greekThreshold = DEFAULT_GREEK_THRESHOLD;
	private float justification = javax.swing.JLabel.LEFT_ALIGNMENT;
	private boolean constrainHeightToTextHeight = true;
	private boolean constrainWidthToTextWidth = true;
	private transient TextLayout[] lines;
    
	public PText() {
		super();
		setTextPaint(Color.BLACK);
	}

	public PText(String aText) {
		this();
		setText(aText);
	}
	
	/**
	 * Return the justificaiton of the text in the bounds.
	 * @return float
	 */
	public float getJustification() {
		return justification;
	}

	/**
     * Sets the justificaiton of the text in the bounds.
	 * @param just
	 */	
	public void setJustification(float just) {
		justification = just;
		recomputeLayout();
	}

	/**
	 * Get the paint used to paint this nodes text.
	 * @return Paint
	 */
	public Paint getTextPaint() {
		return textPaint;
	}

	/**
	 * Set the paint used to paint this node's text background.
	 * @param textPaint
	 */		
	public void setTextPaint(Paint textPaint) {
		this.textPaint = textPaint;
		invalidatePaint();
	}

    public boolean isConstrainWidthToTextWidth() {
        return constrainWidthToTextWidth;
    }

	/**
	 * Controls whether this node changes its width to fit the width 
	 * of its text. If flag is true it does; if flag is false it doesn't
	 */
	public void setConstrainWidthToTextWidth(boolean constrainWidthToTextWidth) {
		this.constrainWidthToTextWidth = constrainWidthToTextWidth;
		recomputeLayout();
	}

    public boolean isConstrainHeightToTextHeight() {
        return constrainHeightToTextHeight;
    }

	/**
	 * Controls whether this node changes its height to fit the height 
	 * of its text. If flag is true it does; if flag is false it doesn't
	 */
	public void setConstrainHeightToTextHeight(boolean constrainHeightToTextHeight) {
		this.constrainHeightToTextHeight = constrainHeightToTextHeight;
		recomputeLayout();
	}

	/**
	 * Returns the current greek threshold. When the screen font size will be below
	 * this threshold the text is rendered as 'greek' instead of drawing the text
	 * glyphs.
	 */
	public double getGreekThreshold() {
		return greekThreshold;
	}

	/**
	 * Sets the current greek threshold. When the screen font size will be below
	 * this threshold the text is rendered as 'greek' instead of drawing the text
	 * glyphs.
	 * 
	 * @param threshold minimum screen font size.
	 */
	public void setGreekThreshold(double threshold) {
		greekThreshold = threshold;
		invalidatePaint();
	}
		
	public String getText() {
		return text;
	}

	/**
	 * Set the text for this node. The text will be broken up into multiple
	 * lines based on the size of the text and the bounds width of this node.
	 */
	public void setText(String aText) {
		String old = text;
		text = aText;
		lines = null;
		recomputeLayout();
		invalidatePaint();
		firePropertyChange(PROPERTY_CODE_TEXT, PROPERTY_TEXT, old, text);
	}
	
	/**
	 * Returns the font of this PText.
	 * @return the font of this PText.
	 */ 
	public Font getFont() {
		if (font == null) {
			font = DEFAULT_FONT;
		}
		return font;
	}
	
	/**
	 * Set the font of this PText. Note that in Piccolo if you want to change
	 * the size of a text object it's often a better idea to scale the PText
	 * node instead of changing the font size to get that same effect. Using
	 * very large font sizes can slow performance.
	 */
	public void setFont(Font aFont) {
		Font old = font;
		font = aFont;
		lines = null;
		recomputeLayout();
		invalidatePaint();
		firePropertyChange(PROPERTY_CODE_FONT, PROPERTY_FONT, old, font);
	}

	private static final TextLayout[] EMPTY_TEXT_LAYOUT_ARRAY = new TextLayout[0];
		
	/**
	 * Compute the bounds of the text wrapped by this node. The text layout
	 * is wrapped based on the bounds of this node.
	 */
	public void recomputeLayout() {
		ArrayList linesList = new ArrayList();
		double textWidth = 0;
		double textHeight = 0;

		if (text != null && text.length() > 0) {		
			AttributedString atString = new AttributedString(text);
			atString.addAttribute(TextAttribute.FONT, getFont());
			AttributedCharacterIterator itr = atString.getIterator();
			LineBreakMeasurer measurer = new LineBreakMeasurer(itr, PPaintContext.RENDER_QUALITY_HIGH_FRC);
			float availableWidth = constrainWidthToTextWidth ? Float.MAX_VALUE : (float) getWidth();
			
			int nextLineBreakOffset = text.indexOf('\n');
			if (nextLineBreakOffset == -1) {
				nextLineBreakOffset = Integer.MAX_VALUE;
			} else {
				nextLineBreakOffset++;
			}
			
			while (measurer.getPosition() < itr.getEndIndex()) {
				TextLayout aTextLayout = computeNextLayout(measurer, availableWidth, nextLineBreakOffset);

				if (nextLineBreakOffset == measurer.getPosition()) {
					nextLineBreakOffset = text.indexOf('\n', measurer.getPosition());
					if (nextLineBreakOffset == -1) {
						nextLineBreakOffset = Integer.MAX_VALUE;
					} else {
						nextLineBreakOffset++;
					}
				}
								
				linesList.add(aTextLayout);
				textHeight += aTextLayout.getAscent();
				textHeight += aTextLayout.getDescent() + aTextLayout.getLeading();
				textWidth = Math.max(textWidth, aTextLayout.getAdvance());
			}
		}
				
		lines = (TextLayout[]) linesList.toArray(EMPTY_TEXT_LAYOUT_ARRAY);
					
		if (constrainWidthToTextWidth || constrainHeightToTextHeight) {
			double newWidth = getWidth();
			double newHeight = getHeight();
			
			if (constrainWidthToTextWidth) {
				newWidth = textWidth;
			}
			
			if (constrainHeightToTextHeight) {
				newHeight = textHeight;
			}
	
			super.setBounds(getX(), getY(), newWidth, newHeight);
		}	
	}
	
	// provided in case someone needs to override the way that lines are wrapped.
	protected TextLayout computeNextLayout(LineBreakMeasurer measurer, float availibleWidth, int nextLineBreakOffset) {
		return measurer.nextLayout(availibleWidth, nextLineBreakOffset, false);
	}
		
	protected void paint(PPaintContext paintContext) {		
		super.paint(paintContext);
		
		float screenFontSize = getFont().getSize() * (float) paintContext.getScale();
		if (textPaint != null && screenFontSize > greekThreshold) {
			float x = (float) getX();
			float y = (float) getY();
			float bottomY = (float) getHeight() + y;
			
			Graphics2D g2 = paintContext.getGraphics();
			
			if (lines == null) {
				recomputeLayout();
				repaint();
				return;
			}

			g2.setPaint(textPaint);
			
			for (int i = 0; i < lines.length; i++) {
                TextLayout tl = lines[i];
				y += tl.getAscent();
				
				if (bottomY < y) {
					return;
				}
                
                float offset = (float) (getWidth() - tl.getAdvance()) * justification;
                tl.draw(g2, x + offset, y);
	
				y += tl.getDescent() + tl.getLeading();
			}
		}
	}
	
	protected void internalUpdateBounds(double x, double y, double width, double height) {
		recomputeLayout();
	}

	//****************************************************************
	// Debugging - methods for debugging
	//****************************************************************
	
	/**
	 * Returns a string representing the state of this node. This method is
	 * intended to be used only for debugging purposes, and the content and
	 * format of the returned string may vary between implementations. The
	 * returned string may be empty but may not be <code>null</code>.
	 *
	 * @return  a string representation of this node's state
	 */
	protected String paramString() {
		StringBuffer result = new StringBuffer();

		result.append("text=" + (text == null ? "null" : text));
		result.append(",font=" + (font == null ? "null" : font.toString()));
		result.append(',');
		result.append(super.paramString());

		return result.toString();
	}
}
