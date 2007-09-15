package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class contains all the information we need from the folders beneath the pythonpath.
 * 
 * @author Fabio
 */
public class ModulesFoundStructure {

    /**
     * Inner class to contain what we found within a zip file.
     * 
     * @author Fabio
     */
    public static class ZipContents{
        
        public ZipContents(File zipFile) {
            this.zipFile = zipFile;
        }

        /**
         * May be any zip file (.zip, .jar, .egg, etc) 
         */
        public File zipFile;
        
        /**
         * These are the modules found within the zip file.
         * 
         * If it is a jar file, those are the dirs that contain clasess.
         * If it is a zip file with .py files, those are the actual .py files.
         */
        public Set<String> foundModules = new HashSet<String>();
    }
    
    
    /**
     * Contains: file found -> module name it was found with.
     */
    public Map<File, String> regularModules = new HashMap<File, String>();
    
    /**
     * For each zip, there should be one entry.
     */
    public List<ZipContents> zipContents = new ArrayList<ZipContents>();

}
