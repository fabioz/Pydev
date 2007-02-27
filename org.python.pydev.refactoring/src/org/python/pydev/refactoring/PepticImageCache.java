package org.python.pydev.refactoring;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.refactoring.ui.UITexts;

public class PepticImageCache {

	private Map<String, Image> imageHash = new HashMap<String, Image>(10);

	private Image missing = null;

	public void dispose() {
		Iterator e = imageHash.values().iterator();
		while (e.hasNext())
			((Image) e.next()).dispose();
		if (missing != null) {
			missing.dispose();
		}
	}

	public Image get(String key) {
		Image image = (Image) imageHash.get(key);
		if (image == null) {
			ImageDescriptor desc;
			try {
				desc = getDescriptor(key);
				image = desc.createImage();
				imageHash.put(key, image);
			} catch (NoClassDefFoundError e) {
				return null;
			} catch (UnsatisfiedLinkError e) {
				return null;
			}
		}
		return image;
	}

	public ImageDescriptor getDescriptor(String key) {
		return PepticPlugin.imageDescriptorFromPlugin(PepticPlugin.PLUGIN_ID, UITexts.imagePath + key);
	}

}
