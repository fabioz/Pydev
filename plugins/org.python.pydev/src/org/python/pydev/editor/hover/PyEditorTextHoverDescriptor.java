/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mark Leone - Modifications and enhancements for PyDev
 *******************************************************************************/
package org.python.pydev.editor.hover;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.osgi.framework.Bundle;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;

/**
 * Describes a PyDev editor text hover.
 *
 * @since 2.1
 */
public class PyEditorTextHoverDescriptor {

    private static final String HOVER_TAG = "pyTextHover"; //$NON-NLS-1$
    private static final String LABEL_ATTRIBUTE = "label"; //$NON-NLS-1$
    private static final String ACTIVATE_PLUG_IN_ATTRIBUTE = "activate"; //$NON-NLS-1$
    private static final String DESCRIPTION_ATTRIBUTE = "description"; //$NON-NLS-1$

    public static final String ATT_PYDEV_HOVER_PRIORITY = "priority";
    public static final String ATT_PYDEV_HOVER_LABEL = "label";
    public static final String ATT_PYDEV_HOVER_CLASS = "class";
    public static final String ATT_PYDEV_HOVER_ID = "id";
    public static final String ATT_PYDEV_HOVER_PREEMPT = "preempt";
    public static final String ATT_PYDEV_HOVER_ENABLE = "enable";
    public static final int DEFAULT_HOVER_PRIORITY = 100;

    public static final String NO_MODIFIER = "0"; //$NON-NLS-1$
    public static final Integer DEFAULT_MODIFIER_MASK = 0;
    public static final int HIGHEST_PRIORITY = 1;
    private static final String COMBINING_HOVER_ID = "org.python.pydev.combininghover";
    private static final String COMBINING_HOVER_LABEL = "PyDev Combining Hover";
    private static final String COMBINING_HOVER_DESCR = "A Text Hover which combines hover info from contributed hovers";

    int fStateMask;

    String fModifierString;

    private boolean fIsEnabled;

    private IConfigurationElement fElement;

    private Integer fPriority;

    private Boolean fPreempt;

    private AbstractPyEditorTextHover fHover;

