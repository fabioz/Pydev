/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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

    /**
     * Called when the <code>Cancel</code> button is pressed on the preference
     * page. Implementations should revert the preference page settings to the
     * stored preference values.
     */
    void performCancel();
}