/*
 * Created on Oct 15, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Fabio Zadrozny
 */
public class FolderNode {
    public Map<Object, Object> subFolders = new HashMap<Object, Object>();
    public Map<Object, Object> files = new HashMap<Object, Object>();
    public Object node;
}
