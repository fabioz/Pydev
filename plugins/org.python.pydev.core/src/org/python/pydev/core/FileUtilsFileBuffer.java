/******************************************************************************
* Copyright (C) 2012-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.IllegalCharsetNameException;
import java.util.zip.ZipFile;

import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * File utilities that need access to:
 * - ITextFileBufferManager
 * - IProject/IResource
 *
 * Also, the functions to load documents may suppose they're dealing with Python files (i.e.:
 * to get the encoding to open the stream properly if we weren't able to get the stream from
 * the ITextFileBufferManager).
 */
public class FileUtilsFileBuffer {
    /**
     * Characters that files in the filesystem cannot have.
     */
    public static char[] INVALID_FILESYSTEM_CHARS = { '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '[', ']', '{',
            '}', '=', '+', '.', ' ', '`', '~', '\'', '"', ',', ';' };

    /**
     * @return a valid name for a project so that the returned name can be used to create a file in the filesystem
     */
    public static String getValidProjectName(IProject project) {
        String name = project.getName();

        for (char c : INVALID_FILESYSTEM_CHARS) {
            name = name.replace(c, '_');
        }

        return name;
    }

    public static IDocument getDocFromFile(java.io.File f) throws IOException {
        return getDocFromFile(f, true);
    }

    /**
     * @return a string with the contents from a path within a zip file.
     */
    public static String getStringFromZip(File f, String pathInZip) throws Exception {
        return (String) getCustomReturnFromZip(f, pathInZip, String.class);
    }

    /**
     * @return a document with the contents from a path within a zip file.
     */
    public static IDocument getDocFromZip(File f, String pathInZip) throws Exception {
        return (IDocument) getCustomReturnFromZip(f, pathInZip, IDocument.class);
    }

    /**
     * @param f the zip file that should be opened
     * @param pathInZip the path within the zip file that should be gotten
     * @param returnType the class that specifies the return type of this method.
     * If null, it'll return in the fastest possible way available.
     * Valid options are:
     *      String.class
     *      IDocument.class
     *      FastStringBuffer.class
     *
     * @return an object with the contents from a path within a zip file, having the return type
     * of the object specified by the parameter returnType.
     */
    public static Object getCustomReturnFromZip(File f, String pathInZip, Class<? extends Object> returnType)
            throws Exception {

        try (ZipFile zipFile = new ZipFile(f, ZipFile.OPEN_READ);) {
            try (InputStream inputStream = zipFile.getInputStream(zipFile.getEntry(pathInZip));) {
                return FileUtils.getStreamContents(inputStream, null, null, returnType);
            }
        }
    }

    /**
     * @return a string with the contents of the passed file
     */
    public static String getStringFromFile(java.io.File f, boolean loadIfNotInWorkspace) throws IOException {
        return (String) getCustomReturnFromFile(f, loadIfNotInWorkspace, String.class);
    }

    /**
     * @return the document given its 'filesystem' file
     */
    public static IDocument getDocFromFile(java.io.File f, boolean loadIfNotInWorkspace) throws IOException {
        return (IDocument) getCustomReturnFromFile(f, loadIfNotInWorkspace, IDocument.class);
    }

    /**
     * @param f the file from where we want to get the contents
     * @param returnType the class that specifies the return type of this method.
     * If null, it'll return in the fastest possible way available.
     * Valid options are:
     *      String.class
     *      IDocument.class
     *      FastStringBuffer.class
     *
     *
     * @return an object with the contents from the file, having the return type
     * of the object specified by the parameter returnType.
     */
    public static Object getCustomReturnFromFile(java.io.File f, boolean loadIfNotInWorkspace,
            Class<? extends Object> returnType) throws IOException {

        IPath path = Path.fromOSString(FileUtils.getFileAbsolutePath(f));
        IDocument doc = getDocFromPath(path);

        if (doc != null) {
            if (returnType == null || returnType == IDocument.class) {
                return doc;

            } else if (returnType == String.class) {
                return doc.get();

            } else if (returnType == FastStringBuffer.class) {
                return new FastStringBuffer(doc.get(), 16);

            } else {
                throw new RuntimeException("Don't know how to treat requested return type: " + returnType);
            }
        }

        if (doc == null && loadIfNotInWorkspace) {
            FileInputStream stream = new FileInputStream(f);
            try {
                String encoding = FileUtils.getPythonFileEncoding(f);
                return FileUtils.getStreamContents(stream, encoding, null, returnType);
            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }
        return doc;
    }

    /**
     * @param path the path we're interested in
     * @return a file buffer to be used.
     */
    public static ITextFileBuffer getBufferFromPath(IPath path) {
        return FileUtils.getBufferFromPath(path);
    }

    /**
     * @return null if it was unable to get the document from the path (this may happen if it was not refreshed).
     * Or the document that represents the file
     */
    public static IDocument getDocFromPath(IPath path) {
        return FileUtils.getDocFromPath(path);
    }

    public static ICallback0<IDocument> getDocOnCallbackFromResource(final IResource resource) {
        return new ICallback0<IDocument>() {

            private IDocument cache;
            private boolean calledOnce = false;

            public IDocument call() {
                if (!calledOnce) {
                    calledOnce = true;
                    cache = getDocFromResource(resource);
                }
                return cache;
            }

        };
    }

    /**
     * Returns a document, created with the contents of a resource (first tries to get from the 'FileBuffers',
     * and if that fails, it creates one reading the file.
     */
    public static IDocument getDocFromResource(IResource resource) {
        return FileUtils.getDocFromResource(resource);
    }

    /**
     * The encoding declared in the document is returned (according to the PEP: http://www.python.org/doc/peps/pep-0263/)
     */
    public static String getPythonFileEncoding(IDocument doc, String fileLocation) throws IllegalCharsetNameException {
        Reader inputStreamReader = new StringReader(doc.get());
        return FileUtils.getPythonFileEncoding(inputStreamReader, fileLocation);
    }

}
