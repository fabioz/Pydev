package org.python.pydev.core;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.callbacks.ICallback0;
import org.python.pydev.core.log.Log;

import com.aptana.shared_core.utils.FastStringBuffer;
import com.aptana.shared_core.utils.REF;

public class FileUtils {
    /**
     * Regular expression for finding the encoding in a python file.
     */
    private static final Pattern ENCODING_PATTERN = Pattern.compile("coding[:=][\\s]*([-\\w.]+)");

    /**
     * Useful to silent it on tests
     */
    public static boolean LOG_ENCODING_ERROR = true;

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
                return getStreamContents(inputStream, null, null, returnType);
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

        IPath path = Path.fromOSString(REF.getFileAbsolutePath(f));
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
                    doc = (IDocument) getStreamContents(file.getContents(true), null, null, IDocument.class);
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

        FastStringBuffer buffer = REF.fillBufferWithStream(contentStream, encoding, monitor);
        if (buffer == null) {
            return null;
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
    }

}
