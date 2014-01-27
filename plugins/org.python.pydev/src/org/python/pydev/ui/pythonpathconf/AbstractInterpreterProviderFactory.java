/******************************************************************************
* Copyright (C) 2013  Jonah Graham
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.ui.pythonpathconf;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.python.pydev.shared_core.io.FileUtils;

public abstract class AbstractInterpreterProviderFactory implements IInterpreterProviderFactory {

    public AbstractInterpreterProviderFactory() {
        super();
    }

    private static final int INVALID = -1;

    private int matchesPattern(Pattern[] patterns, File afile) {
        // Add other conditions here if stricter file validation is necessary.
        for (int i = 0; i < patterns.length; i++) {
            Pattern pattern = patterns[i];
            if (pattern.matcher(afile.getName().toLowerCase()).matches()) {
                return i;
            }
        }
        return INVALID;
    }

    public String[] searchPaths(java.util.Set<String> pathsToSearch, final List<String> expectedPatterns) {
        return searchPaths(pathsToSearch, expectedPatterns, true);
    }

    /**
     * Searches a set of paths for files whose names match any of the provided patterns.
     * @param pathsToSearch The paths to search for files.
     * @param expectedPatterns A list of regex patterns that the filenames to find must match.
     * The patterns are in order of decreasing priority, meaning that filenames matching the
     * pattern at index i will appear earlier in the returned array than filenames matching
     * patterns at index i+1.
     * @return An array of all matching filenames found, in order of decreasing priority.
     */
    public String[] searchPaths(java.util.Set<String> pathsToSearch, final List<String> expectedPatterns,
            boolean followLinks) {
        int n = expectedPatterns.size();

        @SuppressWarnings("unchecked")
        LinkedHashSet<String>[] pathSetsByPriority = new LinkedHashSet[n];
        LinkedHashSet<String> prioritizedPaths = new LinkedHashSet<String>();

        Pattern[] patterns = new Pattern[n];
        for (int i = 0; i < n; i++) {
            patterns[i] = Pattern.compile(expectedPatterns.get(i));
        }

        for (String s : pathsToSearch) {
            String pathname = s.trim();
            if (pathname.length() > 0) {
                File file = new File(pathname);
                if (file.isDirectory()) {
                    File[] available = file.listFiles();
                    if (available != null) {
                        for (File afile : available) {
                            int priority = matchesPattern(patterns, afile);
                            if (priority != INVALID) {
                                if (pathSetsByPriority[priority] == null) {
                                    pathSetsByPriority[priority] = new LinkedHashSet<String>();
                                }
                                LinkedHashSet<String> pathSet = pathSetsByPriority[priority];
                                File f = new File(file, afile.getName());
                                if (!followLinks) {
                                    pathSet.add(FileUtils.getFileAbsolutePathNotFollowingLinks(f));
                                } else {
                                    pathSet.add(FileUtils.getFileAbsolutePath(f));
                                }
                            }
                        }
                    }
                }
            }
        }

        for (LinkedHashSet<String> pathSet : pathSetsByPriority) {
            if (pathSet != null) {
                prioritizedPaths.addAll(pathSet);
            }
        }
        return prioritizedPaths.toArray(new String[prioritizedPaths.size()]);
    }
}