package org.python.pydev.red_core;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.editor.IPyEditCallbackListener;
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
		
		edit.onCreatePartControl.registerListener(new IPyEditCallbackListener() {
			
			public Object call(Object obj) {
				Composite parent = (Composite) obj;
				themeableEditorColorsExtension.setParent(parent);
				Composite newParent = themeableEditorFindBarExtension.createFindBarComposite(parent);
				return newParent;
			}
		});
		
		edit.onAfterCreatePartControl.registerListener(new IPyEditCallbackListener() {
			
			public Object call(Object obj) {
				themeableEditorFindBarExtension.createFindBar(adaptable.getISourceViewer());
				themeableEditorColorsExtension.overrideThemeColors();
				return null;
			}
		});
		
		edit.onInitializeLineNumberRulerColumn.registerListener(new IPyEditCallbackListener() {
			
			public Object call(Object obj) {
				themeableEditorColorsExtension.initializeLineNumberRulerColumn((LineNumberRulerColumn) obj);
				return null;
			}
		});
		
		edit.onDispose.registerListener(new IPyEditCallbackListener() {
			
			public Object call(Object obj) {
				themeableEditorColorsExtension.dispose();
				return null;
			}
		});
		
		edit.onHandlePreferenceStoreChanged.registerListener(new IPyEditCallbackListener() {
			
			public Object call(Object event) {
				themeableEditorColorsExtension.handlePreferenceStoreChanged((PropertyChangeEvent) event);
				return null;
			}
		});
		
		edit.onCreateSourceViewer.registerListener(new IPyEditCallbackListener() {
			
			public Object call(Object viewer) {
				themeableEditorColorsExtension.createBackgroundPainter((ISourceViewer) viewer);
				return null;
			}
		});

		edit.onCreateActions.registerListener(new IPyEditCallbackListener() {
			
			public Object call(Object obj) {
				themeableEditorFindBarExtension.createFindBarActions();
				return null;
			}
		});
		
		edit.onGetAdapter.registerListener(new IPyEditCallbackListener() {
			
			public Object call(Object adaptable) {
				return themeableEditorFindBarExtension.getFindBarDecoratorAdapter((Class) adaptable);
			}
		});
	}

}
