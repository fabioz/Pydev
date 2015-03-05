/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.console;

import java.io.IOException;
import java.lang.ref.WeakReference;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.texteditor.IUpdate;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.core.PydevDebugPreferencesInitializer;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.actions.BaseAction;

public class ShowPromptOverlayAction extends BaseAction implements IUpdate, IEditorActionDelegate,
        IPropertyChangeListener {

    private final WeakReference<PromptOverlay> promptOverlay;
    private Menu fMenu;
    private final IPreferenceStore preferences;
    private final SetLayoutAction setLayoutAction;
    private final SetFullLayoutAction setFullLayoutAction;
    private final SetBufferedOutputAction setBufferedOutputAction;

    private IMenuCreator menuCreator;

    public ShowPromptOverlayAction(PromptOverlay promptOverlay) {
        this.promptOverlay = new WeakReference<PromptOverlay>(promptOverlay);
        preferences = PydevDebugPlugin.getDefault().getPreferenceStore();
        preferences.addPropertyChangeListener(this);

        this.setLayoutAction = new SetLayoutAction(this.promptOverlay);
        this.setFullLayoutAction = new SetFullLayoutAction(this.promptOverlay);
        this.setBufferedOutputAction = new SetBufferedOutputAction(this.promptOverlay);

        update();
        this.menuCreator = new IMenuCreator() {

            @Override
            public Menu getMenu(Menu parent) {
                return null;
            }

            @Override
            public void dispose() {
                if (fMenu != null) {
                    fMenu.dispose();
                }
                fMenu = null;
            }

            @Override
            public Menu getMenu(Control parent) {
                if (fMenu != null) {
                    fMenu.dispose();
                }

                fMenu = new Menu(parent);

                addActionToMenu(fMenu, setLayoutAction);
                addActionToMenu(fMenu, setFullLayoutAction);
                addActionToMenu(fMenu, setBufferedOutputAction);

                return fMenu;
            }

            private void addActionToMenu(Menu parent, Action action) {
                ActionContributionItem item = new ActionContributionItem(action);
                item.fill(parent, -1);
            }

        };
        setMenuCreator(this.menuCreator);
    }

    @Override
    public void update() {
        PromptOverlay overlay = promptOverlay.get();
        if (overlay == null) {
            return;
        }
        boolean show = preferences.getBoolean(PydevDebugPreferencesInitializer.SHOW_CONSOLE_PROMPT_ON_DEBUG);
        if (show) {
            this.setImageDescriptor(SharedUiPlugin.getImageCache().getDescriptor(UIConstants.CONSOLE_ENABLED));
            this.setToolTipText("Hide console prompt");

        } else {
            this.setImageDescriptor(SharedUiPlugin.getImageCache().getDescriptor(UIConstants.CONSOLE_DISABLED));
            this.setToolTipText("Show console prompt");
        }
        overlay.setOverlayVisible(show);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (PydevDebugPreferencesInitializer.SHOW_CONSOLE_PROMPT_ON_DEBUG.equals(event.getProperty())) {
            this.update();
        }
    }

    @Override
    public void run(IAction action) {
        preferences.setValue(PydevDebugPreferencesInitializer.SHOW_CONSOLE_PROMPT_ON_DEBUG,
                !preferences.getBoolean(PydevDebugPreferencesInitializer.SHOW_CONSOLE_PROMPT_ON_DEBUG));

        if (preferences instanceof IPersistentPreferenceStore) {
            try {
                ((IPersistentPreferenceStore) preferences).save();
            } catch (IOException e) {
                Log.log(e);
            }
        }
    }

    @Override
    public void run() {
        run(this);
    }

    public void dispose() {
        this.menuCreator.dispose();
        preferences.removePropertyChangeListener(this);
        this.setLayoutAction.dispose();
        this.setBufferedOutputAction.dispose();
    }

}
