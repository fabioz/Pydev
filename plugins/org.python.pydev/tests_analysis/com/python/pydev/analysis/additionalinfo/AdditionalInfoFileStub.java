package com.python.pydev.analysis.additionalinfo;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.shared_core.resource_stubs.AbstractIFileStub;

public class AdditionalInfoFileStub extends AbstractIFileStub implements IFile {

    public final String name;
    private String contents;
    public boolean created;

    public AdditionalInfoFileStub(String name) {
        this.name = name;
    }

    public String getStrContents() {
        return contents;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void create(InputStream source, boolean force, IProgressMonitor monitor) throws CoreException {
        created = true;
        try {
            int i = source.available();
            byte[] bs = new byte[i];
            source.read(bs);
            this.contents = new String(bs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IPath getRawLocation() {
        return null;
    }
}
