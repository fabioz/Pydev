/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 28, 2006
 * @author Fabio
 */
package org.python.pydev.plugin.nature;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.shared_core.resource_stubs.AbstractIFileStub;

public class FileStub2 extends AbstractIFileStub implements IFile {

    public final String name;
    private String contents;
    public boolean created;

    public FileStub2(String name) {
        this.name = name;
    }

    public String getStrContents() {
        return contents;
    }

    @Override
    public String getName() {
        return this.name;
    }

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
