/*
 * Created on Oct 14, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.python.pydev.core.structure.FastStringBuffer;


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

    public Map<Object, Object> folders = new HashMap<Object, Object>();
    public Map<Object, Object> files = new HashMap<Object, Object>();
    
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
        FolderNode parentNode = (FolderNode) getFolder(parent);
        
        FolderNode newNode = new FolderNode();
        newNode.node = node;
        if(parentNode == null){
            throw new RuntimeException("The folder being added:"+node.toString()+" didn't have its parent found.");
        }
        
        parentNode.subFolders.put(node, newNode);
        folders.put(node, newNode);
    }

    public Object getFolder(Object obj){
        return getIt(obj,folders);
    }
    
    public Object getFile(Object obj){
        return getIt(obj,files);
    }
    
    /**
     * @param obj
     * @return
     */
    private Object getIt(Object obj, Map<Object, Object> m) {
        Object object = m.get(obj);
        if (object == null){
            for (Iterator<Object> iter = m.keySet().iterator(); iter.hasNext();) {
                Object element = iter.next();
                if(element.equals(obj)){
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
     * @param exec
     * @param notExecuted
     */
    public void addFile(Object node, Object parent, int stmts, int exec, String notExecuted) {
        FolderNode folderNode = (FolderNode) getFolder(parent);
        
        if (folderNode == null){
            throw new RuntimeException("A file node ("+node.toString()+")MUST have a related folder node.");
        }
        
        FileNode fileNode = new FileNode();
        fileNode.exec = exec;
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
     * @param exec
     * @param notExecuted
     */
    public void addFile(Object node, Object parent, String desc) {
        FolderNode folderNode = (FolderNode) getFolder(parent);
        
        if (folderNode == null){
            throw new RuntimeException("A file node ("+node.toString()+")MUST have a related folder node.");
        }
        
        ErrorFileNode fileNode = new ErrorFileNode();
        fileNode.node = node;
        fileNode.desc = desc;
        
        folderNode.files.put(node, fileNode);
        files.put(node, fileNode);
    }

    public List<Object> getFiles(Object node) throws NodeNotFoudException{
        FolderNode folderNode = (FolderNode) getFolder(node);
        if (folderNode == null){
            Object fileNode = getFile(node);
            if (fileNode == null){
                throw new NodeNotFoudException("The node has not been found: "+node.toString());
            }
            ArrayList<Object> list = new ArrayList<Object>();
            list.add(fileNode);
            return list;
        }
        
        
        //we have a folder node.
        ArrayList<Object> list = new ArrayList<Object>();
        recursivelyFillList(folderNode, list);
        return list;
    }
    
    /**
     * @param folderNode
     * @param list
     */
    private void recursivelyFillList(FolderNode folderNode, ArrayList<Object> list) {
        list.addAll(sortCollectionWithToString(folderNode.files.values()));
        
        //get its sub folders
        for (Iterator<Object> it = sortCollectionWithToString(folderNode.subFolders.values()).iterator(); it.hasNext();) {
            recursivelyFillList((FolderNode) it.next(), list);
        }
    }

    private List<Object> sortCollectionWithToString(Collection<Object> collection) {
        List<Object> vals = new ArrayList<Object>(collection);
        Collections.sort(vals, new Comparator<Object>(){

            public int compare(Object o1, Object o2) {
                return o1.toString().compareTo(o2.toString());
            }}
        );
        return vals;
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
        

        FastStringBuffer buffer = new FastStringBuffer();
        try {
            List<Object> list = getFiles(node);  //array of FileNode
            
            //40 chars for name.
            buffer.append("Name                                    Stmts     Exec     Cover  Missing\n");
            buffer.append("-----------------------------------------------------------------------------\n");
            
            int totalExecuted = 0;
            int totalStmts = 0;
            
            for (Object element:list) {
                buffer.append(element.toString()).append("\n");
                if(element instanceof FileNode){ //it may have been an error node...
                    totalExecuted += ((FileNode)element).exec;
                    totalStmts += ((FileNode)element).stmts;
                }
            }
            
            buffer.append("-----------------------------------------------------------------------------\n");
            buffer.append(FileNode.toString("TOTAL",totalStmts, totalExecuted, "")).append("\n");
            
        } catch (NodeNotFoudException e) {
            buffer.append("File has no statistics.");
        }
        return buffer.toString();
    }

    /**
     * 
     */
    public void clear() {
        folders.clear();
        files.clear();
        
    }
    


}
