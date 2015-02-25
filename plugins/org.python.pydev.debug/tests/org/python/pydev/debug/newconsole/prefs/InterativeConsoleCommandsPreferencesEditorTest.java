/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under1 the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole.prefs;

import java.io.File;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.shared_core.callbacks.ICallbackListener;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.preferences.ScopedPreferences;

public class InterativeConsoleCommandsPreferencesEditorTest extends TestCase {

    private File baseDir;

    @Override
    protected void setUp() throws Exception {
        FileUtils.IN_TESTS = true;
        baseDir = new File(FileUtils.getFileAbsolutePath(new File(
                "InterativeConsoleCommandsPreferencesEditorTest.temporary_dir")));
        try {
            FileUtils.deleteDirectoryTree(baseDir);
        } catch (Exception e) {
            //ignore
        }
        if (baseDir.exists()) {
            throw new AssertionError("Not expecting: " + baseDir + " to exist.");
        }
        baseDir.mkdirs();
        ScopedPreferences.USER_HOME_IN_TESTS = baseDir.getAbsolutePath();
        ScopedPreferences.WORKSPACE_DIR_IN_TESTS = new File(baseDir, "workspace").getAbsolutePath();
    }

    @Override
    protected void tearDown() throws Exception {
        ScopedPreferences.USER_HOME_IN_TESTS = null;
        ScopedPreferences.WORKSPACE_DIR_IN_TESTS = null;
        try {
            FileUtils.deleteDirectoryTree(baseDir);
        } catch (Exception e) {
            //ignore
        }
    }

    public void testInteractiveConsoleCommandsPreferencesEditor() throws Exception {
        InterativeConsoleCommandsPreferencesEditor editor = new InterativeConsoleCommandsPreferencesEditor();

        Display display = Display.getCurrent();
        if (display == null) {
            display = Display.getDefault();
        }

        Shell parent = new Shell(display);
        parent.setLayout(new FillLayout());

        editor.createContents(parent);

        InteractiveConsoleCommand cmd = new InteractiveConsoleCommand("Plot");
        cmd.keybinding = "F3";
        cmd.commandText = "some text";
        editor.addCommand(cmd);

        assertEquals(editor.getCombo().getItemCount(), 1);
        editor.performSave();
        editor.loadCommands();
        assertEquals(editor.getCombo().getItemCount(), 1);
        assertEquals("Plot", editor.getCombo().getText());
        editor.removeSelectedCommand();
        assertEquals("", editor.getCombo().getText());
        assertEquals(editor.getCombo().getItemCount(), 0);

        cmd = new InteractiveConsoleCommand("Plot");
        editor.addCommand(cmd);
        assertEquals("Plot", editor.getCombo().getText());
        editor.setCommandText("Plot0");
        editor.setKeybindingText("F2");
        assertEquals("Plot0", cmd.commandText);
        assertEquals("F2", cmd.keybinding);

        cmd = new InteractiveConsoleCommand("Plot2");
        editor.addCommand(cmd);
        assertEquals("Plot2", editor.getCombo().getText());
        editor.setCommandText("Plot it");
        editor.setKeybindingText("F1");

        editor.selectComboText("Plot");
        assertEquals("Plot0", editor.getCommandText());
        assertEquals("F2", editor.getCommandKeybinding());

        editor.selectComboText("Plot2");

        editor.removeSelectedCommand();
        assertEquals("Plot", editor.getCombo().getText());

        final ArrayList<Object> lst = new ArrayList<>();
        ICallbackListener<Object> iCallbackListener = new ICallbackListener<Object>() {

            @Override
            public Object call(Object obj) {
                synchronized (lst) {
                    lst.add(1);
                }
                return null;
            }
        };
        InteractiveConsoleCommand.registerOnCommandsChangedCallback(iCallbackListener);

        editor.performSave();
        for (int i = 0; i < 10; i++) {
            synchronized (lst) {
                if (lst.size() > 0) {
                    break;
                }
            }
            synchronized (this) {
                this.wait(50);
            }
            if (i == 9) {
                fail("Did not get notification that the commands changed.");
            }
        }
        InteractiveConsoleCommand.unregisterOnCommandsChangedCallback(iCallbackListener);

        // Uncomment below to see results.
        //        goToManual(display, parent);
    }

    private void goToManual(Display display, Shell parent) {
        parent.open();

        while (!parent.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }
}
