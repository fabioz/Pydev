/*
 * Created on Nov 10, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.python.pydev.pyunit;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author ggheorg
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestReportLabelProvider extends LabelProvider 
implements ITableLabelProvider, IColorProvider {
	
	private Image[] images;
	
	public TestReportLabelProvider() {
		images = new Image[2];
	    //TODO: Use an ImageCache object instead.
		images[TestResult.OK] = createImage("icons/testok.gif");
		images[TestResult.FAILED] = createImage("icons/testerr.gif");
	}
	
	private static Image createImage(String path) {
	    //TODO: Use an ImageCache object instead.
		URL url = PydevPlugin.getDefault().getDescriptor().getInstallURL();
		ImageDescriptor descriptor = null;
		try {
			descriptor = ImageDescriptor.createFromURL(new URL(url, path));
		} catch (MalformedURLException e) {
			descriptor = ImageDescriptor.getMissingImageDescriptor();
		}
		return descriptor.createImage();
	}
	
	public String getColumnText(Object element, int columnIndex) {
		TestResult result = (TestResult)element;
		switch(columnIndex) {
		case 0:
			return result.method + " - " + result.klass;
		case 1:
			return Long.toString(result.testDuration());
		}
		return null;
	}

	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == 0)
			return images[((TestResult)element).status];
		return null;
	}

	public void dispose() {
		for (int i = 0; i < images.length; i++)
			images[i].dispose();
	}
	
	public Color getForeground(Object element) {
		if (((TestResult)element).isFailure())
			return Display.getDefault().getSystemColor(SWT.COLOR_RED);
		return null;
	}
	
	public Color getBackground(Object element) {
		return null;
	}
}
