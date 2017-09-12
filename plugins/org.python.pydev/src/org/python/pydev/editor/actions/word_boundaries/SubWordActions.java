package org.python.pydev.editor.actions.word_boundaries;

import java.text.BreakIterator;
import java.text.CharacterIterator;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.TextNavigationAction;

public class SubWordActions {

    /**
     * Text navigation action to navigate to the next sub-word.
     *
     * @since 3.0
     */
    protected abstract class NextSubWordAction extends TextNavigationAction {

        protected JavaWordIterator fIterator = new JavaWordIterator();

        /**
         * Creates a new next sub-word action.
         *
         * @param code Action code for the default operation. Must be an action code from @see org.eclipse.swt.custom.ST.
         */
        protected NextSubWordAction(int code) {
            super(getSourceViewer().getTextWidget(), code);
        }

        /*
         * @see org.eclipse.jface.action.IAction#run()
         */
        @Override
        public void run() {
            // Check whether we are in a java code partition and the preference is enabled
            final IPreferenceStore store = getPreferenceStore();
            if (!store.getBoolean(PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION)) {
                super.run();
                return;
            }

            final ISourceViewer viewer = getSourceViewer();
            final IDocument document = viewer.getDocument();
            try {
                fIterator.setText((CharacterIterator) new DocumentCharacterIterator(document));
                int position = widgetOffset2ModelOffset(viewer, viewer.getTextWidget().getCaretOffset());
                if (position == -1) {
                    return;
                }

                int next = findNextPosition(position);
                if (isBlockSelectionModeEnabled()
                        && document.getLineOfOffset(next) != document.getLineOfOffset(position)) {
                    super.run(); // may navigate into virtual white space
                } else if (next != BreakIterator.DONE) {
                    setCaretPosition(next);
                    getTextWidget().showSelection();
                    fireSelectionChanged();
                }
            } catch (BadLocationException x) {
                // ignore
            }
        }

        /**
         * Finds the next position after the given position.
         *
         * @param position the current position
         * @return the next position
         */
        protected int findNextPosition(int position) {
            ISourceViewer viewer = getSourceViewer();
            int widget = -1;
            int next = position;
            while (next != BreakIterator.DONE && widget == -1) { // XXX: optimize
                next = fIterator.following(next);
                if (next != BreakIterator.DONE) {
                    widget = modelOffset2WidgetOffset(viewer, next);
                }
            }

            IDocument document = viewer.getDocument();
            LinkedModeModel model = LinkedModeModel.getModel(document, position);
            if (model != null && next != BreakIterator.DONE) {
                LinkedPosition linkedPosition = model.findPosition(new LinkedPosition(document, position, 0));
                if (linkedPosition != null) {
                    int linkedPositionEnd = linkedPosition.getOffset() + linkedPosition.getLength();
                    if (position != linkedPositionEnd && linkedPositionEnd < next) {
                        next = linkedPositionEnd;
                    }
                } else {
                    LinkedPosition nextLinkedPosition = model.findPosition(new LinkedPosition(document, next, 0));
                    if (nextLinkedPosition != null) {
                        int nextLinkedPositionOffset = nextLinkedPosition.getOffset();
                        if (position != nextLinkedPositionOffset && nextLinkedPositionOffset < next) {
                            next = nextLinkedPositionOffset;
                        }
                    }
                }
            }

            return next;
        }

        /**
         * Sets the caret position to the sub-word boundary given with <code>position</code>.
         *
         * @param position Position where the action should move the caret
         */
        protected abstract void setCaretPosition(int position);
    }

    /**
     * Text navigation action to navigate to the next sub-word.
     *
     * @since 3.0
     */
    public class NavigateNextSubWordAction extends NextSubWordAction {

        /**
         * Creates a new navigate next sub-word action.
         */
        public NavigateNextSubWordAction() {
            super(ST.WORD_NEXT);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor.NextSubWordAction#setCaretPosition(int)
         */
        @Override
        protected void setCaretPosition(final int position) {
            getTextWidget().setCaretOffset(modelOffset2WidgetOffset(getSourceViewer(), position));
        }
    }

    /**
     * Text operation action to delete the next sub-word.
     *
     * @since 3.0
     */
    public class DeleteNextSubWordAction extends NextSubWordAction implements IUpdate {

