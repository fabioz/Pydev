/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IObjectActionDelegate;
import org.python.pydev.builder.PyDevBuilderVisitor;
import org.python.pydev.builder.VisitorMemo;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.IModule;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.ui.actions.resources.PyResourceAction;
import org.python.pydev.utils.PyFileListing;

import com.python.pydev.analysis.builder.AnalysisBuilderRunnable;
import com.python.pydev.analysis.builder.AnalysisBuilderVisitor;

/**
 * @author fabioz
 *
 */
public class ForceCodeAnalysisOnTree extends PyResourceAction implements IObjectActionDelegate {

    /* (non-Javadoc)
     * @see org.python.pydev.ui.actions.resources.PyResourceAction#confirmRun()
     */
    @Override
    protected boolean confirmRun() {
        return true;
    }

    private Set<IFile> filesVisited = new HashSet<IFile>();

    /* (non-Javadoc)
     * @see org.python.pydev.ui.actions.resources.PyResourceAction#afterRun(int)
     */
    @Override
    protected void afterRun(int resourcesAffected) {
        filesVisited.clear();
    }

    /* (non-Javadoc)
     * @see org.python.pydev.ui.actions.resources.PyResourceAction#beforeRun()
     */
    @Override
    protected void beforeRun() {
        filesVisited.clear();
    }

    /* (non-Javadoc)
     * @see org.python.pydev.ui.actions.resources.PyResourceAction#doActionOnResource(org.eclipse.core.resources.IResource, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected int doActionOnResource(IResource next, IProgressMonitor monitor) {
        List<IFile> filesToVisit = new ArrayList<IFile>();
        if (next instanceof IContainer) {
            List<IFile> l = PyFileListing.getAllIFilesBelow((IContainer) next);

            for (Iterator<IFile> iter = l.iterator(); iter.hasNext();) {
                IFile element = iter.next();
                if (element != null) {
                    filesToVisit.add(element);
                }
            }
        } else if (next instanceof IFile) {
            filesToVisit.add((IFile) next);
        }

        PythonNature nature = PythonNature.getPythonNature(next);
        AnalysisBuilderVisitor visitor = new AnalysisBuilderVisitor();
        visitor.visitingWillStart(new NullProgressMonitor(), false, null);
        FastStringBuffer buf = new FastStringBuffer();
        for (IFile f : filesToVisit) {
            if (monitor.isCanceled()) {
                break;
            }
            if (filesVisited.contains(f)) {
                continue;
            }
            filesVisited.add(f);
            monitor.setTaskName(buf.clear().append("Scheduling: ").append(f.getName()).toString());
            IDocument doc = FileUtilsFileBuffer.getDocFromResource(f);
            visitor.memo = new VisitorMemo();
            visitor.memo.put(PyDevBuilderVisitor.IS_FULL_BUILD, false);
            long documentTime = System.currentTimeMillis();
            visitor.memo.put(PyDevBuilderVisitor.DOCUMENT_TIME, documentTime);
            String moduleName;
            try {
                moduleName = nature.resolveModule(f);
            } catch (MisconfigurationException e) {
                Log.log(e);
                continue;
            }
            if (moduleName == null) {
                continue;
            }
            AnalysisBuilderVisitor.setModuleNameInCache(visitor.memo, f, moduleName);
            IModule module = nature.getAstManager().getModule(moduleName, nature, true);
            if (module == null) {
                Log.log(IStatus.WARNING, "Unable to get module: " + moduleName + " for resource: " + f, null);
                continue;
            }
            visitor.doVisitChangedResource(nature, f, doc, null, module, new NullProgressMonitor(), true,
                    AnalysisBuilderRunnable.ANALYSIS_CAUSE_PARSER, documentTime, false);
        }
        visitor.visitingEnded(new NullProgressMonitor());
        return 1;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.ui.actions.resources.PyResourceAction#needsUIThread()
     */
    @Override
    protected boolean needsUIThread() {
        return false;
    }
}
