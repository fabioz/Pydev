package com.python.pydev.analysis.actions;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.TwoPaneElementSelector;
import org.python.pydev.core.uiutils.DialogMemento;
import org.python.pydev.ui.UIConstants;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.additionalinfo.IInfo;

public class GlobalsTwoPaneElementSelector extends TwoPaneElementSelector{

	private static final class NameIInfoLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
		    IInfo info = (IInfo) element;
		    return info.getName();
		}

		@Override
		public Image getImage(Object element) {
		    IInfo info = (IInfo) element;
		    return AnalysisPlugin.getImageForTypeInfo(info);
		}
	}

	private static final class ModuleIInfoLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
		    IInfo info = (IInfo) element;
		    StringBuffer buf = new StringBuffer(info.getDeclaringModuleName());
		    String path = info.getPath();
		    if(path != null && path.length() > 0){
		        buf.append("/");
		        buf.append(path);
		    }
		    return buf.toString();
		}

		@Override
		public Image getImage(Object element) {
		    return org.python.pydev.plugin.PydevPlugin.getImageCache().get(UIConstants.COMPLETION_PACKAGE_ICON);
		}
	}

	private DialogMemento memento;
	/**
	 * Constructor
	 */
	public GlobalsTwoPaneElementSelector(Shell parent) {
		super(parent, new NameIInfoLabelProvider(), new ModuleIInfoLabelProvider());
		memento = new DialogMemento(getShell(), "com.python.pydev.analysis.actions.GlobalsTwoPaneElementSelector");
	}

 	public boolean close() {
 		memento.writeSettings(getShell());
 		return super.close();
 	}
 
 	public Control createDialogArea(Composite parent) {
 		memento.readSettings();
 		return super.createDialogArea(parent);
 	}
 
    protected Point getInitialSize() {
  	  return memento.getInitialSize(super.getInitialSize(), getShell());
    }
 
 	protected Point getInitialLocation(Point initialSize) {
 	    return memento.getInitialLocation(initialSize, super.getInitialLocation(initialSize), getShell());
    }

}
