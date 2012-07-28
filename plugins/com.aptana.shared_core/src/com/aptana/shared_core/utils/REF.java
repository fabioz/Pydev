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
package com.aptana.shared_core.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.environment.Constants;

import com.aptana.shared_core.callbacks.ICallback;
import com.aptana.shared_core.string.FastStringBuffer;

/**
 * @author Fabio Zadrozny
 */
public class REF {

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
     * @param file the file we want to read
     * @return the contents of the file as a string
     */
    private static String getFileContents(File file) {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            FastStringBuffer buf = fillBufferWithStream(stream, null, null);
            return buf.toString();
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

    /**
     * Log with base is missing in java!
     */
    public static double log(double a, double base) {
        return Math.log(a) / Math.log(base);
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
