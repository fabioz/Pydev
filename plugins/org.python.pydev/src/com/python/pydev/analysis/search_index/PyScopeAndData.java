package com.python.pydev.analysis.search_index;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_ui.search.AbstractSearchResultsViewerFilter.IMatcher;
import org.python.pydev.shared_ui.search.ScopeAndData;
import org.python.pydev.shared_ui.search.SearchIndexData;

public class PyScopeAndData {

    public static List<IPythonNature> getPythonNatures(ScopeAndData scopeAndData) {
        if (scopeAndData.scope == SearchIndexData.SCOPE_PROJECTS) {
            IMatcher matcher = PySearchResultsViewerFilter.createMatcher(scopeAndData.scopeData, true);
            ArrayList<IPythonNature> ret = new ArrayList<>();
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            for (IProject project : workspace.getRoot().getProjects()) {
                if (project != null && project.exists() && project.isOpen()) {
                    if (PySearchResultsViewerFilter.filterMatches(project.getName(), matcher)) {
                        ret.add(PythonNature.getPythonNature(project));
                    }
                }
            }
            if (ret.size() == 0) {
                Log.log("Unable to resolve projects to search from string: '" + scopeAndData.scopeData
                        + "' (searching workspace).");
                ret.addAll(PythonNature.getAllPythonNatures());
            }
            return ret;
        }

        if (scopeAndData.scope == SearchIndexData.SCOPE_MODULES) {
            ArrayList<IPythonNature> ret = new ArrayList<>();

            IMatcher matcher = PySearchResultsViewerFilter.createMatcher(scopeAndData.scopeData, true);

            List<IPythonNature> allPythonNatures = PythonNature.getAllPythonNatures();
            for (IPythonNature nature : allPythonNatures) {
                Set<String> allModuleNames = nature.getAstManager().getModulesManager().getAllModuleNames(false, "");
                for (String s : allModuleNames) {
                    if (PySearchResultsViewerFilter.filterMatches(s, matcher)) {
                        ret.add(nature);
                        break;
                    }
                }
            }
            return ret;
        }
        if (scopeAndData.scope == SearchIndexData.SCOPE_WORKSPACE) {
            return PythonNature.getAllPythonNatures();
        }

        Log.log("Unable to deal with scope: " + scopeAndData.scope + ". Searching workspace.");
        return PythonNature.getAllPythonNatures();
    }

}
