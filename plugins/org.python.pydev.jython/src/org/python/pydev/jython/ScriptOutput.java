/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.jython;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.python.pydev.jython.ui.JyScriptingPreferencesPage;
import org.python.pydev.shared_core.callbacks.ICallback0;

/**
 * This class is used so that we can control the output of the script.
 */
public class ScriptOutput extends OutputStream {
    /**
     * Indicates whether we should write to the console or not
     */
    private boolean writeToConsole;

    /**
     * Stream to the console we want to write
     */
    private ICallback0<IOConsoleOutputStream> out;

    /**
     * Constructor - the user is able to define whether he wants to write to the console or not.
     * 
     * @param color the color of the output written
     */
    public ScriptOutput(ICallback0<IOConsoleOutputStream> outputStream, boolean writeToConsole) {
        this.writeToConsole = writeToConsole;
        out = outputStream;
    }

    /**
     * Constructor - Uses the properties from the JyScriptingPreferencesPage to know if we should write to
     * the console or not
     * 
     * @param color the color of the output written
     */
    public ScriptOutput(ICallback0<IOConsoleOutputStream> outputStream) {
        this(outputStream, JyScriptingPreferencesPage.getShowScriptingOutput());
        IPropertyChangeListener listener = new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                writeToConsole = JyScriptingPreferencesPage.getShowScriptingOutput();
            }
        };
        JythonPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(listener);
    }

    /**
     * OutputStream interface
     */
    @Override
    public void write(int b) throws IOException {
        if (writeToConsole) {
            IOConsoleOutputStream out = getOutputStream();
            out.write(b);
        }
    }

    /**
     * @return the output stream to use
     */
    private IOConsoleOutputStream getOutputStream() throws MalformedURLException {
        return out.call();
    }

}