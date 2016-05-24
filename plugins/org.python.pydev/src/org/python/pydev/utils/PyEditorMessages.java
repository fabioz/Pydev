/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mark Leone - Modifications for PyDev
 *******************************************************************************/
package org.python.pydev.utils;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
public final class PyEditorMessages extends NLS {

    private static final String BUNDLE_NAME = PyEditorMessages.class.getName();

    private PyEditorMessages() {
        // Do not instantiate
    }

    public static String EditorUtility_concatModifierStrings;
    public static String PyEditorHoverConfigurationBlock_delimiter;
    public static String PyEditorHoverConfigurationBlock_hoverPreferences;
    public static String PyEditorHoverConfigurationBlock_annotationRollover;
    public static String PyEditorHoverConfigurationBlock_modifierColumnTitle;
    public static String PyEditorHoverConfigurationBlock_nameColumnTitle;
    public static String PyEditorHoverConfigurationBlock_priorityColumnTitle;
    public static String PyEditorHoverConfigurationBlock_preemptColumnTitle;
    public static String PyEditorHoverConfigurationBlock_keyModifier;
    public static String PyEditorHoverConfigurationBlock_insertDelimiterAndModifierAndDelimiter;
    public static String PyEditorHoverConfigurationBlock_insertDelimiterAndModifier;
    public static String PyEditorHoverConfigurationBlock_insertModifierAndDelimiter;
    public static String PyEditorHoverConfigurationBlock_description;
    public static String PyEditorHoverConfigurationBlock_modifierIsNotValid;
    public static String PyEditorHoverConfigurationBlock_modifierIsNotValidForHover;
    public static String PyEditorHoverConfigurationBlock_duplicateModifier;

    static {
        NLS.initializeMessages(BUNDLE_NAME, PyEditorMessages.class);
    }

}
