package org.python.pydev.navigator.properties;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.navigator.elements.PythonNode;

public class PyPropertyTester extends PropertyTester{

    /**
     * Converts the given expected value to a <code>String</code>.
     * 
     * @param expectedValue
     *            the expected value (may be <code>null</code>).
     * @return the empty string if the expected value is <code>null</code>,
     *         otherwise the <code>toString()</code> representation of the
     *         expected value
     */
    protected String toString(Object expectedValue) {
        return expectedValue == null ? "" : expectedValue.toString(); //$NON-NLS-1$
    }

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if(receiver instanceof IWrappedResource){
            if(receiver instanceof PythonNode){
                return false;//do not filter python nodes.
            }
            IWrappedResource resource = (IWrappedResource) receiver;
            IResource res = (IResource) resource.getAdapter(IResource.class);
            if(res != null){
                if (property.equals("name")) {
                    return new StringMatcherSimple(toString(expectedValue)).match(res.getName());
                }
            }            
        }
        return false;
    }

}
