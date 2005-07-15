/*
 * Created on 15/07/2005
 */
package org.python.pydev.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.python.pydev.core.log.Log;

import sun.misc.BASE64Decoder;

public class Utils {
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
            Log.log(e);
            return null;
        }
    }

}
