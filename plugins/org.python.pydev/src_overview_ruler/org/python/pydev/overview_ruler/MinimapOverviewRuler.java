package org.python.pydev.overview_ruler;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.core.uiutils.RunInUiThread;

public class MinimapOverviewRuler extends CopiedOverviewRuler {

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

    /**
     * Helper to get the first char position in a string.
     */
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

    /**
     * Lock to access the stacked parameters.
     */
    private final static Object lockStackedParameters = new Object();

    /**
     * Redraws a temporary image in the background and after that's finished, replaces the new base image and asks
     * for a new redraw.
     */
    private final class RedrawJob extends Job {

        private RedrawJob(String name) {
            super(name);
            this.setPriority(Job.SHORT);
            this.setSystem(true);
        }

        private FastStack<Object[]> stackedParameters = new FastStack<Object[]>();

        /**
         * Note: the GC and marginColor need to be disposed after they're used.
         */
        private void setParameters(GC gc, Color styledTextForeground, Point size, StyledTextContent content, int lineCount, int marginCols,
                Color marginColor, int spacing, int imageHeight, Transform transform, Image tmpImage) {
            synchronized (lockStackedParameters) {
                stackedParameters.push(new Object[] { gc, styledTextForeground, size, content, lineCount, marginCols, marginColor, spacing,
                        imageHeight, transform, tmpImage });
            }
        }

        /**
         * Redraws the base image (i.e.: lines)
         */
        private void redrawBaseImage(GC gc, Color styledTextForeground, Point size, StyledTextContent content, int lineCount,
                int marginCols, Color marginColor, int spacing, int imageHeight, Transform transform, IProgressMonitor monitor) {
            gc.setForeground(styledTextForeground);
            gc.setAlpha(200);
            gc.setTransform(transform);
            int x1 = 0, y1 = 0, x2 = 0, y2 = 0;

            for (int i = 0; i < lineCount; i++) {
                if (monitor.isCanceled()) {
                    return;
                }
                String line = rightTrim(content.getLine(i));

                //if(lineCount > 5000){
                //    if(!PySelection.matchesClassLine(line) && !PySelection.matchesFunctionLine(line)){
                //        y1 = y2 = y1 + spacing;
                //        continue; //Only print lines related to classes/functions
                //    }
                //}

                x1 = getFirstCharPosition(line);
                x2 = line.length();

                if (x2 > 0) {
                    gc.drawLine(x1, y1, x2, y2);
                }
                y1 = y2 = y1 + spacing;
            }
            if (monitor.isCanceled()) {
                return;
            }
            gc.setForeground(marginColor);
            gc.setBackground(marginColor);
            gc.drawLine(marginCols, 0, marginCols, imageHeight);
        }

        /**
         * Calls the method to draw image and later replaces the base image to be used and calls a new redraw.
         */
        @Override
        protected IStatus run(IProgressMonitor monitor) {
            Object[] parameters;
            synchronized (lockStackedParameters) {
                parameters = stackedParameters.pop();
                disposeStackedParameters();
            }

            GC gc = (GC) parameters[0];
            Color styledTextForeground = (Color) parameters[1];
            Point size = (Point) parameters[2];
            StyledTextContent content = (StyledTextContent) parameters[3];
            int lineCount = (Integer) parameters[4];
            int marginCols = (Integer) parameters[5];
            Color marginColor = (Color) parameters[6];
            int spacing = (Integer) parameters[7];
            int imageHeight = (Integer) parameters[8];
            Transform transform = (Transform) parameters[9];
            final Image image = (Image) parameters[10];

            try {
                redrawBaseImage(gc, styledTextForeground, size, content, lineCount, marginCols, marginColor, spacing, imageHeight,
                        transform, monitor);
            } catch (Throwable e) {
                Log.log(e);
            } finally {
                gc.dispose();
                marginColor.dispose();
            }
            boolean disposeOfImage = true;
            try {
                if (!monitor.isCanceled()) {
                    final Canvas c = fCanvas;
                    if (c != null && !c.isDisposed()) {
                        disposeOfImage = false;
                        RunInUiThread.async(new Runnable() {

                            public void run() {
                                //The baseImage should only be disposed in the UI thread (so, no locks are needed to 
                                //replace/dispose the image)
                                if (baseImage != null && !baseImage.isDisposed()) {
                                    baseImage.dispose();
                                }

                                if (c != null && !c.isDisposed()) {
                                    baseImage = image;
                                    MinimapOverviewRuler.this.redraw();
                                } else {
                                    image.dispose();
                                }
                            }
                        });
                    }
                }
            } finally {
                if (disposeOfImage) {
                    image.dispose();
                }
            }

            return Status.OK_STATUS;
        }

        /**
         * Disposes of any parameters in the stack that need an explicit dispose().
         */
        public void disposeStackedParameters() {
            synchronized (lockStackedParameters) {
                while (stackedParameters.size() > 0) {
                    Object[] disposeOfParameters = stackedParameters.pop();
                    GC gc = (GC) disposeOfParameters[0];
                    Color marginColor = (Color) disposeOfParameters[6];
                    gc.dispose();
                    marginColor.dispose();
                }
            }
        }
    }

    /**
     * Helper to deal with the drag.
     */
    private boolean mousePressed = false;

    public MinimapOverviewRuler(IAnnotationAccess annotationAccess, ISharedTextColors sharedColors) {
        super(annotationAccess, 120, sharedColors);

    }
    
    private WeakReference<StyledText> styledText;
    
    private final PaintListener paintListener = new PaintListener() {
        
        public void paintControl(PaintEvent e) {
            if(!fCanvas.isDisposed()){
                MinimapOverviewRuler.this.redraw();
            }
        }
    };


