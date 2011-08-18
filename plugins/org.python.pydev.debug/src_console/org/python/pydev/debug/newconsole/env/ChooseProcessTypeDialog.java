/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole.env;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.NotConfiguredInterpreterException;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * Helper to choose which kind of jython run will it be.
 */
final class ChooseProcessTypeDialog extends Dialog {
    
    private Button checkboxForCurrentEditor;

    private Button checkboxPython;
    
    private Button checkboxJython;
    
    private Button checkboxIronpython;
    
    private Button checkboxJythonEclipse;

    private PyEdit activeEditor;

    private IInterpreterManager interpreterManager;
    
    private List<IPythonNature> natures = new ArrayList<IPythonNature>();

    ChooseProcessTypeDialog(Shell shell, PyEdit activeEditor) {
        super(shell);
        this.activeEditor = activeEditor;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        checkboxForCurrentEditor = new Button(area, SWT.RADIO);
        checkboxForCurrentEditor.setToolTipText("Creates a console with the PYTHONPATH used by the current editor (and jython/python/iron python depending on the project type).");
        configureEditorButton();

        
        checkboxPython = new Button(area, SWT.RADIO);
        checkboxPython.setToolTipText("Creates a Python console with the PYTHONPATH containing all the python projects in the workspace.");
        configureButton(checkboxPython, "Python", PydevPlugin.getPythonInterpreterManager());
        
        checkboxJython = new Button(area, SWT.RADIO);
        checkboxJython.setToolTipText("Creates a Jython console with the PYTHONPATH containing all the python projects in the workspace.");
        configureButton(checkboxJython, "Jython", PydevPlugin.getJythonInterpreterManager());
        
        checkboxIronpython = new Button(area, SWT.RADIO);
        checkboxIronpython.setToolTipText("Creates an Iron Python console with the PYTHONPATH containing all the python projects in the workspace.");
        configureButton(checkboxIronpython, "Iron Python", PydevPlugin.getIronpythonInterpreterManager());
        
        checkboxJythonEclipse = new Button(area, SWT.RADIO);
        checkboxJythonEclipse.setToolTipText("Creates a Jython console using the running Eclipse environment (can potentially halt Eclipse depending on what's done).");
        configureButton(checkboxJythonEclipse, "Jython using VM running Eclipse", new JythonEclipseInterpreterManager());
        
        return area;
    }
    
    /**
     * Configures a button related to a given interpreter manager.
     */
    private void configureButton(Button checkBox, String python, IInterpreterManager interpreterManager) {
        boolean enabled = false;
        String text;
        try{
            if(interpreterManager.getDefaultInterpreterInfo(false) != null){
                text = python+" console";
                enabled = true;
            }else{
                throw new NotConfiguredInterpreterException();
            }
        }catch(MisconfigurationException e){
            text = "Unable to create console for "+python+" (interpreter not configured)";
        }
        checkBox.setText(text);
        checkBox.setEnabled(enabled);
    }

    /**
     * Configures a button related to an editor.
     * @throws MisconfigurationException 
     */
    private void configureEditorButton(){
        boolean enabled = false;
        String text;
        try{
            if(this.activeEditor != null){
                IPythonNature nature = this.activeEditor.getPythonNature();
                if(nature != null){
                    
                    if(nature.getRelatedInterpreterManager().getDefaultInterpreterInfo(false) != null){
                        text = "Console for currently active editor";
                        enabled = true;
                    }else{
                        throw new NotConfiguredInterpreterException();
                    }
                }else{
                    text = "No python nature configured for the current editor";
                }
            }else{
                text = "Unable to create console for current editor (no active editor)";
            }
        }catch(MisconfigurationException e){
            //expected
            text = "Unable to create console for current editor (interpreter not configured for the editor)";
        }
        checkboxForCurrentEditor.setText(text);
        checkboxForCurrentEditor.setEnabled(enabled);
    }

    
    /**
     * Sets the internal pythonpath chosen.
     */
    @Override
    protected void okPressed() {
        if(checkboxForCurrentEditor.isEnabled() && checkboxForCurrentEditor.getSelection()){
            IProject project = this.activeEditor.getProject();
            PythonNature nature = PythonNature.getPythonNature(project);
            natures.add(nature);
            IInterpreterManager relatedInterpreterManager = nature.getRelatedInterpreterManager();
            this.interpreterManager = relatedInterpreterManager;
            
        }else if(checkboxPython.isEnabled() && checkboxPython.getSelection()){
            this.interpreterManager = PydevPlugin.getPythonInterpreterManager();
            
        }else if(checkboxJython.isEnabled() && checkboxJython.getSelection()){
            this.interpreterManager = PydevPlugin.getJythonInterpreterManager();
            
        }else if(checkboxJythonEclipse.isEnabled() && checkboxJythonEclipse.getSelection()){
            this.interpreterManager = new JythonEclipseInterpreterManager();
            
        }else if(checkboxIronpython.isEnabled() && checkboxIronpython.getSelection()){
            this.interpreterManager = PydevPlugin.getIronpythonInterpreterManager();
            
        }
        
        super.okPressed();
    }

    
    /**
     * @return the pythonpath/nature to be used or null if not configured (note that the nature can be null)
     */
    public Tuple<Collection<String>, IPythonNature> getPythonpathAndNature(IInterpreterInfo interpreter) {
        
        if(this.interpreterManager != null){
            if(this.natures.size() == 1){
                //chosen for the editor
                IPythonNature nature = this.natures.get(0);
                return new Tuple<Collection<String>, IPythonNature>(new ArrayList<String>(
                        nature.getPythonPathNature().getCompleteProjectPythonPath(
                        interpreter, this.interpreterManager)), nature);

            }
            
            //we need to get the natures matching the one selected in all the projects.
            IWorkspace w = ResourcesPlugin.getWorkspace();
            HashSet<String> pythonpath = new HashSet<String>();
            for(IProject p:w.getRoot().getProjects()){
                PythonNature nature = PythonNature.getPythonNature(p);
                try{
                    if(nature != null){
                        if(nature.getRelatedInterpreterManager() == this.interpreterManager){
                            natures.add(nature);
                            List<String> completeProjectPythonPath = nature.getPythonPathNature().
                                    getCompleteProjectPythonPath(interpreter, this.interpreterManager);
                            if(completeProjectPythonPath != null){
                                pythonpath.addAll(completeProjectPythonPath);
                            }else{
                                Log.logInfo("Unable to get pythonpath for project: "+nature.getProject()+" (initialization not finished).");
                            }
                        }
                    }
                }catch(Exception e){
                    Log.log(e);
                }
            }
            return new Tuple<Collection<String>, IPythonNature>(pythonpath, null);
        }
        
        return null;
    }

    public IInterpreterManager getInterpreterManager() {
        return this.interpreterManager;
    }
    
    public List<IPythonNature> getNatures() {
        return natures;
    }
}