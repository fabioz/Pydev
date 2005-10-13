/*
 * Created on 12/10/2005
 */
package org.python.pydev.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.python.pydev.core.log.Log;

/**
 * This class can be used to work on deltas. It is able to save and restore data on a 'delta' fashion.
 * 
 * It is supposed to be used in the following way:
 * 
 * When a class needs to save a lot of data in the disk and this data is gathered in deltas, it only does the first large
 * processing and then uses this class to keep track of other changes (and from time to time, reorganizes all the data and
 * erases the deltas).
 * 
 * This object is supposed to be used by another that knows what is the data being added and how to restore that data.
 * Also, the order in which the deltas are generated is important.
 * 
 * @author Fabio
 */
public class DeltaSaver {
    
    /**
     * Superclass of all commands
     * 
     * @author Fabio
     */
    public abstract static class DeltaCommand implements Serializable{

        public Serializable data;

        public DeltaCommand(Serializable o) {
            this.data = o;
        }

        public abstract void processWith(IDeltaProcessor deltaProcessor);
        
    }

    /**
     * Delete command
     * 
     * @author Fabio
     */
    public static class DeltaDeleteCommand extends DeltaCommand {

        private static final long serialVersionUID = 1;

        public DeltaDeleteCommand(Serializable o) {
            super(o);
        }

        public void processWith(IDeltaProcessor deltaProcessor){
            deltaProcessor.processDelete(data);
        }
    }

    
    /**
     * Insert command
     * 
     * @author Fabio
     */
    public static class DeltaInsertCommand extends DeltaCommand{

        private static final long serialVersionUID = 1;

        public DeltaInsertCommand(Serializable o) {
            super(o);
        }
        
        public void processWith(IDeltaProcessor deltaProcessor){
            deltaProcessor.processInsert(data);
        }
    }
    
    
    /**
     * Update command
     * 
     * @author Fabio
     */
    public static class DeltaUpdateCommand extends DeltaCommand{
        
        private static final long serialVersionUID = 1;
        
        public DeltaUpdateCommand(Serializable o) {
            super(o);
        }
        
        public void processWith(IDeltaProcessor deltaProcessor){
            deltaProcessor.processUpdate(data);
        }
    }

    /**
     * Directory where the deltas should be saved / restored.
     */
    private File dirToSaveDeltas;
    
    /**
     * This is equal to '.'+extension
     */
    private String suffix;

    /**
     * List of commands
     */
    private List<DeltaCommand> commands;
    
    /**
     * Used to keep track of a number to use to save the command
     */
    private int nCommands;
    
    /**
     * @param dirToSaveDeltas this is the directory where the deltas should be saved
     * @param extension this is the extension that should be given to the deltas
     */
    public DeltaSaver(File dirToSaveDeltas, String extension) {
        this.dirToSaveDeltas = dirToSaveDeltas;
        this.suffix = "."+extension;
        this.commands = new ArrayList<DeltaCommand>();
        validateDir();
        loadDeltas();
    }


    /**
     * Checks if the dir is correct
     */
    private void validateDir() {
        if(this.dirToSaveDeltas.exists() == false){
            throw new RuntimeException("The path passed to save / restore deltas does not exist ("+dirToSaveDeltas+")");
        }
        if(this.dirToSaveDeltas.isDirectory() == false){
            throw new RuntimeException("The path passed to save / restore deltas is not actually a directory ("+dirToSaveDeltas+")");
        }
    }

    /**
     * Gets existing deltas in the disk
     */
    private void loadDeltas() {
        ArrayList<File> deltasFound = findDeltas();
        for (File file : deltasFound) {
            try {
                DeltaCommand cmd = (DeltaCommand) IOUtils.readFromFile(file);
                addRestoredCommand(cmd);
            } catch (Exception e) {
                Log.log(e);
            }
        }
        
    }

    /**
     * @return a list of files with all the deltas in the dir we are acting upon
     */
    private ArrayList<File> findDeltas() {
        ArrayList<File> deltasFound = new ArrayList<File>();
        File[] files = this.dirToSaveDeltas.listFiles();
        for (File file : files) {
            if(file.exists() && file.isFile() && file.getName().endsWith(suffix)){
                deltasFound.add(file);
            }
        }
        //also, sort by the name (which must be an integer)
        Collections.sort(deltasFound, new Comparator<File>(){

            public int compare(File o1, File o2) {
                String i = FullRepIterable.headAndTail(o1.getName())[0];
                String j = FullRepIterable.headAndTail(o2.getName())[0];
                return new Integer(i).compareTo(new Integer(j));
            }}
        );
        return deltasFound;
    }

    /**
     * Adds some command (adds to list and NOT to the disk)
     * 
     * @param command the command found in the disk
     */
    private void addRestoredCommand(DeltaCommand command) {
        this.commands.add(command);
    }
    
    /**
     * Adds some command (adds to list and to the disk)
     * 
     * @param command the command to be added
     */
    public void addCommand(DeltaCommand command) {
        File file = new File(this.dirToSaveDeltas, nCommands+suffix);
        nCommands++;
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        REF.writeToFile(command, file);
        this.commands.add(command);
    }

    /**
     * @return the number of available deltas
     */
    public int availableDeltas() {
        return this.commands.size();
    }

    /**
     * Clears all deltas in the disk (and in memory... also restarts numbering the deltas)
     */
    public void clearAll() {
        ArrayList<File> deltas = findDeltas();
        for (File file : deltas) {
            file.delete();
        }
        this.commands.clear();
        nCommands = 0;
    }

    public void addInsertCommand(Serializable o) {
        addCommand(new DeltaInsertCommand(o));
    }

    public void addDeleteCommand(Serializable o) {
        addCommand(new DeltaDeleteCommand(o));
    }

    public void addUpdateCommand(Serializable o) {
        addCommand(new DeltaUpdateCommand(o));
    }

    /**
     * Passes the current deltas to the delta processor.
     */
    public void processDeltas(IDeltaProcessor deltaProcessor) {
        for (DeltaCommand cmd : this.commands) {
            cmd.processWith(deltaProcessor);
        }
        deltaProcessor.endProcessing();
        this.clearAll();
    }

}



class IOUtils {
    /**
     * @param astOutputFile
     * @return
     */
    public static Object readFromFile(File astOutputFile) {
        try {
            InputStream input = new FileInputStream(astOutputFile);
            ObjectInputStream in = new ObjectInputStream(input);
            Object o = in.readObject();
            in.close();
            input.close();
            return o;
        } catch (Exception e) {
            Log.log(e);
            return null;
        }
    }

}