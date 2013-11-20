/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.python.pydev.editor.PyEdit;

/**
 * The main plugin class to be used in the desktop.
 */
public class PydevPlugin extends AbstractUIPlugin {

    //The shared instance.
    private static PydevPlugin plugin;
    public static final String ANNOTATIONS_CACHE_KEY = "MarkOccurrencesJob Annotations";
    public static final String OCCURRENCE_ANNOTATION_TYPE = "com.python.pydev.occurrences";

    public static final String PLUGIN_ID = "com.python.pydev";

    /**
     * The constructor.
     */
    public PydevPlugin() {
        plugin = this;
    }

    /**
     * This method is called when the plug-in is stopped
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    /**
     * Returns the shared instance.
     */
    public static PydevPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given
     * plug-in relative path.
     *
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    /**
     * @return the list of occurrence annotations in the pyedit
     */
    @SuppressWarnings("unchecked")
    public static final List<Annotation> getOccurrenceAnnotationsInPyEdit(final PyEdit pyEdit) {
        List<Annotation> toRemove = new ArrayList<Annotation>();
        final Map<String, Object> cache = pyEdit.cache;

        if (cache == null) {
            return toRemove;
        }

        List<Annotation> inEdit = (List<Annotation>) cache.get(ANNOTATIONS_CACHE_KEY);
        if (inEdit != null) {
            Iterator<Annotation> annotationIterator = inEdit.iterator();
            while (annotationIterator.hasNext()) {
                Annotation annotation = annotationIterator.next();
                if (annotation.getType().equals(OCCURRENCE_ANNOTATION_TYPE)) {
                    toRemove.add(annotation);
                }
            }
        }
        return toRemove;
    }

    public static String getPluginID() {
        return getDefault().getBundle().getSymbolicName();
    }

}
