/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.search.copied;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.search.ui.IQueryListener;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.python.pydev.core.log.Log;


public class SearchResultUpdater implements IResourceChangeListener, IQueryListener {
    private AbstractTextSearchResult fResult;

    public SearchResultUpdater(AbstractTextSearchResult result) {
        fResult = result;
        NewSearchUI.addQueryListener(this);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        IResourceDelta delta = event.getDelta();
        if (delta != null)
            handleDelta(delta);
    }

    private void handleDelta(IResourceDelta d) {
        try {
            d.accept(new IResourceDeltaVisitor() {
                @Override
                public boolean visit(IResourceDelta delta) throws CoreException {
                    switch (delta.getKind()) {
                        case IResourceDelta.ADDED:
                            return false;
                        case IResourceDelta.REMOVED:
                            IResource res = delta.getResource();
                            if (res instanceof IFile) {
                                Match[] matches = fResult.getMatches(res);
                                fResult.removeMatches(matches);
                            }
                            break;
                        case IResourceDelta.CHANGED:
                            // handle changed resource
                            break;
                    }
                    return true;
                }
            });
        } catch (CoreException e) {
            Log.log(e);
        }
    }

    @Override
    public void queryAdded(ISearchQuery query) {
        // don't care
    }

    @Override
    public void queryRemoved(ISearchQuery query) {
        if (fResult.equals(query.getSearchResult())) {
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
            NewSearchUI.removeQueryListener(this);
        }
    }

    @Override
    public void queryStarting(ISearchQuery query) {
        // don't care
    }

    @Override
    public void queryFinished(ISearchQuery query) {
        // don't care
    }
}
