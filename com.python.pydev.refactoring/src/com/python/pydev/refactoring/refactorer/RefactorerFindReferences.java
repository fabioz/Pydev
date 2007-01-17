/*
 * Created on Dec 9, 2006
 * @author Fabio
 */
package com.python.pydev.refactoring.refactorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.TextSearchQueryProvider;
import org.eclipse.search.ui.text.TextSearchQueryProvider.TextSearchInput;
import org.python.pydev.core.REF;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Refactorer used to find the references given some refactoring request.
 * 
 * @author Fabio
 */
public class RefactorerFindReferences {
    
    /**
     * If this field is not null, the return will be forced without actually doing
     * a search in files.
     * 
     * This is intended to help in testing features that depend on the search.
     */
    public static List<IFile> FORCED_RETURN;
    
    /**
     * class used to configure the input for a text search.
     */
    private static class PyTextSearchInput extends TextSearchInput {
        
        private final String fSearchText;
        private final boolean fIsCaseSensitive;
        private final boolean fIsRegEx;
        private final FileTextSearchScope fScope;

        public PyTextSearchInput(String searchText, boolean isCaseSensitive, boolean isRegEx, FileTextSearchScope scope) {
            fSearchText= searchText;
            fIsCaseSensitive= isCaseSensitive;
            fIsRegEx= isRegEx;
            fScope= scope;
        }

        public String getSearchText() {
            return fSearchText;
        }

        public boolean isCaseSensitiveSearch() {
            return fIsCaseSensitive;
        }

        public boolean isRegExSearch() {
            return fIsRegEx;
        }

        public FileTextSearchScope getScope() {
            return fScope;
        }
    }


    /**
     * Find the references that may have the text we're looking for.
     * 
     * @param request the request with the info for the find
     * @return an array of IFile with the files that may have the references we're
     * interested about (note that those may not actually contain the matches we're
     * interested in -- it is just a helper to refine our search).
     */
    public List<IFile> findPossibleReferences(RefactoringRequest request) {
        if(FORCED_RETURN != null){
            ArrayList<IFile> ret = new ArrayList<IFile>();
            for(IFile f: FORCED_RETURN){
                //only for testing purposes
                String object = (String) REF.invoke(f, "getFileContents", new Object[0]);
                if(object.indexOf(request.duringProcessInfo.initialName) != -1){
                    ret.add(f);
                }
            }
            return ret;
        }
        
        List<IFile> l = new ArrayList<IFile>();
        
        try {
            TextSearchQueryProvider searchQueryProvider = TextSearchQueryProvider.getPreferred();
            
            ArrayList<IProject> resourcesToSearch = new ArrayList<IProject>();
            IProject project = request.nature.getProject();
            resourcesToSearch.addAll(Arrays.asList(project.getReferencingProjects()));
            resourcesToSearch.addAll(Arrays.asList(project.getReferencedProjects()));
            resourcesToSearch.add(project);
            
            TextSearchInput textSearchInput = new PyTextSearchInput(request.duringProcessInfo.initialName, 
                    true, false, FileTextSearchScope.newSearchScope(resourcesToSearch.toArray(new IProject[0]),
                            PythonPathHelper.WILDCARD_VALID_SOURCE_FILES, true));
            
            final ISearchQuery query = searchQueryProvider.createQuery(textSearchInput);
            IStatus status = query.run(new NullProgressMonitor());
            
            if (status.matches(IStatus.CANCEL)) {
                return l;
            }
            
            if (!status.isOK()){
                PydevPlugin.log(status);
                
                if (status.getSeverity() == IStatus.ERROR) {
                    ErrorDialog.openError(PyAction.getShell(), "Error when searching for references", "Error when searching for references", status);
                }
                return l;
            }
                
            //all is ok
            AbstractTextSearchResult searchResult = (AbstractTextSearchResult) query.getSearchResult();
            Object[] elements = searchResult.getElements();
            for (Object object : elements) {
                if (object instanceof IFile) {
                    l.add((IFile) object);
                }
            }
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
        return l;
    }

}
