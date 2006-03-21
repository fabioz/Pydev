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
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.python.pydev.jython.ui.JyScriptingPreferencesPage;

/**
 * This class is used so that we can control the output of the script.
 */
class ScriptOutput extends OutputStream{
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
	 * Constructor
	 * 
	 * @param color the color of the output written
	 */
	public ScriptOutput(Color color){
		this.color = color;
		IPropertyChangeListener listener = new Preferences.IPropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent event) {
				writeToConsole = JyScriptingPreferencesPage.getShowScriptingOutput();
			}
		};
		writeToConsole = JyScriptingPreferencesPage.getShowScriptingOutput();
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
			out = getConsole().newOutputStream();
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
	
	
	// -------------- static things
	
	/**
	 * This is the console we are writing to
	 */
	private static MessageConsole fConsole;

	/**
	 * @return the console to use
	 */
	private static MessageConsole getConsole() throws MalformedURLException {
		if (fConsole == null){
			fConsole = new MessageConsole("", JythonPlugin.getBundleInfo().getImageCache().getDescriptor("icons/python.gif"));
			ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[]{fConsole});
		}
		return fConsole;
	}
}