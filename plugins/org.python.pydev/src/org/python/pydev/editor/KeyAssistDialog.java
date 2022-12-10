/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.contexts.IContextService;
import org.python.pydev.editor.actions.OfflineActionTarget;
import org.python.pydev.shared_ui.EditorUtils;

/**
 * <p>
 * A dialog displaying a list of action infos. The dialog will execute a command
 * if it is selected.
 * </p>
 * <p>
 * The methods on this class are not thread-safe and must be run from the UI
 * thread.
 * </p>
 */
public class KeyAssistDialog extends PopupDialog {

    /**
     * The ordered list of action infos corresponding to the table.
     */
    private final List<ActionInfo> bindings = new ArrayList<ActionInfo>();

    /**
     * The table containing of the possible completions. This value is
     * <code>null</code> until the dialog is created.
     */
    private Table completionsTable = null;

    private SortedMap<String, ActionInfo> keybindingToActionInfo;

    private OfflineActionTarget offlineActionTarget;

    /**
     * Constructs a new instance of <code>KeyAssistDialog</code>. When the
     * dialog is first constructed, it contains no widgets. The dialog is first
     * created with no parent. If a parent is required, call
     * <code>setParentShell()</code>. Also, between uses, it might be
     * necessary to call <code>setParentShell()</code> as well.
     * 
     * @param workbench
     *            The workbench in which this dialog is created; must not be
     *            <code>null</code>.
     * @param associatedKeyboard
     *            The key binding listener for the workbench; must not be
     *            <code>null</code>.
     * @param associatedState
     *            The key binding state associated with the workbench; must not
     *            be <code>null</code>.
     */
    public KeyAssistDialog(final PyEdit pyedit) {
        //Note: had to change to HOVER_SHELLSTYLE instead of INFOPOPUP_SHELLSTYLE because
        //otherwise the focus would end up in a null Control in linux (GTK),
        //which made the dialog show and hide quickly and go out of the ctrl+2 mode.
        //See: http://sourceforge.net/tracker/?func=detail&aid=2984743&group_id=85796&atid=577329
        super((Shell) null, PopupDialog.HOVER_SHELLSTYLE, false, false, false, false, false, null, null);
        this.setInfoText("   Ctrl+2 actions   ");
    }

    /**
     * Closes this shell, but first remembers some state of the dialog. This way
     * it will have a response if asked to open the dialog again or if asked to
     * open the keys preference page. This does not remember the internal state.
     * 
     * @return Whether the shell was already closed.
     */
    @Override
    public final boolean close() {
        return close(false);
    }

    /**
     * Closes this shell, but first remembers some state of the dialog. This way
     * it will have a response if asked to open the dialog again or if asked to
     * open the keys preference page.
     * 
     * @param rememberState
     *            Whether the internal state should be remembered.
     * @return Whether the shell was already closed.
     */
    public final boolean close(final boolean rememberState) {
        return close(rememberState, true);
    }

    /**
     * Closes this shell, but first remembers some state of the dialog. This way
     * it will have a response if asked to open the dialog again or if asked to
     * open the keys preference page.
     * 
     * @param rememberState
     *            Whether the internal state should be remembered.
     * @param resetState
     *            Whether the state should be reset.
     * @return Whether the shell was already closed.
     */
    private final boolean close(final boolean rememberState, final boolean resetState) {
        if (rememberState) {
            // Remember the previous width.
            completionsTable = null;
        }

        return super.close();
    }

    /**
     * Sets the position for the dialog based on the position of the workbench
     * window. The dialog is flush with the bottom right corner of the workbench
     * window. However, the dialog will not appear outside of the display's
     * client area.
     * 
     * @param size
     *            The final size of the dialog; must not be <code>null</code>.
     */
    private final void configureLocation(final Point size) {
        final Shell shell = getShell();

        final Shell workbenchWindowShell = EditorUtils.getShell();
        final int xCoord;
        final int yCoord;
        if (workbenchWindowShell != null) {
            /*
             * Position the shell at the bottom right corner of the workbench
             * window
             */
            final Rectangle workbenchWindowBounds = workbenchWindowShell.getBounds();
            xCoord = workbenchWindowBounds.x + workbenchWindowBounds.width - size.x - 10;
            yCoord = workbenchWindowBounds.y + workbenchWindowBounds.height - size.y - 10;

        } else {
            xCoord = 0;
            yCoord = 0;

        }
        final Rectangle bounds = new Rectangle(xCoord, yCoord, size.x, size.y);
        shell.setBounds(getConstrainedShellBounds(bounds));
    }

    /**
     * Sets the size for the dialog based on its previous size. The width of the
     * dialog is its previous width, if it exists. Otherwise, it is simply the
     * packed width of the dialog. The maximum width is 40% of the workbench
     * window's width. The dialog's height is the packed height of the dialog to
     * a maximum of half the height of the workbench window.
     * 
     * @return The size of the dialog
     */
    private final Point configureSize() {
        final Shell shell = getShell();

        // Get the packed size of the shell.
        shell.pack();
        final Point size = shell.getSize();

        // Enforce maximum sizing.
        final Shell workbenchWindowShell = EditorUtils.getShell();
        if (workbenchWindowShell != null) {
            final Point workbenchWindowSize = workbenchWindowShell.getSize();
            final int maxWidth = workbenchWindowSize.x * 2 / 5;
            final int maxHeight = workbenchWindowSize.y / 2;
            if (size.x > maxWidth) {
                size.x = maxWidth;
            }
            if (size.y > maxHeight) {
                size.y = maxHeight;
            }
        }

        // Set the size for the shell.
        shell.setSize(size);
        return size;
    }

