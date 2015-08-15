/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.search_index;

import java.util.Collection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.search.ui.NewSearchUI;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_ui.search.AbstractSearchIndexPage;
import org.python.pydev.shared_ui.search.ScopeAndData;
import org.python.pydev.shared_ui.search.SearchIndexData;

import com.python.pydev.analysis.AnalysisPlugin;

public class PySearchIndexPage extends AbstractSearchIndexPage {

    public PySearchIndexPage() {
        super(AnalysisPlugin.getDefault());
    }

    @Override
    public boolean performAction() {
        ScopeAndData scopeAndData = getScopeAndData();
        SearchIndexData data = new SearchIndexData(fPattern.getText(), fIsCaseSensitiveCheckbox.getSelection(),
                fIsWholeWordCheckbox.getSelection(), scopeAndData.scope, scopeAndData.scopeData, "*"); // filenamePattern is always * for Python searches (we'll always be searching the whole index).
        PySearchIndexQuery query = new PySearchIndexQuery(data);
        NewSearchUI.runQueryInBackground(query);
        searchIndexDataHistory.add(data);
        searchIndexDataHistory.writeConfiguration();
        return true;
    }

    @Override
    protected void checkSelectedResource(Collection<String> projectNames, Collection<String> moduleNames,
            IResource resource) {
        if (resource != null && resource.isAccessible()) {
            IProject project = resource.getProject();
            projectNames.add(project.getName());
            PythonNature nature = PythonNature.getPythonNature(project);
            String moduleName;
            try {
                moduleName = nature.resolveModule(resource);
            } catch (MisconfigurationException e) {
                Log.log(e);
                return;
            }
            if (moduleName != null) {
                for (String s : moduleNames) {
                    if (s.endsWith(".*")) {
                        if (moduleName.startsWith(s.substring(0, s.length() - 1))) {
                            //There's already another one which includes what we're about to add.
                            return;
                        }
                    }
                }
                if (resource instanceof IContainer) {
                    moduleNames.add(moduleName + ".*");
                } else {
                    moduleNames.add(moduleName);
                }
            }
        }
    }

}
