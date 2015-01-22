/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.pycremover.PycHandlerBuilderVisitor;
import org.python.pydev.builder.pylint.PyLintVisitor;
import org.python.pydev.builder.syntaxchecker.PySyntaxChecker;
import org.python.pydev.builder.todo.PyTodoVisitor;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.PyCodeCompletionVisitor;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.utils.PyFileListing;

/**
 * This builder only passes through python files
 *
 * @author Fabio Zadrozny
 */
public class PyDevBuilder extends IncrementalProjectBuilder {

    private static final boolean DEBUG = false;

    /**
     *
     * @return a list of visitors for building the application.
     */
    public List<PyDevBuilderVisitor> getVisitors() {
        List<PyDevBuilderVisitor> list = new ArrayList<PyDevBuilderVisitor>();
        list.add(new PyTodoVisitor());
        list.add(new PyLintVisitor());
        list.add(new PyCodeCompletionVisitor());
        list.add(new PycHandlerBuilderVisitor());
        list.add(new PySyntaxChecker());

        list.addAll(ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_BUILDER));
        return list;
    }

    /**
     * Marking that no locking should be done during the build.
     */
    public ISchedulingRule getRule() {
        return null;
    }

    @Override
    public ISchedulingRule getRule(int kind, Map<String, String> args) {
        return null;
    }

    /**
     * Builds the project.
     *
     * @see org.eclipse.core.internal.events InternalBuilder#build(int, java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {

        if (PyDevBuilderPrefPage.usePydevBuilders() == false) {
            return null;
        }

        if (kind == IncrementalProjectBuilder.FULL_BUILD || kind == IncrementalProjectBuilder.CLEAN_BUILD) {
            // Do a Full Build: Use a ResourceVisitor to process the tree.
            //Timer timer = new Timer();
            performFullBuild(monitor);
            //timer.printDiff("Total time for analysis of: " + getProject());

        } else {
            // Build it with a delta
            IResourceDelta delta = getDelta(getProject());

            if (delta == null) {
                //no delta (unspecified changes?... let's do a full build...)
                performFullBuild(monitor);

            } else {
                VisitorMemo memo = new VisitorMemo();
                memo.put(PyDevBuilderVisitor.IS_FULL_BUILD, false); //mark it as delta build

                // ok, we have a delta
                // first step is just counting them
                PyDevDeltaCounter counterVisitor = new PyDevDeltaCounter();
                counterVisitor.memo = memo;
                delta.accept(counterVisitor);

                List<PyDevBuilderVisitor> visitors = getVisitors();

                //sort by priority
                Collections.sort(visitors);

                PydevGrouperVisitor grouperVisitor = new PydevGrouperVisitor(visitors, monitor,
                        counterVisitor.getNVisited());
                grouperVisitor.memo = memo;

                try (AutoCloseable closeable = withStartEndVisitingNotifications(visitors, monitor, false, null)) {
                    try {
                        delta.accept(grouperVisitor);
                    } catch (Exception e) {
                        Log.log(e);
                    }
                    try {
                        grouperVisitor.finishDelayedVisits();
                    } catch (Exception e) {
                        Log.log(e);
                    }
                } catch (Exception e1) {
                    Log.log(e1);
                }
            }
        }
        return null;
    }

    /**
     * Processes all python files.
     *
     * @param monitor
     */
    private void performFullBuild(IProgressMonitor monitor) throws CoreException {
        IProject project = getProject();

        //we need the project...
        if (project != null) {
            IPythonNature nature = PythonNature.getPythonNature(project);

            //and the nature...
            if (nature != null && nature.startRequests()) {

                try {
                    IPythonPathNature pythonPathNature = nature.getPythonPathNature();
                    pythonPathNature.getProjectSourcePath(false); //this is just to update the paths (in case the project name has just changed)

                    List<IFile> resourcesToParse = new ArrayList<IFile>();

                    List<PyDevBuilderVisitor> visitors = getVisitors();
                    try (AutoCloseable closable = withStartEndVisitingNotifications(visitors, monitor, true, nature)) {
                        monitor.beginTask("Building...", (visitors.size() * 100) + 30);

                        IResource[] members = project.members();

                        if (members != null) {
                            // get all the python files to get information.
                            int len = members.length;
                            for (int i = 0; i < len; i++) {
                                try {
                                    IResource member = members[i];
                                    if (member == null) {
                                        continue;
                                    }

                                    if (member.getType() == IResource.FILE) {
                                        addToResourcesToParse(resourcesToParse, (IFile) member, nature);

                                    } else if (member.getType() == IResource.FOLDER) {
                                        //if it is a folder, let's get all python files that are beneath it
                                        //the heuristics to know if we have to analyze them are the same we have
                                        //for a single file
                                        List<IFile> files = PyFileListing.getAllIFilesBelow((IFolder) member);

                                        for (IFile file : files) {
                                            if (file != null) {
                                                addToResourcesToParse(resourcesToParse, file, nature);
                                            }
                                        }
                                    } else {
                                        if (DEBUG) {
                                            System.out.println("Unknown type: " + member.getType());
                                        }
                                    }
                                } catch (Exception e) {
                                    // that's ok...
                                }
                            }
                            monitor.worked(30);
                            buildResources(resourcesToParse, monitor, visitors);
                        }
                    } catch (Exception e1) {
                        Log.log(e1);
                    }

                } finally {
                    nature.endRequests();
                }
            }
        }
        monitor.done();

    }

    private AutoCloseable withStartEndVisitingNotifications(final List<PyDevBuilderVisitor> visitors,
            final IProgressMonitor monitor,
            boolean isFullBuild, IPythonNature nature) {
        for (PyDevBuilderVisitor visitor : visitors) {
            try {
                visitor.visitingWillStart(monitor, isFullBuild, nature);
            } catch (Throwable e) {
                Log.log(e);
            }
        }
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                for (PyDevBuilderVisitor visitor : visitors) {
                    try {
                        visitor.visitingEnded(monitor);
                    } catch (Throwable e) {
                        Log.log(e);
                    }
                }
            }
        };
    }

    /**
     * @param resourcesToParse the list where the resource may be added
     * @param member the resource we are adding
     * @param nature the nature associated to the resource
     */
    private void addToResourcesToParse(List<IFile> resourcesToParse, IFile member, IPythonNature nature) {
        //analyze it only if it is a valid source file
        String fileExtension = member.getFileExtension();
        if (DEBUG) {
            System.out.println("Checking name:'" + member.getName() + "' projPath:'" + member.getProjectRelativePath()
                    + "' ext:'" + fileExtension + "'");
            System.out.println("loc:'" + member.getLocation() + "' rawLoc:'" + member.getRawLocation() + "'");

        }
        if (fileExtension != null && PythonPathHelper.isValidSourceFile("." + fileExtension)) {
            if (DEBUG) {
                System.out.println("Adding resource to parse:" + member.getProjectRelativePath());
            }
            resourcesToParse.add(member);
        }
    }

    /**
     * Default implementation. Visits each resource once at a time. May be overridden if a better implementation is needed.
     *
     * @param resourcesToParse list of resources from project that are python files.
     * @param monitor
     * @param visitors
     */
    public void buildResources(List<IFile> resourcesToParse, IProgressMonitor monitor,
            List<PyDevBuilderVisitor> visitors) {

        // we have 100 units here
        double inc = (visitors.size() * 100) / (double) resourcesToParse.size();

        double total = 0;
        int totalResources = resourcesToParse.size();
        int i = 0;

        FastStringBuffer bufferToCreateString = new FastStringBuffer();

        boolean loggedMisconfiguration = false;
        long lastProgressTime = 0;

        Object memoSharedProjectState = null;
        for (Iterator<IFile> iter = resourcesToParse.iterator(); iter.hasNext() && monitor.isCanceled() == false;) {
            i += 1;
            total += inc;
            IFile r = iter.next();

            PythonPathHelper.markAsPyDevFileIfDetected(r);

            IPythonNature nature = PythonNature.getPythonNature(r);
            if (nature == null) {
                continue;
            }
            if (!nature.startRequests()) {
                continue;
            }
            try {
                String moduleName;
                try {
                    //we visit external because we must index them
                    moduleName = nature.resolveModuleOnlyInProjectSources(r, true);
                    if (moduleName == null) {
                        continue; // we only analyze resources that are in the pythonpath
                    }
                } catch (Exception e1) {
                    if (!loggedMisconfiguration) {
                        loggedMisconfiguration = true; //No point in logging it over and over again.
                        Log.log(e1);
                    }
                    continue;
                }

                //create new memo for each resource
                VisitorMemo memo = new VisitorMemo();
                memo.setSharedProjectState(memoSharedProjectState);
                memo.put(PyDevBuilderVisitor.IS_FULL_BUILD, true); //mark it as full build

                ICallback0<IDocument> doc = FileUtilsFileBuffer.getDocOnCallbackFromResource(r);
                memo.put(PyDevBuilderVisitor.DOCUMENT_TIME, System.currentTimeMillis());

                PyDevBuilderVisitor.setModuleNameInCache(memo, r, moduleName);

                for (Iterator<PyDevBuilderVisitor> it = visitors.iterator(); it.hasNext()
                        && monitor.isCanceled() == false;) {

                    try {
                        PyDevBuilderVisitor visitor = it.next();
                        visitor.memo = memo; //setting the memo must be the first thing.

                        long currentTimeMillis = System.currentTimeMillis();
                        if (currentTimeMillis - lastProgressTime > 300) {
                            communicateProgress(monitor, totalResources, i, r, visitor, bufferToCreateString);
                            lastProgressTime = currentTimeMillis;
                        }

                        //on a full build, all visits are as some add...
                        visitor.visitAddedResource(r, doc, monitor);
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }

                if (total > 1) {
                    monitor.worked((int) total);
                    total -= (int) total;
                }
                memoSharedProjectState = memo.getSharedProjectState();
            } finally {
                nature.endRequests();
            }
        }
    }

    /**
     * Used so that we can communicate the progress to the user
     *
     * @param bufferToCreateString: this is a buffer that's emptied and used to create the string to be shown to the
     * user with the progress.
     */
    public static void communicateProgress(IProgressMonitor monitor, int totalResources, int i, IResource r,
            PyDevBuilderVisitor visitor, FastStringBuffer bufferToCreateString) {
        if (monitor != null) {
            bufferToCreateString.clear();
            bufferToCreateString.append("PyDev: Analyzing ");
            bufferToCreateString.append(i);
            bufferToCreateString.append(" of ");
            bufferToCreateString.append(totalResources);
            bufferToCreateString.append(" (");
            bufferToCreateString.append(r.getName());
            bufferToCreateString.append(")");

            //in this case the visitor does not have the progress and therefore does not communicate the progress
            String name = bufferToCreateString.toString();
            monitor.subTask(name);
        }
    }

}
