/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.fastparser.FastParser;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_ui.EditorUtils;

public class PyCopyQualifiedName extends PyAction {

    @Override
    public void run(IAction action) {
        FastStringBuffer buf = new FastStringBuffer();
        try {
            PyEdit pyEdit = getPyEdit();

            PySelection pySelection = new PySelection(pyEdit);

            IPythonNature nature = pyEdit.getPythonNature();
            File editorFile = pyEdit.getEditorFile();

            if (editorFile != null) {
                if (nature != null) {
                    String mod = nature.resolveModule(editorFile);
                    if (mod != null) {
                        buf.append(mod);

                    } else {
                        //Support for external files (not in PYTHONPATH).
                        buf.append(FullRepIterable.getFirstPart(editorFile.getName()));

                    }
                } else {
                    buf.append(FullRepIterable.getFirstPart(editorFile.getName()));
                }
            }

            List<stmtType> path = FastParser.parseToKnowGloballyAccessiblePath(pySelection.getDoc(),
                    pySelection.getStartLineIndex());
            for (stmtType stmtType : path) {
                if (buf.length() > 0) {
                    buf.append('.');
                }
                buf.append(NodeUtils.getRepresentationString(stmtType));
            }

        } catch (MisconfigurationException e1) {
            Log.log(e1);
            return;
        }

        Transfer[] dataTypes = new Transfer[] { TextTransfer.getInstance() };
        Object[] data = new Object[] { buf.toString() };

        Clipboard clipboard = new Clipboard(EditorUtils.getShell().getDisplay());
        try {
            clipboard.setContents(data, dataTypes);
        } catch (SWTError e) {
            if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
                throw e;
            }
            MessageDialog.openError(EditorUtils.getShell(), "Error copying to clipboard.", e.getMessage());
        } finally {
            clipboard.dispose();
        }
    }

}
