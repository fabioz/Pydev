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
    public Map subFolders = new HashMap();
    public Map files = new HashMap();
    public Object node;
}