        /**
         * Creates a new delete next sub-word action.
         */
        public DeleteNextSubWordAction() {
            super(ST.DELETE_WORD_NEXT);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor.NextSubWordAction#setCaretPosition(int)
         */
        @Override
        protected void setCaretPosition(final int position) {
            if (!validateEditorInputState()) {
                return;
            }

            final ISourceViewer viewer = getSourceViewer();
            StyledText text = viewer.getTextWidget();
            Point widgetSelection = text.getSelection();
            if (isBlockSelectionModeEnabled() && widgetSelection.y != widgetSelection.x) {
                final int caret = text.getCaretOffset();
                final int offset = modelOffset2WidgetOffset(viewer, position);

                if (caret == widgetSelection.x) {
                    text.setSelectionRange(widgetSelection.y, offset - widgetSelection.y);
                } else {
                    text.setSelectionRange(widgetSelection.x, offset - widgetSelection.x);
                }
                text.invokeAction(ST.DELETE_NEXT);
            } else {
                Point selection = viewer.getSelectedRange();
                final int caret, length;
                if (selection.y != 0) {
                    caret = selection.x;
                    length = selection.y;
                } else {
                    caret = widgetOffset2ModelOffset(viewer, text.getCaretOffset());
                    length = position - caret;
                }

                try {
                    viewer.getDocument().replace(caret, length, ""); //$NON-NLS-1$
                } catch (BadLocationException exception) {
                    // Should not happen
                }
            }
        }

        /*
         * @see org.eclipse.ui.texteditor.IUpdate#update()
         */
        @Override
        public void update() {
            setEnabled(isEditorInputModifiable());
        }
    }

    /**
     * Text operation action to select the next sub-word.
     *
     * @since 3.0
     */
    public class SelectNextSubWordAction extends NextSubWordAction {

        /**
         * Creates a new select next sub-word action.
         */
        public SelectNextSubWordAction() {
            super(ST.SELECT_WORD_NEXT);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor.NextSubWordAction#setCaretPosition(int)
         */
        @Override
        protected void setCaretPosition(final int position) {
            final ISourceViewer viewer = getSourceViewer();

            final StyledText text = viewer.getTextWidget();
            if (text != null && !text.isDisposed()) {

                final Point selection = text.getSelection();
                final int caret = text.getCaretOffset();
                final int offset = modelOffset2WidgetOffset(viewer, position);

                if (caret == selection.x) {
                    text.setSelectionRange(selection.y, offset - selection.y);
                } else {
                    text.setSelectionRange(selection.x, offset - selection.x);
                }
            }
        }
    }

    /**
     * Text navigation action to navigate to the previous sub-word.
     *
     * @since 3.0
     */
    protected abstract class PreviousSubWordAction extends TextNavigationAction {

        protected JavaWordIterator fIterator = new JavaWordIterator();

        /**
         * Creates a new previous sub-word action.
         *
         * @param code Action code for the default operation. Must be an action code from @see org.eclipse.swt.custom.ST.
         */
        protected PreviousSubWordAction(final int code) {
            super(getSourceViewer().getTextWidget(), code);
        }

        /*
         * @see org.eclipse.jface.action.IAction#run()
         */
        @Override
        public void run() {
            // Check whether we are in a java code partition and the preference is enabled
            final IPreferenceStore store = getPreferenceStore();
            if (!store.getBoolean(PreferenceConstants.EDITOR_SUB_WORD_NAVIGATION)) {
                super.run();
                return;
            }

            final ISourceViewer viewer = getSourceViewer();
            final IDocument document = viewer.getDocument();
            try {
                fIterator.setText((CharacterIterator) new DocumentCharacterIterator(document));
                int position = widgetOffset2ModelOffset(viewer, viewer.getTextWidget().getCaretOffset());
                if (position == -1) {
                    return;
                }

                int previous = findPreviousPosition(position);
                if (isBlockSelectionModeEnabled()
                        && document.getLineOfOffset(previous) != document.getLineOfOffset(position)) {
                    super.run(); // may navigate into virtual white space
                } else if (previous != BreakIterator.DONE) {
                    setCaretPosition(previous);
                    getTextWidget().showSelection();
                    fireSelectionChanged();
                }
            } catch (BadLocationException x) {
                // ignore - getLineOfOffset failed
            }

        }

        /**
         * Finds the previous position before the given position.
         *
         * @param position the current position
         * @return the previous position
         */
        protected int findPreviousPosition(int position) {
            ISourceViewer viewer = getSourceViewer();
            int widget = -1;
            int previous = position;
            while (previous != BreakIterator.DONE && widget == -1) { // XXX: optimize
                previous = fIterator.preceding(previous);
                if (previous != BreakIterator.DONE) {
                    widget = modelOffset2WidgetOffset(viewer, previous);
                }
            }

            IDocument document = viewer.getDocument();
            LinkedModeModel model = LinkedModeModel.getModel(document, position);
            if (model != null && previous != BreakIterator.DONE) {
                LinkedPosition linkedPosition = model.findPosition(new LinkedPosition(document, position, 0));
                if (linkedPosition != null) {
                    int linkedPositionOffset = linkedPosition.getOffset();
                    if (position != linkedPositionOffset && previous < linkedPositionOffset) {
                        previous = linkedPositionOffset;
                    }
                } else {
                    LinkedPosition previousLinkedPosition = model
                            .findPosition(new LinkedPosition(document, previous, 0));
                    if (previousLinkedPosition != null) {
                        int previousLinkedPositionEnd = previousLinkedPosition.getOffset()
                                + previousLinkedPosition.getLength();
                        if (position != previousLinkedPositionEnd && previous < previousLinkedPositionEnd) {
                            previous = previousLinkedPositionEnd;
                        }
                    }
                }
            }

            return previous;
        }

        /**
         * Sets the caret position to the sub-word boundary given with <code>position</code>.
         *
         * @param position Position where the action should move the caret
         */
        protected abstract void setCaretPosition(int position);
    }

