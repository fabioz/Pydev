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

    static {
        NLS.initializeMessages(BUNDLE_NAME, PyEditorMessages.class);
    }

}
