package com.python.pydev;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextUtilities;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.core.docutils.StringUtils;

import com.python.pydev.interactiveconsole.InteractiveConsolePreferencesPage;

public class PydevExtensionInitializer extends AbstractPreferenceInitializer{
	public static final String USER_NAME = "USER_NAME";
	public static final String USER_EMAIL = "USER_EMAIL";
	public static final String LICENSE = "LICENSE";
	public static final String LIC_TIME = "LIC_TIME";
	public static final String LIC_TYPE = "LIC_TYPE";
	public static final String LIC_DEVS = "LIC_DEVS";
	
	public static final String DEFAULT_SCOPE = "com.python.pydev";

	@Override
	public void initializeDefaultPreferences() {
		Preferences node = new DefaultScope().getNode(DEFAULT_SCOPE);
		
		node.put(USER_NAME, "");
		node.put(USER_EMAIL, "");
		node.put(LICENSE, "");
		node.put(LIC_TIME, "");
		node.put(LIC_TYPE, "");
		node.put(LIC_DEVS, "");

	    String initialInterpreterCommands = ""+
	    "import sys; sys.ps1=''; sys.ps2=''\r\n"+
	    "print >> sys.stderr, 'PYTHONPATH:'\r\n"+
	    "for p in sys.path:\r\n"+
	    "    print >> sys.stderr,  p\r\n" +
	    "\r\n" +                                                //to finish the for scope
	    "print >> sys.stderr, 'Ok, all set up... Enjoy'\r\n"+
	    "";
	    
	    //passing and empty document we will get the default line delimiter.
	    String defaultLineDelimiter = TextUtilities.getDefaultLineDelimiter(new Document());
	    initialInterpreterCommands = StringUtils.replaceAll(initialInterpreterCommands, "\r\n", defaultLineDelimiter);

        node.putBoolean(InteractiveConsolePreferencesPage.EVAL_ON_NEW_LINE, InteractiveConsolePreferencesPage.DEFAULT_EVAL_ON_NEW_LINE);
        node.putBoolean(InteractiveConsolePreferencesPage.SHOW_CONSOLE_INPUT, InteractiveConsolePreferencesPage.DEFAULT_SHOW_CONSOLE_INPUT);
        node.put(InteractiveConsolePreferencesPage.INITIAL_INTERPRETER_CMDS, initialInterpreterCommands);
        node.put(InteractiveConsolePreferencesPage.INTERACTIVE_CONSOLE_VM_ARGS, InteractiveConsolePreferencesPage.DEFAULT_INTERACTIVE_CONSOLE_VM_ARGS);
	}
}
