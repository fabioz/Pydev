/*
 * License: Common Public License v1.0
 * Created on 04/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.REF;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.runners.SimpleRunner;

public class JythonInterpreterEditor extends AbstractInterpreterEditor{

    public JythonInterpreterEditor(String labelText, Composite parent, IInterpreterManager interpreterManager) {
        super(IInterpreterManager.JYTHON_INTERPRETER_PATH, labelText, parent, interpreterManager);
    }

    @Override
    public String[] getInterpreterFilterExtensions() {
        return new String[] { "*.jar", "*.*" };
    }

    
    protected String getAutoNewInput() {
        
        try {
            Map<String, String> env = SimpleRunner.getDefaultSystemEnv();
            List<String> pathsToSearch = new ArrayList<String>();
            if(env.containsKey("JYTHON_HOME")){
                pathsToSearch.add(env.get("JYTHON_HOME"));
            }
            if(env.containsKey("PYTHON_HOME")){
                pathsToSearch.add(env.get("PYTHON_HOME"));
            }
            if(env.containsKey("JYTHONHOME")){
                pathsToSearch.add(env.get("JYTHONHOME"));
            }
            if(env.containsKey("PYTHONHOME")){
                pathsToSearch.add(env.get("PYTHONHOME"));
            }
            if(env.containsKey("PATH")){
                String path = env.get("PATH");
                String separator = SimpleRunner.getPythonPathSeparator();
                final List<String> split = StringUtils.split(path, separator);
                pathsToSearch.addAll(split);
            }
            
            for(String s:pathsToSearch){
                if(s.trim().length() > 0){
                    File file = new File(s.trim());
                    if(file.isDirectory()){
                        String[] available = file.list();
                        for(String jar:available){
                            if(jar.toLowerCase().equals("jython.jar")){
                                return REF.getFileAbsolutePath(new File(file, jar));
                            }
                        }
                    }
                }
            }
        } catch (CoreException e) {
            PydevPlugin.log(e);
        }
        
        return null;
    }
    
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        super.doFillIntoGrid(parent, numColumns);
        this.autoConfigButton.setToolTipText("Will try to find Jython on the PATH (will fail if not available)");
    }

}