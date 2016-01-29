package org.python.pydev.utils;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IConfigurationElement;

/**
 * Allows to sort an array based on their elements' configuration elements
 * according to the natural sort order of the specified attribute.
 * <p>
 * This class may be subclassed.
 * </p>
 *
 */
public abstract class ConfigurationElementAttributeSorter {

    private String attName;

    /**
     * Sorts the given array based on its elements' configuration elements
     * according to the natural sort order of the specified attribute.
     *
     * @param elements the array to be sorted
     * @param attribute the name of the attribute on whose values the elements are to be sorted
     */
    public final void sort(Object[] elements, String attribute) {
        this.attName = attribute;
        Arrays.sort(elements, new ConfigurationElementAttributeComparator<Object>(elements, attribute));
    }

    /**
     * Returns the configuration element for the given object.
     *
     * @param object the object
     * @return the object's configuration element, must not be <code>null</code>
     */
    public abstract IConfigurationElement getConfigurationElement(Object object);

    /**
     * Compare configuration elements according to the prerequisite relation
     * of their defining plug-ins.
     */
    private class ConfigurationElementAttributeComparator<O> implements Comparator<O> {

        public ConfigurationElementAttributeComparator(O[] elements, String attribute) {
            Assert.isNotNull(elements);
        }

        /*
         * @see Comparator#compare(java.lang.Object, java.lang.Object)
         * @since 2.0
         */
        public int compare(O object0, O object1) {

            IConfigurationElement e0 = getConfigurationElement(object0);
            IConfigurationElement e1 = getConfigurationElement(object1);
            if (e0 != null && e1 != null) {
                String e0String = e0.getAttribute(attName);
                String e1String = e1.getAttribute(attName);

                if (e0String != null && e1String != null) {

                    Integer val0 = null;
                    Integer val1 = null;

                    try {
                        val0 = Integer.parseInt(e0String);
                        val1 = Integer.parseInt(e1String);
                    } catch (NumberFormatException e) {
                        return e0String.compareTo(e1String);
                    }

                    if (val0 != null && val1 != null) {
                        return val0.compareTo(val1);
                    }
                }
            }

            return 0;
        }
    }
}