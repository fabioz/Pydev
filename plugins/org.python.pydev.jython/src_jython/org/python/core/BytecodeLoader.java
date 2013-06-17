// Copyright (c) Corporation for National Research Initiatives

package org.python.core;

import java.util.Vector;

/**
 * Utility class for loading of compiled python modules and java classes defined
 * in python modules.
 */
public class BytecodeLoader {

    static Vector init() {
        Vector parents = new Vector();
        parents.addElement(imp.getSyspathJavaLoader());
        return parents;
    }

    static Class findParentClass(Vector parents, String name) throws ClassNotFoundException {
        for (int i = 0; i < parents.size(); i++) {
            try {
                return ((ClassLoader) parents.elementAt(i)).loadClass(name);
            } catch (ClassNotFoundException e) {
            }
        }
        // couldn't find the .class file on sys.path
        throw new ClassNotFoundException(name);
    }

    static void compileClass(Class c) {
        // This method has caused much trouble. Using it breaks jdk1.2rc1
        // Not using it can make SUN's jdk1.1.6 JIT slightly unhappy.
        // Don't use by default, but allow python.options.compileClass to
        // override
        if (!Options.skipCompile) {
            // System.err.println("compile: "+name);
            Compiler.compileClass(c);
        }
    }

    private static Class loaderClass = null;

    private static Loader makeLoader() {
        if (loaderClass == null) {
            synchronized (BytecodeLoader.class) {
                String version = System.getProperty("java.version");
                if (version.compareTo("1.2") >= 0) {
                    try {
                        loaderClass = Class.forName("org.python.core.BytecodeLoader2");
                    } catch (Throwable e) {
                        loaderClass = BytecodeLoader1.class;
                    }
                } else
                    loaderClass = BytecodeLoader1.class;
            }
        }
        try {
            return (Loader) loaderClass.newInstance();
        } catch (Exception e) {
            return new BytecodeLoader1();
        }
    }

    /**
     * Turn the java byte code in data into a java class.
     * 
     * @param name the name of the class
     * @param referents a list of superclass and interfaces that the new class
     *            will reference.
     * @param data the java byte code.
     */
    public static Class makeClass(String name, Vector referents, byte[] data) {
        Loader loader = makeLoader();

        if (referents != null) {
            for (int i = 0; i < referents.size(); i++) {
                try {
                    Class cls = (Class) referents.elementAt(i);
                    ClassLoader cur = cls.getClassLoader();
                    if (cur != null) {
                        loader.addParent(cur);
                    }
                } catch (SecurityException e) {
                }
            }
        }
        return loader.loadClassFromBytes(name, data);
    }

    /**
     * Turn the java byte code for a compiled python module into a java class.
     * 
     * @param name the name of the class
     * @param data the java byte code.
     */
    public static PyCode makeCode(String name, byte[] data, String filename) {
        try {
            Class c = makeClass(name, null, data);
            Object o = c.getConstructor(new Class[] { String.class }).newInstance(new Object[] { filename });
            return ((PyRunnable) o).getMain();
        } catch (Exception e) {
            throw Py.JavaError(e);
        }
    }
}
