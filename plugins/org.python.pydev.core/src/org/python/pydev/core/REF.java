/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.service.environment.Constants;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.core.callbacks.ICallback0;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;

/**
 * @author Fabio Zadrozny
 */
public class REF {

    /**
     * Regular expression for finding the encoding in a python file.
     */
    private static final Pattern ENCODING_PATTERN = Pattern.compile("coding[:=][\\s]*([-\\w.]+)");

    /**
     * @return true if the passed object has a field with the name passed.
     */
    public static boolean hasAttr(Object o, String attr) {
        try {
            o.getClass().getDeclaredField(attr);
        } catch (SecurityException e) {
            return false;
        } catch (NoSuchFieldException e) {
            return false;
        }
        return true;
    }

    /**
     * @return the field from a class that matches the passed attr name (or null if it couldn't be found)
     */
    public static Field getAttrFromClass(Class<? extends Object> c, String attr) {
        try {
            return c.getDeclaredField(attr);
        } catch (SecurityException e) {
        } catch (NoSuchFieldException e) {
        }
        return null;
    }

    /**
     * @return the field from a class that matches the passed attr name (or null if it couldn't be found)
     * @see #getAttrObj(Object, String) to get the actual value of the field.
     */
    public static Field getAttr(Object o, String attr) {
        try {
            return o.getClass().getDeclaredField(attr);
        } catch (SecurityException e) {
        } catch (NoSuchFieldException e) {
        }
        return null;
    }

    public static Object getAttrObj(Object o, String attr) {
        return getAttrObj(o, attr, false);
    }

    public static Object getAttrObj(Object o, String attr, boolean raiseExceptionIfNotAvailable) {
        return getAttrObj(o.getClass(), o, attr, raiseExceptionIfNotAvailable);
    }

