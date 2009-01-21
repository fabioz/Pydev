package com.python.pydev.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

public class EnvGetter {
    

    /**
     * @param properties
     * @return
     */
    public static String getPropertiesStr(Properties properties) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            properties.store(out, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toString();
    }
    
    
}
