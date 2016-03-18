/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.python.pydev.debug.ui.actions.AbstractBreakpointRulerAction;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.callbacks.ICallbackListener;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;
import org.python.pydev.shared_ui.editor.IPyEditListener4;
import org.python.pydev.shared_ui.utils.PyMarkerUtils;

/**
 * This class is used to keep the annotations related to the debugger in sync with external editors
 * (if we're not dealing with an external editor, this class won't actually do anything)
 * 
 * @author Fabio
 */
public class PyEditBreakpointSync implements IPyEditListener, IPyEditListener4 {

    public static class PyEditBreakpointSyncImpl implements IBreakpointListener, IPyEditListener, IPyEditListener4 {

        private PyEdit edit;

        public PyEditBreakpointSyncImpl(PyEdit edit) {
            this.edit = edit;
        }

        // breakpoints listening ---------------------------------------------------------------------------------------
        // breakpoints listening ---------------------------------------------------------------------------------------
        // breakpoints listening ---------------------------------------------------------------------------------------

        @Override
        public void breakpointAdded(IBreakpoint breakpoint) {
            updateAnnotations();
        }

        @Override
        public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
        }

        @Override
        public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
            updateAnnotations();
        }

        // pyedit listening --------------------------------------------------------------------------------------------
        // pyedit listening --------------------------------------------------------------------------------------------
        // pyedit listening --------------------------------------------------------------------------------------------

        @Override
        public void onCreateActions(ListResourceBundle resources, BaseEditor baseEditor, IProgressMonitor monitor) {
        }

        @Override
        public void onDispose(BaseEditor baseEditor, IProgressMonitor monitor) {
            if (this.edit != null) {
                this.edit = null;
                IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
                breakpointManager.removeBreakpointListener(this);
            }
        }

        @Override
        public void onEditorCreated(BaseEditor baseEditor) {
        }

        @Override
        public void onSave(BaseEditor baseEditor, IProgressMonitor monitor) {
            updateAnnotations();
        }

        /**
         * When the document is set, this class will start listening for the breakpoint manager, so that any changes in it
         * will update the debug annotations.
         */
        @Override
        public void onSetDocument(IDocument document, BaseEditor baseEditor, IProgressMonitor monitor) {
            PyEdit edit = (PyEdit) baseEditor;
            if (this.edit != null) {
                this.edit = null;
                IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
                breakpointManager.removeBreakpointListener(this);
            }

            if (AbstractBreakpointRulerAction.isExternalFileEditor(edit)) {
                this.edit = edit;
                IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
                breakpointManager.addBreakpointListener(this);
            }

            //initial update (the others will be on changes)
            updateAnnotations();
        }

        // update annotations ----------------------------------------------------------------------------------------------
        // update annotations ----------------------------------------------------------------------------------------------
        // update annotations ----------------------------------------------------------------------------------------------

        @SuppressWarnings("unchecked")
        private void updateAnnotations() {
            if (edit == null) {
                return;
            }
            IDocumentProvider provider = edit.getDocumentProvider();
            if (provider == null) {
                return;
            }
            IAnnotationModel model = provider.getAnnotationModel(edit.getEditorInput());
            if (model == null) {
                return;
            }

            IAnnotationModelExtension modelExtension = (IAnnotationModelExtension) model;

            List<Annotation> existing = new ArrayList<Annotation>();
            Iterator<Annotation> it = model.getAnnotationIterator();
            if (it == null) {
                return;
            }
            while (it.hasNext()) {
                existing.add(it.next());
            }

            IDocument doc = edit.getDocument();
            IResource resource = PyMarkerUtils.getResourceForTextEditor(edit);
            IEditorInput externalFileEditorInput = AbstractBreakpointRulerAction.getExternalFileEditorInput(edit);
            List<IMarker> markers = AbstractBreakpointRulerAction.getMarkersFromEditorResource(resource, doc,
                    externalFileEditorInput, 0, false, model);

            Map<Annotation, Position> annotationsToAdd = new HashMap<Annotation, Position>();
            for (IMarker m : markers) {
                Position pos = PyMarkerUtils.getMarkerPosition(doc, m, model);
                MarkerAnnotation newAnnotation = new MarkerAnnotation(m);
                annotationsToAdd.put(newAnnotation, pos);
            }

            //update all in a single step
            modelExtension.replaceAnnotations(existing.toArray(new Annotation[0]), annotationsToAdd);
        }
    }

    @Override
    public void onSave(BaseEditor baseEditor, IProgressMonitor monitor) {
    }

    @Override
    public void onCreateActions(ListResourceBundle resources, BaseEditor baseEditor, IProgressMonitor monitor) {
    }

    @Override
    public void onDispose(BaseEditor baseEditor, IProgressMonitor monitor) {
    }

    @Override
    public void onSetDocument(IDocument document, BaseEditor baseEditor, IProgressMonitor monitor) {
    }

    @Override
    public void onEditorCreated(final BaseEditor baseEditor) {
        final PyEdit edit = (PyEdit) baseEditor;
        Map<String, Object> cache = edit.getCache();

        //Register listener that'll keep the breakpoints in sync for external files
        String key = "PyEditBreakpointSync.PyEditBreakpointSyncImpl";
        PyEditBreakpointSyncImpl syncImpl = (PyEditBreakpointSyncImpl) cache.get(key);
        if (syncImpl == null) {
            syncImpl = new PyEditBreakpointSyncImpl(edit);
            edit.addPyeditListener(syncImpl);
            cache.put(key, syncImpl);
        }

        //Register the adapter for IToggleBreakpointsTarget
        edit.onGetAdapter.registerListener(new ICallbackListener<Class<?>>() {

            @Override
            public Object call(Class<?> obj) {
                if (IToggleBreakpointsTarget.class == obj) {
                    Map<String, Object> cache = edit.getCache();
                    String key = "PyEditBreakpointSync.ToggleBreakpointsTarget";
                    Object object = cache.get(key);
                    if (object == null) {
                        object = new PyToggleBreakpointsTarget();
                        cache.put(key, object);
                    }

                    return object;
                }
                return null;
            }
        });
    }
}