    /**
     * Text navigation action to navigate to the previous sub-word.
     *
     * @since 3.0
     */
    public class NavigatePreviousSubWordAction extends PreviousSubWordAction {

        /**
         * Creates a new navigate previous sub-word action.
         */
        public NavigatePreviousSubWordAction() {
            super(ST.WORD_PREVIOUS);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor.PreviousSubWordAction#setCaretPosition(int)
         */
        @Override
        protected void setCaretPosition(final int position) {
            getTextWidget().setCaretOffset(modelOffset2WidgetOffset(getSourceViewer(), position));
        }
    }

    /**
     * Text operation action to delete the previous sub-word.
     *
     * @since 3.0
     */
    public class DeletePreviousSubWordAction extends PreviousSubWordAction implements IUpdate {

        /**
         * Creates a new delete previous sub-word action.
         */
        public DeletePreviousSubWordAction() {
            super(ST.DELETE_WORD_PREVIOUS);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor.PreviousSubWordAction#setCaretPosition(int)
         */
        @Override
        protected void setCaretPosition(int position) {
            if (!validateEditorInputState()) {
                return;
            }

            final int length;
            final ISourceViewer viewer = getSourceViewer();
            StyledText text = viewer.getTextWidget();
            Point widgetSelection = text.getSelection();
            if (isBlockSelectionModeEnabled() && widgetSelection.y != widgetSelection.x) {
                final int caret = text.getCaretOffset();
                final int offset = modelOffset2WidgetOffset(viewer, position);

                if (caret == widgetSelection.x) {
                    text.setSelectionRange(widgetSelection.y, offset - widgetSelection.y);
                } else {
                    text.setSelectionRange(widgetSelection.x, offset - widgetSelection.x);
                }
                text.invokeAction(ST.DELETE_PREVIOUS);
            } else {
                Point selection = viewer.getSelectedRange();
                if (selection.y != 0) {
                    position = selection.x;
                    length = selection.y;
                } else {
                    length = widgetOffset2ModelOffset(viewer, text.getCaretOffset()) - position;
                }

                try {
                    viewer.getDocument().replace(position, length, ""); //$NON-NLS-1$
                } catch (BadLocationException exception) {
                    // Should not happen
                }
            }
        }

        /*
         * @see org.eclipse.ui.texteditor.IUpdate#update()
         */
        @Override
        public void update() {
            setEnabled(isEditorInputModifiable());
        }
    }

    /**
     * Text operation action to select the previous sub-word.
     *
     * @since 3.0
     */
    public class SelectPreviousSubWordAction extends PreviousSubWordAction {

        /**
         * Creates a new select previous sub-word action.
         */
        public SelectPreviousSubWordAction() {
            super(ST.SELECT_WORD_PREVIOUS);
        }

        /*
         * @see org.eclipse.jdt.internal.ui.javaeditor.JavaEditor.PreviousSubWordAction#setCaretPosition(int)
         */
        @Override
        protected void setCaretPosition(final int position) {
            final ISourceViewer viewer = getSourceViewer();

            final StyledText text = viewer.getTextWidget();
            if (text != null && !text.isDisposed()) {

                final Point selection = text.getSelection();
                final int caret = text.getCaretOffset();
                final int offset = modelOffset2WidgetOffset(viewer, position);

                if (caret == selection.x) {
                    text.setSelectionRange(selection.y, offset - selection.y);
                } else {
                    text.setSelectionRange(selection.x, offset - selection.x);
                }
            }
        }
    }

    public static interface ISubWordEditorWrapper {
        public int widgetOffset2ModelOffset(ISourceViewer viewer, int caretOffset);

        public boolean validateEditorInputState();

        public boolean isBlockSelectionModeEnabled();

        public IPreferenceStore getPreferenceStore();

        public boolean isEditorInputModifiable();

        public int modelOffset2WidgetOffset(ISourceViewer viewer, int position);

        public ISourceViewer getSourceViewer();
    }

    private ISubWordEditorWrapper wrapper;

    public SubWordActions(ISubWordEditorWrapper wrapper) {
        this.wrapper = wrapper;
    }

    public int widgetOffset2ModelOffset(ISourceViewer viewer, int caretOffset) {
        return this.wrapper.widgetOffset2ModelOffset(viewer, caretOffset);
    }

    public boolean validateEditorInputState() {
        return this.wrapper.validateEditorInputState();
    }

    public boolean isBlockSelectionModeEnabled() {
        return this.wrapper.isBlockSelectionModeEnabled();
    }

    public IPreferenceStore getPreferenceStore() {
        return this.wrapper.getPreferenceStore();
    }

    public boolean isEditorInputModifiable() {
        return this.wrapper.isEditorInputModifiable();
    }

    public int modelOffset2WidgetOffset(ISourceViewer viewer, int position) {
        return this.wrapper.modelOffset2WidgetOffset(viewer, position);
    }

    protected ISourceViewer getSourceViewer() {
        return this.wrapper.getSourceViewer();
    }

}
