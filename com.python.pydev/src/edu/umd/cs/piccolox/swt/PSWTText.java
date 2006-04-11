/**
 * Copyright (C) 1998-1999 by University of Maryland, College Park, MD 20742, USA
 * All rights reserved.
 */
package edu.umd.cs.piccolox.swt;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.*;
import java.util.*;

import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * <b>PSWTText</b> creates a visual component to support text. Multiple lines can
 * be entered, and basic editing is supported. A caret is drawn,
 * and can be repositioned with mouse clicks.  The text object is positioned
 * so that its upper-left corner is at the origin, though this can be changed
 * with the translate methods.
 * <P>
 * <b>Warning:</b> Serialized and ZSerialized objects of this class will not be
 * compatible with future Jazz releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running the
 * same version of Jazz. A future release of Jazz will provide support for long
 * term persistence.
 */
public class PSWTText extends PNode {

    /**
     * Below this magnification render text as 'greek'.
     */
    static protected final double   DEFAULT_GREEK_THRESHOLD = 5.5;

    /**
     * Default color of text rendered as 'greek'.
     */
    static protected final Color   DEFAULT_GREEK_COLOR = Color.gray;

    /**
     * Default font name of text.
     */
    static protected final String  DEFAULT_FONT_NAME = "Helvetica";

    /**
     * Default font style for text.
     */
    static protected final int     DEFAULT_FONT_STYLE = Font.PLAIN;

    /**
     * Default font size for text.
     */
    static protected final int     DEFAULT_FONT_SIZE = 12;

    /**
     * Default font for text.
     */
    static protected final Font    DEFAULT_FONT = new Font(DEFAULT_FONT_NAME, DEFAULT_FONT_STYLE, DEFAULT_FONT_SIZE);

    /**
     * Default color for text.
     */
    static protected final Color   DEFAULT_PEN_COLOR = Color.black;

    /**
     * Default text when new text area is created.
     */
    static protected final String  DEFAULT_TEXT = "";

    /**
     * Default padding
     */
    static protected final int    DEFAULT_PADDING = 2;
    
    /**
     * Below this magnification text is rendered as greek.
     */
    protected double             greekThreshold = DEFAULT_GREEK_THRESHOLD;

    /**
     * Color for greek text.
     */
    protected Color             greekColor = DEFAULT_GREEK_COLOR;

    /**
     * Current pen color.
     */
    protected Color             penColor  = DEFAULT_PEN_COLOR;

    /**
     * Current text font.
     */
    protected Font               font = DEFAULT_FONT;

    /**
     * The amount of padding on each side of the text
     */
    protected int                padding = DEFAULT_PADDING;

    
    /**
     * Each vector element is one line of text.
     */
    protected ArrayList            lines = new ArrayList();

    
    /**
     * Translation offset X.
     */
    protected double translateX = 0.0;

    /**
     * Translation offset Y.
     */
    protected double translateY = 0.0;

    /**
     * Default constructor for PSWTTest.
     */
    public PSWTText() {
        this("", DEFAULT_FONT);
    }

    /**
     * PSWTTest constructor with initial text.
     * @param str The initial text.
     */
    public PSWTText(String str) {
        this(str, DEFAULT_FONT);
    }

    /**
     * PSWTTest constructor with initial text and font.
     * @param str The initial text.
     * @param font The font for this PSWTText component.
     */
    public PSWTText(String str, Font font) {
        setText(str);
        this.font = font;

        recomputeBounds();
    }

    //****************************************************************************
    //
    //                  Get/Set and pairs
    //
    //***************************************************************************

    /**
     * Returns the current pen color.
     */
    public Color getPenColor() {return penColor;}

    /**
     * Sets the current pen color.
     * @param color use this color.
     */
    public void setPenColor(Color color) {
        penColor = color;
        repaint();
    }

    /**
     * Returns the current pen paint.
     */
    public Paint getPenPaint() {
        return penColor;
    }

    /**
     * Sets the current pen paint.
     * @param aPaint use this paint.
     */
    public void setPenPaint(Paint aPaint) {
        penColor = (Color)aPaint;
    }

    /**
     * Returns the current background color.
     */
    public Color getBackgroundColor() {
    	return (Color)getPaint();
    }

    /**
     * Sets the current background color.
     * @param color use this color.
     */
    public void setBackgroundColor(Color color) {
		super.setPaint(color);
    }

