package org.python.pydev.shared_core.path_watch;

import java.io.File;
import java.io.FileFilter;

public interface IPathWatch {

    void stopTrack(File path, IFilesystemChangesListener listener);

    boolean hasTracker(File path, IFilesystemChangesListener listener);

    void dispose();

    /**
     * A listener will start tracking changes at the given path.
     */
    void track(File path, IFilesystemChangesListener listener);

    void setDirectoryFileFilter(FileFilter fileFilter, FileFilter dirsFilter);

}