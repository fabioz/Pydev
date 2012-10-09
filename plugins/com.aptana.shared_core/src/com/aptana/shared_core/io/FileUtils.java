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
package com.aptana.shared_core.io;

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
import java.io.UnsupportedEncodingException;
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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import com.aptana.shared_core.callbacks.ICallback;
import com.aptana.shared_core.log.Log;
import com.aptana.shared_core.string.FastStringBuffer;
import com.aptana.shared_core.utils.PlatformUtils;

/**
 * @author Fabio Zadrozny
 */
public class FileUtils {

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

    public static FastStringBuffer fillBufferWithStream(InputStream contentStream, String encoding,
            IProgressMonitor monitor) throws IOException {
        FastStringBuffer buffer;
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
            buffer = new FastStringBuffer(DEFAULT_FILE_SIZE);
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
                if (in != null)
                    in.close();
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
            if (PlatformUtils.isWindowsPlatform()) {
                openDirExecutable = "explorer";
                return openDirExecutable;

            }

            if (PlatformUtils.isMacOsPlatform()) {
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
    public static Object getStreamContents(InputStream contentStream, String encoding, IProgressMonitor monitor,
            Class<? extends Object> returnType) throws IOException {

        FastStringBuffer buffer = fillBufferWithStream(contentStream, encoding, monitor);
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
                if (stream != null) {
                    stream.close();
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    public static Object getFileContentsCustom(File file, Class<? extends Object> returnType) {
        return getFileContentsCustom(file, null, returnType);
    }

    /**
     * @param file the file we want to read
     * @return the contents of the file as a string
     */
    public static String getFileContents(File file) {
        return (String) getFileContentsCustom(file, null, String.class);
    }

    /**
     * To get file contents for a python file, the encoding is required!
     */
    public static String getPyFileContents(File file) {
        return (String) getFileContentsCustom(file, getPythonFileEncoding(file), String.class);
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
     * This is usually what's on disk
     */
    public static String BOM_UTF8 = new String(new char[] { 0xEF, 0xBB, 0xBF });
    /**
     * When we convert a string from the disk to a java string, if it had an UTF-8 BOM, it'll have that BOM converted
     * to this BOM. See: org.python.pydev.parser.PyParser27Test.testBom()
     */
    public static String BOM_UNICODE = new String(new char[] { 0xFEFF });

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
     * Returns if the given file has a python shebang (i.e.: starts with #!... python)
     * 
     * Will close the reader.
     */
    public static boolean hasPythonShebang(Reader inputStreamReader) throws IllegalCharsetNameException {

        BufferedReader reader = new BufferedReader(inputStreamReader);
        try {
            String l1 = reader.readLine();
            if (l1 != null) {
                if (isPythonShebangLine(l1)) {
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

    public static boolean isPythonShebangLine(String l1) {
        //Special case to skip bom.
        if (l1.startsWith(BOM_UTF8)) {
            l1 = l1.substring(BOM_UTF8.length());
        }

        if (l1.startsWith("#!") && l1.indexOf("python") != -1) {
            return true;
        }
        return false;
    }
}
