package org.python.pydev.overview_ruler;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

public class MinimapOverviewRuler extends CopiedOverviewRuler {

    private boolean mousePressed = false;
    
    /**
     * @param annotationAccess
     * @param width
     * @param sharedColors
     * @param discolorTemporaryAnnotation
     */
    public MinimapOverviewRuler(IAnnotationAccess annotationAccess, ISharedTextColors sharedColors) {
        super(annotationAccess, 120, sharedColors);

    }

    @Override
    protected void doubleBufferPaint(GC dest) {
        if (fTextViewer != null) {
            fCanvas.setBackground(fTextViewer.getTextWidget().getBackground());
            fCanvas.setForeground(fTextViewer.getTextWidget().getForeground());
        }
        super.doubleBufferPaint(dest);
    }

    /**
     * Removes whitespaces and tabs at the end of the string.
     */
    public static String rightTrim(final String input) {
        int len = input.length();
        int st = 0;
        int off = 0;

        while ((st < len) && (input.charAt(off + len - 1) <= ' ')) {
            len--;
        }
        return input.substring(0, len);
    }

    public static int getFirstCharPosition(String src) {
        int i = 0;
        boolean breaked = false;
        while (i < src.length()) {
            if (Character.isWhitespace(src.charAt(i)) == false && src.charAt(i) != '\t') {
                i++;
                breaked = true;
                break;
            }
            i++;
        }
        if (!breaked) {
            i++;
        }
        return (i - 1);
    }
    
    @Override
    public Control createControl(Composite parent, ITextViewer textViewer) {
        Control ret = super.createControl(parent, textViewer);
        fCanvas.addMouseListener(new MouseAdapter() {
            public void mouseDown(MouseEvent event) {
                onMouseDown(event);
            }
            @Override
            public void mouseUp(MouseEvent event) {
                onMouseUp(event);
            }
        });

        fCanvas.addMouseMoveListener(new MouseMoveListener() {
            public void mouseMove(MouseEvent event) {
                onMouseMove(event);
            }
        });
        return ret;
    }
    
    private void onMouseDown(MouseEvent event) {
        mousePressed = true;
    }
    
    private void onMouseUp(MouseEvent event) {
        mousePressed = false;
    }
    
    private void onMouseMove(MouseEvent event) {
        if(mousePressed){
            event.button = 1;
            super.handleMouseDown(event);
        }
    }
    
    
    @Override
    protected void doPaint1(GC paintGc) {
        //Draw the minimap
        if (fTextViewer != null) {
            StyledText styledText = fTextViewer.getTextWidget();
            Point size = fCanvas.getSize();
            if (size.x != 0 && size.y != 0) {

                StyledTextContent content = styledText.getContent();
                int lineCount = content.getLineCount();
                IPreferenceStore preferenceStore = EditorsUI.getPreferenceStore();
                int marginCols = preferenceStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
                String strColor = preferenceStore.getString(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR);
                RGB asRGB = StringConverter.asRGB(strColor);
                Color color = new Color(Display.getCurrent(), asRGB);
                Color black = new Color(Display.getCurrent(), new RGB(0, 0, 0));

                int maxChars = (int) (marginCols + (marginCols * 0.1));
                int spacing = 1;
                int imageHeight = lineCount * spacing;
                int imageWidth = maxChars;

                Image image = new Image(Display.getCurrent(), size.x, size.y);

                try {
                    GC gc = new GC(image);
                    gc.setAdvanced(true);
                    gc.setAntialias(SWT.ON);
                    gc.setBackground(styledText.getBackground());
                    gc.setForeground(styledText.getBackground());
                    gc.fillRectangle(0, 0, size.x, size.y);
                    
                    gc.setForeground(styledText.getForeground());


                    //int newImageHeight = imageHeight;
                    //if (newImageHeight > size.y) {
                    //    newImageHeight = size.y;
                    //}
                    double scaleX = size.x / (double) imageWidth;
                    double scaleY = size.y / (double) imageHeight;

                    Transform transform = new Transform(Display.getCurrent());
                    transform.scale((float) scaleX, (float) scaleY);
                    gc.setTransform(transform);

                    try {
                        int x1 = 0, y1 = 0, x2 = 0, y2 = 0;

                        for (int i = 0; i < lineCount; i++) {
                            String line = rightTrim(content.getLine(i));
                            x1 = getFirstCharPosition(line);
                            x2 = line.length();

                            if (x2 > 0) {
                                gc.drawLine(x1, y1, x2, y2);
                            }
                            y1 = y2 = y1 + spacing;
                        }

                        gc.setForeground(color);
                        gc.setBackground(color);
                        gc.drawLine(marginCols, 0, marginCols, imageHeight);

                        Rectangle clientArea = styledText.getClientArea();
                        int top = styledText.getLineIndex(0);
                        int bottom = styledText.getLineIndex(clientArea.height) + 1;

                        float rect[] = new float[] { 0, top * spacing, imageWidth, (bottom * spacing) - (top * spacing) };
                        transform.transform(rect);

                        gc.setTransform(null);

                        gc.setLineWidth(3);
                        gc.setAlpha(150);
                        gc.fillRectangle(Math.round(rect[0]), Math.round(rect[1]), Math.round(rect[2]), Math.round(rect[3]));
                        gc.setAlpha(255);
                        gc.drawRectangle(Math.round(rect[0]), Math.round(rect[1]), Math.round(rect[2]), Math.round(rect[3]));

                        
                        gc.setForeground(black);
                        gc.drawRectangle(0, 0, size.x, size.y);
                    } finally {
                        gc.dispose();
                    }
                    paintGc.drawImage(image, 0, 0);
                } finally {
                    image.dispose();
                    color.dispose();
                    black.dispose();
                }

            }
        }
        super.doPaint1(paintGc);
    }

}
