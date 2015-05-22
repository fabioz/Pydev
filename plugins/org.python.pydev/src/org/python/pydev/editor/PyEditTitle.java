/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.concurrency.SingleJobRunningPool;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.preferences.PyTitlePreferencesPage;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.utils.RunInUiThread;

/**
 * The whole picture:
 *
 * 1. In django it's common to have multiple files with the same name
 *
 * 2. __init__ files are everywhere
 *
 * We need a way to uniquely identify those.
 *
 * Options:
 *
 * - For __init__ files, an option would be having a different icon and adding the package
 * name instead of the __init__ (so if an __init__ is under my_package, we would show
 * only 'my_package' and would change the icon for the opened editor).
 *
 * - For the default django files (models.py, settings.py, tests.py, views.py), we could use
 * the same approach -- in fact, make that configurable!
 *
 * - For any file (including the cases above), if the name would end up being duplicated, change
 * the title so that all names are always unique (note that the same name may still be used if
 * the icon is different).
 */
/*default*/final class PyEditTitle implements IPropertyChangeListener {

    /**
     * Singleton access for the title management.
     */
    private static PyEditTitle singleton;

    /**
     * Lock for accessing the singleton.
     */
    private static Object lock = new Object();

    /**
     * Helper to ensure that only a given job is running at some time.
     */
    private SingleJobRunningPool jobPool = new SingleJobRunningPool();

    private PyEditTitle() {
        IPreferenceStore preferenceStore = PydevPlugin.getDefault().getPreferenceStore();
        preferenceStore.addPropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent event) {
        //When the
        String property = event.getProperty();
        if (PyTitlePreferencesPage.isTitlePreferencesProperty(property)) {

            Job job = new Job("Invalidate title") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        List<IEditorReference> currentEditorReferences;
                        do {
                            currentEditorReferences = getCurrentEditorReferences();
                            synchronized (this) {
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException e) {
                                    //ignore.
                                }
                            }
                        } while (PydevPlugin.isAlive() && currentEditorReferences == null); //stop trying if the plugin is stopped;

                        if (currentEditorReferences != null) {
                            final List<IEditorReference> refs = currentEditorReferences;
                            RunInUiThread.sync(new Runnable() {

                                public void run() {
                                    for (final IEditorReference iEditorReference : refs) {
                                        final IEditorPart editor = iEditorReference.getEditor(true);
                                        if (editor instanceof PyEdit) {
                                            try {
                                                invalidateTitle((PyEdit) editor, iEditorReference.getEditorInput());
                                            } catch (PartInitException e) {
                                                //ignore
                                            }
                                        }
                                    }
                                }
                            });
                        }

                    } finally {
                        jobPool.removeJob(this);
                    }
                    return Status.OK_STATUS;
                }
            };
            job.setPriority(Job.SHORT);
            jobPool.addJob(job);
        }
    }

    /**
     * This method will update the title of all the editors that have a title that would match
     * the passed input.
     *
     * Note that on the first try it will update the images of all editors.
     * @param pyEdit
     */
    public static void invalidateTitle(PyEdit pyEdit, IEditorInput input) {
        synchronized (lock) {
            boolean createdSingleton = false;
            if (singleton == null) {
                singleton = new PyEditTitle();
                createdSingleton = true;

            }
            //updates the title and image for the passed input.
            singleton.invalidateTitleInput(pyEdit, input);

            if (createdSingleton) {
                //In the first time, we need to invalidate all icons (because eclipse doesn't restore them the way we left them).
                //Note that we don't need to do that for titles because those are properly saved on close/restore.
                singleton.restoreAllPydevEditorsWithDifferentIcon();
            }
        }
    }

    /**
     * Sadly, we have to restore all pydev editors that have a different icon to make it correct.
     *
     * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=308740
     */
    private void restoreAllPydevEditorsWithDifferentIcon() {
        if (!PyTitlePreferencesPage.useCustomInitIcon()) {
            return;
        }
        Job job = new Job("Invalidate images") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    while (PydevPlugin.isAlive() && !doRestoreAllPydevEditorsWithDifferentIcons()) { //stop trying if the plugin is stopped
                        synchronized (this) {
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                //ignore.
                            }
                        }
                    }
                    ;
                } finally {
                    jobPool.removeJob(this);
                }
                return Status.OK_STATUS;
            }
        };
        job.setPriority(Job.SHORT);
        jobPool.addJob(job);
    }

    /**
     * Sadly, we have to restore all pydev editors to make the icons correct.
     *
     * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=308740
     */
    private boolean doRestoreAllPydevEditorsWithDifferentIcons() {
        if (!PyTitlePreferencesPage.useCustomInitIcon()) {
            return true; //Ok, nothing custom!
        }

        final List<IEditorReference> editorReferences = getCurrentEditorReferences();
        if (editorReferences == null) {
            //couldn't be gotten.
            return false;
        }
        //Update images

        RunInUiThread.async(new Runnable() {

            public void run() {
                for (IEditorReference iEditorReference : editorReferences) {
                    try {
                        if (iEditorReference != null) {
                            IPath pathFromInput = getPathFromInput(iEditorReference.getEditorInput());
                            if (pathFromInput != null) {
                                String lastSegment = pathFromInput.lastSegment();
                                if (lastSegment != null
                                        && (lastSegment.startsWith("__init__.") || PyTitlePreferencesPage
                                                .isDjangoModuleToDecorate(lastSegment))) {
                                    iEditorReference.getEditor(true); //restore it.
                                }
                            }
                        }
                    } catch (PartInitException e) {
                        //ignore
                    }

                    //Note, removed the code below -- just restoring the editor is enough and the
                    //only way to make it work for now. See: https://bugs.eclipse.org/bugs/show_bug.cgi?id=308740

                    //			try {
                    //				IEditorInput input = iEditorReference.getEditorInput();
                    //				IPath path = getPathFromInput(input);
                    //				updateImage(null, iEditorReference, path);
                    //			} catch (PartInitException e) {
                    //				//ignore
                    //			}
                }
            }
        });

        return true;
    }

    /**
     * Updates the title text and image of the given pyEdit (based on the passed input).
     *
     * That will be done depending on the other open editors (if the user has chosen
     * unique names).
     */
    private void invalidateTitleInput(final PyEdit pyEdit, final IEditorInput input) {
        if (input == null) {
            return;
        }

        final IPath pathFromInput = getPathFromInput(input);
        if (pathFromInput == null || pathFromInput.segmentCount() == 0) {
            return; //not much we can do!
        }

        final String lastSegment = pathFromInput.lastSegment();
        if (lastSegment == null) {
            return;
        }

        final String initHandling = PyTitlePreferencesPage.getInitHandling();
        final String djangoModulesHandling = PyTitlePreferencesPage.getDjangoModulesHandling();

        //initially set this as the title (and change it later to a computed name).
        String computedEditorTitle = getPartNameInLevel(1, pathFromInput, initHandling, djangoModulesHandling,
                input).o1;

        pyEdit.setEditorTitle(computedEditorTitle);
        updateImage(pyEdit, null, pathFromInput);

        if (!PyTitlePreferencesPage.getEditorNamesUnique()) {
            return; //the user accepts having the same name for 2 files, no more work to do.
        }

        Job job = new Job("Invalidate title") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    while (PydevPlugin.isAlive()
                            && !initializeTitle(pyEdit, input, pathFromInput, lastSegment, initHandling,
                                    djangoModulesHandling)) { //stop trying if the plugin is stopped
                        synchronized (this) {
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                //ignore.
                            }
                        }
                    }
                    ;
                } finally {
                    jobPool.removeJob(this);
                }
                return Status.OK_STATUS;
            }
        };
        job.setPriority(Job.SHORT);
        jobPool.addJob(job);

    }

    /**
     * Updates the image of the passed editor.
     */
    private void updateImage(PyEdit pyEdit, IEditorReference iEditorReference, IPath path) {
        String lastSegment = path.lastSegment();
        if (lastSegment != null) {
            if (lastSegment.startsWith("__init__.")) {
                Image initIcon = PyTitlePreferencesPage.getInitIcon();
                if (initIcon != null) {
                    if (pyEdit != null) {
                        pyEdit.setEditorImage(initIcon);
                    } else {
                        setEditorReferenceImage(iEditorReference, initIcon);
                    }
                }

            } else if (PyTitlePreferencesPage.isDjangoModuleToDecorate(lastSegment)) {
                try {
                    IEditorInput editorInput;
                    if (pyEdit != null) {
                        editorInput = pyEdit.getEditorInput();
                    } else {
                        editorInput = iEditorReference.getEditorInput();
                    }
                    if (isDjangoHandledModule(PyTitlePreferencesPage.getDjangoModulesHandling(), editorInput,
                            lastSegment)) {
                        Image image = PyTitlePreferencesPage.getDjangoModuleIcon(lastSegment);
                        if (pyEdit != null) {
                            pyEdit.setEditorImage(image);
                        } else {
                            setEditorReferenceImage(iEditorReference, image);
                        }
                    }
                } catch (PartInitException e) {
                    //ignore
                }
            }
        }
    }

    /**
     * 2 pydev editors should never have the same title, so, this method will make sure that
     * this won't happen.
     *
     * @return true if it was able to complete and false if some requisite is not available.
     */
    private boolean initializeTitle(final PyEdit pyEdit, IEditorInput input, final IPath pathFromInput,
            String lastSegment, String initHandling, String djangoModulesHandling) {

        List<IEditorReference> editorReferences = getCurrentEditorReferences();
        if (editorReferences == null) {
            //couldn't be gotten.
            return false;
        }

        Map<IPath, List<IEditorReference>> partsAndPaths = removeEditorsNotMatchingCurrentName(lastSegment,
                editorReferences);

        if (partsAndPaths.size() > 1) {
            ArrayList<IPath> keys = new ArrayList<IPath>(partsAndPaths.keySet());

            //There are other editors with the same name... (1 is the editor that requested the change)
            //let's make them unique!
            int level = 0;
            List<String> names = new ArrayList<String>();
            Map<String, Integer> allNames = new HashMap<String, Integer>();

            do {
                names.clear();
                allNames.clear();
                level++;
                for (int i = 0; i < keys.size(); i++) {
                    IPath path = keys.get(i);
                    List<IEditorReference> refs = partsAndPaths.get(path);
                    if (refs == null || refs.size() == 0) {
                        Log.log("Unexpected condition. Key path without related editors: " + path);
                        keys.remove(i);
                        i--; //make up for the removed editor.
                        continue;
                    }

                    IEditorInput editorInput;
                    try {
                        //Note that we're only getting one reference here for the editor input.
                        //The problem is that for knowing about django, we use this editor, which means that
                        //we may have a bug in which if we have a file that's within a django project and
                        //another out of a django project, we'll leave by chance how it'll be displayed... seems
                        //a bit exoteric in real cases, but if it happens this behaviour may need to be changed.
                        editorInput = refs.get(0).getEditorInput();
                    } catch (PartInitException e) {
                        continue;
                    }
                    Tuple<String, Boolean> nameAndReachedMax = getPartNameInLevel(level, path, initHandling,
                            djangoModulesHandling, editorInput);
                    if (nameAndReachedMax.o2) { //maximum level reached for path
                        setEditorReferenceTitle(refs, nameAndReachedMax.o1);
                        keys.remove(i);
                        i--; //make up for the removed editor.
                        continue;
                    }
                    names.add(nameAndReachedMax.o1);
                    Integer count = allNames.get(nameAndReachedMax.o1);
                    if (count == null) {
                        allNames.put(nameAndReachedMax.o1, 1);
                    } else {
                        allNames.put(nameAndReachedMax.o1, count + 1);
                    }
                }
                for (int i = 0; i < keys.size(); i++) {
                    String finalName = names.get(i);
                    Integer count = allNames.get(finalName);
                    if (count == 1) { //no duplicate found
                        IPath path = keys.get(i);
                        List<IEditorReference> refs = partsAndPaths.get(path);
                        setEditorReferenceTitle(refs, finalName);

                        keys.remove(i);
                        names.remove(i);
                        allNames.remove(finalName);
                        i--; //make up for the removed editor.
                    }
                }
            } while (allNames.size() > 0);
        }
        return true;
    }

    /**
     * @return a list of all the editors that have the last segment as 'currentName'
     */
    private Map<IPath, List<IEditorReference>> removeEditorsNotMatchingCurrentName(String currentName,
            List<IEditorReference> editorReferences) {
        Map<IPath, List<IEditorReference>> ret = new HashMap<IPath, List<IEditorReference>>();
        for (Iterator<IEditorReference> it = editorReferences.iterator(); it.hasNext();) {
            IEditorReference iEditorReference = it.next();
            try {
                IEditorInput otherInput = iEditorReference.getEditorInput();

                //Always get the 'original' name and not the currently set name, because
                //if we previously had an __init__.py editor which we renamed to package/__init__.py
                //and we open a new __init__.py, we want it renamed to new_package/__init__.py
                IPath pathFromOtherInput = getPathFromInput(otherInput);
                if (pathFromOtherInput == null) {
                    continue;
                }

                String lastSegment = pathFromOtherInput.lastSegment();
                if (lastSegment == null) {
                    continue;
                }

                if (!currentName.equals(lastSegment)) {
                    continue;
                }
                List<IEditorReference> list = ret.get(pathFromOtherInput);
                if (list == null) {
                    list = new ArrayList<IEditorReference>();
                    ret.put(pathFromOtherInput, list);
                }
                list.add(iEditorReference);
            } catch (Throwable e) {
                Log.log(e);
            }
        }
        return ret;
    }

    /**
     * @return the current editor references or null if no editor references are available.
     *
     * Note that this method may be slow as it will need UI access (which is asynchronously
     * gotten)
     */
    private List<IEditorReference> getCurrentEditorReferences() {
        final List<IEditorReference[]> editorReferencesFound = new ArrayList<IEditorReference[]>();

        RunInUiThread.async(new Runnable() {

            public void run() {
                IEditorReference[] found = null;
                try {
                    IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    if (workbenchWindow == null) {
                        return;
                    }
                    IWorkbenchPage activePage = workbenchWindow.getActivePage();
                    if (activePage == null) {
                        return;
                    }
                    found = activePage.getEditorReferences();
                } finally {
                    editorReferencesFound.add(found);
                }
            }
        });
        while (editorReferencesFound.size() == 0) {
            synchronized (this) {
                try {
                    wait(10);
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        }
        IEditorReference[] editorReferences = editorReferencesFound.get(0);
        if (editorReferences == null) {
            return null;
        }
        ArrayList<IEditorReference> ret = new ArrayList<IEditorReference>();
        for (IEditorReference iEditorReference : editorReferences) {
            if (!PyEdit.EDITOR_ID.equals(iEditorReference.getId())) {
                continue; //only analyze Pydev editors
            }
            ret.add(iEditorReference);
        }
        return ret;
    }

    /**
     * Sets the image of the passed editor reference. Will try to restore the editor for
     * doing that.
     *
     * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=308740
     */
    private void setEditorReferenceImage(final IEditorReference iEditorReference, final Image image) {
        RunInUiThread.async(new Runnable() {

            public void run() {
                try {
                    IEditorPart editor = iEditorReference.getEditor(true);
                    if (editor instanceof PyEdit) {
                        ((PyEdit) editor).setEditorImage(image);
                    }

                } catch (Throwable e) {
                    Log.log(e);
                }
            }
        });
    }

    /**
     * Sets the title of the passed editor reference. Will try to restore the editor for
     * doing that.
     *
     * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=308740
     */
    private void setEditorReferenceTitle(final List<IEditorReference> refs, final String title) {
        if (refs == null) {
            return;
        }
        final int size = refs.size();
        if (size == 1) {
            for (final IEditorReference ref : refs) {

                if (title.equals(ref.getTitle())) {
                    //Nothing to do if it's already the same.
                    return;
                }

                RunInUiThread.async(new Runnable() {

                    public void run() {
                        try {
                            IEditorPart editor = ref.getEditor(true);
                            if (editor instanceof PyEdit) {
                                ((PyEdit) editor).setEditorTitle(title);
                            }
                        } catch (Throwable e) {
                            Log.log(e);
                        }
                    }
                });
            }
        } else if (size > 1) {

            RunInUiThread.async(new Runnable() {

                public void run() {
                    try {
                        final String key = "PyEditTitleEditorNumberSet";
                        final Set<Integer> used = new HashSet<Integer>();
                        final ArrayList<PyEdit> toSet = new ArrayList<PyEdit>();

                        for (final IEditorReference ref : refs) {
                            PyEdit editor = (PyEdit) ref.getEditor(true);
                            Integer curr = (Integer) editor.cache.get(key);
                            if (curr != null) {
                                used.add(curr);
                                editor.setEditorTitle(title + " #" + (curr + 1));
                            } else {
                                toSet.add(editor);
                            }
                        }

                        //Calculate the next integer available
                        final ICallback0<Integer> next = new ICallback0<Integer>() {
                            private int last = 0;

                            public Integer call() {
                                for (; last < Integer.MAX_VALUE; last++) {
                                    if (used.contains(last)) {
                                        continue;
                                    }
                                    used.add(last);
                                    return last;
                                }
                                throw new AssertionError("Should not get here");
                            }
                        };

                        //If it got here in toSet, it still must be set!
                        for (PyEdit editor : toSet) {
                            Integer i = next.call();
                            editor.setEditorTitle(title + " #" + (i + 1));
                        }
                    } catch (Throwable e) {
                        Log.log(e);
                    }
                }
            });
        }
    }

    /**
     * @param input
     * @return a tuple with the part name to be used and a boolean indicating if the maximum level
     * has been reached for this path.
     */
    private Tuple<String, Boolean> getPartNameInLevel(int level, IPath path, String initHandling,
            String djangoModulesHandling, IEditorInput input) {
        String name = input.getName();
        String[] segments = path.segments();
        if (segments.length == 0) {
            return new Tuple<String, Boolean>("", true);
        }
        if (segments.length == 1) {
            return new Tuple<String, Boolean>(segments[1], true);
        }

        boolean handled = isDjangoHandledModule(djangoModulesHandling, input, name);
        if (handled
                && djangoModulesHandling == PyTitlePreferencesPage.TITLE_EDITOR_DJANGO_MODULES_SHOW_PARENT_AND_DECORATE) {
            String[] dest = new String[segments.length - 1];
            System.arraycopy(segments, 0, dest, 0, dest.length);
            segments = dest;

        } else if (initHandling != PyTitlePreferencesPage.TITLE_EDITOR_INIT_HANDLING_IN_TITLE) {
            if (name.startsWith("__init__.")) {
                //remove the __init__.
                String[] dest = new String[segments.length - 1];
                System.arraycopy(segments, 0, dest, 0, dest.length);
                segments = dest;
                if (dest.length > 0) {
                    name = dest[dest.length - 1];
                }
            }
        }

        int startAt = segments.length - level;
        if (startAt < 0) {
            startAt = 0;
        }

        int endAt = segments.length - 1;

        String modulePart = StringUtils.join(".", segments, startAt, endAt);

        if (!PyTitlePreferencesPage.getTitleShowExtension()) {
            int i = name.lastIndexOf('.');
            if (i != -1) {
                name = name.substring(0, i);
            }
        }
        if (modulePart.length() > 0) {
            return new Tuple<String, Boolean>(name + " (" + modulePart + ")", startAt == 0);
        } else {
            return new Tuple<String, Boolean>(name, startAt == 0);
        }
    }

    private boolean isDjangoHandledModule(String djangoModulesHandling, IEditorInput input, String lastSegment) {
        boolean handled = false;
        if (djangoModulesHandling == PyTitlePreferencesPage.TITLE_EDITOR_DJANGO_MODULES_SHOW_PARENT_AND_DECORATE
                || djangoModulesHandling == PyTitlePreferencesPage.TITLE_EDITOR_DJANGO_MODULES_DECORATE) {

            if (input instanceof IFileEditorInput) {
                IFileEditorInput iFileEditorInput = (IFileEditorInput) input;
                IFile file = iFileEditorInput.getFile();
                IProject project = file.getProject();
                try {
                    if (project.hasNature(PythonNature.DJANGO_NATURE_ID)) {
                        if (PyTitlePreferencesPage.isDjangoModuleToDecorate(lastSegment)) {
                            //remove the module name.
                            handled = true;
                        }
                    }
                } catch (CoreException e) {
                    Log.log(e);
                }
            }
        }
        return handled;
    }

    /**
     * @return This is the Path that the editor is editing.
     */
    private IPath getPathFromInput(IEditorInput otherInput) {
        IPath path = null;
        if (otherInput instanceof IPathEditorInput) {
            IPathEditorInput iPathEditorInput = (IPathEditorInput) otherInput;
            try {
                path = iPathEditorInput.getPath();
            } catch (IllegalArgumentException e) {
                //ignore: we may have the trace below inside the FileEditorInput.
                //java.lang.IllegalArgumentException
                //at org.eclipse.ui.part.FileEditorInput.getPath(FileEditorInput.java:208)
                //at org.python.pydev.editor.PyEditTitle.getPathFromInput(PyEditTitle.java:751)

            }
        }
        if (path == null) {
            if (otherInput instanceof IFileEditorInput) {
                IFileEditorInput iFileEditorInput = (IFileEditorInput) otherInput;
                path = iFileEditorInput.getFile().getFullPath();
            }
        }
        if (path == null) {
            try {
                if (otherInput instanceof IURIEditorInput) {
                    IURIEditorInput iuriEditorInput = (IURIEditorInput) otherInput;
                    path = Path.fromOSString(new File(iuriEditorInput.getURI()).toString());
                }
            } catch (Throwable e) {
                //Ignore (IURIEditorInput not available on 3.2)
            }
        }
        return path;
    }

}
