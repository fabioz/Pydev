/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.navigator.elements.ISortedElement;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.UIConstants;

/**
 * This class represents a file or folder that's inside a zip file.
 */
public class PythonpathZipChildTreeNode extends TreeNode<LabelAndImage>implements ISortedElement {

    /**
     * Identifies whether we already calculated the children
     */
    private boolean calculated = false;

    /**
     * Is this a file for a directory?
     */
    public final boolean isDir;

    /**
     * Helper structure to get data from the zip
     */
    public final ZipStructure zipStructure;

    /**
     * The path inside the zip for this node
     */
    public final String zipPath;

    /**
     * Marks whether this is a python package (has __init__) or not.
     */
    private boolean isPackage;

    /**
     * If this is a dir, these are the contents from this dir.
     */
    private List<String> dirContents;

    /**
     * @param zipStructure helper to deal with zip
     * @param zipPath the path in the zip for this node
     * @param icon if not provided, it'll be calculated
     * @param isPythonpathRoot identifies whether we're directly in the root of the zip file
     */
    public PythonpathZipChildTreeNode(TreeNode<LabelAndImage> parent, ZipStructure zipStructure, String zipPath,
            Image icon, boolean isPythonpathRoot) {
        super(parent, null); //data will be set later
        try {
            this.zipStructure = zipStructure;
            this.zipPath = zipPath;
            this.isDir = StringUtils.endsWith(zipPath, '/');
            if (isDir) {
                dirContents = zipStructure.contents(zipPath);
                //This one can only be a package if its parent is a root or if it's also a package.
                if (isPythonpathRoot) {
                    isPackage = true;

                } else if (parent instanceof PythonpathZipChildTreeNode
                        && ((PythonpathZipChildTreeNode) parent).isPackage) {
                    for (String s : dirContents) {
                        if (PythonPathHelper.isValidInitFile(s)) {
                            isPackage = true;
                            break;
                        }
                    }

                }
            }

            //Update the icon if it wasn't received.
            if (icon == null) {
                ImageCache imageCache = PydevPlugin.getImageCache();
                if (isDir) {
                    if (isPackage) {
                        icon = imageCache.get(UIConstants.FOLDER_PACKAGE_ICON);
                    } else {
                        icon = imageCache.get(UIConstants.FOLDER_ICON);
                    }
                } else {
                    if (PythonPathHelper.isValidSourceFile(zipPath)) {
                        icon = imageCache.get(UIConstants.PY_FILE_ICON);
                    } else {
                        icon = imageCache.get(UIConstants.FILE_ICON);
                    }
                }
            }
        } finally {
            setData(new LabelAndImage(getLabel(zipPath), icon));
        }
    }

    /**
     * @return the label for the passed zip path.
     *
     * E.g.:
     * For /dir/foo/file.py, this will return 'file.py'
     * For /dir/foo/dir2/, this will return 'dir2'
     */
    private static String getLabel(String zipPath) {
        if (StringUtils.endsWith(zipPath, '/')) {
            zipPath = zipPath.substring(0, zipPath.length() - 1); //remove last char
        }

        int lastIndexOf = zipPath.lastIndexOf('/');
        if (lastIndexOf == -1) {
            return zipPath;
        } else {
            return zipPath.substring(lastIndexOf + 1);
        }
    }

    @Override
    public boolean hasChildren() {
        return isDir && dirContents != null && dirContents.size() > 0;
    }

    public int getRank() {
        return isDir ? ISortedElement.RANK_PYTHON_FOLDER : ISortedElement.RANK_PYTHON_FILE;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public synchronized List<TreeNode/*LabelAndImage*/> getChildren() {
        if (!calculated) {
            this.calculated = true;
            if (isDir && dirContents != null) {
                for (String childPath : dirContents) {
                    new PythonpathZipChildTreeNode(this, zipStructure, childPath, null, false);
                }
            }
        }
        return super.getChildren();
    }

}
