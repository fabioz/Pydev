/*
 * @author atotic
 * Created: Jan 2, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.console.IHyperlink;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.model.Location;

/**
 * Line tracker that hyperlinks error lines: 'File "D:\mybad.py" line 3\n n Syntax error'
 *
 * see org.eclipse.ant.internal.ui.console.BuildFailedTracker
 */
public class PythonConsoleLineTracker implements IConsoleLineTracker {

    private IConsole console; // console we are attached to
    /** pattern for detecting error lines */
    static Pattern linePattern = Pattern.compile("\\s*File \\\"([^\\\"]*)\\\", line (\\d*).*");

    /**
     * Opens up a file with a given line
     */
    public class ConsoleLink implements IHyperlink {
    
        ItemPointer pointer;
    
        public ConsoleLink(ItemPointer pointer) {
            this.pointer = pointer;
        }
        
        public void linkEntered(){
            
        }
        
        public void linkExited(){
            
        }

        public void linkActivated() {
            PyOpenAction open = new PyOpenAction();
            open.run(pointer);
        }
    }
    
    public void init(IConsole console) {
        this.console = console;
    }

    /**
     * Hyperlink error lines to the editor.
     * 
     * Based on org.eclipse.debug.ui.console.IConsoleLineTracker#lineAppended(org.eclipse.jface.text.IRegion)
     */
    public void lineAppended(IRegion line) {
        int lineOffset = line.getOffset();
        int lineLength = line.getLength();
        try {
            String text = console.getDocument().get(lineOffset, lineLength);
            Matcher m = linePattern.matcher(text);
            String fileName = null;
            String lineNumber = null;
            int fileStart = -1;
            // match
            if (m.matches()) {
                fileName = m.group(1);
                lineNumber = m.group(2);
                fileStart = 2; // The beginning of the line, "File  "
            }
            // hyperlink if we found something
            if (fileName != null) {
                int num = -1;
                try {
                    num = lineNumber != null ? Integer.parseInt(lineNumber) : 0;
                }
                catch (NumberFormatException e) {
                    num = 0;
                }
                IHyperlink link = null;
                IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(new Path(fileName));
                if (files.length > 0 && files[0].exists())
                    link = new FileLink(files[0], null, -1, -1, num);
                else {    // files outside of the workspace
                    File realFile = new File(fileName);
                    if (realFile.exists()) {
                        ItemPointer p = new ItemPointer(realFile, new Location(num-1, 0), null);
                        link = new ConsoleLink(p);
                    }
                }
                if (link != null){
                    console.addLink(link, lineOffset + fileStart, lineLength - fileStart);
                }
            }
        } catch (BadLocationException e) {
            PydevDebugPlugin.log(IStatus.ERROR, "unexpected error", e);
        }
    }

    /**
     * NOOP.
     * @see org.eclipse.debug.ui.console.IConsoleLineTracker#dispose()
     */
    public void dispose() {
    }

}
