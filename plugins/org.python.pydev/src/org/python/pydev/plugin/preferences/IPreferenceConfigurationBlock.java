/**
 * Copyright (c) 2016 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 * 
 * Copied from the JDT implementation of
 * <code>org.eclipse.cdt.internal.ui.preferences.IPreferenceConfigurationBlock</code>.
 */
package org.python.pydev.plugin.preferences;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Interface for preference configuration blocks which can either be
 * wrapped by a {@link org.python.pydev.plugin.preferences.AbstractConfigurationBlockPreferencePage}
 * or be included in some preference page.
 * <p>
 * Clients may implement this interface.
 * </p>
 */
public interface IPreferenceConfigurationBlock {

    /**
     * Creates the preference control.
     *
     * @param parent the parent composite to which to add the preferences control
     * @return the control that was added to <code>parent</code>
     */
    Control createControl(Composite parent);

    /**
     * Called after creating the control. Implementations should load the
     * preferences values and update the controls accordingly.
     */
    void initialize();

    /**
     * Called when the <code>OK</code> button is pressed on the preference
     * page. Implementations should commit the configured preference settings
     * into their form of preference storage.
     */
    void performOk();

    /**
     * Called when the <code>Defaults</code> button is pressed on the
     * preference page. Implementation should reset any preference settings to
     * their default values and adjust the controls accordingly.
     */
    void performDefaults();

    /**
     * Called when the preference page is being disposed. Implementations should
     * free any resources they are holding on to.
     */
    void dispose();
}