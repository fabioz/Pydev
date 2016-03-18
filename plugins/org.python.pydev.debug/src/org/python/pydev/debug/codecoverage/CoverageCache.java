/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 14, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.callbacks.ICallbackWithListeners;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.tooltips.presenter.StyleRangeWithCustomData;

/**
 * 
 * The structure is as follows:
 * 
 * folders: contains a link to all the folder nodes.
 * files: contains a link to all the file nodes.
 * 
 * the folder contains a structure that allows us to get folder nodes that are below it. 
 * 
 * @author Fabio Zadrozny
 */
public class CoverageCache {

    public Map<File, ICoverageNode> folders = new HashMap<File, ICoverageNode>();
    public Map<File, ICoverageNode> files = new HashMap<File, ICoverageNode>();
    public static final ICallbackWithListeners<StyleRange> onStyleCreated = new CallbackWithListeners<StyleRange>();

    /**
     * 
     * @param node
     */
    public void addFolder(File node) {
        FolderNode c = new FolderNode();
        c.node = node;
        folders.put(node, c);
    }

    /**
     * 
     * @param node
     * @param parent
     */
    public void addFolder(File node, File parent) {
        FolderNode parentNode = (FolderNode) getFolder(parent);

        FolderNode newNode = new FolderNode();
        newNode.node = node;
        if (parentNode == null) {
            throw new RuntimeException("The folder being added:" + node.toString() + " didn't have its parent found.");
        }

        parentNode.subFolders.put(node, newNode);
        folders.put(node, newNode);
    }

    public FolderNode getFolder(File obj) {
        return (FolderNode) getIt(obj, folders);
    }

    public ICoverageNode getFile(File obj) {
        return getIt(obj, files);
    }

    /**
     * @param obj
     * @return
     */
    private ICoverageNode getIt(File obj, Map<File, ICoverageNode> m) {
        ICoverageNode object = m.get(obj);
        if (object == null) {
            for (Iterator<File> iter = m.keySet().iterator(); iter.hasNext();) {
                Object element = iter.next();
                if (element.equals(obj)) {
                    return m.get(element);
                }
            }
        }
        return object;
    }

    /**
     * 
     * @param node
     * @param parent
     * @param stmts
     * @param miss
     * @param notExecuted
     */
    public void addFile(File node, File parent, int stmts, int miss, String notExecuted) {
        FolderNode folderNode = (FolderNode) getFolder(parent);

        if (folderNode == null) {
            throw new RuntimeException("A file node (" + node.toString() + ")MUST have a related folder node.");
        }

        FileNode fileNode = new FileNode();
        fileNode.miss = miss;
        fileNode.node = node;
        fileNode.notExecuted = notExecuted;
        fileNode.stmts = stmts;

        folderNode.files.put(node, fileNode);
        files.put(node, fileNode);
    }

    /**
     * 
     * @param node
     * @param parent
     * @param stmts
     * @param miss
     * @param notExecuted
     */
    public void addFile(File node, File parent, String desc) {
        FolderNode folderNode = (FolderNode) getFolder(parent);

        if (folderNode == null) {
            throw new RuntimeException("A file node (" + node.toString() + ")MUST have a related folder node.");
        }

        ErrorFileNode fileNode = new ErrorFileNode();
        fileNode.node = node;
        fileNode.desc = desc;

        folderNode.files.put(node, fileNode);
        files.put(node, fileNode);
    }

    public List<ICoverageNode> getFiles(File node) throws NodeNotFoudException {
        FolderNode folderNode = (FolderNode) getFolder(node);
        if (folderNode == null) {
            ICoverageNode fileNode = getFile(node);
            if (fileNode == null) {
                throw new NodeNotFoudException("The node has not been found: " + node.toString());
            }
            ArrayList<ICoverageNode> list = new ArrayList<ICoverageNode>();
            list.add(fileNode);
            return list;
        }

        //we have a folder node.
        ArrayList<ICoverageNode> list = new ArrayList<ICoverageNode>();
        recursivelyFillList(folderNode, list);
        return list;
    }

