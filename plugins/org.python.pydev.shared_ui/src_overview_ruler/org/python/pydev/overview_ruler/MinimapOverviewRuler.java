/**
 * Copyright (c) 2013-2014 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.overview_ruler;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import org.python.pydev.shared_core.callbacks.ICallbackListener;
import org.python.pydev.shared_core.callbacks.ICallbackWithListeners;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.structure.FastStack;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.outline.IOutlineModel;
import org.python.pydev.shared_ui.outline.IParsedItem;
import org.python.pydev.shared_ui.utils.RunInUiThread;

public class MinimapOverviewRuler extends CopiedOverviewRuler {

    private Color selectionColor;

    private IPropertyChangeListener listener;

    private IPreferenceStore preferenceStore;

    private IOutlineModel fOutlineModel;

    private IPropertyChangeListener propertyListener;

    private ICallbackListener<IOutlineModel> modelListener;

    private Color getSelectionColor() {
        if (selectionColor == null || selectionColor.isDisposed()) {
            preferenceStore = SharedUiPlugin.getDefault().getPreferenceStore();
            fillSelectionColorField();

            this.listener = new IPropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent event) {
                    if (MinimapOverviewRulerPreferencesPage.MINIMAP_SELECTION_COLOR.equals(event.getProperty())) {
                        selectionColor.dispose();
                        selectionColor = null;
                        fillSelectionColorField();
                    }
                }
            };
            preferenceStore.addPropertyChangeListener(listener);
        }
        return selectionColor;
    }

    private void fillSelectionColorField() {
        String colorCode = preferenceStore.getString(MinimapOverviewRulerPreferencesPage.MINIMAP_SELECTION_COLOR);
        RGB asRGB = StringConverter.asRGB(colorCode);
        selectionColor = new Color(Display.getDefault(), asRGB);
    }

    @Override
    protected void handleDispose() {
        try {
            if (preferenceStore != null && listener != null) {
                preferenceStore.removePropertyChangeListener(listener);
                preferenceStore = null;
                listener = null;
            }
        } catch (Exception e) {
            Log.log(e);
        }
        try {
            if (preferenceStore != null && propertyListener != null) {
                preferenceStore.removePropertyChangeListener(propertyListener);
                preferenceStore = null;
                listener = null;
            }
        } catch (Exception e) {
            Log.log(e);
        }
        try {
            if (selectionColor != null) {
                selectionColor.dispose();
            }
            selectionColor = null;
        } catch (Exception e) {
            Log.log(e);
        }
        try {
            if (fOutlineModel != null && modelListener != null) {
                ICallbackWithListeners<IOutlineModel> onModelChangedListener = fOutlineModel
                        .getOnModelChangedCallback();
                onModelChangedListener.unregisterListener(modelListener);
                modelListener = null;
            }
        } catch (Exception e) {
            Log.log(e);
        }
        fOutlineModel = null;
        super.handleDispose();
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

    @SuppressWarnings("unused")
    private static final class Parameters {
        public final GC gc;
        public final Color styledTextForeground;
        public final Point size;
        public final int lineCount;
        public final int marginCols;
        public final Color marginColor;
        public final int spacing;
        public final int imageHeight;
        public final Transform transform;
        public final Image tmpImage;

        public Parameters(GC gc, Color styledTextForeground, Point size,
                int lineCount, int marginCols, Color marginColor, int spacing, int imageHeight, Transform transform,
                Image tmpImage) {
            this.gc = gc;
            this.styledTextForeground = styledTextForeground;
            this.size = size;
            this.lineCount = lineCount;
            this.marginCols = marginCols;
            this.marginColor = marginColor;
            this.spacing = spacing;
            this.imageHeight = imageHeight;
            this.transform = transform;
            this.tmpImage = tmpImage;
        }

        public void dispose() {
            gc.dispose();
            marginColor.dispose();
            transform.dispose();
        }

        public boolean isDisposed() {
            if (gc.isDisposed()) {
                return true;
            }
            if (marginColor.isDisposed()) {
                return true;
            }
            if (tmpImage.isDisposed()) {
                return true;
            }
            return false;
        }
    }

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

        private FastStack<Parameters> stackedParameters = new FastStack<Parameters>(20);

        /**
         * Note: the GC and marginColor need to be disposed after they're used.
         */
        private void setParameters(Parameters parameters) {
            synchronized (lockStackedParameters) {
                stackedParameters.push(parameters);
            }
        }

        /**
         * Redraws the base image based on the StyledText contents.
         *
         * (i.e.: draw the lines)
         */
        private void redrawBaseImage(Parameters parameters, IProgressMonitor monitor) {
            if (MinimapOverviewRulerPreferencesPage.getShowMinimapContents() && parameters.lineCount > 0
                    && parameters.size.x > 0) {

                GC gc = parameters.gc;
                gc.setForeground(parameters.styledTextForeground);
                gc.setAlpha(200);

                gc.setTransform(parameters.transform);

                IOutlineModel outlineModel = fOutlineModel;

                int x1, x2, y, beginLine;
                if (outlineModel != null) {
                    IParsedItem root = outlineModel.getRoot();
                    if (root == null) {
                        Log.log("Minimap overview ruler is trying to use outlineModel which was already disposed.");
                        return;
                    }
                    IParsedItem[] children = root.getChildren();
                    for (IParsedItem iParsedItem : children) {
                        if (monitor.isCanceled()) {
                            return;
                        }

                        beginLine = iParsedItem.getBeginLine() - 1;
                        y = (int) ((float) beginLine * parameters.imageHeight / parameters.lineCount);
                        x1 = iParsedItem.getBeginCol();
                        x2 = x1 + (iParsedItem.toString().length() * 5);
                        gc.drawLine(x1, y, x2 - x1, y);

                        IParsedItem[] children2 = iParsedItem.getChildren();
                        for (IParsedItem iParsedItem2 : children2) {
                            if (monitor.isCanceled()) {
                                return;
                            }
                            beginLine = iParsedItem2.getBeginLine() - 1;
                            y = (int) ((float) beginLine * parameters.imageHeight / parameters.lineCount);
                            x1 = iParsedItem2.getBeginCol();
                            x2 = x1 + (iParsedItem2.toString().length() * 5);
                            gc.drawLine(x1, y, x2 - x1, y);

                        }
                    }

                }

                //This would draw the margin.
                //gc.setForeground(marginColor);
                //gc.setBackground(marginColor);
                //gc.drawLine(marginCols, 0, marginCols, imageHeight);
            }
        }

        /**
         * Calls the method to draw image and later replaces the base image to be used and calls a new redraw.
         */
        @Override
        protected IStatus run(IProgressMonitor monitor) {
            final Parameters parameters;
            List<Parameters> stackedParametersClone;

            synchronized (lockStackedParameters) {
                if (stackedParameters.empty()) {
                    //Not much to do in this case...
                    return Status.OK_STATUS;
                }
                parameters = stackedParameters.pop();
                stackedParametersClone = fetchStackedParameters();
            }

            disposeStackedParameters(stackedParametersClone);

            if (parameters.isDisposed()) {
                return Status.OK_STATUS;
            }

            try {
                redrawBaseImage(parameters, monitor);
            } catch (Throwable e) {
                Log.log(e);
            } finally {
                parameters.gc.dispose();
                parameters.marginColor.dispose();
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
                                    baseImage = parameters.tmpImage;
                                    MinimapOverviewRuler.this.redraw();
                                } else {
                                    parameters.tmpImage.dispose();
                                }
                            }
                        });
                    }
                }
            } finally {
                if (disposeOfImage) {
                    parameters.tmpImage.dispose();
                }
            }

            return Status.OK_STATUS;
        }

        private List<Parameters> fetchStackedParameters() {
            ArrayList<Parameters> stackedParametersClone = new ArrayList<Parameters>();

            synchronized (lockStackedParameters) {
                while (stackedParameters.size() > 0) {
                    Parameters disposeOfParameters = stackedParameters.pop();
                    stackedParametersClone.add(disposeOfParameters);
                }
            }

            return stackedParametersClone;
        }

        /**
         * Disposes of any parameters in the stack that need an explicit dispose().
         */
        public void disposeStackedParameters() {
            disposeStackedParameters(fetchStackedParameters());
        }

        private void disposeStackedParameters(List<Parameters> stackedParametersClone) {
            for (Parameters disposeOfParameters : stackedParametersClone) {
                disposeOfParameters.dispose();
            }
        }
    }

    public MinimapOverviewRuler(IAnnotationAccess annotationAccess, ISharedTextColors sharedColors,
            IOutlineModel outlineModel) {
        super(annotationAccess, MinimapOverviewRulerPreferencesPage.getMinimapWidth(), sharedColors);
        this.fOutlineModel = outlineModel;
        propertyListener = new IPropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {
                if (MinimapOverviewRulerPreferencesPage.MINIMAP_WIDTH.equals(event.getProperty())) {
                    updateWidth();
                }
            }
        };

        if (outlineModel != null) {
            modelListener = new ICallbackListener<IOutlineModel>() {

                @Override
                public Object call(IOutlineModel obj) {
                    lastModelChange = System.currentTimeMillis();
                    update();
                    return null;
                }
            };
            ICallbackWithListeners<IOutlineModel> onModelChangedListener = outlineModel.getOnModelChangedCallback();
            onModelChangedListener.registerListener(modelListener);
        }
    }

    private void updateWidth() {
        fWidth = MinimapOverviewRulerPreferencesPage.getMinimapWidth();
    }

    private WeakReference<StyledText> styledText;

    private final PaintListener paintListener = new PaintListener() {

        public void paintControl(PaintEvent e) {
            if (!fCanvas.isDisposed()) {
                MinimapOverviewRuler.this.redraw();
            }
        }
    };

    private Color lastBackground;
    private Color lastForeground;

    @Override
    protected void doubleBufferPaint(GC dest) {
        if (fTextViewer != null) {
            StyledText textWidget = fTextViewer.getTextWidget();
            //Calling setBackground/setForeground leads to a repaint on some Linux variants (ubuntu 12), so
            //we must only call it if it actually changed to prevent a repaint.
            //View: https://sw-brainwy.rhcloud.com/tracker/LiClipse/120
            Color background = textWidget.getBackground();
            if (lastBackground == null || !lastBackground.equals(background)) {
                fCanvas.setBackground(background);
                lastBackground = background;
            }

            Color foreground = textWidget.getForeground();
            if (lastForeground == null || !lastForeground.equals(foreground)) {
                fCanvas.setForeground(foreground);
                lastForeground = foreground;
            }

        }
        super.doubleBufferPaint(dest);
    }

    @Override
    public Control createControl(Composite parent, ITextViewer textViewer) {
        Control ret = super.createControl(parent, textViewer);
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
        if (!textWidget.isDisposed()) {
            styledText = new WeakReference<StyledText>(textWidget);
            textWidget.addPaintListener(paintListener);
        }

        return ret;
    }

    private void onMouseMove(MouseEvent event) {
        if ((event.stateMask & SWT.BUTTON1) != 0) {
            handleDrag(event);
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
            if (styledText != null) {
                StyledText textWidget = styledText.get();
                if (textWidget != null && !textWidget.isDisposed()) {
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
    private long lastModelChange;
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

                    final int lineCount = super.getLineCount(styledText);
                    IPreferenceStore preferenceStore = EditorsUI.getPreferenceStore();
                    final int marginCols = preferenceStore
                            .getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
                    String strColor = preferenceStore
                            .getString(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLOR);
                    RGB marginRgb = StringConverter.asRGB(strColor);
                    Color marginColor = new Color(Display.getCurrent(), marginRgb);

                    int maxChars = (int) (marginCols + (marginCols * 0.1));
                    final int spacing = 1;
                    int imageHeight = lineCount * spacing;
                    int imageWidth = maxChars;

                    Color background = styledText.getBackground();
                    boolean isDark = (background.getRed() * 0.21) + (background.getGreen() * 0.71)
                            + (background.getBlue() * 0.07) <= 128;
                    Object[] currCacheKey = new Object[] { document.getModificationStamp(), size.x, size.y,
                            styledText.getForeground(), background, marginCols, marginRgb, lastModelChange };

                    double scaleX = size.x / (double) imageWidth;
                    double scaleY = size.y / (double) imageHeight;
                    Transform transform = new Transform(Display.getCurrent());
                    transform.scale((float) scaleX, (float) scaleY);
                    final Color styledTextForeground = styledText.getForeground();

                    if (baseImage == null || !Arrays.equals(this.cacheKey, currCacheKey)) {
                        this.cacheKey = currCacheKey;

                        Image tmpImage = new Image(Display.getCurrent(), size.x, size.y);
                        final GC gc = new GC(tmpImage);
                        gc.setAdvanced(true);
                        gc.setAntialias(SWT.ON);
                        gc.setBackground(background);
                        gc.setForeground(background);
                        gc.fillRectangle(0, 0, size.x, size.y);

                        final Color marginColor2 = new Color(Display.getCurrent(), marginRgb);
                        redrawJob.cancel();
                        redrawJob.setParameters(new Parameters(gc, styledTextForeground, size, lineCount, marginCols,
                                marginColor2, spacing, imageHeight, transform, tmpImage));

                        redrawJob.schedule();
                    }

                    try {
                        if (baseImage != null && !baseImage.isDisposed()) {
                            if (lastImage != null && !lastImage.isDisposed()) {
                                lastImage.dispose();
                            }

                            Image image = new Image(Display.getCurrent(), size.x, size.y);
                            GC gc2 = new GC(image);
                            gc2.setAntialias(SWT.ON);
                            try {
                                gc2.setBackground(background);
                                gc2.fillRectangle(0, 0, size.x, size.y);
                                gc2.drawImage(baseImage, 0, 0);

                                Rectangle clientArea = styledText.getClientArea();
                                int top = styledText.getLineIndex(0);
                                int bottom = styledText.getLineIndex(clientArea.height) + 1;

                                float rect[] = new float[] { 0, top * spacing, imageWidth,
                                        (bottom * spacing) - (top * spacing) };
                                transform.transform(rect);

                                //Draw only a line at the left side.
                                gc2.setLineWidth(3);
                                gc2.setAlpha(30);
                                gc2.setForeground(styledTextForeground);
                                gc2.drawLine(0, 0, 0, size.y);

                                //Draw the selection area
                                if (!isDark) {
                                    gc2.setAlpha(30);
                                } else {
                                    gc2.setAlpha(100);
                                }
                                Color localSelectionColor = this.getSelectionColor();
                                if (localSelectionColor.isDisposed()) {
                                    //Shouldn't really happen as we should do all in the main thread, but just in case...
                                    localSelectionColor = styledText.getSelectionBackground();
                                }

                                gc2.setForeground(localSelectionColor);
                                gc2.setBackground(localSelectionColor);

                                //Fill selected area in the overview ruler.
                                gc2.fillRectangle(Math.round(rect[0]), Math.round(rect[1]), Math.round(rect[2]),
                                        Math.round(rect[3]));

                                //Draw a border around the selected area
                                gc2.setAlpha(255);
                                gc2.setLineWidth(1);
                                gc2.drawRectangle(Math.round(rect[0]), Math.round(rect[1]), Math.round(rect[2]) - 1,
                                        Math.round(rect[3]));

                                //This would draw a border around the whole overview bar.
                                //gc2.drawRectangle(0, 0, size.x, size.y);
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
                    }

                }
            }
        }
        super.doPaint1(paintGc);
    }

    MouseEvent lastMouseDown = null;

    @Override
    protected void handleMouseDown(MouseEvent event) {
        this.handleDrag(event);
        lastMouseDown = event;
    }

    @Override
    protected void handleMouseUp(MouseEvent event) {
        if (lastMouseDown != null) {
            int diff = Math.abs(lastMouseDown.x - event.x);
            if (diff > 3) {
                return;
            }
            diff = Math.abs(lastMouseDown.y - event.y);
            if (diff > 3) {
                return;
            }
            if (lastMouseDown.time - event.time < 1000) {
                super.handleMouseDown(event);
            }
        }
        lastMouseDown = null;
    }

    /**
     * Handles mouse clicks.
     *
     * @param event the mouse button down event
     */
    private void handleDrag(MouseEvent event) {
        if (fTextViewer != null) {
            int[] lines = toLineNumbers(event.y);
            int selectedLine = lines[0];
            Position p = null;
            try {
                IDocument document = fTextViewer.getDocument();
                IRegion lineInformation = document.getLineInformation(selectedLine);
                p = new Position(lineInformation.getOffset(), 0);

                if (p != null) {
                    StyledText styledText = fTextViewer.getTextWidget();
                    Rectangle clientArea = styledText.getClientArea();
                    int top = styledText.getLineIndex(0);
                    int bottom = styledText.getLineIndex(clientArea.height) + 1;

                    int middle = (int) (((bottom - top) / 2.0));
                    if (selectedLine < middle) {
                        fTextViewer.setTopIndex(0);

                    } else {
                        fTextViewer.setTopIndex(selectedLine - middle);
                    }
                }
            } catch (BadLocationException e) {
                // do nothing
            }
            fTextViewer.getTextWidget().setFocus();
        }
    }
}