    /**
     * Returns all PyDev editor text hovers contributed to the workbench.
     *
     * @return an array with the contributed text hovers
     */
    public static PyEditorTextHoverDescriptor[] getContributedHovers() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IConfigurationElement[] elements = registry
                .getConfigurationElementsFor(ExtensionHelper.PYDEV_HOVER2);
        PyEditorTextHoverDescriptor[] hoverDescs = createDescriptors(elements);
        initializeDefaultHoverPreferences(elements);
        initializeHoversFromPreferences(hoverDescs);
        return hoverDescs;
    }

    /**
     * Computes the state mask for the given modifier string.
     *
     * @param modifiers the string with the modifiers, separated by '+', '-', ';', ',' or '.'
     * @return the state mask or -1 if the input is invalid
     */
    public static int computeStateMask(String modifiers) {
        if (modifiers == null) {
            return -1;
        }

        if (modifiers.length() == 0) {
            return SWT.NONE;
        }

        int stateMask = 0;
        StringTokenizer modifierTokenizer = new StringTokenizer(modifiers, ",;.:+-* "); //$NON-NLS-1$
        while (modifierTokenizer.hasMoreTokens()) {
            int modifier = PyAction.findLocalizedModifier(modifierTokenizer.nextToken());
            if (modifier == 0 || (stateMask & modifier) == modifier) {
                return -1;
            }
            stateMask = stateMask | modifier;
        }
        return stateMask;
    }

    /**
     * Creates a new PyDev Editor text hover descriptor from the given combining
     * hover implementation.
     *
     * @param element the combining hover
     */
    public PyEditorTextHoverDescriptor(PydevCombiningHover hover) {
        Assert.isNotNull(hover);
        fHover = hover;
        fPriority = HIGHEST_PRIORITY;
        fIsEnabled = PyHoverPreferencesPage.getCombineHoverInfo();
    }

    /**
     * Creates a new PyDev Editor text hover descriptor from the given configuration element.
     *
     * @param element the configuration element
     */
    private PyEditorTextHoverDescriptor(IConfigurationElement element) {
        Assert.isNotNull(element);
        fElement = element;
    }

    /**
     * Creates the PyDev editor text hover.
     *
     * @return the text hover
     */
    public AbstractPyEditorTextHover createTextHover() {
        if (fElement != null) {
            String pluginId = fElement.getContributor().getName();
            boolean isHoversPlugInActivated = Platform.getBundle(pluginId).getState() == Bundle.ACTIVE;
            if (isHoversPlugInActivated || canActivatePlugIn()) {
                try {
                    return (AbstractPyEditorTextHover) fElement
                            .createExecutableExtension(ATT_PYDEV_HOVER_CLASS);
                } catch (CoreException x) {
                    Log.log(x);
                }
            }
            return null;
        } else {
            return fHover;
        }
    }

    //---- XML Attribute accessors ---------------------------------------------

    /**
     * Returns the hover's id.
     *
     * @return the id
     */
    public String getId() {
        if (fElement != null) {
            return fElement.getAttribute(ATT_PYDEV_HOVER_ID);
        }
        return COMBINING_HOVER_ID;
    }

    /**
     * Returns the hover's class name.
     *
     * @return the class name
     */
    public String getHoverClassName() {
        if (fElement != null) {
            return fElement.getAttribute(ATT_PYDEV_HOVER_CLASS);
        }
        return fHover.getClass().getName();
    }

    /**
     * Returns the hover's label.
     *
     * @return the label
     */
    public String getLabel() {
        if (fElement != null) {
            String label = fElement.getAttribute(LABEL_ATTRIBUTE);
            if (label != null) {
                return label;
            }

            // Return simple class name
            label = getHoverClassName();
            int lastDot = label.lastIndexOf('.');
            if (lastDot >= 0 && lastDot < label.length() - 1) {
                return label.substring(lastDot + 1);
            } else {
                return label;
            }
        }
        return COMBINING_HOVER_LABEL;
    }

    /**
     * Returns the hover's description.
     *
     * @return the hover's description or <code>null</code> if not provided
     */
    public String getDescription() {
        if (fElement != null) {
            return fElement.getAttribute(DESCRIPTION_ATTRIBUTE);
        }
        return COMBINING_HOVER_DESCR;
    }

    public boolean canActivatePlugIn() {
        return (fElement == null ? false
                : Boolean.valueOf(fElement.getAttribute(ACTIVATE_PLUG_IN_ATTRIBUTE)).booleanValue());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !obj.getClass().equals(this.getClass()) || getId() == null) {
            return false;
        }
        return getId().equals(((PyEditorTextHoverDescriptor) obj).getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public static PyEditorTextHoverDescriptor[] createDescriptors(IConfigurationElement[] elements) {
        List<PyEditorTextHoverDescriptor> result = new ArrayList<PyEditorTextHoverDescriptor>(elements.length);
        for (int i = 0; i < elements.length; i++) {
            IConfigurationElement element = elements[i];
            if (HOVER_TAG.equals(element.getName())) {
                PyEditorTextHoverDescriptor desc = new PyEditorTextHoverDescriptor(element);
                result.add(desc);
            }
        }

        return result.toArray(new PyEditorTextHoverDescriptor[result.size()]);
    }

    public static void initializeDefaultHoverPreferences(IConfigurationElement[] elements) {
        for (IConfigurationElement element : elements) {
            String id = element.getAttribute(PyEditorTextHoverDescriptor.ATT_PYDEV_HOVER_ID);

            //modifier
            PydevPrefs.getPreferenceStore().setDefault(
                    PyHoverPreferencesPage.KEY_TEXT_HOVER_MODIFIER + id, PyEditorTextHoverDescriptor.NO_MODIFIER);
            //state mask
            PydevPrefs.getPreferenceStore().setDefault(
                    PyHoverPreferencesPage.KEY_TEXT_HOVER_MODIFIER_MASK + id,
                    PyEditorTextHoverDescriptor.DEFAULT_MODIFIER_MASK);

            //preempt
            String attVal = element.getAttribute(PyEditorTextHoverDescriptor.ATT_PYDEV_HOVER_PREEMPT);
            PydevPlugin.getDefault().getPreferenceStore().setDefault(
                    PyHoverPreferencesPage.KEY_TEXT_HOVER_PREEMPT + id, Boolean.parseBoolean(attVal));

            //enable
            attVal = element.getAttribute(PyEditorTextHoverDescriptor.ATT_PYDEV_HOVER_ENABLE);
            PydevPlugin.getDefault().getPreferenceStore().setDefault(
                    PyHoverPreferencesPage.KEY_TEXT_HOVER_ENABLE + id, Boolean.parseBoolean(attVal));

            //priority
            attVal = element.getAttribute(PyEditorTextHoverDescriptor.ATT_PYDEV_HOVER_PRIORITY);
            int priority;
            try {
                priority = Integer.parseInt(attVal);
                priority = (priority >= HIGHEST_PRIORITY ? priority : HIGHEST_PRIORITY);
            } catch (NumberFormatException e) {
                priority = DEFAULT_HOVER_PRIORITY;
            }
            PydevPlugin.getDefault().getPreferenceStore().setDefault(
                    PyHoverPreferencesPage.KEY_TEXT_HOVER_PRIORITY + id, priority);
        }
    }

    public static void initializeHoversFromPreferences(PyEditorTextHoverDescriptor[] hovers) {
        for (int i = 0; i < hovers.length; i++) {
            //enable
            hovers[i].fIsEnabled = PydevPlugin.getDefault().getPreferenceStore()
                    .getBoolean(PyHoverPreferencesPage.KEY_TEXT_HOVER_ENABLE + hovers[i].getId());

            //preempt
            hovers[i].fPreempt = PydevPlugin.getDefault().getPreferenceStore()
                    .getBoolean(PyHoverPreferencesPage.KEY_TEXT_HOVER_PREEMPT + hovers[i].getId());

            //priority
            String sPriority = PydevPlugin.getDefault().getPreferenceStore()
                    .getString(PyHoverPreferencesPage.KEY_TEXT_HOVER_PRIORITY + hovers[i].getId());
            try {
                hovers[i].fPriority = Integer.parseInt(sPriority);
            } catch (NumberFormatException e) {
                hovers[i].fPriority = DEFAULT_HOVER_PRIORITY;
            }

            //modifier
            String modifierString = PydevPrefs.getPreferenceStore()
                    .getString(PyHoverPreferencesPage.KEY_TEXT_HOVER_MODIFIER + hovers[i].getId());
            if (modifierString == null || modifierString.equals(PyEditorTextHoverDescriptor.NO_MODIFIER)) {
                modifierString = ""; //$NON-NLS-1$
            }
            hovers[i].fModifierString = modifierString;

            //state mask
            hovers[i].fStateMask = PyEditorTextHoverDescriptor.computeStateMask(modifierString);
            if (hovers[i].fStateMask == -1) {
                // Fallback: use stored modifier masks
                try {
                    hovers[i].fStateMask = Integer.parseInt(PydevPrefs.getPreferenceStore().getString(
                            PyHoverPreferencesPage.KEY_TEXT_HOVER_MODIFIER_MASK + hovers[i].getId()));
                } catch (NumberFormatException ex) {
                    hovers[i].fStateMask = -1;
                }
                // Fix modifier string
                int stateMask = hovers[i].fStateMask;
                if (stateMask == -1) {
                    hovers[i].fModifierString = ""; //$NON-NLS-1$
                } else {
                    hovers[i].fModifierString = PyAction.getModifierString(stateMask);
                }
            }
        }
    }

    /**
     * Returns the configured modifier getStateMask for this hover.
     *
     * @return the hover modifier stateMask or -1 if no hover is configured
     */
    public int getStateMask() {
        return fStateMask;
    }

    /**
     * Returns the modifier String as set in the preference store.
     *
     * @return the modifier string
     */
    public String getModifierString() {
        return fModifierString;
    }

    /**
     * Returns whether this hover is enabled or not.
     *
     * @return <code>true</code> if enabled
     */
    public boolean isEnabled() {
        return fIsEnabled;
    }

    /**
     * Sets the hover's enabled state
     */
    public void setIsEnabled(boolean enabled) {
        fIsEnabled = enabled;
    }

    /**
     * Returns the hover's priority.
     *
     * @return the priority
     */
    public Integer getPriority() {
        return fPriority;
    }

    /**
     * Sets the hover's priority
     */
    public void setPriority(int priority) {
        fPriority = priority;
    }

    /**
     * Returns the hover's preempt setting.
     *
     * @return the preempt setting
     */
    public Boolean isPreempt() {
        return fPreempt;
    }

    /**
     * Sets the hover's preempt attribute
     */
    public void setIsPreempt(boolean preempt) {
        fPreempt = preempt;
    }

    /**
     * Returns this hover descriptors configuration element.
     *
     * @return the configuration element
     */
    public IConfigurationElement getConfigurationElement() {
        return fElement;
    }

}
