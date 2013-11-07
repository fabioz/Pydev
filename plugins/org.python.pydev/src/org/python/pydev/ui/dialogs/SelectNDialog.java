/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_core.structure.TreeNodeContentProvider;
import org.python.pydev.shared_ui.EditorUtils;

@SuppressWarnings("rawtypes")
public class SelectNDialog {

    /**
     * @param labelProvider: usually a TreeNodeLabelProvider
     * @param title: message in shell
     * @param message: message above tree
     * @param checkElapsedBeforeClose: if true we'll only accept a click after some secs (to prevent an accidental click
     * in the case of an unrequested dialog).
     */
    public static List<TreeNode> selectElements(TreeNode root, ILabelProvider labelProvider, String title,
            String message, final boolean checkElapsedBeforeClose, List initialSelection) {
        Shell shell = EditorUtils.getShell();

        CheckedTreeSelectionDialog dialog = new CheckedTreeSelectionDialog(shell, labelProvider,
                new TreeNodeContentProvider()) {

            private final DialogButtonEnablementHelper helper = new DialogButtonEnablementHelper(
                    !checkElapsedBeforeClose);

            @Override
            public boolean close() {
                if (!helper.areButtonsEnabled()) {
                    return false;
                }
                return super.close();
            }

            @Override
            protected void cancelPressed() {
                if (!helper.areButtonsEnabled()) {
                    return;
                }
                super.cancelPressed();
            }

            @Override
            protected void okPressed() {
                if (!helper.areButtonsEnabled()) {
                    return;
                }
                super.okPressed();
            }

            @Override
            protected void constrainShellSize() {
                helper.onConstrainShellSize();
                super.constrainShellSize();
            }

            @Override
            protected Button createButton(Composite parent, int id, String label, boolean defaultButton) {
                Button button = super.createButton(parent, id, label, defaultButton);
                helper.onCreateButton(button, id);
                return button;
            }

            @Override
            protected void createButtonsForButtonBar(Composite parent) {
                // create OK and Cancel buttons by default
                createButton(parent, IDialogConstants.OK_ID, "Apply selected changes (Ignore unselected)",
                        true);
                createButton(parent, IDialogConstants.CANCEL_ID,
                        "Don't ask again (Ignore all)", false);
            }
        };

        dialog.setTitle(title);

        dialog.setMessage(message);

        dialog.setInput(root);

        dialog.setInitialElementSelections(initialSelection);

        dialog.setExpandedElements(initialSelection.toArray());

        if (dialog.open() == CheckedTreeSelectionDialog.OK) {
            Object[] result = dialog.getResult();
            if (result != null) {
                ArrayList<TreeNode> ret = new ArrayList<TreeNode>(result.length);
                for (Object o : result) {
                    ret.add((TreeNode) o);
                }
                return ret;
            }
        }

        return null;
    }
}
