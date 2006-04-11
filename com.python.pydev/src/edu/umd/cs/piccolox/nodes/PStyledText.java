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
package edu.umd.cs.piccolox.nodes;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * @author Lance Good
 */
public class PStyledText extends PNode {

	protected static FontRenderContext SWING_FRC = new FontRenderContext(null,true,false);
	protected static Line2D paintLine = new Line2D.Double();

	protected Document document;
	protected transient ArrayList stringContents;
	protected transient LineInfo[] lines;

	protected boolean editing;
	protected Insets insets = new Insets(0,0,0,0);
	protected boolean constrainHeightToTextHeight = true;
	protected boolean constrainWidthToTextWidth = true;

	/**
	 * Constructor for PStyledText.
	 */
	public PStyledText() {
		super();
	}

	/**
	 * Controls whether this node changes its width to fit the width 
	 * of its text. If flag is true it does; if flag is false it doesn't
	 */
	public void setConstrainWidthToTextWidth(boolean constrainWidthToTextWidth) {
		this.constrainWidthToTextWidth = constrainWidthToTextWidth;
		recomputeLayout();
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
	 * Controls whether this node changes its width to fit the width 
	 * of its text. If flag is true it does; if flag is false it doesn't
	 */
	public boolean getConstrainWidthToTextWidth() {
		return constrainWidthToTextWidth;
	}

	/**
	 * Controls whether this node changes its height to fit the height 
	 * of its text. If flag is true it does; if flag is false it doesn't
	 */
	public boolean getConstrainHeightToTextHeight() {
		return constrainHeightToTextHeight;
	}

	/**
	 * Get the document for this PStyledText
	 */
	public Document getDocument() {
		return document;	
	}
		
	/**
	 * Set the document on this PStyledText
	 */
	public void setDocument(Document document) {
		// Save the document
		this.document = document;
		
		syncWithDocument();
	}
	
	
	public void syncWithDocument() {
		// The paragraph start and end indices
		ArrayList pEnds = null;
		
		// The current position in the specified range
		int pos = 0;
	
		// First get the actual text and stick it in an Attributed String
		try {

			stringContents = new ArrayList();
			pEnds = new ArrayList();
			
			String s = document.getText(0,document.getLength());
			StringTokenizer tokenizer = new StringTokenizer(s,"\n",true);
	
			// lastNewLine is used to detect the case when two newlines follow in direct succession
			// & lastNewLine should be true to start in case the first character is a newline
			boolean lastNewLine = true;
			for(int i=0; tokenizer.hasMoreTokens(); i++) {
				String token = tokenizer.nextToken();
				
				// If the token 
				if (token.equals("\n")) {
					if (lastNewLine) {
						stringContents.add(new AttributedString(" "));
						pEnds.add(new RunInfo(pos,pos+1));
						
						pos = pos + 1;

						lastNewLine = true;
					}
					else {
						pos = pos + 1;
						
						lastNewLine = true;
					}
				}
				// If the token is empty - create an attributed string with a single space
				// since LineBreakMeasurers don't work with an empty string
				// - note that this case should only arise if the document is empty
				else if (token.equals("")) {
					stringContents.add(new AttributedString(" "));
					pEnds.add(new RunInfo(pos,pos));

					lastNewLine = false;					
				}
				// This is the normal case - where we have some text
				else {
					stringContents.add(new AttributedString(token));
					pEnds.add(new RunInfo(pos,pos+token.length()));

					// Increment the position
					pos = pos+token.length();
					
					lastNewLine = false;
				}
			}
			
			// Add one more newline if the last character was a newline
			if (lastNewLine) {
				stringContents.add(new AttributedString(" "));
				pEnds.add(new RunInfo(pos,pos+1));
				
				lastNewLine = false;
			}
		}
		catch (Exception e) {
			e.printStackTrace();	
		}

		// The default style context - which will be reused
		StyleContext style = StyleContext.getDefaultStyleContext();

		RunInfo pEnd = null;
		for (int i = 0; i < stringContents.size(); i++) {
			pEnd = (RunInfo)pEnds.get(i);
			pos = pEnd.runStart;

			// The current element will be used as a temp variable while searching
			// for the leaf element at the current position
			Element curElement = null;

			// Small assumption here that there is one root element - can fix
			// for more general support later
			Element rootElement = document.getDefaultRootElement();

			// If the string is length 0 then we just need to add the attributes once
			if (pEnd.runStart != pEnd.runLimit) {
				// OK, now we loop until we find all the leaf elements in the range
				while (pos < pEnd.runLimit) {
					
					// Before each pass, start at the root
					curElement = rootElement;
	
					// Now we descend the hierarchy until we get to a leaf
					while (!curElement.isLeaf()) {
						curElement =
							curElement.getElement(curElement.getElementIndex(pos));
					}

					// These are the mandatory attributes

					AttributeSet attributes = curElement.getAttributes();
					Color foreground = style.getForeground(attributes);

					((AttributedString)stringContents.get(i)).addAttribute(
						TextAttribute.FOREGROUND,
						foreground,
						(int)Math.max(0,curElement.getStartOffset()-pEnd.runStart),
						(int)Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));
					
					Font font = (attributes.isDefined(StyleConstants.FontSize) || attributes.isDefined(StyleConstants.FontFamily)) ? style.getFont(attributes) : null;
					if (font == null) {
					    if (document instanceof DefaultStyledDocument) {
					        font = style.getFont(((DefaultStyledDocument)document).getCharacterElement(pos).getAttributes());
					        if (font == null) {
					            font = style.getFont(((DefaultStyledDocument)document).getParagraphElement(pos).getAttributes());
					        }
					        if (font == null) {
					            font = style.getFont(rootElement.getAttributes());
					        }
					    }
					    else {
					        font = style.getFont(rootElement.getAttributes());
					    }
					}					
					if (font != null) {
						((AttributedString)stringContents.get(i)).addAttribute(
								TextAttribute.FONT,
								font,
								(int)Math.max(0,curElement.getStartOffset()-pEnd.runStart),
								(int)Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));				    
					}
					
					// These are the optional attributes
					
					Color background = (attributes.isDefined(StyleConstants.Background)) ? style.getBackground(attributes) : null;
					if (background != null) {
						((AttributedString)stringContents.get(i)).addAttribute(
								TextAttribute.BACKGROUND,
								background,
								(int)Math.max(0,curElement.getStartOffset()-pEnd.runStart),
								(int)Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));					    
					}
					
					boolean underline = StyleConstants.isUnderline(attributes);
					if (underline) {
						((AttributedString)stringContents.get(i)).addAttribute(
							TextAttribute.UNDERLINE,
							Boolean.TRUE,
							(int)Math.max(0,curElement.getStartOffset()-pEnd.runStart),
							(int)Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));						
					}
					
					boolean strikethrough = StyleConstants.isStrikeThrough(attributes);
					if (strikethrough) {
						((AttributedString)stringContents.get(i)).addAttribute(
							TextAttribute.STRIKETHROUGH,
							Boolean.TRUE,
							(int)Math.max(0,curElement.getStartOffset()-pEnd.runStart),
							(int)Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));						
					}
	