    /**
     * @param folderNode
     * @param list
     */
    private void recursivelyFillList(FolderNode folderNode, ArrayList<ICoverageNode> list) {
        list.addAll(sortCollectionWithCoverageLeafNodes(folderNode.files.values()));

        //get its sub folders
        for (Iterator<ICoverageNode> it = sortCollectionWithToString(folderNode.subFolders.values()).iterator(); it
                .hasNext();) {
            recursivelyFillList((FolderNode) it.next(), list);
        }
    }

    private List<ICoverageLeafNode> sortCollectionWithCoverageLeafNodes(Collection<ICoverageLeafNode> collection) {
        List<ICoverageLeafNode> vals = new ArrayList<ICoverageLeafNode>(collection);
        Collections.sort(vals, new Comparator<Object>() {

            @Override
            public int compare(Object o1, Object o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
        return vals;
    }

    private List<ICoverageNode> sortCollectionWithToString(Collection<ICoverageNode> collection) {
        List<ICoverageNode> vals = new ArrayList<ICoverageNode>(collection);
        Collections.sort(vals, new Comparator<Object>() {

            @Override
            public int compare(Object o1, Object o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
        return vals;
    }

    /**
     * 
     * @param node
     * @return an Object such that the positions contain:
     * 0 - string representing the data received, such as:
     * 
     *  Name            Stmts   Miss  Cover   Missing
     *  ---------------------------------------------
     *  file_to_test        7      6    85%   8
     *  file_to_test2      13      9    69%   12-14, 17
     *  ---------------------------------------------
     *  TOTAL              20     15    75%
     * 
     */
    public Tuple<String, List<StyleRange>> getStatistics(String baseLocation, File node) {
        List<StyleRange> ranges = new ArrayList<StyleRange>();
        if (baseLocation == null) {
            baseLocation = "";
        }

        FastStringBuffer buffer = new FastStringBuffer();

        try {
            List<ICoverageNode> list = getFiles(node); //array of FileNode

            //40 chars for name.
            int nameNumberOfColumns = PyCoveragePreferences.getNameNumberOfColumns();
            buffer.append("Name").appendN(' ', nameNumberOfColumns - 4)
                    .append("  Stmts     Miss      Cover  Missing\n");
            buffer.appendN('-', nameNumberOfColumns);
            buffer.append("-------------------------------------\n");

            int totalMiss = 0;
            int totalStmts = 0;

            for (ICoverageNode element : list) {
                if (element instanceof FileNode) { //it may have been an error node...
                    FileNode fileNode = (FileNode) element;
                    int start = buffer.length();
                    fileNode.appendToBuffer(buffer, baseLocation, nameNumberOfColumns).append("\n");
                    int len = buffer.indexOf(' ', start) - start;
                    StyleRangeWithCustomData styleRange = new StyleRangeWithCustomData(start, len, null, null);
                    styleRange.underline = true;
                    try {
                        styleRange.underlineStyle = SWT.UNDERLINE_LINK;
                    } catch (Throwable e) {
                        //Ignore (not available on earlier versions of eclipse)
                    }
                    onStyleCreated.call(styleRange);
                    ranges.add(styleRange);
                    styleRange.customData = element;

                    totalMiss += fileNode.miss;
                    totalStmts += fileNode.stmts;
                } else {
                    buffer.append(element.toString()).append("\n");
                }
            }

            buffer.appendN('-', nameNumberOfColumns);
            buffer.append("-------------------------------------\n");
            FileNode.appendToBuffer(buffer, "TOTAL", totalStmts, totalMiss, "", nameNumberOfColumns).append("\n");

        } catch (NodeNotFoudException e) {
            buffer.append("File has no statistics.");
        }
        return new Tuple<String, List<StyleRange>>(buffer.toString(), ranges);
    }

    /**
     * 
     */
    public void clear() {
        folders.clear();
        files.clear();

    }

}
