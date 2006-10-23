/*
 * Created on Oct 19, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.python.pydev.core.FullRepIterable;

public class ModelAdapter implements IAdapterFactory{

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        IAdaptable adapter = (IAdaptable) adaptableObject;
        Object ret = adapter.getAdapter(adapterType);
        //System.out.println("ModelAdapter adaptableObject="+FullRepIterable.getLastPart(adaptableObject.getClass().getName())+
        //        " adapterType="+FullRepIterable.getLastPart(adapterType.getName())+" ret="+ret);
        return ret;
    }

    public Class[] getAdapterList() {
        return new Class[]{IWrappedResource.class};
    }

}
