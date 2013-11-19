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

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
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
     * Determines if we're in tests: When in tests, some warnings may be supressed.
     */
    public static boolean IN_TESTS = false;

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

        ZipFile zipFile = new ZipFile(f, ZipFile.OPEN_READ);
        try {
            InputStream inputStream = zipFile.getInputStream(zipFile.getEntry(pathInZip));
            try {
                return FileUtils.getStreamContents(inputStream, null, null, returnType);
            } finally {
                inputStream.close();
            }
        } finally {
            zipFile.close();
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
                    if (stream != null)
                        stream.close();
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
    @SuppressWarnings("deprecation")
    public static ITextFileBuffer getBufferFromPath(IPath path) {
        try {
            try {

                //eclipse 3.3 has a different interface
                ITextFileBufferManager textFileBufferManager = ITextFileBufferManager.DEFAULT;
                if (textFileBufferManager != null) {//we don't have it in tests
                    ITextFileBuffer textFileBuffer = textFileBufferManager.getTextFileBuffer(path,
                            LocationKind.LOCATION);

                    if (textFileBuffer != null) { //we don't have it when it is not properly refreshed
                        return textFileBuffer;
                    }
                }

            } catch (Throwable e) {//NoSuchMethod/NoClassDef exception 
                if (e instanceof ClassNotFoundException || e instanceof LinkageError
                        || e instanceof NoSuchMethodException || e instanceof NoSuchMethodError
                        || e instanceof NoClassDefFoundError) {

                    ITextFileBufferManager textFileBufferManager = FileBuffers.getTextFileBufferManager();

                    if (textFileBufferManager != null) {//we don't have it in tests
                        ITextFileBuffer textFileBuffer = textFileBufferManager.getTextFileBuffer(path);

                        if (textFileBuffer != null) { //we don't have it when it is not properly refreshed
                            return textFileBuffer;
                        }
                    }
                } else {
                    throw e;
                }

            }
            return null;

        } catch (Throwable e) {
            //private static final IWorkspaceRoot WORKSPACE_ROOT= ResourcesPlugin.getWorkspace().getRoot();
            //throws an error and we don't even have access to the FileBuffers class in tests
            if (!IN_TESTS) {
                Log.log("Unable to get doc from text file buffer");
            }
            return null;
        }
    }

    /**
     * @return null if it was unable to get the document from the path (this may happen if it was not refreshed).
     * Or the document that represents the file
     */
    public static IDocument getDocFromPath(IPath path) {
        ITextFileBuffer buffer = getBufferFromPath(path);
        if (buffer != null) {
            return buffer.getDocument();
        }
        return null;
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
        IProject project = resource.getProject();
        if (project != null && resource instanceof IFile && resource.exists()) {

            IFile file = (IFile) resource;

            try {
                if (!file.isSynchronized(IResource.DEPTH_ZERO)) {
                    file.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
                }
                IPath path = file.getFullPath();

                IDocument doc = getDocFromPath(path);
                if (doc == null) {
                    //can this actually happen?... yeap, it can (if file does not exist)
                    doc = (IDocument) FileUtils.getStreamContents(file.getContents(true), null, null, IDocument.class);
                }
                return doc;
            } catch (CoreException e) {
                //it may stop existing from the initial exists check to the getContents call
                return null;
            } catch (Exception e) {
                Log.log(e);
            }
        }
        return null;
    }

    /**
     * The encoding declared in the document is returned (according to the PEP: http://www.python.org/doc/peps/pep-0263/)
     */
    public static String getPythonFileEncoding(IDocument doc, String fileLocation) throws IllegalCharsetNameException {
        Reader inputStreamReader = new StringReader(doc.get());
        return FileUtils.getPythonFileEncoding(inputStreamReader, fileLocation);
    }

}
