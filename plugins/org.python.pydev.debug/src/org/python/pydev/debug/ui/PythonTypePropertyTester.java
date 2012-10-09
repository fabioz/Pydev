package org.python.pydev.debug.ui;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.navigator.elements.IWrappedResource;

public class PythonTypePropertyTester extends PropertyTester {

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        IFile iFile = getIFile(receiver);
        if (iFile != null) {
            if (PythonPathHelper.markAsPyDevFileIfDetected(iFile)) {
                return true;
            }
        }
        return false;
    }

    private IFile getIFile(Object receiver) {
        if (receiver instanceof IWrappedResource) {
            IWrappedResource wrappedResource = (IWrappedResource) receiver;
            Object actualObject = wrappedResource.getActualObject();
            if (actualObject instanceof IFile) {
                return (IFile) actualObject;
            }
        }
        if (receiver instanceof IAdaptable) {
            IAdaptable iAdaptable = (IAdaptable) receiver;
            return (IFile) iAdaptable.getAdapter(IFile.class);
        }
        return null;
    }

}
