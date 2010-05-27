package org.python.pydev.red_core;

import java.lang.ref.WeakReference;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.editor.PyEdit;

import com.aptana.editor.common.extensions.IThemeableEditor;

/**
 * Provides the needed adaptation so that the pydev editor can use the features from Aptana red core.
 */
public class PyEditThemeAdaptable implements IAdaptable, IThemeableEditor{

	private WeakReference<PyEdit> edit;


	public PyEditThemeAdaptable(PyEdit edit) {
		this.edit = new WeakReference<PyEdit>(edit);
	}

	
	public Object getAdapter(Class adapter) {
		if(ITextEditor.class == adapter){
			return edit.get();
		}
		return null;
	}

	public ISourceViewer getISourceViewer() {
		return edit.get().getISourceViewer();
	}

	public ISelectionProvider getSelectionProvider() {
		return edit.get().getSelectionProvider();
	}

	public IVerticalRuler getIVerticalRuler() {
		return edit.get().getIVerticalRuler();
	}
	
}
