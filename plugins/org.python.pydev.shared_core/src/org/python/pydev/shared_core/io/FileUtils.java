/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.shared_core.io;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
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
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * @author Fabio Zadrozny
 */
public class FileUtils {

    /**
     * Determines if we're in tests: When in tests, some warnings may be supressed.
     */
    public static boolean IN_TESTS = false;

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

        Object o = null;
        try {
            try (ObjectInputStream in = new ObjectInputStream(input)) {
                o = readFromFileMethod.call(in);
            } finally {
                input.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return o;
    }

    /**
     * Appends the contents of the passed string to the given file.
     */
    public static void appendStrToFile(String str, String file) {
        try {
            try (FileOutputStream stream = new FileOutputStream(file, true)) {
                stream.write(str.getBytes());
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
        try (FileOutputStream stream = new FileOutputStream(file)) {
            stream.write(bytes);
        } catch (FileNotFoundException e) {
            Log.log(e);
        } catch (IOException e) {
            Log.log(e);
        }
    }

    public static void writeToFile(Object o, File file) {
        writeToFile(o, file, false);
    }

    /**
     * Writes the contents of the passed string to the given file.
     */
    public static void writeToFile(Object o, File file, boolean zip) {
        try {
            OutputStream out = new FileOutputStream(file);
            if (zip) {
                out = new GZIPOutputStream(out);
            }
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

    public static Object readFromFile(File file) {
        return readFromFile(file, false);
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
    public static Object readFromFile(File file, boolean zip) {
        try (FileInputStream fin = new FileInputStream(file)) {
            try (InputStream in = new BufferedInputStream(zip ? new GZIPInputStream(fin) : fin)) {
                try (ObjectInputStream stream = new ObjectInputStream(in)) {
                    Object o = stream.readObject();
                    return o;
                }
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
     * This version does not resolve links.
     */
    public static String getFileAbsolutePathNotFollowingLinks(File f) {
        try {
            return f.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
        } catch (IOException e) {
            return f.getAbsolutePath();
        }

    }

    /**
     * This version resolves links.
     */
    public static String getFileAbsolutePath(File f) {
        try {
            return f.getCanonicalPath();
        } catch (IOException e) {
            return f.getAbsolutePath();
        }
    }

    public static void copyFile(String srcFilename, String dstFilename) {
        copyFile(new File(srcFilename), new File(dstFilename));
    }

    /**
     * Copy a file from one place to another.
     * @param srcFilename the source file
     * @param dstFilename the destination
     */
    public static void copyFile(File srcFilename, File dstFilename) {
        try {
            Files.copy(srcFilename.toPath(), dstFilename.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    public static FastStringBuffer fillBufferWithStream(InputStream contentStream, String encoding,
            IProgressMonitor monitor) throws IOException {
        return fillBufferWithStream(contentStream, encoding, monitor, null);
    }

    public static FastStringBuffer fillBufferWithStream(InputStream contentStream, String encoding,
            IProgressMonitor monitor, FastStringBuffer buffer) throws IOException {
        Reader in = null;
        try {

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
            int BUFFER_SIZE = 2 * 1024;
            if (buffer == null) {
                int DEFAULT_FILE_SIZE = 8 * BUFFER_SIZE;

                //discover how to actually read the passed input stream.
                int available = contentStream.available();
                if (DEFAULT_FILE_SIZE < available) {
                    DEFAULT_FILE_SIZE = available;
                }
                buffer = new FastStringBuffer(DEFAULT_FILE_SIZE);
            }

            char[] readBuffer = new char[BUFFER_SIZE];
            int n = in.read(readBuffer);
            while (n > 0) {
                if (monitor != null && monitor.isCanceled()) {
                    return null;
                }

                buffer.append(readBuffer, 0, n);
                n = in.read(readBuffer);
            }

        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
        return buffer;
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
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }

        if (!file.delete()) {
            throw new IOException("Delete operation failed when deleting: " + file);
        }
    }

    public static void openDirectory(File dir) {
        try {
            Desktop.getDesktop().open(dir);
        } catch (IOException e1) {
            Log.log(e1);
        }
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

    /**
     * Get the contents from a given stream.
     * @param returnType the class that specifies the return type of this method.
     * If null, it'll return in the fastest possible way available (i.e.: FastStringBuffer).
     *
     * Valid options are:
     *      String.class
     *      IDocument.class
     *      FastStringBuffer.class
     *
     */
    public static <T> T getStreamContents(InputStream contentStream, String encoding, IProgressMonitor monitor,
            Class<T> returnType) throws IOException {

        FastStringBuffer buffer = fillBufferWithStream(contentStream, encoding, monitor);
        if (buffer == null) {
            return null;
        }

        //return it in the way specified by the user
        if (returnType == null || returnType == FastStringBuffer.class) {
            return (T) buffer;

        } else if (returnType == IDocument.class) {
            Document doc = new Document(buffer.toString());
            return (T) doc;

        } else if (returnType == String.class) {
            return (T) buffer.toString();

        } else {
            throw new RuntimeException("Don't know how to handle return type: " + returnType);
        }
    }

    /**
     * Gets the contents from the stream and closes it!
     */
    public static String getStreamContents(InputStream stream, String encoding, IProgressMonitor monitor) {
        try {
            return getStreamContents(stream, encoding, monitor, String.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    public static byte[] getFileContentsBytes(File file) throws IOException {
        return Files.readAllBytes(Paths.get(file.toURI()));
    }

    /**
     * @param file the file we want to read
     * @return the contents of the file as a string
     */
    public static <T> T getFileContentsCustom(File file, String encoding, Class<T> returnType) {
        try (FileInputStream stream = new FileInputStream(file)) {
            return getStreamContents(stream, encoding, null, returnType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getFileContentsCustom(File file, Class<T> returnType) {
        return getFileContentsCustom(file, null, returnType);
    }

    /**
     * @param file the file we want to read
     * @return the contents of the file as a string
     */
    public static String getFileContents(File file) {
        return getFileContentsCustom(file, null, String.class);
    }

    /**
     * To get file contents for a python file, the encoding is required!
     */
    public static String getPyFileContents(File file) {
        return getFileContentsCustom(file, getPythonFileEncoding(file), String.class);
    }

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
        try {
            List<String> lines = readLines(inputStreamReader, 2);
            int readLines = lines.size();
            String lEnc = null;

            //pep defines that coding must be at 1st or second line: http://www.python.org/doc/peps/pep-0263/
            String l1 = readLines > 0 ? lines.get(0) : null;
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
                String l2 = readLines > 1 ? lines.get(1) : null;

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
        } finally {
            try {
                inputStreamReader.close();
            } catch (IOException e1) {
            }
        }
        ret = getValidEncoding(ret, fileLocation);
        return ret;
    }

    /**
     * This is usually what's on disk
     */
    public static String BOM_UTF8 = StringUtils.BOM_UTF8;
    /**
     * When we convert a string from the disk to a java string, if it had an UTF-8 BOM, it'll have that BOM converted
     * to this BOM. See: org.python.pydev.parser.PyParser27Test.testBom()
     */
    public static String BOM_UNICODE = StringUtils.BOM_UNICODE;

    /**
     * @param fileLocation may be null
     */
    /*package*/public static String getValidEncoding(String ret, String fileLocation) {
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
     * Useful to silent it on tests
     */
    public static boolean LOG_ENCODING_ERROR = true;
    /**
     * Regular expression for finding the encoding in a python file.
     */
    public static final Pattern ENCODING_PATTERN = Pattern.compile("coding[:=][\\s]*([-\\w.]+)");

    /**
     * The encoding declared in the file is returned (according to the PEP: http://www.python.org/doc/peps/pep-0263/)
     */
    public static String getPythonFileEncoding(File f) throws IllegalCharsetNameException {
        try (FileInputStream fileInputStream = new FileInputStream(f)) {
            Reader inputStreamReader = new InputStreamReader(new BufferedInputStream(fileInputStream));

            //NOTE: the reader will be closed at 'getPythonFileEncoding'.
            String pythonFileEncoding = getPythonFileEncoding(inputStreamReader, f.getAbsolutePath());
            return pythonFileEncoding;
        } catch (IOException e) {
            Log.log(e);
            return null;
        }
    }

    /**
     * Returns if the given file has a python shebang (i.e.: starts with #!... python)
     *
     * Will close the reader.
     */
    public static boolean hasPythonShebang(Reader inputStreamReader) throws IllegalCharsetNameException {

        try {
            List<String> lines = readLines(inputStreamReader, 1);
            if (lines.size() > 0) {
                if (isPythonShebangLine(lines.get(0))) {
                    return true;
                }
            }
        } finally {
            try {
                inputStreamReader.close();
            } catch (IOException e1) {
            }
        }
        return false;
    }

    public static boolean isPythonShebangLine(String l1) {
        //Special case to skip bom.
        l1 = StringUtils.removeBom(l1);

        if (l1.startsWith("#!") && l1.indexOf("python") != -1) {
            return true;
        }
        return false;
    }

    /**
     * This is an utility method to read a specified number of lines. It is internal because it won't read a line
     * if the line is too big (this prevents loading too much in memory if we open a binary file that doesn't really
     * have a line break there).
     *
     * See: #PyDev-125: OutOfMemoryError with large binary file (https://sw-brainwy.rhcloud.com/tracker/PyDev/125)
     *
     * @return a list of strings with the lines that were read.
     */
    public static List<String> readLines(Reader inputStreamReader, int lines) {
        if (lines <= 0) {
            return new ArrayList<String>(0);
        }
        List<String> ret = new ArrayList<String>(lines);

        try {
            char[] cbuf = new char[1024 * lines];
            //Consider that a line is not longer than 1024 chars (more than enough for a coding or shebang declaration).
            int read = inputStreamReader.read(cbuf);
            if (read > 0) {
                for (String line : StringUtils.iterLines(new String(cbuf, 0, read))) {
                    ret.add(line);
                    if (lines == ret.size()) {
                        return ret;
                    }
                }
            }

        } catch (Exception e) {
            Log.log(e);
        }
        return ret;
    }

    /**
     * Utility that'll open a file and read it until we get to the given line which when found is returned.
     *
     * Throws exception if we're unable to find the given line.
     *
     * @param lineNumber: 1-based
     */
    public static String getLineFromFile(File file, int lineNumber) throws FileNotFoundException, IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                int i = 1; //1-based
                while ((line = reader.readLine()) != null) {
                    if (i == lineNumber) {
                        return line;
                    }
                    i++;
                }
            }
        }
        throw new IOException(StringUtils.format("Unable to find line: %s in file: %s", lineNumber, file));
    }

    /**
     * Iterates a directory recursively and returns the lastModified time for the files found
     * (provided that the filter accepts the given file).
     *
     * Will return 0 if no files are accepted in the filter.
     */
    public static long getLastModifiedTimeFromDir(File file, FileFilter filesFilter, FileFilter dirFilter, int levels) {
        if (levels <= 0) {
            return 0;
        }
        long max = 0;
        if (file.isDirectory()) {
            Path path = Paths.get(file.toURI());

            //Automatic resource management.
            try (DirectoryStream<Path> newDirectoryStream = Files.newDirectoryStream(path)) {
                Iterator<Path> it = newDirectoryStream.iterator();
                while (it.hasNext()) {
                    Path path2 = it.next();
                    File file2 = path2.toFile();
                    if (file2.isDirectory()) {
                        if (dirFilter.accept(file2)) {
                            max = Math.max(max,
                                    getLastModifiedTimeFromDir(file2, filesFilter, dirFilter, levels - 1));
                        }
                    } else {
                        if (filesFilter.accept(file2)) {
                            max = Math.max(max, FileUtils.lastModified(file2));
                        }
                    }
                }
            } catch (IOException e) {
                Log.log(e);
            }
        } else {
            if (filesFilter.accept(file)) {
                max = Math.max(max, FileUtils.lastModified(file));
            }
        }
        return max;
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
                    doc = FileUtils.getStreamContents(file.getContents(true), null, null, IDocument.class);
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

    /**
     * @param onFile - true keeps on searching and false terminates the searching.
     */
    public static void visitDirectory(File file, final boolean recursive, final ICallback<Boolean, Path> onFile)
            throws IOException {
        final Path rootDir = Paths.get(FileUtils.getFileAbsolutePath(file));
        visitDirectory(rootDir, recursive, onFile);
    }

    /**
     * @param onFile - true keeps on searching and false terminates the searching.
     */
    public static void visitDirectory(Path rootDir, final boolean recursive, final ICallback<Boolean, Path> onFile)
            throws IOException {

        Files.walkFileTree(rootDir, new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path path,
                    BasicFileAttributes atts) throws IOException {
                return recursive ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes mainAtts)
                    throws IOException {
                if (!onFile.call(path)) {
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path path,
                    IOException exc) throws IOException {
                return recursive ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path path, IOException exc)
                    throws IOException {
                Log.log(exc);
                return FileVisitResult.CONTINUE;
            }
        });

    }

    public static long lastModified(File file) {
        try {
            // Has a higher precision.
            final Path path = Paths.get(file.toURI());
            return lastModified(path);
        } catch (IOException e) {
            final long lastModified = file.lastModified();
            Log.log("Error. returning: " + lastModified, e);
            return lastModified;
        }
    }

    public static long lastModified(final Path path) throws IOException {
        long ret = Files.getLastModifiedTime(path).to(TimeUnit.NANOSECONDS);
        // System.out.println("\nFound:");
        // System.out.println(ret);
        // System.out.println(file.lastModified());
        return ret;
    }

    public static String getFileExtension(String name) {
        return StringUtils.getFileExtension(name);
    }

    public static class ReadLines {

        public final List<String> lines;
        private byte[] cbuf;
        private int nChars;

        public ReadLines(List<String> lines, byte[] cbuf, int nChars) {
            this.lines = lines;
            this.cbuf = cbuf;
            this.nChars = nChars;
        }

        public int size() {
            if (lines == null) {
                return 0;
            }
            return lines.size();
        }

        public boolean isBinary() {
            return cbuf != null ? !StringUtils.isValidTextString(cbuf, nChars) : false;
        }

    }

    public static ReadLines readLines(File file) {
        List<String> lines = null;
        byte[] cbuf = null;
        int nChars = -1;
        if (file.exists()) {
            try {
                FileInputStream stream = new FileInputStream(file);
                try {
                    lines = new ArrayList<String>(2);
                    cbuf = new byte[1024 * 2];
                    //Consider that a line is not longer than 1024 chars (more than enough for a coding or shebang declaration).
                    nChars = stream.read(cbuf);
                    if (nChars > 0) {
                        for (String line : StringUtils.iterLines(new String(cbuf, 0, nChars))) {
                            lines.add(line);
                            if (2 == lines.size()) {
                                break;
                            }
                        }
                    }

                } finally {
                    stream.close();
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
        return new ReadLines(lines, cbuf, nChars);
    }

}
