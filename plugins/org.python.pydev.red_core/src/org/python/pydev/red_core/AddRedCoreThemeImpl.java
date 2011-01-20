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

import com.aptana.editor.common.extensions.ThemeableEditorExtension;
import com.aptana.editor.common.extensions.FindBarEditorExtension;

public class AddRedCoreThemeImpl {
	
	private FindBarEditorExtension themeableEditorFindBarExtension;
	private ThemeableEditorExtension themeableEditorColorsExtension;

	public void installRedCoreTheme(final PyEdit edit) {
		final PyEditThemeAdaptable adaptable = new PyEditThemeAdaptable(edit);
		themeableEditorFindBarExtension = new FindBarEditorExtension(adaptable);
		themeableEditorColorsExtension = new ThemeableEditorExtension(adaptable);
		
		edit.onCreatePartControl.registerListener(new ICallbackListener() {
			
			public Object call(Object obj) {
				Composite parent = (Composite) obj;
				themeableEditorColorsExtension.setParent(parent);
				Composite newParent = themeableEditorFindBarExtension.createFindBarComposite(parent);
				return newParent;
			}
		});
		
		edit.onAfterCreatePartControl.registerListener(new ICallbackListener() {
			
			public Object call(Object obj) {
				themeableEditorFindBarExtension.createFindBar(adaptable.getISourceViewer());
				themeableEditorColorsExtension.overrideThemeColors();
				return null;
			}
		});
		
		edit.onInitializeLineNumberRulerColumn.registerListener(new ICallbackListener() {
			
			public Object call(Object obj) {
				themeableEditorColorsExtension.initializeLineNumberRulerColumn((LineNumberRulerColumn) obj);
				return null;
			}
		});
		
		edit.onDispose.registerListener(new ICallbackListener() {
			
			public Object call(Object obj) {
				themeableEditorColorsExtension.dispose();
				return null;
			}
		});
		
		edit.onHandlePreferenceStoreChanged.registerListener(new ICallbackListener() {
			
			public Object call(Object event) {
				themeableEditorColorsExtension.handlePreferenceStoreChanged((PropertyChangeEvent) event);
				return null;
			}
		});
		
		edit.onCreateSourceViewer.registerListener(new ICallbackListener() {
			
			public Object call(Object viewer) {
				themeableEditorColorsExtension.createBackgroundPainter((ISourceViewer) viewer);
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
