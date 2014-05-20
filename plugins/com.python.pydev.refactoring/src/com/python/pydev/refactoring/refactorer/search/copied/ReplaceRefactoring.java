/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.search.copied;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextEditChangeGroup;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.search.internal.ui.Messages;
import org.eclipse.search.ui.text.Match;
import org.eclipse.search2.internal.ui.InternalSearchUI;
import org.eclipse.search2.internal.ui.text.PositionTracker;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.refactoring.core.base.PyTextFileChange;

import com.python.pydev.refactoring.ChangedFilesChecker;
import com.python.pydev.refactoring.refactorer.search.AbstractPythonSearchQuery;
import com.python.pydev.refactoring.refactorer.search.PythonFileSearchResult;
import com.python.pydev.ui.search.FileMatch;
import com.python.pydev.ui.search.LineElement;
import com.python.pydev.ui.search.SearchMessages;

@SuppressWarnings("restriction")
public class ReplaceRefactoring extends Refactoring {

    private static class MatchGroup {
        public TextEditChangeGroup group;
        public FileMatch match;

        public MatchGroup(TextEditChangeGroup group, FileMatch match) {
            this.group = group;
            this.match = match;
        }
    }

    public static class SearchResultUpdateChange extends Change {

        private MatchGroup[] fMatchGroups;
        private Match[] fMatches;
        private final PythonFileSearchResult fResult;
        private final boolean fIsRemove;

        public SearchResultUpdateChange(PythonFileSearchResult result, MatchGroup[] matchGroups, boolean isRemove) {
            fResult = result;
            fMatchGroups = matchGroups;
            fMatches = null;
            fIsRemove = isRemove;
        }

        public SearchResultUpdateChange(PythonFileSearchResult result, Match[] matches, boolean isRemove) {
            fResult = result;
            fMatches = matches;
            fMatchGroups = null;
            fIsRemove = isRemove;
        }

        @Override
        public Object getModifiedElement() {
            return null;
        }

        @Override
        public String getName() {
            return SearchMessages.ReplaceRefactoring_result_update_name;
        }

        @Override
        public void initializeValidationData(IProgressMonitor pm) {
        }

        @Override
        public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
            return new RefactoringStatus();
        }

        private Match[] getMatches() {
            if (fMatches == null) {
                ArrayList<FileMatch> matches = new ArrayList<FileMatch>();
                for (int i = 0; i < fMatchGroups.length; i++) {
                    MatchGroup curr = fMatchGroups[i];
                    if (curr.group.isEnabled()) {
                        matches.add(curr.match);
                    }
                }
                fMatches = matches.toArray(new Match[matches.size()]);
                fMatchGroups = null;
            }
            return fMatches;
        }