    /**
     * Creates the content area for the key assistant. This creates a table and
     * places it inside the composite. The composite will contain a list of all
     * the key bindings.
     * 
     * @param parent
     *            The parent composite to contain the dialog area; must not be
     *            <code>null</code>.
     */
    @Override
    protected final Control createDialogArea(final Composite parent) {
        // First, register the shell type with the context support
        registerShellType();

        // Create a composite for the dialog area.
        final Composite composite = new Composite(parent, SWT.NONE);
        final GridLayout compositeLayout = new GridLayout();
        compositeLayout.marginHeight = 0;
        compositeLayout.marginWidth = 0;
        composite.setLayout(compositeLayout);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setBackground(parent.getBackground());

        // Layout the partial matches.
        if (keybindingToActionInfo.isEmpty()) {
            createEmptyDialogArea(composite);
        } else {
            createTableDialogArea(composite);
        }
        return composite;
    }

    /**
     * Creates an empty dialog area with a simple message saying there were no
     * matches. This is used if no partial matches could be found. This should
     * not really ever happen, but might be possible if the commands are
     * changing while waiting for this dialog to open.
     * 
     * @param parent
     *            The parent composite for the dialog area; must not be
     *            <code>null</code>.
     */
    private final void createEmptyDialogArea(final Composite parent) {
        final Label noMatchesLabel = new Label(parent, SWT.NULL);
        noMatchesLabel.setText("No matches");
        noMatchesLabel.setLayoutData(new GridData(GridData.FILL_BOTH));
        noMatchesLabel.setBackground(parent.getBackground());
    }

    /**
     * Creates a dialog area with a table of the partial matches for the current
     * key binding state. The table will be either the minimum width, or
     * <code>previousWidth</code> if it is not
     * <code>NO_REMEMBERED_WIDTH</code>.
     * 
     * @param parent
     *            The parent composite for the dialog area; must not be
     *            <code>null</code>.
     * @param partialMatches
     *            The lexicographically sorted map of partial matches for the
     *            current state; must not be <code>null</code> or empty.
     */
    private final void createTableDialogArea(final Composite parent) {
        // Layout the table.
        completionsTable = new Table(parent, SWT.FULL_SELECTION | SWT.SINGLE);
        final GridData gridData = new GridData(GridData.FILL_BOTH);
        completionsTable.setLayoutData(gridData);
        completionsTable.setBackground(parent.getBackground());
        completionsTable.setLinesVisible(true);

        // Initialize the columns and rows.
        bindings.clear();
        final TableColumn columnCommandName = new TableColumn(completionsTable, SWT.LEFT, 0);
        final TableColumn columnKeySequence = new TableColumn(completionsTable, SWT.LEFT, 1);
        final Iterator itemsItr = keybindingToActionInfo.entrySet().iterator();
        while (itemsItr.hasNext()) {
            final Map.Entry entry = (Entry) itemsItr.next();
            final String sequence = (String) entry.getKey();
            final ActionInfo actionInfo = (ActionInfo) entry.getValue();
            final String[] text = { sequence, actionInfo.description };
            final TableItem item = new TableItem(completionsTable, SWT.NULL);
            item.setText(text);
            item.setData("ACTION_INFO", actionInfo);
            bindings.add(actionInfo);
        }

        Dialog.applyDialogFont(parent);
        columnKeySequence.pack();
        columnCommandName.pack();

        /*
         * If you double-click on the table, it should execute the selected
         * command.
         */
        completionsTable.addListener(SWT.DefaultSelection, new Listener() {
            @Override
            public final void handleEvent(final Event event) {
                executeKeyBinding(event);
            }
        });
    }

    /**
     * Handles the default selection event on the table of possible completions.
     * This attempts to execute the given command.
     */
    private final void executeKeyBinding(final Event trigger) {
        // Try to execute the corresponding command.
        final int selectionIndex = completionsTable.getSelectionIndex();
        if (selectionIndex >= 0) {
            ActionInfo actionInfo = bindings.get(selectionIndex);
            actionInfo.action.run();
            this.offlineActionTarget.leave();
        }
    }

    /**
     * Opens this dialog with the list of bindings for the user to select from.
     * @param offlineActionTarget 
     * 
     * @return The return code from this dialog.
     * @since 3.3
     */
    public final int open(Collection<ActionInfo> bindings, OfflineActionTarget offlineActionTarget) {
        // If the dialog is already open, dispose the shell and recreate it.
        final Shell shell = getShell();
        if (shell != null) {
            close(false, false);
        }
        this.offlineActionTarget = offlineActionTarget;

        keybindingToActionInfo = new TreeMap<String, ActionInfo>();
        for (ActionInfo a : bindings) {
            keybindingToActionInfo.put(a.binding, a);
        }

        create();

        // Configure the size and location.
        final Point size = configureSize();
        configureLocation(size);

        // Call the super method.
        return super.open();
    }

    /**
     * Registers the shell as the same type as its parent with the context
     * support. This ensures that it does not modify the current state of the
     * application.
     */
    private final void registerShellType() {
        final Shell shell = getShell();
        final IContextService contextService = EditorUtils.getActiveWorkbenchWindow().getService(
                IContextService.class);
        contextService.registerShell(shell, contextService.getShellType((Shell) shell.getParent()));
    }

    public Table getCompletionsTable() {
        return this.completionsTable;
    }

}
