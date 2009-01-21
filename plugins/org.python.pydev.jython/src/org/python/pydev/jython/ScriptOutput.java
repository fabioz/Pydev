/**
 * 
 */
package org.python.pydev.jython;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.python.pydev.jython.ui.JyScriptingPreferencesPage;

/**
 * This class is used so that we can control the output of the script.
 */
public class ScriptOutput extends OutputStream{
    /**
     * Indicates whether we should write to the console or not
     */
    private boolean writeToConsole;
    
    /**
     * Stream to the console we want to write
     */
    private IOConsoleOutputStream out;
    
    /**
     * This is the color of the output
     */
    private Color color;

    /**
     * Console associated with this output
     */
    private IOConsole fConsole;
    
    /**
     * Constructor - the user is able to define whether he wants to write to the console or not.
     * 
     * @param color the color of the output written
     */
    public ScriptOutput(Color color, IOConsole console, boolean writeToConsole){
        this.color = color;
        this.fConsole = console;
        this.writeToConsole = writeToConsole;
    }
    
    /**
     * Constructor - Uses the properties from the JyScriptingPreferencesPage to know if we should write to
     * the console or not
     * 
     * @param color the color of the output written
     */
    public ScriptOutput(Color color, MessageConsole console){
        this(color, console, JyScriptingPreferencesPage.getShowScriptingOutput());
        IPropertyChangeListener listener = new Preferences.IPropertyChangeListener(){
            public void propertyChange(PropertyChangeEvent event) {
                writeToConsole = JyScriptingPreferencesPage.getShowScriptingOutput();
            }
        };
        JythonPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(listener);
    }
    
    /**
     * OutputStream interface
     */
    @Override
    public void write(int b) throws IOException {
        if(writeToConsole){
            IOConsoleOutputStream out = getOutputStream();
            out.write(b);
        }
    }

    /**
     * @return the output stream to use
     */
    private IOConsoleOutputStream getOutputStream() throws MalformedURLException {
        if(out == null){
            out = fConsole.newOutputStream();
            synchronized (Display.getDefault()) {
                Display.getDefault().syncExec(new Runnable(){

                    public void run() {
                        out.setColor(color);
                    }
                });
            }
        }
        return out;
    }
    }