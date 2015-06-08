package com.python.pydev.analysis.search_index;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;

public class GroupByAction extends Action {

    private SearchIndexResultPage fPage;
    private int bit;
    private ImageDescriptor enabledWithBitOn;
    private ImageDescriptor enabledWithBitOff;

    public GroupByAction(SearchIndexResultPage page, int bit, ImageDescriptor imageDescriptorOn, String name) {
        super(name);
        this.enabledWithBitOn = imageDescriptorOn;
        this.enabledWithBitOff = ImageDescriptor.createWithFlags(imageDescriptorOn, SWT.IMAGE_DISABLE);
        setToolTipText(name);
        fPage = page;
        this.bit = bit;

        updateImage();

    }

    private void updateImage() {
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
