package org.python.pydev.editorinput;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.plugin.PydevPlugin;

/**
 * This class enables Eclipse to get the contents from a file that was found within a zip file. 
 * 
 * @author Fabio
 */
public class PydevZipFileStorage implements IStorage{

    private final File zipFile;
    private final String zipPath;

    public PydevZipFileStorage(File zipFile, String zipPath){
        this.zipFile = zipFile;
        this.zipPath = zipPath;
    }
    
    public InputStream getContents() throws CoreException {
        try {
            ZipFile f = new ZipFile(this.zipFile);
            return f.getInputStream(f.getEntry(this.zipPath));
        } catch (Exception e) {
            throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR, "Error getting contents from zip file", e));
        }
    }

    public IPath getFullPath() {
        return Path.fromOSString(this.zipFile.getAbsolutePath()).append(new Path(this.zipPath));
    }

    public String getName() {
    	List<String> split = StringUtils.split(zipPath, '/');
    	if(split.size() > 0){
    		return split.get(split.size()-1);
    	}
        return this.zipPath;
    }

    public boolean isReadOnly() {
        return true;
    }

    public Object getAdapter(Class adapter) {
        return null;
    }

}
