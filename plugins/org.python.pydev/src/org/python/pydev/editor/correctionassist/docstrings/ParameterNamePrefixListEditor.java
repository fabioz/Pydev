/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.correctionassist.docstrings;

import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;

/**
 * @author olof
 * Using null ("\0") as string separator
 */
public class ParameterNamePrefixListEditor extends ListEditor {

    public ParameterNamePrefixListEditor(String name, String labelText, Composite parent) {
        super(name, labelText, parent);
    }

    @Override
    protected String createList(String[] items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            sb.append(item);
            sb.append("\0");
        }
        return sb.toString();
    }

    @Override
    protected String getNewInputObject() {
        InputDialog d = new InputDialog(getShell(), "Type doctag generation", "Enter a parameter prefix", null, null);
        d.open();
        return d.getValue();
    }

    @Override
    protected String[] parseString(String stringList) {
        List<String> items = new LinkedListWarningOnSlowOperations<String>();
        StringTokenizer st = new StringTokenizer(stringList, "\0");

        while (st.hasMoreTokens()) {
            items.add(st.nextToken());
        }
        String prefixesList[] = new String[items.size()];

        return items.toArray(prefixesList);
    }
}
