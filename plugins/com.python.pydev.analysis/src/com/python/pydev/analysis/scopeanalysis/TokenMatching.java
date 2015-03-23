/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.scopeanalysis;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;

public class TokenMatching {

    public static class ReusableMatchAccess extends TextSearchMatchAccess {

        private int fOffset;
        private int fLength;
        private IFile fFile;
        private CharSequence fContent;

        public void initialize(IFile file, int offset, int length, CharSequence content) {
            fFile = file;
            fOffset = offset;
            fLength = length;
            fContent = content;
        }

        @Override
        public IFile getFile() {
            return fFile;
        }

        @Override
        public int getMatchOffset() {
            return fOffset;
        }

        @Override
        public int getMatchLength() {
            return fLength;
        }

        @Override
        public int getFileContentLength() {
            return fContent.length();
        }

        @Override
        public char getFileContentChar(int offset) {
            return fContent.charAt(offset);
        }

        @Override
        public String getFileContent(int offset, int length) {
            return fContent.subSequence(offset, offset + length).toString(); // must pass a copy!
        }
    }

    private final ReusableMatchAccess fMatchAccess;
    private final TextSearchRequestor fCollector;
    private final CharSequence fSearchText;

    public TokenMatching(TextSearchRequestor collector, ReusableMatchAccess matchAccess, CharSequence searchText) {
        this.fCollector = collector;
        this.fMatchAccess = matchAccess;
        this.fSearchText = searchText;
    }

    public TokenMatching(TextSearchRequestor collector, CharSequence searchText) {
        this(collector, new ReusableMatchAccess(), searchText);
    }

    public TokenMatching(CharSequence searchText) {
        this(new TextSearchRequestor() {
        }, new ReusableMatchAccess(), searchText);
    }

    /**
     * @return whether we have some match (will collect the first match and return)
     */
    public boolean hasMatch(String searchInput) throws CoreException {
        return hasMatch(null, searchInput, new NullProgressMonitor());
    }

    /**
     * @return whether we have some match (will collect the first match and return)
     */
    public boolean hasMatch(IFile file, String searchInput, IProgressMonitor monitor) throws CoreException {
        return collectMatches(null, searchInput, new NullProgressMonitor(), true);
    }

    /**
     * This method will return true if there is any match in the given searchInput regarding the
     * fSearchText.
     *
     * It will call the TextSearchRequestor.acceptPatternMatch on the first match and then bail out...
     *
     * @note that it has to be a 'token' match, and not only a substring match for it to be valid.
     *
     * @param file this is the file that contains the match
     * @param searchInput the sequence where we want to find the match
     * @return true if it did collect something and false otherwise
     * @throws CoreException
     */
    public boolean collectMatches(IFile file, final String searchInput, IProgressMonitor monitor, boolean onlyFirstMatch)
            throws CoreException {
        boolean foundMatch = false;
        try {
            int k = 0;
            int total = 0;
            char prev = (char) -1;
            final int searchTextLen = fSearchText.length();
            final int searchInputLen = searchInput.length();

            try {
                for (int i = 0; i < searchInputLen; i++) {
                    total += 1;
                    char c = searchInput.charAt(i);
                    if (c == fSearchText.charAt(k) && (k > 0 || !Character.isJavaIdentifierPart(prev))) {
                        k += 1;
                        if (k == searchTextLen) {
                            k = 0;

                            //now, we have to see if is really an 'exact' match (so, either we're in the last
                            //char or the next char is not actually a word)
                            boolean ok = false;
                            if (i + 1 == searchInputLen) {
                                ok = true;
                            } else {
                                c = searchInput.charAt(i + 1);
                                if (!Character.isJavaIdentifierPart(c)) {
                                    ok = true;
                                }
                            }

                            if (ok) {
                                fMatchAccess.initialize(file, i - searchTextLen + 1, searchTextLen, searchInput);
                                fCollector.acceptPatternMatch(fMatchAccess);
                                foundMatch = true;
                                if (onlyFirstMatch) {
                                    return foundMatch; //return on first match
                                }
                            }
                        }
                    } else {
                        k = 0;
                    }
                    prev = c;

                    if (total++ == 20) {
                        if (monitor.isCanceled()) {
                            throw new OperationCanceledException("Operation Canceled");
                        }
                        total = 0;
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                //That's Ok...
            }

        } finally {
            fMatchAccess.initialize(null, 0, 0, ""); // clear references
        }
        return foundMatch;
    }

    /**
     * @return a list with the offsets for the match in the full string.
     * Note that the offsets will be ordered
     * @throws CoreException
     */
    public static ArrayList<Integer> getMatchOffsets(String match, String fullString) throws CoreException {
        final ArrayList<Integer> offsets = new ArrayList<Integer>();
        TextSearchRequestor textSearchRequestor = new TextSearchRequestor() {
            @Override
            public boolean acceptPatternMatch(TextSearchMatchAccess matchAccess) throws CoreException {
                offsets.add(matchAccess.getMatchOffset());
                return true;
            }
        };
        TokenMatching matching = new TokenMatching(textSearchRequestor, match);
        matching.collectMatches(null, fullString, new NullProgressMonitor(), false);

        return offsets;
    }
}