    @Override
    protected void doubleBufferPaint(GC dest) {
        if (fTextViewer != null) {
            fCanvas.setBackground(fTextViewer.getTextWidget().getBackground());
            fCanvas.setForeground(fTextViewer.getTextWidget().getForeground());
        }
        super.doubleBufferPaint(dest);
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

        fCanvas.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent event) {
                onDispose();
            }
        });
        
        StyledText textWidget = textViewer.getTextWidget();
        if(!textWidget.isDisposed()){
            styledText = new WeakReference<StyledText>(textWidget);
            textWidget.addPaintListener(paintListener);
        }

        return ret;
    }

    private void onMouseDown(MouseEvent event) {
        mousePressed = true;
    }

    private void onMouseUp(MouseEvent event) {
        mousePressed = false;
    }

    private void onMouseMove(MouseEvent event) {
        if (mousePressed) {
            event.button = 1;
            super.handleMouseDown(event);
        }
    }

    private void onDispose() {
        try {
            if (baseImage != null && !baseImage.isDisposed()) {
                baseImage.dispose();
                baseImage = null;
            }
            if (lastImage != null && !lastImage.isDisposed()) {
                lastImage.dispose();
                lastImage = null;
            }
            if(styledText != null){
                StyledText textWidget = styledText.get();
                if(textWidget != null && !textWidget.isDisposed()){
                    textWidget.removePaintListener(paintListener);
                }
                
            }
        } catch (Throwable e) {
            Log.log(e);
        }
        try {
            redrawJob.cancel();
            redrawJob.disposeStackedParameters();
        } catch (Throwable e) {
            Log.log(e);
        }
    }

    private volatile Image baseImage;
    private volatile Image lastImage;
    private Object[] cacheKey;
    private final RedrawJob redrawJob = new RedrawJob("Redraw overview ruler");

    @Override
    protected void doPaint1(GC paintGc) {
        //Draw the minimap
        if (fTextViewer != null) {
            IDocumentExtension4 document = (IDocumentExtension4) fTextViewer.getDocument();
            if (document != null) {
                final StyledText styledText = fTextViewer.getTextWidget();
                final Point size = fCanvas.getSize();
                if (size.x != 0 && size.y != 0) {

                    final StyledTextContent content = styledText.getContent();
                    final int lineCount = content.getLineCount();
                    IPreferenceStore preferenceStore = EditorsUI.getPreferenceStore();
                    final int marginCols = preferenceStore
                            .getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
                    String strColor = preferenceStore.getString(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR);
                    RGB marginRgb = StringConverter.asRGB(strColor);
                    Color marginColor = new Color(Display.getCurrent(), marginRgb);
                    Color black = new Color(Display.getCurrent(), new RGB(0, 0, 0));

                    int maxChars = (int) (marginCols + (marginCols * 0.1));
                    final int spacing = 1;
                    final int imageHeight = lineCount * spacing;
                    int imageWidth = maxChars;

                    Object[] currCacheKey = new Object[] { document.getModificationStamp(), size.x, size.y, styledText.getForeground(),
                            styledText.getBackground(), marginCols, marginRgb };

                    double scaleX = size.x / (double) imageWidth;
                    double scaleY = size.y / (double) imageHeight;
                    final Transform transform = new Transform(Display.getCurrent());
                    transform.scale((float) scaleX, (float) scaleY);

                    if (baseImage == null || !Arrays.equals(this.cacheKey, currCacheKey)) {
                        this.cacheKey = currCacheKey;

                        Image tmpImage = new Image(Display.getCurrent(), size.x, size.y);
                        final GC gc = new GC(tmpImage);
                        gc.setAdvanced(true);
                        gc.setAntialias(SWT.ON);
                        gc.setBackground(styledText.getBackground());
                        gc.setForeground(styledText.getBackground());
                        gc.fillRectangle(0, 0, size.x, size.y);

                        final Color styledTextForeground = styledText.getForeground();
                        final Color marginColor2 = new Color(Display.getCurrent(), marginRgb);
                        redrawJob.cancel();
                        redrawJob.setParameters(gc, styledTextForeground, size, content, lineCount, marginCols, marginColor2, spacing,
                                imageHeight, transform, tmpImage);

                        redrawJob.schedule();
                    }

                    try {
                        if (baseImage != null && !baseImage.isDisposed()) {
                            if (lastImage != null && !lastImage.isDisposed()) {
                                lastImage.dispose();
                            }

                            Image image = new Image(Display.getCurrent(), size.x, size.y);
                            GC gc2 = new GC(image);
                            try {
                                gc2.drawImage(baseImage, 0, 0);

                                Rectangle clientArea = styledText.getClientArea();
                                int top = styledText.getLineIndex(0);
                                int bottom = styledText.getLineIndex(clientArea.height) + 1;

                                float rect[] = new float[] { 0, top * spacing, imageWidth, (bottom * spacing) - (top * spacing) };
                                transform.transform(rect);

                                gc2.setLineWidth(3);
                                gc2.setAlpha(150);
                                gc2.fillRectangle(Math.round(rect[0]), Math.round(rect[1]), Math.round(rect[2]), Math.round(rect[3]));
                                gc2.setAlpha(255);
                                gc2.drawRectangle(Math.round(rect[0]), Math.round(rect[1]), Math.round(rect[2]), Math.round(rect[3]));

                                gc2.setForeground(black);
                                gc2.drawRectangle(0, 0, size.x, size.y);
                            } finally {
                                gc2.dispose();
                            }
                            lastImage = image;
                        }
                        if (lastImage != null && !lastImage.isDisposed()) {
                            paintGc.drawImage(lastImage, 0, 0);
                        }
                    } finally {
                        marginColor.dispose();
                        black.dispose();
                    }

                }
            }
        }
        super.doPaint1(paintGc);
    }

}
