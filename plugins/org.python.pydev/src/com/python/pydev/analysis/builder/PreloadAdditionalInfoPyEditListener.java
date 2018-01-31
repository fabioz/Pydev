/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.builder;

import java.util.ListResourceBundle;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;
import org.python.pydev.shared_ui.editor.IPyEditListener3;

import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;

/**
 * When the editor is disposed, if needed this class will remove the markers from the related
 * file (if no other editor is still editing the same file) and will remove the hash from the
 * additional info.
 * 
 * @author Fabio
 */
public class PreloadAdditionalInfoPyEditListener implements IPyEditListener, IPyEditListener3 {

    @Override
    public void onCreateActions(ListResourceBundle resources, BaseEditor baseEditor, IProgressMonitor monitor) {

    }

    @Override
    public void onDispose(BaseEditor baseEditor, IProgressMonitor monitor) {
    }

    @Override
    public void onSave(BaseEditor baseEditor, IProgressMonitor monitor) {

    }

    @Override
    public void onSetDocument(IDocument document, BaseEditor baseEditor, IProgressMonitor monitor) {

    }

    @Override
    public void onInputChanged(BaseEditor baseEditor, IEditorInput oldInput, IEditorInput input,
            IProgressMonitor monitor) {
        if (input != null) {
            IResource adapter = (IResource) input.getAdapter(IResource.class);
            if (adapter != null) {
                IProject project = adapter.getProject();
                final PythonNature nature = PythonNature.getPythonNature(project);
                if (nature != null) {
                    Job job = new Job("Preload additional info") {

                        @Override
                        protected IStatus run(IProgressMonitor monitor) {
                            try {
                                AdditionalProjectInterpreterInfo.getAdditionalInfo(nature);
                            } catch (MisconfigurationException e) {
                                Log.log(e);
                            }
                            return Status.OK_STATUS;
                        }
                    };
                    job.setSystem(true);
                    job.schedule(100);
                }
            }
        }
    }

}
