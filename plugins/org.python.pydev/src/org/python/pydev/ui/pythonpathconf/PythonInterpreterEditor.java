/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 08/08/2005
 */
package org.python.pydev.ui.pythonpathconf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.ListDialog;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

import at.jta.Key;
import at.jta.Regor;

public class PythonInterpreterEditor extends AbstractInterpreterEditor {

    public PythonInterpreterEditor(String labelText, Composite parent, IInterpreterManager interpreterManager) {
        super(IInterpreterManager.PYTHON_INTERPRETER_PATH, labelText, parent, interpreterManager);
    }

    @Override
    public String[] getInterpreterFilterExtensions() {
        if (REF.isWindowsPlatform()) {
            return new String[] { "*.exe", "*.*" };
        }
        return null;
    }

    @Override
    protected Tuple<String, String> getAutoNewInput() throws CancelException {
        List<String> pathsToSearch = new ArrayList<String>();
        if (!REF.isWindowsPlatform()) {
            pathsToSearch.add("/usr/bin");
            pathsToSearch.add("/usr/local/bin");
            Tuple<String, String> ret = super.getAutoNewInputFromPaths(pathsToSearch, "python", "python");
            if (ret != null) {
                return ret;
            }
        } else {
            //On windows we can try to see the installed versions...
            List<File> foundVersions = new ArrayList<File>();
            try {
                Regor regor = new Regor();

                //The structure for Python is something as Software\\Python\\PythonCore\\2.6\\InstallPath
                for (Key root : new Key[] { Regor.HKEY_LOCAL_MACHINE, Regor.HKEY_CURRENT_USER }) {
                    Key key = regor.openKey(root, "Software\\Python\\PythonCore", Regor.KEY_READ);
                    if (key != null) {
                        try {
                            List l = regor.listKeys(key);
                            for (Object o : l) {
                                Key openKey = regor.openKey(key, (String) o + "\\InstallPath", Regor.KEY_READ);
                                if (openKey != null) {
                                    try {
                                        byte buf[] = regor.readValue(openKey, "");
                                        if (buf != null) {
                                            String parseValue = Regor.parseValue(buf);
                                            //Ok, this should be the directory where it's installed, try to find a 'python.exe' there...
                                            File file = new File(parseValue, "python.exe");
                                            if (file.isFile()) {
                                                foundVersions.add(file);
                                            }
                                        }
                                    } finally {
                                        regor.closeKey(openKey);
                                    }
                                }
                            }
                        } finally {
                            regor.closeKey(key);
                        }
                    }
                }

            } catch (Throwable e) {
                Log.log(e);
            }
            if (foundVersions.size() == 1) {
                return new Tuple<String, String>(getUniqueInterpreterName("python"), foundVersions.get(0).toString());
            }
            if (foundVersions.size() > 1) {
                //The user should select which one to use...
                ListDialog listDialog = new ListDialog(PyAction.getShell());

                listDialog.setContentProvider(new ArrayContentProvider());
                listDialog.setLabelProvider(new LabelProvider() {
                    @Override
                    public Image getImage(Object element) {
                        return PydevPlugin.getImageCache().get(UIConstants.PY_INTERPRETER_ICON);
                    }
                });
                listDialog.setInput(foundVersions.toArray());
                listDialog
                        .setMessage("Multiple interpreters were found installed.\nPlease select which one you want to configure.");

                int open = listDialog.open();
                if (open != ListDialog.OK) {
                    throw cancelException;
                }
                Object[] result = listDialog.getResult();
                if (result == null || result.length == 0) {
                    throw cancelException;
                }
                return new Tuple<String, String>(getUniqueInterpreterName("python"), result[0].toString());

            }
        }

        return new Tuple<String, String>(getUniqueInterpreterName("python"), "python"); //This should be enough to find it from the PATH or any other way it's defined.
    }

    protected void doFillIntoGrid(Composite parent, int numColumns) {
        super.doFillIntoGrid(parent, numColumns);
        this.autoConfigButton.setToolTipText("Will try to find Python on the PATH (will fail if not available)");
    }

}