    /**
     * Returns the current greek threshold. Below this magnification
     * text is rendered as 'greek'.
     */
    public double getGreekThreshold() {return greekThreshold;}

    /**
     * Sets the current greek threshold. Below this magnification
     * text is rendered as 'greek'.
     * @param threshold compared to renderContext magnification.
     */
    public void setGreekThreshold(double threshold) {
        greekThreshold = threshold;
        repaint();
    }

    /**
     * Returns the current font.
     */
    public Font getFont() {return font;}

    /**
     * Return the text within this text component.
     * Multline text is returned as a single string
     * where each line is separated by a newline character.
     * Single line text does not have any newline characters.
     */
    public String getText() {
        String line;
        String result = new String();
        int lineNum = 0;

        for (Iterator i = lines.iterator() ; i.hasNext() ; ) {
            if (lineNum > 0) {
                result += '\n';
            }
            line = (String)i.next();
            result += line;
            lineNum++;
        }

        return result;
    }

    /**
     * Sets the font for the text.
     * <p>
     * <b>Warning:</b> Java has a serious bug in that it does not support very small
     * fonts.  In particular, fonts that are less than about a pixel high just don't work.
     * Since in Jazz, it is common to create objects of arbitrary sizes, and then scale them,
     * an application can easily create a text object with a very small font by accident.
     * The workaround for this bug is to create a larger font for the text object, and
     * then scale the node down correspondingly.
     * @param aFont use this font.
     */
    public void setFont(Font aFont) {
        font = aFont;

		recomputeBounds();
    }

    /**
     * Sets the text of this visual component to str. Multiple lines
     * of text are separated by a newline character.
     * @param str use this string.
     */
    public void setText(String str) {
        int pos = 0;
        int index;
        boolean done = false;
        lines = new ArrayList();
        do {
            index = str.indexOf('\n', pos);
            if (index == -1) {
                lines.add(str);
                done = true;
            } else {
                lines.add(str.substring(0, index));
                str = str.substring(index + 1);
            }
        } while (!done);

		recomputeBounds();
    }

    /**
     * Set text translation offset X.
     * @param x the X translation.
     */
    public void setTranslateX(double x) {
        setTranslation(x, translateY);
    }

    /**
     * Get the X offset translation.
     * @return the X translation.
     */
    public double getTranslateX() {
        return translateX;
    }

    /**
     * Set text translation offset Y.
     * @param y the Y translation.
     */
    public void setTranslateY(double y) {
        setTranslation(translateX, y);
    }

    /**
     * Get the Y offset translation.
     * @return the Y translation.
     */
    public double getTranslateY() {
        return translateY;
    }

    /**
     * Set the text translation offset to the specified position.
     * @param x the X-coord of translation
     * @param y the Y-coord of translation
     */
    public void setTranslation(double x, double y) {
        translateX = x;
        translateY = y;

		recomputeBounds();
    }

    /**
     * Set the text translation offset to point p.
     * @param p The translation offset.
     */
    public void setTranslation(Point2D p) {
        setTranslation(p.getX(), p.getY());
    }

    /**
     * Get the text translation offset.
     * @return The translation offset.
     */
    public Point2D getTranslation() {
        Point2D p = new Point2D.Double(translateX, translateY);
        return p;
    }

    /**
     * Renders the text object.
     * <p>
     * The transform, clip, and composite will be set appropriately when this object
     * is rendered.  It is up to this object to restore the transform, clip, and composite of
     * the Graphics2D if this node changes any of them. However, the color, font, and stroke are
     * unspecified by Jazz.  This object should set those things if they are used, but
     * they do not need to be restored.
     *
     * @param ppc Contains information about current render.
     */
    public void paint(PPaintContext ppc) {
        Graphics2D g2 = ppc.getGraphics();
        AffineTransform at = null;
        boolean translated = false;
        if (!lines.isEmpty()) {

            if ((translateX != 0.0) || (translateY != 0.0)) {
                at = g2.getTransform(); // save transform
                g2.translate(translateX, translateY);
                translated = true;
            }

                                // If font too small and not antialiased, then greek
            double renderedFontSize = font.getSize() * ppc.getScale();
                                // BBB: HACK ALERT - July 30, 1999
                                // This is a workaround for a bug in Sun JDK 1.2.2 where
                                // fonts that are rendered at very small magnifications show up big!
                                // So, we render as greek if requested (that's normal)
                                // OR if the font is very small (that's the workaround)
            if ((renderedFontSize < 0.5) ||
                (renderedFontSize < greekThreshold)) {
                paintAsGreek(ppc);
            } else {
                paintAsText(ppc);
            }
            if (translated) {
                g2.setTransform(at); // restore transform
            }
        }
    }

