/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 12/10/2005
 */
package org.python.pydev.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;

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
    public abstract class DeltaCommand {

        public final X data;

        public DeltaCommand(X o) {
            this.data = o;
        }

        public abstract void processWith(IDeltaProcessor<X> deltaProcessor);

        /**
         * @return String with 3 letters describing the command (written to disk and used to restore it later on).
         */
        public abstract String getCommandFileDesc();

    }

    /**
     * Delete command
     * 
     * @author Fabio
     */
    public class DeltaDeleteCommand extends DeltaCommand {

        public DeltaDeleteCommand(X o) {
            super(o);
        }

        public void processWith(IDeltaProcessor<X> deltaProcessor) {
            deltaProcessor.processDelete(data);
        }

        public String getCommandFileDesc() {
            return "DEL";
        }

    }

    /**
     * Insert command
     * 
     * @author Fabio
     */
    public class DeltaInsertCommand extends DeltaCommand {

        public DeltaInsertCommand(X o) {
            super(o);
        }

        public void processWith(IDeltaProcessor<X> deltaProcessor) {
            deltaProcessor.processInsert(data);
        }

        public String getCommandFileDesc() {
            return "INS";
        }
    }

    /**
     * Update command
     * 
     * @author Fabio
     */
    public class DeltaUpdateCommand extends DeltaCommand {

        public DeltaUpdateCommand(X o) {
            super(o);
        }

        public void processWith(IDeltaProcessor<X> deltaProcessor) {
            deltaProcessor.processUpdate(data);
        }

        public String getCommandFileDesc() {
            return "UPD";
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
     * This is the method that should read the data in the delta from a file...
     */
    private ICallback<X, String> readFromFileMethod;

    /**
     * Convert the object to a representation to be put on the disk (readFromFileMethod will be used with the same
     * data passed here later on... if it cannot be read, null should be returned).
     */
    private ICallback<String, X> toFileMethod;

    /**
     * @param dirToSaveDeltas this is the directory where the deltas should be saved
     * @param extension this is the extension that should be given to the deltas
     */
    public DeltaSaver(File dirToSaveDeltas, String extension, ICallback<X, String> readFromFileMethod,
            ICallback<String, X> toFileMethod) {
        this.dirToSaveDeltas = dirToSaveDeltas;
        this.suffix = "." + extension;
        this.commands = Collections.synchronizedList(new ArrayList<DeltaCommand>());
        this.readFromFileMethod = readFromFileMethod;
        this.toFileMethod = toFileMethod;
        validateDir();
        loadDeltas();
    }

    /**
     * Checks if the dir is correct
     */
    private void validateDir() {
        if (this.dirToSaveDeltas.exists() == false) {
            throw new RuntimeException("The path passed to save / restore deltas does not exist (" + dirToSaveDeltas
                    + ")");
        }
        if (this.dirToSaveDeltas.isDirectory() == false) {
            throw new RuntimeException("The path passed to save / restore deltas is not actually a directory ("
                    + dirToSaveDeltas + ")");
        }
    }

    /**
     * Gets existing deltas in the disk
     */
    private void loadDeltas() {
        synchronized (this.commands) {
            ArrayList<File> deltasFound = findDeltas();
            for (File file : deltasFound) {
                try {
                    @SuppressWarnings("unchecked")
                    DeltaCommand cmd = (DeltaCommand) readFromFile(file, this.readFromFileMethod);
                    if (cmd != null && cmd.data != null) {
                        addRestoredCommand(cmd);
                    }
                } catch (Exception e) {
                    Log.log(e);
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
        if (files != null) {
            for (File file : files) {
                if (file.exists() && file.isFile() && file.getName().endsWith(suffix)) {
                    deltasFound.add(file);
                }
            }
        }
        //also, sort by the name (which must be an integer)
        Collections.sort(deltasFound, new Comparator<File>() {

            public int compare(File o1, File o2) {
                String i = FullRepIterable.headAndTail(o1.getName())[0];
                String j = FullRepIterable.headAndTail(o2.getName())[0];
                return new Integer(i).compareTo(new Integer(j));
            }
        });
        return deltasFound;
    }

    /**
     * Adds some command (adds to list and NOT to the disk)
     * 
     * @param command the command found in the disk
     */
    private void addRestoredCommand(DeltaCommand command) {
        synchronized (this.commands) {
            this.commands.add(command);
        }
    }

    /**
     * Adds some command (adds to list and to the disk)
     * 
     * @param command the command to be added
     */
    public void addCommand(DeltaCommand command) {
        synchronized (this.commands) {
            File file = new File(this.dirToSaveDeltas, nCommands + suffix);
            nCommands++;
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //always write the command and its data separately
            String write = toFileMethod.call((X) command.data);
            if (write == null) {
                Log.log("Null returned to write from data: " + command.data);
            } else {
                writeToFile(command, write, file);
                this.commands.add(command);
            }
        }
    }

    /**
     * @return the number of available deltas
     */
    public int availableDeltas() {
        synchronized (this.commands) {
            return this.commands.size();
        }
    }

    /**
     * Clears all deltas in the disk (and in memory... also restarts numbering the deltas)
     */
    public void clearAll() {
        synchronized (this.commands) {
            ArrayList<File> deltas = findDeltas();
            for (File file : deltas) {
                if (file.exists()) {
                    file.delete();
                }
            }
            this.commands.clear();
            nCommands = 0;
        }
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
    public synchronized void processDeltas(IDeltaProcessor<X> deltaProcessor) {
        synchronized (this.commands) {
            ArrayList<DeltaCommand> commandsToProcess = new ArrayList<DeltaCommand>(this.commands);
            boolean processed = false;
            for (DeltaCommand cmd : commandsToProcess) {
                try {
                    cmd.processWith(deltaProcessor);
                    processed = true;
                } catch (Exception e) {
                    Log.log(e);
                }
            }
            if (processed) {
                //if nothing happened, we don't end the processing (no need to do it)
                deltaProcessor.endProcessing();
            }
            this.clearAll();
        }
    }

    public void writeToFile(DeltaCommand command, String data, File file) {
        try {
            FastStringBuffer buf = new FastStringBuffer(command.getCommandFileDesc(), data.length());
            buf.append(data);
            REF.writeStrToFile(buf.toString(), file);
        } catch (Exception e) {
            Log.log(e);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public DeltaSaver.DeltaCommand readFromFile(File astOutputFile, ICallback<X, String> readFromFileMethod) {
        try {
            boolean deletFile = false;
            //the file is not even there
            if (!astOutputFile.exists()) {
                return null;
            }
            String fileContents = REF.getFileContents(astOutputFile);
            DeltaSaver.DeltaCommand o = null;
            try {
                if (fileContents.startsWith("UPD")) {
                    o = new DeltaSaver.DeltaUpdateCommand(readFromFileMethod.call(fileContents.substring(3)));

                } else if (fileContents.startsWith("DEL")) {
                    o = new DeltaSaver.DeltaDeleteCommand(readFromFileMethod.call(fileContents.substring(3)));

                } else if (fileContents.startsWith("INS")) {
                    o = new DeltaSaver.DeltaInsertCommand(readFromFileMethod.call(fileContents.substring(3)));

                }
            } catch (Exception e) {
                //the format has changed (no real problem here... just erase the file)
                deletFile = true;
                o = null;
            }
            if (deletFile) {
                if (astOutputFile.exists()) {
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
