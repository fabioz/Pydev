package org.python.pydev.debug.referrers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.PyVariable;
import org.python.pydev.debug.model.XMLUtils.XMLToReferrersInfo;

public class ReferrersViewContentProvider implements ITreeContentProvider {

    @Override
    public boolean hasChildren(Object element) {
        try {
            if (element instanceof XMLToReferrersInfo) {
                return true;
            }
            if (element instanceof IVariable) {
                Object[] objects = childrenCache.get(element);
                if (objects != null && objects.length > 0) {
                    return true;
                }
                IVariable iVariable = (IVariable) element;
                return iVariable.getValue().hasVariables();
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return false;

    }

    @Override
    public void dispose() {
        childrenCache.clear();
        parentCache.clear();
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        childrenCache.clear();
        parentCache.clear();
    }

    @Override
    public Object[] getElements(Object inputElement) {
        XMLToReferrersInfo[] arr = (XMLToReferrersInfo[]) inputElement;
        Object[] ret = new Object[arr.length];
        int len = arr.length;
        for (int i = 0; i < len; i++) {
            ret[i] = getChildren(arr[i])[0];
        }
        return ret;
    }

    private final Map<Object, Object[]> childrenCache = new HashMap<>();
    private final Map<Object, Object> parentCache = new HashMap<>();

    @Override
    public Object[] getChildren(Object element) {
        Object[] inCache = childrenCache.get(element);
        if (inCache != null) {
            return inCache;
        }
        if (element != null) {
            try {
                if (element instanceof XMLToReferrersInfo) {
                    XMLToReferrersInfo xmlToReferrersInfo = (XMLToReferrersInfo) element;

                    //Set that the for is the direct child of our root.
                    childrenCache.put(element, new Object[] { xmlToReferrersInfo.forVar });
                    parentCache.put(xmlToReferrersInfo.forVar, element);

                    //Add forVar children (and set them as parents).
                    childrenCache.put(xmlToReferrersInfo.forVar, xmlToReferrersInfo.vars);

                    PyVariable[] vars = xmlToReferrersInfo.vars;
                    for (PyVariable pyVariable : vars) {
                        parentCache.put(pyVariable, xmlToReferrersInfo.forVar);
                    }

                } else if (element instanceof IVariable) {
                    IVariable parentVariable = (IVariable) element;
                    IVariable[] childrenVariables = parentVariable.getValue().getVariables();
                    for (IVariable childVariable : childrenVariables) {
                        parentCache.put(childVariable, parentVariable);
                    }
                    childrenCache.put(parentVariable, childrenVariables);

                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
        Object[] objects = childrenCache.get(element);
        if (objects == null) {
            Log.log("Children of: " + element + " is null");
            objects = new Object[0];
        }
        return objects;
    }

    @Override
    public Object getParent(Object element) {
        return parentCache.get(element);
    }
}
