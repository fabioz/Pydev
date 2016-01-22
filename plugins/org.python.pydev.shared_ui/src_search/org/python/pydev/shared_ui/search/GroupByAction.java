/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.search;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;

public class GroupByAction extends Action {

    private AbstractSearchIndexResultPage fPage;
    private int bit;
    private ImageDescriptor enabledWithBitOn;
    private ImageDescriptor enabledWithBitOff;

    public GroupByAction(AbstractSearchIndexResultPage page, int bit, ImageDescriptor imageDescriptorOn, String name) {
        super(name);
        this.enabledWithBitOn = imageDescriptorOn;
        this.enabledWithBitOff = ImageDescriptor.createWithFlags(imageDescriptorOn, SWT.IMAGE_DISABLE);
        setToolTipText(name);
        fPage = page;
        this.bit = bit;

        updateImage();
    }

    public void updateImage() {
        if ((fPage.getGroupWithConfiguration() & bit) != 0) {
            setImageDescriptor(enabledWithBitOn);

        } else {
            setImageDescriptor(enabledWithBitOff);

        }

    }

    @Override
    public void run() {
        int initialConfig = fPage.getGroupWithConfiguration();
        boolean isBitEnabled = (initialConfig & this.bit) != 0;
        int newConfig;
        if (isBitEnabled) {
            newConfig = initialConfig ^ this.bit;
        } else {
            newConfig = initialConfig | this.bit;
        }
        fPage.setGroupWithConfiguration(newConfig);
        this.updateImage();
    }

}
