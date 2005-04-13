/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.utils;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;

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
    
    
    
}