					// And set the position to the end of the given attribute
					pos = curElement.getEndOffset();
				}
			}
			else {
				// Before each pass, start at the root
				curElement = rootElement;

				// Now we descend the hierarchy until we get to a leaf
				while (!curElement.isLeaf()) {
					curElement =
						curElement.getElement(curElement.getElementIndex(pos));
				}

				// These are the mandatory attributes
				
				AttributeSet attributes = curElement.getAttributes();
				Color foreground = style.getForeground(attributes);

				((AttributedString)stringContents.get(i)).addAttribute(
					TextAttribute.FOREGROUND,
					foreground,
					(int)Math.max(0,curElement.getStartOffset()-pEnd.runStart),
					(int)Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));			

				// These are the optional attributes

				Font font = (attributes.isDefined(StyleConstants.FontSize) || attributes.isDefined(StyleConstants.FontFamily)) ? style.getFont(attributes) : null;
				if (font == null) {
				    if (document instanceof DefaultStyledDocument) {
				        font = style.getFont(((DefaultStyledDocument)document).getCharacterElement(pos).getAttributes());
				        if (font == null) {
				            font = style.getFont(((DefaultStyledDocument)document).getParagraphElement(pos).getAttributes());
				        }
				        if (font == null) {
				            font = style.getFont(rootElement.getAttributes());
				        }
				    }
				    else {
				        font = style.getFont(rootElement.getAttributes());
				    }
				}					
				if (font != null) {
					((AttributedString)stringContents.get(i)).addAttribute(
							TextAttribute.FONT,
							font,
							(int)Math.max(0,curElement.getStartOffset()-pEnd.runStart),
							(int)Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));				    
				}				
				
				Color background = (attributes.isDefined(StyleConstants.Background)) ? style.getBackground(attributes) : null;
				if (background != null) {
					((AttributedString)stringContents.get(i)).addAttribute(
							TextAttribute.BACKGROUND,
							background,
							(int)Math.max(0,curElement.getStartOffset()-pEnd.runStart),
							(int)Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));					    
				}
				
				boolean underline = StyleConstants.isUnderline(attributes);
				if (underline) {
					((AttributedString)stringContents.get(i)).addAttribute(
						TextAttribute.UNDERLINE,
						Boolean.TRUE,
						(int)Math.max(0,curElement.getStartOffset()-pEnd.runStart),
						(int)Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));						
				}
					
				boolean strikethrough = StyleConstants.isStrikeThrough(attributes);
				if (strikethrough) {
					((AttributedString)stringContents.get(i)).addAttribute(
						TextAttribute.STRIKETHROUGH,
						Boolean.TRUE,
						(int)Math.max(0,curElement.getStartOffset()-pEnd.runStart),
						(int)Math.min(pEnd.runLimit-pEnd.runStart,curElement.getEndOffset()-pEnd.runStart));						
				}
			}
		}

		recomputeLayout();
	}

	/**
	 * Compute the bounds of the text wrapped by this node. The text layout
	 * is wrapped based on the bounds of this node. If the shrinkBoundsToFit parameter
	 * is true then after the text has been laid out the bounds of this node are shrunk
	 * to fit around those text bounds.
	 */
	public void recomputeLayout() {
		if (stringContents == null) return;

		ArrayList linesList = new ArrayList();

		double textWidth = 0;
		double textHeight = 0;
		
		for(int i=0; i<stringContents.size(); i++) {		

			AttributedString ats = (AttributedString)stringContents.get(i);
			AttributedCharacterIterator itr = ats.getIterator();
						
			LineBreakMeasurer measurer;
			ArrayList breakList = null;
			
			// First we have to do an initial pass with a LineBreakMeasurer to
			// find out where Swing is going to break the lines - i.e.
			// because it doesn't use fractional metrics

			measurer = new LineBreakMeasurer(itr, SWING_FRC);
			breakList = new ArrayList();
			while(measurer.getPosition() < itr.getEndIndex()) {
				if (constrainWidthToTextWidth) {
					measurer.nextLayout(Float.MAX_VALUE);
				} 
				else {
					measurer.nextLayout((float)Math.ceil(getWidth()-insets.left-insets.right));
				}

				breakList.add(new Integer(measurer.getPosition()));
			}

			measurer = new LineBreakMeasurer(itr, PPaintContext.RENDER_QUALITY_HIGH_FRC);

			// Need to change the lineinfo data structure to know about multiple
			// text layouts per line
			
			LineInfo lineInfo = null;
			boolean newLine = true;
			double lineWidth = 0;
			while (measurer.getPosition() < itr.getEndIndex()) {
				TextLayout aTextLayout = null;
				
				if (newLine) {
				    newLine = false;
				    
				    // Add in the old line dimensions
				    double lineHeight = (lineInfo == null) ? 0 : lineInfo.maxAscent+lineInfo.maxDescent+lineInfo.leading;
				    textHeight = textHeight+lineHeight;
				    textWidth = Math.max(textWidth,lineWidth);
				    
				    // Now create a new line
				    lineInfo = new LineInfo();
				    linesList.add(lineInfo);				    
				}
				
			    int lineEnd = ((Integer)breakList.get(0)).intValue();			    
			    if (lineEnd <= itr.getRunLimit()) {
			        breakList.remove(0);
			        newLine = true;
			    }
			    
				aTextLayout = measurer.nextLayout(Float.MAX_VALUE,Math.min(lineEnd,itr.getRunLimit()),false);
				
				SegmentInfo sInfo = new SegmentInfo();
				sInfo.font = (Font)itr.getAttribute(TextAttribute.FONT);
				sInfo.foreground = (Color)itr.getAttribute(TextAttribute.FOREGROUND);
				sInfo.background = (Color)itr.getAttribute(TextAttribute.BACKGROUND);
				sInfo.underline = (Boolean)itr.getAttribute(TextAttribute.UNDERLINE);
				sInfo.layout = aTextLayout;
								
				FontMetrics metrics = StyleContext.getDefaultStyleContext().getFontMetrics((Font)itr.getAttribute(TextAttribute.FONT));
				lineInfo.maxAscent = Math.max(lineInfo.maxAscent,metrics.getMaxAscent());
				lineInfo.maxDescent = Math.max(lineInfo.maxDescent,metrics.getMaxDescent());
				lineInfo.leading = Math.max(lineInfo.leading,metrics.getLeading());
				
				lineInfo.segments.add(sInfo);
				
				itr.setIndex(measurer.getPosition());
				lineWidth = lineWidth+aTextLayout.getAdvance();
			}
			
		    double lineHeight = (lineInfo == null) ? 0 : lineInfo.maxAscent+lineInfo.maxDescent+lineInfo.leading;
		    textHeight = textHeight+lineHeight;
		    textWidth = Math.max(textWidth,lineWidth);
		}
		
		lines = (LineInfo[])linesList.toArray(new LineInfo[0]);
		
		if (constrainWidthToTextWidth || constrainHeightToTextHeight) {
			double newWidth = getWidth();
			double newHeight = getHeight();
			
			if (constrainWidthToTextWidth) {
				newWidth = textWidth + insets.left + insets.right;
			}
			
			if (constrainHeightToTextHeight) {
				newHeight = Math.max(textHeight,getInitialFontHeight()) + insets.top + insets.bottom;
			}
	
			super.setBounds(getX(), getY(), newWidth, newHeight);
		}	
	}

	/**
	 * Get the height of the font at the beginning of the document
	 */
	public double getInitialFontHeight() {

		// Small assumption here that there is one root element - can fix
		// for more general support later
		Element rootElement = document.getDefaultRootElement();

		// The current element will be used as a temp variable while searching
		// for the leaf element at the current position
		Element curElement = rootElement;

		// Now we descend the hierarchy until we get to a leaf
		while (!curElement.isLeaf()) {
			curElement = curElement.getElement(curElement.getElementIndex(0));
		}

		StyleContext context = StyleContext.getDefaultStyleContext();
		Font font = context.getFont(curElement.getAttributes());

		FontMetrics curFM = context.getFontMetrics(font);

		return curFM.getMaxAscent() + curFM.getMaxDescent() + curFM.getLeading();
	}

	protected void paint(PPaintContext paintContext) {
		float x = (float) (getX() + insets.left);
		float y = (float) (getY() + insets.top);
		float bottomY = (float) (getY() + getHeight() - insets.bottom);
		
		if (lines == null || lines.length == 0) {
			return;
		}
		
		Graphics2D g2 = paintContext.getGraphics();
		
		if (getPaint() != null) {
			g2.setPaint(getPaint());
			g2.fill(getBoundsReference());
		}
		
		float curX;
		for (int i=0; i<lines.length; i++) {
			y += lines[i].maxAscent;
			curX = x;

			if (bottomY < y) {
				return;
			}					

			for(int j=0; j<lines[i].segments.size(); j++) {			    
			    SegmentInfo sInfo = (SegmentInfo)lines[i].segments.get(j);
				float width = sInfo.layout.getAdvance();

				if (sInfo.background != null) {
					g2.setPaint(sInfo.background);
					g2.fill(new Rectangle2D.Double(curX,y-lines[i].maxAscent,width,lines[i].maxAscent+lines[i].maxDescent+lines[i].leading));					
				}
				
				if (sInfo.font != null) {
				    g2.setFont(sInfo.font);
				}
				
				// Manually set the paint - this is specified in the AttributedString but seems to be
				// ignored by the TextLayout.  To handle multiple colors we should be breaking up the lines
				// but that functionality can be added later as needed
				g2.setPaint(sInfo.foreground);
				sInfo.layout.draw(g2, curX, y);
	
				// Draw the underline and the strikethrough after the text
				if (sInfo.underline != null) {
					paintLine.setLine(x,y+lines[i].maxDescent/2,x+width,y+lines[i].maxDescent/2);
					g2.draw(paintLine);	
				}
				
				curX = curX + width;
		    }
			
			y += lines[i].maxDescent + lines[i].leading;			
		}
	}	

	public void fullPaint(PPaintContext paintContext) {
		if (!editing) {
			super.fullPaint(paintContext);	
		}	
	}

	/**
	 * Set whether this text is editing
	 */
	public void setEditing(boolean editing) {
		this.editing = editing; 
	}

	/**
	 * Is this document editing
	 */
	public boolean isEditing() {
		return editing; 
	}

	/**
	 * Set the insets of the text
	 */
	public void setInsets(Insets insets) {
		if (insets != null) {
			this.insets.left = insets.left;
			this.insets.right = insets.right;
			this.insets.top = insets.top;
			this.insets.bottom = insets.bottom;

			recomputeLayout();	
		}
	}

	/**
	 * Get the insets of the text
	 */
	public Insets getInsets() {
		return (Insets)insets.clone();	
	}

	/**
	 * Add a call to recompute the layout after each bounds change
	 */
	public boolean setBounds(double x, double y, double w, double h) {
		if (document == null || !super.setBounds(x,y,w,h)) {
			return false;	
		}
		
		recomputeLayout();
		return true;
	}

	/**
	 * Simple class to represent an integer run 
	 */ 
	protected static class RunInfo {
		public int runStart;
		public int runLimit;
		
		public RunInfo() {
		}
		
		public RunInfo(int runStart, int runLimit) {
			this.runStart = runStart;
			this.runLimit = runLimit;	
		}		
	}
	
	/**
	 * Class to represent an integer run and the font in that run
	 */
	protected static class MetricsRunInfo extends RunInfo {
		public FontMetrics metrics; 			
		
		public MetricsRunInfo() {
		    super();
		}
	}
	
	/**
	 * The info for rendering a and computing the bounds of a line
	 */
	protected static class LineInfo {
	    public List segments;
		public double maxAscent;
		public double	maxDescent;
		public double leading;
		
		public LineInfo() {
		    segments = new ArrayList();
		}
	}
	
	protected static class SegmentInfo {
		public TextLayout layout;
		public Font font;
		public Color foreground;
		public Color background;
		public Boolean underline;
		
		public SegmentInfo() {		    
		}
	}
}