        @Override
        public Change perform(IProgressMonitor pm) throws CoreException {
            Match[] matches = getMatches();
            if (fIsRemove) {
                fResult.removeMatches(matches);
            } else {
                fResult.addMatches(matches);
            }
            return new SearchResultUpdateChange(fResult, matches, !fIsRemove);
        }

    }

    private final PythonFileSearchResult fResult;
    private final Object[] fSelection;
    private final boolean fSkipFiltered;

    private HashMap/*<IFile,Set<Match>*/fMatches;

    private String fReplaceString;

    private Change fChange;

    public ReplaceRefactoring(PythonFileSearchResult result, Object[] selection, boolean skipFiltered) {
        Assert.isNotNull(result);

        fResult = result;
        fSelection = selection;
        fSkipFiltered = skipFiltered;

        fMatches = new HashMap();

        fReplaceString = null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
     */
    @Override
    public String getName() {
        return SearchMessages.ReplaceRefactoring_refactoring_name;
    }

    public void setReplaceString(String string) {
        fReplaceString = string;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException,
            OperationCanceledException {
        String searchString = getQuery().getSearchString();
        if (searchString.length() == 0) {
            return RefactoringStatus
                    .createFatalErrorStatus(SearchMessages.ReplaceRefactoring_error_illegal_search_string);
        }
        fMatches.clear();

        if (fSelection != null) {
            for (int i = 0; i < fSelection.length; i++) {
                collectMatches(fSelection[i]);
            }
        } else {
            Object[] elements = fResult.getElements();
            for (int i = 0; i < elements.length; i++) {
                collectMatches(elements[i]);
            }
        }
        if (!hasMatches()) {
            return RefactoringStatus.createFatalErrorStatus(SearchMessages.ReplaceRefactoring_error_no_matches);
        }
        return new RefactoringStatus();
    }

    @SuppressWarnings("unchecked")
    private void collectMatches(Object object) throws CoreException {
        if (object instanceof LineElement) {
            LineElement lineElement = (LineElement) object;
            FileMatch[] matches = lineElement.getMatches(fResult);
            for (int i = 0; i < matches.length; i++) {
                FileMatch fileMatch = matches[i];
                if (!isSkipped(fileMatch)) {
                    getBucket(fileMatch.getFile()).add(fileMatch);
                }
            }
        } else if (object instanceof IContainer) {
            IContainer container = (IContainer) object;
            IResource[] members = container.members();
            for (int i = 0; i < members.length; i++) {
                collectMatches(members[i]);
            }
        } else if (object instanceof IFile) {
            Match[] matches = fResult.getMatches(object);
            if (matches.length > 0) {
                Collection<FileMatch> bucket = null;
                for (int i = 0; i < matches.length; i++) {
                    FileMatch fileMatch = (FileMatch) matches[i];
                    if (!isSkipped(fileMatch)) {
                        if (bucket == null) {
                            bucket = getBucket((IFile) object);
                        }
                        bucket.add(fileMatch);
                    }
                }
            }
        }
    }

    public int getNumberOfFiles() {
        return fMatches.keySet().size();
    }

    public int getNumberOfMatches() {
        int count = 0;
        for (Iterator iterator = fMatches.values().iterator(); iterator.hasNext();) {
            Collection bucket = (Collection) iterator.next();
            count += bucket.size();
        }
        return count;
    }

    public boolean hasMatches() {
        return !fMatches.isEmpty();
    }

    private boolean isSkipped(FileMatch match) {
        return !fSkipFiltered && match.isFiltered();
    }

    @SuppressWarnings("unchecked")
    private Collection getBucket(IFile file) {
        Collection col = (Collection) fMatches.get(file);
        if (col == null) {
            col = new HashSet();
            fMatches.put(file, col);
        }
        return col;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    @SuppressWarnings("unchecked")
    public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        if (fReplaceString == null) {
            return RefactoringStatus.createFatalErrorStatus(SearchMessages.ReplaceRefactoring_error_no_replace_string);
        }

        Pattern pattern = null;
        AbstractPythonSearchQuery query = getQuery();
        if (query.isRegexSearch()) {
            pattern = createSearchPattern(query);
        }

        RefactoringStatus resultingStatus = new RefactoringStatus();

        Collection<IFile> allFiles = fMatches.keySet();
        ChangedFilesChecker.checkFiles(allFiles, getValidationContext(), resultingStatus);
        if (resultingStatus.hasFatalError()) {
            return resultingStatus;
        }

        CompositeChange compositeChange = new CompositeChange(SearchMessages.ReplaceRefactoring_composite_change_name);
        compositeChange.markAsSynthetic();

        ArrayList matchGroups = new ArrayList();
        boolean hasChanges = false;
        try {
            for (Iterator iterator = fMatches.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                IFile file = (IFile) entry.getKey();
                Collection bucket = (Collection) entry.getValue();
                if (!bucket.isEmpty()) {
                    try {
                        TextChange change = createFileChange(file, pattern, bucket, resultingStatus, matchGroups);
                        if (change != null) {
                            compositeChange.add(change);
                            hasChanges = true;
                        }
                    } catch (CoreException e) {
                        String message = Messages.format(SearchMessages.ReplaceRefactoring_error_access_file,
                                new Object[] { file.getName(), e.getLocalizedMessage() });
                        return RefactoringStatus.createFatalErrorStatus(message);
                    }
                }
            }
        } catch (PatternSyntaxException e) {
            String message = Messages.format(SearchMessages.ReplaceRefactoring_error_replacement_expression,
                    e.getLocalizedMessage());
            return RefactoringStatus.createFatalErrorStatus(message);
        }
        if (!hasChanges && resultingStatus.isOK()) {
            return RefactoringStatus.createFatalErrorStatus(SearchMessages.ReplaceRefactoring_error_no_changes);
        }

        compositeChange.add(new SearchResultUpdateChange(fResult, (MatchGroup[]) matchGroups
                .toArray(new MatchGroup[matchGroups.size()]), true));

        fChange = compositeChange;
        return resultingStatus;
    }

    @SuppressWarnings("unchecked")
    private TextChange createFileChange(IFile file, Pattern pattern, Collection/*FileMatch*/matches,
            RefactoringStatus resultingStatus, Collection matchGroups) throws PatternSyntaxException, CoreException {
        PositionTracker tracker = InternalSearchUI.getInstance().getPositionTracker();

        TextFileChange change = new PyTextFileChange(Messages.format(
                SearchMessages.ReplaceRefactoring_group_label_change_for_file, file.getName()), file);
        change.setEdit(new MultiTextEdit());

        ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
        manager.connect(file.getFullPath(), LocationKind.IFILE, null);
        try {
            ITextFileBuffer textFileBuffer = manager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
            if (textFileBuffer == null) {
                resultingStatus.addError(Messages.format(SearchMessages.ReplaceRefactoring_error_accessing_file_buffer,
                        file.getName()));
                return null;
            }
            IDocument document = textFileBuffer.getDocument();
            String lineDelimiter = TextUtilities.getDefaultLineDelimiter(document);

            for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
                FileMatch match = (FileMatch) iterator.next();
                int offset = match.getOffset();
                int length = match.getLength();
                Position currentPosition = tracker.getCurrentPosition(match);
                if (currentPosition != null) {
                    offset = currentPosition.offset;
                    if (length != currentPosition.length) {
                        resultingStatus.addError(Messages.format(
                                SearchMessages.ReplaceRefactoring_error_match_content_changed, file.getName()));
                        continue;
                    }
                }

                String originalText = getOriginalText(document, offset, length);
                if (originalText == null) {
                    resultingStatus.addError(Messages.format(
                            SearchMessages.ReplaceRefactoring_error_match_content_changed, file.getName()));
                    continue;
                }

                String replacementString = computeReplacementString(pattern, originalText, fReplaceString,
                        lineDelimiter);
                if (replacementString == null) {
                    resultingStatus.addError(Messages.format(
                            SearchMessages.ReplaceRefactoring_error_match_content_changed, file.getName()));
                    continue;
                }

                ReplaceEdit replaceEdit = new ReplaceEdit(offset, length, replacementString);
                change.addEdit(replaceEdit);
                TextEditChangeGroup textEditChangeGroup = new TextEditChangeGroup(change, new TextEditGroup(
                        SearchMessages.ReplaceRefactoring_group_label_match_replace, replaceEdit));
                change.addTextEditChangeGroup(textEditChangeGroup);
                matchGroups.add(new MatchGroup(textEditChangeGroup, match));
            }
        } finally {
            manager.disconnect(file.getFullPath(), LocationKind.IFILE, null);
        }
        return change;
    }

    private static String getOriginalText(IDocument doc, int offset, int length) {
        try {
            return doc.get(offset, length);
        } catch (BadLocationException e) {
            return null;
        }
    }

    private Pattern createSearchPattern(AbstractPythonSearchQuery query) {
        return PatternConstructor.createPattern(query.getSearchString(), true, true, query.isCaseSensitive(), false);
    }

    private String computeReplacementString(Pattern pattern, String originalText, String replacementText,
            String lineDelimiter) throws PatternSyntaxException {
        if (pattern != null) {
            try {
                replacementText = PatternConstructor.interpretReplaceEscapes(replacementText, originalText,
                        lineDelimiter);

                Matcher matcher = pattern.matcher(originalText);
                StringBuffer sb = new StringBuffer();
                matcher.reset();
                if (matcher.find()) {
                    matcher.appendReplacement(sb, replacementText);
                } else {
                    return null;
                }
                matcher.appendTail(sb);
                return sb.toString();
            } catch (IndexOutOfBoundsException ex) {
                throw new PatternSyntaxException(ex.getLocalizedMessage(), replacementText, -1);
            }
        }
        return replacementText;
    }

    public AbstractPythonSearchQuery getQuery() {
        return (AbstractPythonSearchQuery) fResult.getQuery();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        return fChange;
    }

}
