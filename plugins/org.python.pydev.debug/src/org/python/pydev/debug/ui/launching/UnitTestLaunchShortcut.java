/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: Aug 26, 2003
 */
package org.python.pydev.debug.ui.launching;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.ui.IEditorPart;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.fastparser.FastParser;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class UnitTestLaunchShortcut extends AbstractLaunchShortcut {

    private String arguments = "";

    @Override
    protected String getLaunchConfigurationType() {
        return Constants.ID_PYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE;
    }

    @Override
    protected IInterpreterManager getInterpreterManager(IProject project) {
        return PydevPlugin.getPythonInterpreterManager();
    }

    @Override
    public void launch(IEditorPart editor, String mode) {
        this.arguments = "";
        if (editor instanceof PyEdit) {
            PyEdit pyEdit = (PyEdit) editor;
            PySelection ps = pyEdit.createPySelection();
            String selectedText = ps.getSelectedText();
            if (selectedText.length() > 0) {
                String last = null;
                FastStringBuffer buf = new FastStringBuffer();
                List<stmtType> path = FastParser.parseToKnowGloballyAccessiblePath(ps.getDoc(),
                        ps.getStartLineIndex());
                for (stmtType stmtType : path) {
                    if (buf.length() > 0) {
                        buf.append('.');
                    }
                    last = NodeUtils.getRepresentationString(stmtType);
                    buf.append(last);
                }
                if (last != null) {
                    if (last.equals(selectedText)) {
                        this.arguments = buf.toString();
                    }
                }
            }
        }
        super.launch(editor, mode);
    }

    @Override
    public ILaunchConfigurationWorkingCopy createDefaultLaunchConfigurationWithoutSaving(
            FileOrResource[] resource) throws CoreException {
        ILaunchConfigurationWorkingCopy workingCopy = super
                .createDefaultLaunchConfigurationWithoutSaving(resource);
        if (arguments.length() > 0) {
            workingCopy.setAttribute(Constants.ATTR_UNITTEST_TESTS, arguments);
        }
        return workingCopy;
    }

    @Override
    protected List<ILaunchConfiguration> findExistingLaunchConfigurations(FileOrResource[] file) {
        List<ILaunchConfiguration> ret = new ArrayList<ILaunchConfiguration>();

        List<ILaunchConfiguration> existing = super.findExistingLaunchConfigurations(file);
        for (ILaunchConfiguration launch : existing) {
            boolean matches = false;
            try {
                matches = launch.getAttribute(Constants.ATTR_UNITTEST_TESTS, "").equals(arguments);
            } catch (CoreException e) {
                //ignore
            }
            if (matches) {
                ret.add(launch);
            }
        }
        return ret;
    }
}
