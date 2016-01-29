package org.python.pydev.utils;

import java.util.ResourceBundle;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
public final class PyEditorMessages extends NLS {

    private static final String BUNDLE_FOR_CONSTRUCTED_KEYS = "org.eclipse.jdt.internal.ui.javaeditor.ConstructedJavaEditorMessages";//$NON-NLS-1$
    private static ResourceBundle fgBundleForConstructedKeys = ResourceBundle.getBundle(BUNDLE_FOR_CONSTRUCTED_KEYS);

    /**
     * Returns the message bundle which contains constructed keys.
     *
     * @since 3.1
     * @return the message bundle
     */
    public static ResourceBundle getBundleForConstructedKeys() {
        return fgBundleForConstructedKeys;
    }

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
