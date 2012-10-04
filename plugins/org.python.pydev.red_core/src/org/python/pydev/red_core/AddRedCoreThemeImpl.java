/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.red_core;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.callbacks.ICallbackListener;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.red_core.preferences.PydevRedCorePreferencesInitializer;

import com.aptana.editor.common.extensions.FindBarEditorExtension;
import com.aptana.editor.common.extensions.ThemeableEditorExtension;

public class AddRedCoreThemeImpl {

    private FindBarEditorExtension themeableEditorFindBarExtension;
    private ThemeableEditorExtension themeableEditorColorsExtension;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void installRedCoreTheme(final PyEdit edit) {
        final PyEditThemeAdaptable adaptable = new PyEditThemeAdaptable(edit);
        themeableEditorFindBarExtension = new FindBarEditorExtension(adaptable);

        if (PydevRedCorePreferencesInitializer.getUseAptanaThemes()) {
            //may be null!
            themeableEditorColorsExtension = new ThemeableEditorExtension(adaptable);
        }

        edit.onCreatePartControl.registerListener(new ICallbackListener() {

            public Object call(Object obj) {
                Composite parent = (Composite) obj;
                if (themeableEditorColorsExtension != null) {
                    themeableEditorColorsExtension.setParent(parent);
                }
                Composite newParent = themeableEditorFindBarExtension.createFindBarComposite(parent);
                return newParent;
            }
        });

        edit.onAfterCreatePartControl.registerListener(new ICallbackListener() {

            public Object call(Object obj) {
                themeableEditorFindBarExtension.createFindBar(adaptable.getISourceViewer());
                if (themeableEditorColorsExtension != null) {
                    themeableEditorColorsExtension.overrideThemeColors();
                }
                return null;
            }
        });

        edit.onInitializeLineNumberRulerColumn.registerListener(new ICallbackListener() {

            public Object call(Object obj) {
                if (themeableEditorColorsExtension != null) {
                    themeableEditorColorsExtension.initializeLineNumberRulerColumn((LineNumberRulerColumn) obj);
                }
                return null;
            }
        });

        edit.onDispose.registerListener(new ICallbackListener() {

            public Object call(Object obj) {
                themeableEditorFindBarExtension.dispose();
                if (themeableEditorColorsExtension != null) {
                    themeableEditorColorsExtension.dispose();
                }
                return null;
            }
        });

        edit.onHandlePreferenceStoreChanged.registerListener(new ICallbackListener() {

            public Object call(Object event) {
                if (themeableEditorColorsExtension != null) {
                    themeableEditorColorsExtension.handlePreferenceStoreChanged((PropertyChangeEvent) event);
                }
                return null;
            }
        });

        edit.onCreateSourceViewer.registerListener(new ICallbackListener() {

            public Object call(Object viewer) {
                if (themeableEditorColorsExtension != null) {
                    themeableEditorColorsExtension.createBackgroundPainter((ISourceViewer) viewer);
                }
                return null;
            }
        });

        edit.onCreateActions.registerListener(new ICallbackListener() {

            public Object call(Object obj) {
                themeableEditorFindBarExtension.createFindBarActions();
                return null;
            }
        });

        edit.onGetAdapter.registerListener(new ICallbackListener() {

            public Object call(Object adaptable) {
                return themeableEditorFindBarExtension.getFindBarDecoratorAdapter((Class) adaptable);
            }
        });
    }

}
