/*
 * Created on 12/10/2005
 */
package org.python.pydev.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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
public class DeltaSaver<X> {
    
    /**
     * Superclass of all commands
     * 
     * @author Fabio
     */
    public abstract static class DeltaCommand implements Serializable{

        public transient Object data;

        public DeltaCommand(Object o) {
            this.data = o;
        }

        public abstract void processWith(IDeltaProcessor deltaProcessor);
        
        public void readData(ICallback<Object, ObjectInputStream> readFromFileMethod, ObjectInputStream in) {
            this.data = readFromFileMethod.call(in);
        }

    }

    /**
     * Delete command
     * 
     * @author Fabio
     */
    public static class DeltaDeleteCommand extends DeltaCommand {

        private static final long serialVersionUID = 1;

        public DeltaDeleteCommand(Object o) {
            super(o);
        }

        @SuppressWarnings("unchecked")
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

        public DeltaInsertCommand(Object o) {
            super(o);
        }
        
        @SuppressWarnings("unchecked")
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
        
        public DeltaUpdateCommand(Object o) {
            super(o);
        }
        
        @SuppressWarnings("unchecked")
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
     * This is the method that should read the data in the delta from a file... This is because of the &*(^%EO way eclipse handles this kind of stuff,
     * so, we can't just serialize it from another plugin.
     */
    private ICallback<Object, ObjectInputStream> readFromFileMethod;
    
    /**
     * @param dirToSaveDeltas this is the directory where the deltas should be saved
     * @param extension this is the extension that should be given to the deltas
     */
    public DeltaSaver(File dirToSaveDeltas, String extension, ICallback<Object, ObjectInputStream> readFromFileMethod) {
        this.dirToSaveDeltas = dirToSaveDeltas;
        this.suffix = "."+extension;
        this.commands = Collections.synchronizedList(new ArrayList<DeltaCommand>());
        this.readFromFileMethod = readFromFileMethod;
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
    	synchronized(this.commands){
	        ArrayList<File> deltasFound = findDeltas();
	        for (File file : deltasFound) {
	            try {
	                DeltaCommand cmd = (DeltaCommand) IOUtils.readFromFile(file, this.readFromFileMethod);
	                if(cmd != null && cmd.data != null){
	                	addRestoredCommand(cmd);
	                }
	            } catch (Exception e) {
	                //Log.log(e);
	            }
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
    	synchronized(this.commands){
	        File file = new File(this.dirToSaveDeltas, nCommands+suffix);
	        nCommands++;
	        try {
	            file.createNewFile();
	        } catch (IOException e) {
	            throw new RuntimeException(e);
	        }
	        //always write the command and its data separately
	        IOUtils.writeToFile(command, command.data, file);
	        this.commands.add(command);
    	}
    }

    /**
     * @return the number of available deltas
     */
    public int availableDeltas() {
    	synchronized(this.commands){
    		return this.commands.size();
    	}
    }

    /**
     * Clears all deltas in the disk (and in memory... also restarts numbering the deltas)
     */
    public void clearAll() {
        ArrayList<File> deltas = findDeltas();
        for (File file : deltas) {
        	if(file.exists()){
        		file.delete();
        	}
        }
        this.commands.clear();
        nCommands = 0;
    }

    public void addInsertCommand(X o) {
        addCommand(new DeltaInsertCommand(o));
    }

    public void addDeleteCommand(X o) {
        addCommand(new DeltaDeleteCommand(o));
    }

    public void addUpdateCommand(X o) {
        addCommand(new DeltaUpdateCommand(o));
    }

    /**
     * Passes the current deltas to the delta processor.
     */
    public void processDeltas(IDeltaProcessor<X> deltaProcessor) {
    	synchronized(this.commands){
			boolean processed = false;
	        for (DeltaCommand cmd : this.commands) {
	            try {
					cmd.processWith(deltaProcessor);
					processed = false;
				} catch (Exception e) {
					Log.log(e);
				}
	        }
	        if(processed){
	        	//if nothing happened, we don't end the processing (no need to do it)
	        	deltaProcessor.endProcessing();
	        }
	        this.clearAll();
    	}
    }

}



class IOUtils {
    public static void writeToFile(Object o1, Object o2, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            try {
                ObjectOutputStream stream = new ObjectOutputStream(out);
                stream.writeObject(o1);
                stream.writeObject(o2);
                stream.close();
            } catch (Exception e) {
                Log.log(e);
                throw new RuntimeException(e);
            } finally{
                out.close();
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    /**
     * @param astOutputFile
     * @param readFromFileMethod 
     * @return
     */
    public static Object readFromFile(File astOutputFile, ICallback<Object, ObjectInputStream> readFromFileMethod) {
        try {
        	boolean deletFile = false;
        	//the file is not even there
        	if(!astOutputFile.exists()){
        		return null;
        	}
            InputStream input = new FileInputStream(astOutputFile);
            ObjectInputStream in = new ObjectInputStream(input);
            DeltaSaver.DeltaCommand o = null;
            try {
				o = (DeltaSaver.DeltaCommand) in.readObject();
				o.readData(readFromFileMethod, in);
            } catch (Exception e) {
            	//the format has changed (no real problem here... just erase the file)
            	deletFile = true;
            	o = null;
			} finally {
				in.close();
				input.close();
			}
			if(deletFile){
				if(astOutputFile.exists()){
					astOutputFile.delete();
				}
			}
            return o;
        } catch (Exception e) {
            Log.log(e); 
            return null;
        }
    }

}