/*
 * Created on Oct 14, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


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

    public Map folders = new HashMap();
    public Map files = new HashMap();
    
    /**
     * 
     * @param node
     */
    public void addFolder(Object node) {
        FolderNode c = new FolderNode();
        c.node = node;
        folders.put(node, c);
    }

    /**
     * 
     * @param node
     * @param parent
     */
    public void addFolder(Object node, Object parent) {
        FolderNode parentNode = (FolderNode) folders.get(parent);
        
        FolderNode newNode = new FolderNode();
        newNode.node = node;
        if(parentNode == null){
            throw new RuntimeException("The folder being added didn't have its parent found.");
        }
        
        parentNode.subFolders.put(node, newNode);
        folders.put(node, newNode);
    }

    /**
     * 
     * @param node
     * @param parent
     * @param stmts
     * @param exec
     * @param notExecuted
     */
    public void addFile(Object node, Object parent, int stmts, int exec, String notExecuted) {
        FolderNode folderNode = (FolderNode) folders.get(parent);
        
        if (folderNode == null){
            throw new RuntimeException("A file node MUST have a related folder node.");
        }
        
        FileNode fileNode = new FileNode();
        fileNode.exec = exec;
        fileNode.node = node;
        fileNode.notExecuted = notExecuted;
        fileNode.stmts = stmts;
        
        folderNode.files.put(node, fileNode);
        files.put(node, fileNode);
    }

    public List getFiles(Object node){
        FolderNode folderNode = (FolderNode) folders.get(node);
        if (folderNode == null){
            FileNode fileNode = (FileNode) files.get(node);
            if (fileNode == null){
                throw new RuntimeException("The node has not been found: "+node.toString());
            }
            ArrayList list = new ArrayList();
            list.add(fileNode);
            return list;
        }
        
        
        //we have a folder node.
        ArrayList list = new ArrayList();
        recursivelyFillList(folderNode, list);
        return list;
    }
    
    /**
     * @param folderNode
     * @param list
     */
    private void recursivelyFillList(FolderNode folderNode, ArrayList list) {
        //add its files
        for (Iterator it = folderNode.files.values().iterator(); it.hasNext();) {
            list.add(it.next());
        }
        
        //get its sub folders
        for (Iterator it = folderNode.subFolders.values().iterator(); it.hasNext();) {
            recursivelyFillList((FolderNode) it.next(), list);
        }
    }

    
    /**
     * 
     * @param node
     * @return an Object such that the positions contain:
     * 0 - string representing the data received, such as:
     * 
     *  Name            Stmts   Exec  Cover   Missing
     *  ---------------------------------------------
     *  file_to_test        7      6    85%   8
     *  file_to_test2      13      9    69%   12-14, 17
     *  ---------------------------------------------
     *  TOTAL              20     15    75%
     * 
     */
    public String getStatistics(Object node) {
        

        List list = getFiles(node);  //array of FileNode
        
        StringBuffer buffer = new StringBuffer();
        //40 chars for name.
        buffer.append("Name                                    Stmts     Exec     Cover  Missing\n");
        buffer.append("-----------------------------------------------------------------------------\n");
        
        int totalExecuted = 0;
        int totalStmts = 0;
        
        for (Iterator it = list.iterator(); it.hasNext();) {
            FileNode element = (FileNode) it.next();
            buffer.append(element.toString()+"\n");
            totalExecuted += element.exec;
            totalStmts += element.stmts;
        }
        
        buffer.append("-----------------------------------------------------------------------------\n");
        buffer.append(FileNode.toString("TOTAL",totalStmts, totalExecuted, "")+"\n");
        
        return buffer.toString();
    }
    


}