    /**
     * Paints this object as greek.
     * @param ppc The graphics context to paint into.
     */
    public void paintAsGreek(PPaintContext ppc) {
            Graphics2D g2 = ppc.getGraphics();

            if (greekColor != null) {
                g2.setBackground(greekColor);
                ((SWTGraphics2D)g2).fillRect(0,0,getWidth(),getHeight());
            }
    }

    /**
     * Paints this object normally (show it's text).
     * Note that the entire text gets rendered so that it's upper
     * left corner appears at the origin of this local object.
     * @param ppc The graphics context to paint into.
     */
    public void paintAsText(PPaintContext ppc) {
		SWTGraphics2D sg2 = (SWTGraphics2D)ppc.getGraphics();
		
		if (getPaint() != null) {
            sg2.setBackground((Color)getPaint());
            Rectangle2D rect = new Rectangle2D.Double(0.0, 0.0, getWidth(), getHeight());
            sg2.fillRect(rect.getX(),rect.getY(),rect.getWidth(),rect.getHeight());
        }


        sg2.translate(padding,padding);            

        double scale = Math.min(sg2.getTransform().getScaleX(),sg2.getTransform().getScaleY());
        double dSize = scale*font.getSize();
        double fixupScale = Math.floor(dSize)/dSize;
		
        // This moves the text size down to the next closest integer size - to help it stay in
        // it's alloted bounds.  This is because SWT only supports integer font metrics
        sg2.scale(fixupScale,fixupScale);
        
        
								// Render each line of text
                                // Note that the entire text gets rendered so that it's upper left corner
                                // appears at the origin of this local object.
        sg2.setColor(penColor);
        sg2.setFont(font);

        int lineNum = 0;
        String line;
        double y;

		FontMetrics metrics = sg2.getSWTFontMetrics();

        for (Iterator i = lines.iterator() ; i.hasNext() ; ) {
            line = (String)i.next();

		    // ADDED BY LEG ON 2/25/03 - BUG CAUSING PROBLEMS AT CERTAIN
		    // SCALES WHEN LINE WAS EMPTY
		    line = (line.equals("")) ? " " : line;

            y = (lineNum * metrics.getHeight());

            sg2.drawString(line, (double)0, (double)y);

            lineNum++;
        }
        
        sg2.scale(1/fixupScale,1/fixupScale);
        
        sg2.translate(-padding,-padding);            
    }

    /**
     * Notifies this object that it has changed and that it
     * should update its notion of its bounding box.
     */
    protected void recomputeBounds() {
		Point bds;
        double lineWidth;
        double maxWidth = 0.0;
        double height;

       	height = 0.0;

        boolean hasText = true;
        if ((lines.size() == 1) && (((String)lines.get(0)).equals(""))) {
            hasText = false;
        }

		GC gc = new GC(Display.getDefault());
		SWTGraphics2D g2 = new SWTGraphics2D(gc,Display.getDefault());
		g2.setFont(font);		
		FontMetrics fm = g2.getSWTFontMetrics();
		
        if (!lines.isEmpty() && hasText) {
            String line;
            int lineNum = 0;
            for (Iterator i = lines.iterator() ; i.hasNext() ; ) {
                line = (String)i.next();


                            // Find the longest line in the text
				bds = gc.stringExtent(line);
				lineWidth = bds.x;

                if (lineWidth > maxWidth) {
                    maxWidth = lineWidth;
                }
                            // Find the heighest line in the text
                if (lineNum == 0) {
                    height += fm.getAscent()+fm.getDescent()+fm.getLeading();
                } else {
                    height += fm.getHeight();
                }

                lineNum++;
            }
        } else {
                            // If no text, then we want to have the bounds of a space character,
                            // so get those bounds here
			bds = gc.stringExtent(" ");
            maxWidth = bds.x;
            height = bds.y;
        }

		gc.dispose();

                                // Finally, set the bounds of this text
        setBounds(translateX,translateY,maxWidth+2*DEFAULT_PADDING,height+2*DEFAULT_PADDING);
    }

	protected void internalUpdateBounds(double x, double y, double width, double height) {
		recomputeBounds();
	}
    
}
