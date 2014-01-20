/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.compare;

import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IViewerCreator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.python.pydev.plugin.preferences.PydevPrefs;

/**
 * Required when creating a PyMergeViewer from the plugin.xml file.
 */
public class PyContentMergeViewerCreator implements IViewerCreator {

    public Viewer createViewer(Composite parent, CompareConfiguration mp) {
        return new PyMergeViewer(parent, SWT.NULL, createNewCompareConfiguration(mp));
    }

    /**
     * Creates a new configuration with the pydev preference store so that the colors appear correctly when using
     * Aptana themes.
     * 
     * Also copies the available data from the original compare configuration to the new configuration.
     */
    private CompareConfiguration createNewCompareConfiguration(CompareConfiguration mp) {
        List<IPreferenceStore> stores = PydevPrefs.getDefaultStores(false);
        IPreferenceStore prefs = mp.getPreferenceStore();
        if (prefs != null) {
            //Note, we could use the CompareUIPlugin.getDefault().getPreferenceStore() directly, but it's access
            //is restricted, so, we go to the preferences of the previously created compare configuration.
            stores.add(prefs);
        }

        CompareConfiguration cc = new CompareConfiguration(new ChainedPreferenceStore(
                stores.toArray(new IPreferenceStore[stores.size()])));
        cc.setAncestorImage(mp.getAncestorImage(null));
        cc.setAncestorLabel(mp.getAncestorLabel(null));

        cc.setLeftImage(mp.getLeftImage(null));
        cc.setLeftLabel(mp.getLeftLabel(null));
        cc.setLeftEditable(mp.isLeftEditable());

        cc.setRightImage(mp.getRightImage(null));
        cc.setRightLabel(mp.getRightLabel(null));
        cc.setRightEditable(mp.isRightEditable());

        try {
            cc.setContainer(mp.getContainer());
        } catch (Throwable e) {
            //Ignore: not available in Eclipse 3.2.
        }

        return cc;
    }
}
