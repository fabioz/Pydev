package com.python.pydev.analysis.actions;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.TwoPaneElementSelector;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.core.uiutils.DialogMemento;
import org.python.pydev.ui.UIConstants;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.additionalinfo.IInfo;

/**
 * This is the class that shows the globals browser.
 * 
 * It shows 2 panels, one with the labels for a token and the second with the path to that token
 *
 * @author Fabio
 */
public class GlobalsTwoPaneElementSelector extends TwoPaneElementSelector{

    /**
     * Provides the labels and images for the top panel
     *
     * @author Fabio
     */
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

	/**
	 * Provides the labels and images for the bottom panel
	 *
	 * @author Fabio
	 */
	private static final class ModuleIInfoLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
		    IInfo info = (IInfo) element;
		    String path = info.getPath();
		    int pathLen;
            if(path != null && (pathLen = path.length()) > 0){
		        FastStringBuffer buf = new FastStringBuffer(info.getDeclaringModuleName(), pathLen+5);
		        buf.append("/");
		        buf.append(path);
		        return buf.toString();
		    }
		    return info.getDeclaringModuleName();
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
