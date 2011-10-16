/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.actions.container;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.REF;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.uiutils.RunInUiThread;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PyFormatStd;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.parser.prettyprinterv2.IFormatter;

/**
 * Action used to apply source code formatting to all the available python files.
 *  
 * @author Fabio
 */
public class PySourceFormatAction extends PyContainerAction{
    
    /**
     * This is the class that'll be used for doing the source formatting 
     * (only valid after beforeRun() and before afterRun()).
     */
    private IFormatter formatter;
    
    /**
     * Set with the open files (only valid after beforeRun() and before afterRun())
     */
    private Set<IFile> openFiles;

    /**
     * We need UI access because of opened editors.
     */
    protected boolean needsUIThread(){
        return false;
    }
    
    /**
     * Initialize the open files and the formatter to be used.
     */
    @Override
    protected void beforeRun() {
        openFiles = PyAction.getOpenFiles();
        PyFormatStd std = new PyFormatStd();
        formatter = std.getFormatter();
    }
    
    
    /**
     * Applies source code formatting to the files... 
     * Recursively pass the folders and delete the files (and sum them so that we know how many
     * files were formatted).
     * 
     * @param container the folder from where we want to remove the files
     * @return the number of files formatted
     */
    protected int doActionOnContainer(IContainer container, IProgressMonitor monitor) {
        int formatted = 0;
        try{
            IResource[] members = container.members();
            
            
            for (IResource c:members) {
                if(monitor.isCanceled()){
                    break;
                }
                monitor.worked(1);
                if(c instanceof IContainer){
                    formatted += this.doActionOnContainer((IContainer) c, monitor);
                    
                }else if(c instanceof IFile){
                    final String name = c.getName();
                    if(name != null){
                        monitor.setTaskName("Formatting: "+name);
                        if(PythonPathHelper.isValidSourceFile(name)){
                            IFile file = (IFile) c;
                            final IDocument doc = REF.getDocFromResource(c);
                            
                            final boolean isOpenedFile = openFiles.contains(file);
                            try{
                                if(isOpenedFile){
                                    RunInUiThread.async(new Runnable() {
                                        
                                        public void run() {
                                            try{
                                                formatter.formatAll(doc, null, isOpenedFile, true);
                                            }catch(SyntaxErrorException e){
                                                Log.log(IStatus.ERROR, "Could not source-format file: "+name+ " (invalid syntax).", e);
                                            }
                                        }
                                    });
                                }else{
                                    formatter.formatAll(doc, null, isOpenedFile, true);
                                }
                            }catch(SyntaxErrorException e){
                                Log.log(IStatus.ERROR, "Could not source-format file: "+name+ " (invalid syntax).", e);
                            }
                            formatted += 1;
                            if(isOpenedFile){
                                //This means that it's an open buffer (let the user save it when he wants).
                                continue;
                            }
                            file.setContents(new ByteArrayInputStream(doc.get().getBytes()), true, true, monitor);
                        }
                    }
                }
            }
        } catch (CoreException e) {
            Log.log(e);
        }
            
        return formatted;
    }

    @Override
    protected void afterRun(int formatted) {
        openFiles = null;
        formatter = null;
        MessageDialog.openInformation(null, "Files formatted", StringUtils.format("Formatted %s files.", formatted));
    }

    @Override
    protected boolean confirmRun() {
        return MessageDialog.openConfirm(null, "Confirm source formatting", 
                "Are you sure that you want to recursively apply the source formatting to python files from the selected folder(s)?\n" +
                "\n" +
                "It'll be applied to all the file-types specified in the preferences: pydev > code style > file types.\n" +
                "\n" +
                "This action cannot be undone.");
    }





}
