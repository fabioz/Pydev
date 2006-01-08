/*
 * Created on Nov 12, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.core;

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
import java.lang.reflect.Method;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.core.resources.IProject;
import org.python.pydev.core.log.Log;


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
     * @param file the file we want to read
     * @return the contents of the fil as a string
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

	public static byte[] encodeBase64(ByteArrayOutputStream out) {
		byte[] byteArray = out.toByteArray();
		return encodeBase64(byteArray);
	}

	public static byte[] encodeBase64(byte[] byteArray) {
		return Base64.encodeBase64(byteArray);
	}
    
    /**
     * 
     * @param persisted the base64 string that should be converted to an object.
     * @param readFromFileMethod should be the calback from the plugin that is calling this function
     * 
     * 
     * The callback should be something as:

        new ICallback<Object, ObjectInputStream>(){

            public Object call(ObjectInputStream arg) {
                try {
                    return arg.readObject();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }};

     * 
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object getStrAsObj(String persisted, ICallback<Object, ObjectInputStream> readFromFileMethod) throws IOException, ClassNotFoundException {
        InputStream input = new ByteArrayInputStream(decodeBase64(persisted));
        ObjectInputStream in = new ObjectInputStream(input);
        Object o = readFromFileMethod.call(in);
        in.close();
        input.close();
        return o;
    }

	public static byte[] decodeBase64(String persisted) {
		return Base64.decodeBase64(persisted.getBytes());
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
                Log.log(e);
                throw new RuntimeException(e);
            } finally{
                out.close();
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    public static String getFileAbsolutePath(String f) {
        return getFileAbsolutePath(new File(f));
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

    /**
     * Calls a method for an object
     * 
     * @param obj the object with the method we want to call
     * @param name the method name
     * @param args the arguments received for the call
     * @return the return of the method
     */
    public static Object invoke(Object obj, String name, Object... args) {
        //the args are not checked for the class because if a subclass is passed, the method is not correctly gotten
        //another method might do it...
        try {
            Method[] methods = obj.getClass().getMethods();
            for (Method method : methods) {

                Class[] parameterTypes = method.getParameterTypes();
                if(method.getName().equals(name) && parameterTypes.length == args.length){
                    //check the parameters
                    int i = 0;
                    for (Class param : parameterTypes) {
                        if(!param.isInstance(args[i])){
                            continue;
                        }
                        i++;
                    }
                    //invoke it
                    return method.invoke(obj, args);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("The method with name: "+name+" was not found (or maybe it was found but the parameters didn't match).");
    }

    public static char [] INVALID_FILESYSTEM_CHARS = {
    	'!', '@', '#', '$', '%', '^', '&', '*', 
    	'(', ')', '[', ']', '{', '}', '=', '+',
    	'.', ' ', '`', '~', '\'', '"', ',', ';'};

    public static String getValidProjectName(IProject project) {
		String name = project.getName();
		
		for (char c : INVALID_FILESYSTEM_CHARS) {
			name = name.replace(c, '_');
		}
		
		return name;
	}

    
    
    
}