    /**
     * @return the value of some attribute in the given object
     */
    public static Object getAttrObj(Class<? extends Object> c, Object o, String attr,
            boolean raiseExceptionIfNotAvailable) {
        try {
            Field field = REF.getAttrFromClass(c, attr);
            if (field != null) {
                //get it even if it's not public!
                if ((field.getModifiers() & Modifier.PUBLIC) == 0) {
                    field.setAccessible(true);
                }
                Object obj = field.get(o);
                return obj;
            }
        } catch (Exception e) {
            //ignore
            if (raiseExceptionIfNotAvailable) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    /**
     * @param file the file we want to read
     * @return the contents of the file as a string
     */
    public static String getFileContents(File file) {
        return (String) getFileContentsCustom(file, String.class);
    }

    public static Object getFileContentsCustom(File file, Class<? extends Object> returnType) {
        return getFileContentsCustom(file, null, returnType);
    }

    /**
     * @param file the file we want to read
     * @return the contents of the file as a string
     */
    public static Object getFileContentsCustom(File file, String encoding, Class<? extends Object> returnType) {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            return getStreamContents(stream, null, null, returnType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    /**
     * Gets the contents from the stream and closes it!
     */
    public static String getStreamContents(InputStream stream, String encoding, IProgressMonitor monitor) {
        try {
            return (String) getStreamContents(stream, encoding, monitor, String.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    /**
     * Get the contents from a given stream.
     * @param returnType the class that specifies the return type of this method. 
     * If null, it'll return in the fastest possible way available.
     * Valid options are:
     *      String.class
     *      IDocument.class
     *      FastStringBuffer.class
     *      
     */
    private static Object getStreamContents(InputStream contentStream, String encoding, IProgressMonitor monitor,
            Class<? extends Object> returnType) throws IOException {

        Reader in = null;
        try {
            int BUFFER_SIZE = 2 * 1024;
            int DEFAULT_FILE_SIZE = 8 * BUFFER_SIZE;

            //discover how to actually read the passed input stream.
            int available = contentStream.available();
            if (DEFAULT_FILE_SIZE < available) {
                DEFAULT_FILE_SIZE = available;
            }

            //Note: neither the input stream nor the reader are buffered because we already read in chunks (and make
            //the buffering ourselves), so, making the buffer in this case would be just overhead.

            if (encoding == null) {
                in = new InputStreamReader(contentStream);
            } else {
                try {
                    in = new InputStreamReader(contentStream, encoding);
                } catch (UnsupportedEncodingException e) {
                    Log.log(e);
                    //keep going without the encoding
                    in = new InputStreamReader(contentStream);
                }
            }

            //fill a buffer with the contents
            FastStringBuffer buffer = new FastStringBuffer(DEFAULT_FILE_SIZE);
            char[] readBuffer = new char[BUFFER_SIZE];
            int n = in.read(readBuffer);
            while (n > 0) {
                if (monitor != null && monitor.isCanceled()) {
                    return null;
                }

                buffer.append(readBuffer, 0, n);
                n = in.read(readBuffer);
            }

            //return it in the way specified by the user
            if (returnType == null || returnType == FastStringBuffer.class) {
                return buffer;

            } else if (returnType == IDocument.class) {
                Document doc = new Document(buffer.toString());
                return doc;

            } else if (returnType == String.class) {
                return buffer.toString();

            } else {
                throw new RuntimeException("Don't know how to handle return type: " + returnType);
            }

        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    /**
     * @param o the object we want as a string
     * @return the string representing the object as base64
     */
    public static String getObjAsStr(Object o) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ObjectOutputStream stream = new ObjectOutputStream(out);
            stream.writeObject(o);
            stream.close();
        } catch (Exception e) {
            Log.log(e);
            throw new RuntimeException(e);
        }

        return new String(encodeBase64(out));
    }

    /**
     * @return the contents of the passed ByteArrayOutputStream as a byte[] encoded with base64.
     */
    public static byte[] encodeBase64(ByteArrayOutputStream out) {
        byte[] byteArray = out.toByteArray();
        return encodeBase64(byteArray);
    }

    /**
     * @return the contents of the passed byteArray[] as a byte[] encoded with base64.
     */
    public static byte[] encodeBase64(byte[] byteArray) {
        return Base64.encodeBase64(byteArray);
    }

    /**
     * This method loads the contents of an object that was serialized.
     * 
     * @param readFromFileMethod see {@link #getStrAsObj(String, ICallback)}
     * @param input is the input stream that contains the serialized object
     * 
     * @return the object that was previously serialized in the passed input stream.
     */
    public static Object readFromInputStreamAndCloseIt(ICallback<Object, ObjectInputStream> readFromFileMethod,
            InputStream input) {

        ObjectInputStream in = null;
        Object o = null;
        try {
            try {
                in = new ObjectInputStream(input);
                o = readFromFileMethod.call(in);
            } finally {
                if (in != null) {
                    in.close();
                }
                input.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return o;
    }

    /**
     * Decodes some string that was encoded as base64
     */
    public static byte[] decodeBase64(String persisted) {
        return Base64.decodeBase64(persisted.getBytes());
    }

    /**
     * Appends the contents of the passed string to the given file.
     */
    public static void appendStrToFile(String str, String file) {
        try {
            FileOutputStream stream = new FileOutputStream(file, true);
            try {
                stream.write(str.getBytes());
            } finally {
                stream.close();
            }
        } catch (FileNotFoundException e) {
            Log.log(e);
        } catch (IOException e) {
            Log.log(e);
        }
    }

    /**
     * Writes the contents of the passed string to the given file.
     */
    public static void writeStrToFile(String str, String file) {
        writeStrToFile(str, new File(file));
    }

    public static void writeStrToFile(String str, File file) {
        writeBytesToFile(str.getBytes(), file);
    }

    /**
     * Writes the contents of the passed string to the given file.
     */
    public static void writeBytesToFile(byte[] bytes, File file) {
        try {
            FileOutputStream stream = new FileOutputStream(file);
            try {
                stream.write(bytes);
            } finally {
                stream.close();
            }
        } catch (FileNotFoundException e) {
            Log.log(e);
        } catch (IOException e) {
            Log.log(e);
        }
    }

    /**
     * Writes the contents of the passed string to the given file.
     */
    public static void writeToFile(Object o, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            writeToStreamAndCloseIt(o, out);
        } catch (Exception e) {
            Log.log(e);
        }
    }

    /**
     * Serializes some object to the given stream
     * 
     * @param o the object to be written to some stream
     * @param out the output stream to be used
     */
    public static void writeToStreamAndCloseIt(Object o, OutputStream out) throws IOException {
        //change: checks if we have a buffered output stream (if we don't, one will be provided)
        OutputStream b = null;
        if (out instanceof BufferedOutputStream || out instanceof ByteArrayOutputStream) {
            b = out;
        } else {
            b = new BufferedOutputStream(out);
        }

        try {

            ObjectOutputStream stream = new ObjectOutputStream(b);
            stream.writeObject(o);
            stream.close();
        } catch (Exception e) {
            Log.log(e);
            throw new RuntimeException(e);
        } finally {
            b.close();
        }
    }

    /**
     * Reads some object from a file (an object that was previously serialized)
     * 
     * Important: can only deserialize objects that are defined in this plugin -- 
     * see {@link #getStrAsObj(String, ICallback)} if you want to deserialize objects defined in another plugin.
     * 
     * @param file the file from where we should read
     * @return the object that was read (or null if some error happened while reading)
     */
    public static Object readFromFile(File file) {
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(file));
            try {
                ObjectInputStream stream = new ObjectInputStream(in);
                try {
                    Object o = stream.readObject();
                    return o;
                } finally {
                    stream.close();
                }
            } finally {
                in.close();
            }
        } catch (Exception e) {
            Log.log(e);
            return null;
        }

    }

    /**
     * Get the absolute path in the filesystem for the given file.
     * 
     * @param f the file we're interested in
     * 
     * @return the absolute (canonical) path to the file
     */
    public static String getFileAbsolutePath(String f) {
        return getFileAbsolutePath(new File(f));
    }

    /**
     * @see #getFileAbsolutePath(String)
     */
    public static String getFileAbsolutePath(File f) {
        try {
            return f.getCanonicalPath();
        } catch (IOException e) {
            return f.getAbsolutePath();
        }
    }

    /**
     * Calls a method for an object
     * 
     * @param obj the object with the method we want to call
     * @param name the method name
     * @param args the arguments received for the call
     * @return the return of the method
     * 
     * @throws RuntimeException if the object could not be invoked
     */
    public static Object invoke(Object obj, String name, Object... args) {
        //the args are not checked for the class because if a subclass is passed, the method is not correctly gotten
        //another method might do it...
        Method m = findMethod(obj, name, args);
        return invoke(obj, m, args);
    }

    /**
     * @see #invoke(Object, String, Object...)
     */
    public static Object invoke(Object obj, Method m, Object... args) {
        try {
            return m.invoke(obj, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return a method that has the given name and arguments
     * @throws RuntimeException if the method could not be found
     */
    public static Method findMethod(Object obj, String name, Object... args) {
        return findMethod(obj.getClass(), name, args);
    }

    /**
     * @return a method that has the given name and arguments
     * @throws RuntimeException if the method could not be found
     */
    public static Method findMethod(Class<? extends Object> class_, String name, Object... args) {
        try {
            Method[] methods = class_.getMethods();
            for (Method method : methods) {

                Class<? extends Object>[] parameterTypes = method.getParameterTypes();
                if (method.getName().equals(name) && parameterTypes.length == args.length) {
                    //check the parameters
                    int i = 0;
                    for (Class<? extends Object> param : parameterTypes) {
                        if (!param.isInstance(args[i])) {
                            continue;
                        }
                        i++;
                    }
                    //invoke it
                    return method;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("The method with name: " + name
                + " was not found (or maybe it was found but the parameters didn't match).");
    }

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

    /**
     * Makes an equal comparison taking into account that one of the parameters may be null.
     */
    public static boolean nullEq(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        return o1.equals(o2);
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
                return REF.getStreamContents(inputStream, null, null, returnType);
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

        IPath path = Path.fromOSString(getFileAbsolutePath(f));
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
                String encoding = getPythonFileEncoding(f);
                return getStreamContents(stream, encoding, null, returnType);
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
                    doc = (IDocument) REF.getStreamContents(file.getContents(true), null, null, IDocument.class);
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
        return getPythonFileEncoding(inputStreamReader, fileLocation);
    }

    /**
     * The encoding declared in the file is returned (according to the PEP: http://www.python.org/doc/peps/pep-0263/)
     */
    public static String getPythonFileEncoding(File f) throws IllegalCharsetNameException {
        try {
            final FileInputStream fileInputStream = new FileInputStream(f);
            try {
                Reader inputStreamReader = new InputStreamReader(new BufferedInputStream(fileInputStream));
                String pythonFileEncoding = getPythonFileEncoding(inputStreamReader, f.getAbsolutePath());
                return pythonFileEncoding;
            } finally {
                //NOTE: the reader will be closed at 'getPythonFileEncoding'. 
                try {
                    fileInputStream.close();
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    /**
     * This is usually what's on disk
     */
    public static String BOM_UTF8 = new String(new char[] { 0xEF, 0xBB, 0xBF });

    /**
     * When we convert a string from the disk to a java string, if it had an UTF-8 BOM, it'll have that BOM converted
     * to this BOM. See: org.python.pydev.parser.PyParser27Test.testBom()
     */
    public static String BOM_UNICODE = new String(new char[] { 0xFEFF });

    /**
     * The encoding declared in the reader is returned (according to the PEP: http://www.python.org/doc/peps/pep-0263/)
     * -- may return null
     * 
     * Will close the reader.
     * @param fileLocation the file we want to get the encoding from (just passed for giving a better message 
     * if it fails -- may be null).
     */
    public static String getPythonFileEncoding(Reader inputStreamReader, String fileLocation)
            throws IllegalCharsetNameException {

        String ret = null;
        BufferedReader reader = new BufferedReader(inputStreamReader);
        try {
            String lEnc = null;

            //pep defines that coding must be at 1st or second line: http://www.python.org/doc/peps/pep-0263/
            String l1 = reader.readLine();
            if (l1 != null) {
                //Special case -- determined from the python docs:
                //http://docs.python.org/reference/lexical_analysis.html#encoding-declarations
                //We can return promptly in this case as utf-8 should be always valid.
                if (l1.startsWith(BOM_UTF8)) {
                    return "utf-8";
                }

                if (l1.indexOf("coding") != -1) {
                    lEnc = l1;
                }
            }

            if (lEnc == null) {
                String l2 = reader.readLine();

                //encoding must be specified in first or second line...
                if (l2 != null && l2.indexOf("coding") != -1) {
                    lEnc = l2;
                } else {
                    ret = null;
                }
            }

            if (lEnc != null) {
                lEnc = lEnc.trim();
                if (lEnc.length() == 0) {
                    ret = null;

                } else if (lEnc.charAt(0) == '#') { //it must be a comment line

                    Matcher matcher = ENCODING_PATTERN.matcher(lEnc);
                    if (matcher.find()) {
                        ret = matcher.group(1).trim();
                    }
                }
            }
        } catch (IOException e) {
            Log.log(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e1) {
            }
        }
        ret = getValidEncoding(ret, fileLocation);
        return ret;
    }

    /**
     * @param fileLocation may be null
     */
    /*package*/static String getValidEncoding(String ret, String fileLocation) {
        if (ret == null) {
            return ret;
        }
        final String lower = ret.trim().toLowerCase();
        if (lower.startsWith("latin")) {
            if (lower.indexOf("1") != -1) {
                return "latin1"; //latin1
            }
        }
        if (lower.equals("iso-latin-1-unix")) {
            return "latin1"; //handle case from python libraries
        }
        try {
            if (!Charset.isSupported(ret)) {
                if (LOG_ENCODING_ERROR) {
                    if (fileLocation != null) {
                        if ("uft-8".equals(ret) && fileLocation.endsWith("bad_coding.py")) {
                            return null; //this is an expected error in the python library.
                        }
                    }
                    String msg = "The encoding found: >>" + ret + "<< on " + fileLocation + " is not a valid encoding.";
                    Log.log(IStatus.ERROR, msg, new UnsupportedEncodingException(msg));
                }
                return null; //ok, we've been unable to make it supported (better return null than an unsupported encoding).
            }
            return ret;
        } catch (IllegalCharsetNameException ex) {
            if (LOG_ENCODING_ERROR) {
                String msg = "The encoding found: >>" + ret + "<< on " + fileLocation + " is not a valid encoding.";
                Log.log(IStatus.ERROR, msg, ex);
            }
        }
        return null;
    }

    /**
     * Returns if the given file has a python shebang (i.e.: starts with #!... python)
     * 
     * Will close the reader.
     */
    public static boolean hasPythonShebang(Reader inputStreamReader) throws IllegalCharsetNameException {

        BufferedReader reader = new BufferedReader(inputStreamReader);
        try {
            String l1 = reader.readLine();
            if (l1 != null) {
                //Special case to skip bom.
                if (l1.startsWith(BOM_UTF8)) {
                    l1 = l1.substring(BOM_UTF8.length());
                }

                if (l1.startsWith("#!") && l1.indexOf("python") != -1) {
                    return true;
                }
            }

        } catch (IOException e) {
            Log.log(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e1) {
            }
        }
        return false;
    }

    /**
     * Useful to silent it on tests
     */
    public static boolean LOG_ENCODING_ERROR = true;

    /**
     * Start null... filled on 1st request.
     * 
     * Currently we only care for: windows, mac os or linux (if we need some other special support,
     * this could be improved).
     */
    public static Integer platform;
    public static int WINDOWS = 1;
    public static int MACOS = 2;
    public static int LINUX = 3;

    /**
     * @return whether we are in windows or not
     */
    public static boolean isWindowsPlatform() {
        discoverPlatform();
        return platform == WINDOWS;
    }

    /**
     * @return whether we are in MacOs or not
     */
    public static boolean isMacOsPlatform() {
        discoverPlatform();
        return platform == MACOS;
    }

    private static void discoverPlatform() {
        if (platform == null) {
            try {
                String os = Platform.getOS();
                if (os.equals(Constants.OS_WIN32)) {
                    platform = WINDOWS;
                } else if (os.equals(Constants.OS_MACOSX)) {
                    platform = MACOS;
                } else {
                    platform = LINUX;
                }

            } catch (NullPointerException e) {
                String env = System.getProperty("os.name").toLowerCase();
                if (env.indexOf("win") != -1) {
                    platform = WINDOWS;
                } else if (env.startsWith("mac os")) {
                    platform = MACOS;
                } else {
                    platform = LINUX;
                }
            }
        }
    }

    public static void copyFile(String srcFilename, String dstFilename) {
        copyFile(new File(srcFilename), new File(dstFilename));
    }

    /**
     * Copy a file from one place to another.
     * 
     * Example from: http://www.exampledepot.com/egs/java.nio/File2File.html
     * 
     * @param srcFilename the source file
     * @param dstFilename the destination
     */
    public static void copyFile(File srcFilename, File dstFilename) {
        FileChannel srcChannel = null;
        FileChannel dstChannel = null;
        try {
            // Create channel on the source
            srcChannel = new FileInputStream(srcFilename).getChannel();

            // Create channel on the destination
            dstChannel = new FileOutputStream(dstFilename).getChannel();

            // Copy file contents from source to destination
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // Close the channels
            if (srcChannel != null) {
                try {
                    srcChannel.close();
                } catch (IOException e) {
                    Log.log(e);
                }
            }
            if (dstChannel != null) {
                try {
                    dstChannel.close();
                } catch (IOException e) {
                    Log.log(e);
                }
            }
        }

    }

    /**
     * Copies (recursively) the contents of one directory to another.
     * 
     * @param filter: a callback that can be used to choose files that should not be copied. 
     * If null, all files are copied, otherwise, if filter returns true, it won't be copied, and
     * if it returns false, it will be copied
     * 
     * @param changeFileContents: a callback that's called before copying any file, so that clients
     * have a change of changing the file contents to be written.
     */
    public static void copyDirectory(File srcPath, File dstPath, ICallback<Boolean, File> filter,
            ICallback<String, String> changeFileContents) throws IOException {
        if (srcPath.isDirectory()) {
            if (filter != null && filter.call(srcPath)) {
                return;
            }
            if (!dstPath.exists()) {
                dstPath.mkdir();
            }
            String files[] = srcPath.list();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    copyDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]), filter, changeFileContents);
                }
            }
        } else {
            if (!srcPath.exists()) {
                //do nothing
            } else {
                if (filter != null && filter.call(srcPath)) {
                    return;
                }
                if (changeFileContents == null) {
                    copyFile(srcPath.getAbsolutePath(), dstPath.getAbsolutePath());
                } else {
                    String fileContents = getFileContents(srcPath);
                    fileContents = changeFileContents.call(fileContents);
                    writeStrToFile(fileContents, dstPath);
                }
            }
        }
    }

    /**
     * This method will try to create a backup file of the passed file.
     * @param file this is the file we want to copy as the backup.
     * @return true if it was properly copied and false otherwise.
     */
    public static boolean createBackupFile(File file) {
        if (file != null && file.isFile()) {
            File parent = file.getParentFile();
            if (parent.isDirectory()) {
                String[] list = parent.list();
                HashSet<String> set = new HashSet<String>();
                set.addAll(Arrays.asList(list));
                String initialName = file.getName();
                initialName += ".bak";
                String name = initialName;
                int i = 0;
                while (set.contains(name)) {
                    name = initialName + i;
                    i++;
                }
                copyFile(file.getAbsolutePath(), new File(parent, name).getAbsolutePath());
                return true;
            }
        }
        return false;
    }

    /**
     * Log with base is missing in java!
     */
    public static double log(double a, double base) {
        return Math.log(a) / Math.log(base);
    }

    public static void print(Object... objects) {
        System.out.println(StringUtils.join(" ", objects));
    }

    private static final Map<File, Set<String>> alreadyReturned = new HashMap<File, Set<String>>();
    private static Object lockTempFiles = new Object();

    public static File getTempFileAt(File parentDir, String prefix) {
        return getTempFileAt(parentDir, prefix, "");
    }

    /**
     * @param extension the extension (i.e.: ".py")
     * @return
     */
    public static File getTempFileAt(File parentDir, String prefix, String extension) {
        synchronized (lockTempFiles) {
            Assert.isTrue(parentDir.isDirectory());
            Set<String> current = alreadyReturned.get(parentDir);
            if (current == null) {
                current = new HashSet<String>();
                alreadyReturned.put(parentDir, current);
            }
            current.addAll(getFilesStartingWith(parentDir, prefix));

            FastStringBuffer buf = new FastStringBuffer();

            for (long i = 0; i < Long.MAX_VALUE; i++) {
                String v = buf.clear().append(prefix).append(i).append(extension).toString();
                if (current.contains(v)) {
                    continue;
                }
                File file = new File(parentDir, v);
                if (!file.exists()) {
                    current.add(file.getName());
                    return file;
                }
            }
            return null;
        }
    }

    public static HashSet<String> getFilesStartingWith(File parentDir, String prefix) {
        String[] list = parentDir.list();
        HashSet<String> hashSet = new HashSet<String>();
        if (list != null) {
            for (String string : list) {
                if (string.startsWith(prefix)) {
                    hashSet.add(string);
                }
            }
        }
        return hashSet;
    }

    public static void clearTempFilesAt(File parentDir, String prefix) {
        synchronized (lockTempFiles) {
            try {
                Assert.isTrue(parentDir.isDirectory());
                String[] list = parentDir.list();
                if (list != null) {
                    for (String string : list) {
                        if (string.startsWith(prefix)) {
                            String integer = string.substring(prefix.length());
                            try {
                                Integer.parseInt(integer);
                                try {
                                    new File(parentDir, string).delete();
                                } catch (Exception e) {
                                    //ignore
                                }
                            } catch (NumberFormatException e) {
                                //ignore (not a file we generated)
                            }
                        }
                    }
                }
                alreadyReturned.remove(parentDir);
            } catch (Throwable e) {
                Log.log(e); //never give an error here, just log it.
            }
        }
    }

    /**
     * Yes, this will delete everything under a directory. Use with care!
     */
    public static void deleteDirectoryTree(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files != null) {

            for (int i = 0; i < files.length; ++i) {
                File f = files[i];

                if (f.isDirectory()) {
                    deleteDirectoryTree(f);
                } else {
                    deleteFile(f);
                }
            }
        }
        if (!directory.delete()) {
            throw new IOException("Delete operation failed when deleting: " + directory);
        }
    }

    public static void deleteFile(File file) throws IOException {
        if (!file.exists())
            throw new FileNotFoundException(file.getAbsolutePath());

        if (!file.delete()) {
            throw new IOException("Delete operation failed when deleting: " + file);
        }
    }

    public static void openDirectory(File dir) {
        //Note: on java 6 it seems we could use java.awt.Desktop.
        String executable = getOpenDirectoryExecutable();
        if (executable != null) {
            try {
                if (executable.equals("kfmclient")) {
                    //Yes, KDE needs an exec after kfmclient.
                    Runtime.getRuntime().exec(new String[] { executable, "exec", dir.toString() }, null, dir);

                } else {
                    Runtime.getRuntime().exec(new String[] { executable, dir.toString() }, null, dir);
                }
            } catch (Throwable e) {
                Log.log(e);
            }
        }
    }

    private static String openDirExecutable = null;
    private final static String OPEN_DIR_EXEC_NOT_AVAILABLE = "NOT_AVAILABLE";

    private static String getOpenDirectoryExecutable() {
        if (openDirExecutable == null) {
            if (REF.isWindowsPlatform()) {
                openDirExecutable = "explorer";
                return openDirExecutable;

            }

            if (REF.isMacOsPlatform()) {
                openDirExecutable = "open";
                return openDirExecutable;
            }

            try {
                String env = System.getenv("DESKTOP_LAUNCH");
                if (env != null && env.trim().length() > 0) {
                    openDirExecutable = env;
                    return openDirExecutable;
                }
            } catch (Throwable e) {
                //ignore -- it seems not all java versions have System.getenv
            }

            try {
                Map<String, String> env = System.getenv();
                if (env.containsKey("KDE_FULL_SESSION") || env.containsKey("KDE_MULTIHEAD")) {
                    openDirExecutable = "kfmclient";
                    return openDirExecutable;
                }
                if (env.containsKey("GNOME_DESKTOP_SESSION_ID") || env.containsKey("GNOME_KEYRING_SOCKET")) {
                    openDirExecutable = "gnome-open";
                    return openDirExecutable;
                }
            } catch (Throwable e) {
                //ignore -- it seems not all java versions have System.getenv
            }

            //If it hasn't returned until now, we don't know about it!
            openDirExecutable = OPEN_DIR_EXEC_NOT_AVAILABLE;
        }
        //Yes, we can compare with identity since we know which string we've set.
        if (openDirExecutable == OPEN_DIR_EXEC_NOT_AVAILABLE) {
            return null;
        }
        return openDirExecutable;
    }

    public static boolean getSupportsOpenDirectory() {
        return getOpenDirectoryExecutable() != null;
    }

    public static File createFileFromParts(String... parts) {
        String part0 = parts[0];
        File f = new File(part0);
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            f = new File(f, part);
        }
        return f;
    }
}
