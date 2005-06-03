/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

import org.python.pydev.plugin.PydevPlugin;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * @author Fabio Zadrozny
 */
public class REF {

    public static boolean hasAttr(Object o, String attr){
        try {
            o.getClass().getDeclaredField(attr);
        } catch (SecurityException e) {
            return false;
        } catch (NoSuchFieldException e) {
            return false;
        }
        return true;
    }

    public static Field getAttr(Object o, String attr){
        try {
            return o.getClass().getDeclaredField(attr);
        } catch (SecurityException e) {
            return null;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
    
    public static Object getAttrObj(Object o, String attr){
        if (REF.hasAttr(o, attr)) {
            Field field = REF.getAttr(o, attr);
            try {
                Object obj = field.get(o);
                return obj;
            }catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * @param file
     */
    public static String getFileContents(File file) {
        try {
            FileInputStream stream = new FileInputStream(file);
            int i = stream.available();
            byte[] b = new byte[i];
            stream.read(b);
            return new String(b);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param list
     * @return
     */
    public static String getObjAsStr(Object list) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ObjectOutputStream stream = new ObjectOutputStream(out);
            stream.writeObject(list);
            stream.close();
        } catch (Exception e) {
            PydevPlugin.log(e);
            throw new RuntimeException(e);
        }
    
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(out.toByteArray());
    }

    /**
     * @param persisted
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object getStrAsObj(String persisted) throws IOException, ClassNotFoundException {
        BASE64Decoder decoder = new BASE64Decoder();
        InputStream input = new ByteArrayInputStream(decoder.decodeBuffer(persisted));
        ObjectInputStream in = new ObjectInputStream(input);
        Object list = in.readObject();
        in.close();
        input.close();
        return list;
    }

    /**
     * @param file
     * @param astManager
     */
    public static void writeToFile(Object o, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            try {
                ObjectOutputStream stream = new ObjectOutputStream(out);
                stream.writeObject(o);
                stream.close();
            } catch (Exception e) {
                PydevPlugin.log(e);
                throw new RuntimeException(e);
            } finally{
                out.close();
            }
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
    }

    /**
     * @param astOutputFile
     * @return
     */
    public static Object readFromFile(File astOutputFile) {
        try {
            InputStream input = new FileInputStream(astOutputFile);
            ObjectInputStream in = new ObjectInputStream(input);
            Object o = in.readObject();
            in.close();
            input.close();
            return o;
        } catch (Exception e) {
            PydevPlugin.log(e);
            return null;
        }
    }

    /**
     * @param f
     * @return
     */
    public static String getFileAbsolutePath(File f) {
        try {
            return f.getCanonicalPath();
        } catch (IOException e) {
            return f.getAbsolutePath();
        }
    }
    
    
    
}

