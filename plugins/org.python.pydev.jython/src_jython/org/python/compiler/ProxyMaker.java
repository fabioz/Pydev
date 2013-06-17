// Copyright (c) Corporation for National Research Initiatives
package org.python.compiler;

import java.util.Hashtable;
import java.util.Enumeration;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;
import java.io.*;
import org.python.core.Py;

public class ProxyMaker implements ClassConstants {
    public static final int tBoolean = 0;
    public static final int tByte = 1;
    public static final int tShort = 2;
    public static final int tInteger = 3;
    public static final int tLong = 4;
    public static final int tFloat = 5;
    public static final int tDouble = 6;
    public static final int tCharacter = 7;
    public static final int tVoid = 8;
    public static final int tOther = 9;
    public static final int tNone = 10;

    public static Hashtable types = fillTypes();

    public static Hashtable fillTypes() {
        Hashtable types = new Hashtable();
        types.put(Boolean.TYPE, new Integer(tBoolean));
        types.put(Byte.TYPE, new Integer(tByte));
        types.put(Short.TYPE, new Integer(tShort));
        types.put(Integer.TYPE, new Integer(tInteger));
        types.put(Long.TYPE, new Integer(tLong));
        types.put(Float.TYPE, new Integer(tFloat));
        types.put(Double.TYPE, new Integer(tDouble));
        types.put(Character.TYPE, new Integer(tCharacter));
        types.put(Void.TYPE, new Integer(tVoid));
        return types;
    }

    public static int getType(Class c) {
        if (c == null)
            return tNone;
        Object i = types.get(c);
        if (i == null)
            return tOther;
        else
            return ((Integer) i).intValue();
    }

    Class superclass;
    Class[] interfaces;
    Hashtable names;
    Hashtable supernames = new Hashtable();
    public ClassFile classfile;
    public String myClass;
    public boolean isAdapter = false;

    // Ctor used by makeProxy and AdapterMaker.
    public ProxyMaker(String classname, Class superclass) {
        this.myClass = "org.python.proxies." + classname;
        if (superclass.isInterface()) {
            this.superclass = Object.class;
            this.interfaces = new Class[] { superclass };
        } else {
            this.superclass = superclass;
            this.interfaces = new Class[0];
        }
    }

    // Ctor used by javamaker.
    public ProxyMaker(String myClass, Class superclass, Class[] interfaces) {
        this.myClass = myClass;
        if (superclass == null)
            superclass = Object.class;
        this.superclass = superclass;
        if (interfaces == null)
            interfaces = new Class[0];
        this.interfaces = interfaces;
    }

    public static String mapClass(Class c) {
        String name = c.getName();
        int index = name.indexOf(".");
        if (index == -1)
            return name;

        StringBuffer buf = new StringBuffer(name.length());
        int last_index = 0;
        while (index != -1) {
            buf.append(name.substring(last_index, index));
            buf.append("/");
            last_index = index + 1;
            index = name.indexOf(".", last_index);
        }
        buf.append(name.substring(last_index, name.length()));
        return buf.toString();
    }

    public static String mapType(Class type) {
        if (type.isArray())
            return "[" + mapType(type.getComponentType());

        switch (getType(type)) {
            case tByte:
                return "B";
            case tCharacter:
                return "C";
            case tDouble:
                return "D";
            case tFloat:
                return "F";
            case tInteger:
                return "I";
            case tLong:
                return "J";
            case tShort:
                return "S";
            case tBoolean:
                return "Z";
            case tVoid:
                return "V";
            default:
                return "L" + mapClass(type) + ";";
        }
    }

    public static String makeSignature(Class[] sig, Class ret) {
        StringBuffer buf = new StringBuffer();
        buf.append("(");
        for (int i = 0; i < sig.length; i++) {
            buf.append(mapType(sig[i]));
        }
        buf.append(")");
        buf.append(mapType(ret));
        return buf.toString();
    }

    public void doConstants() throws Exception {
        Code code = classfile.addMethod("<clinit>", "()V", Modifier.STATIC);
        code.return_();
    }

    public static void doReturn(Code code, Class type) throws Exception {
        switch (getType(type)) {
            case tNone:
                break;
            case tCharacter:
            case tBoolean:
            case tByte:
            case tShort:
            case tInteger:
                code.ireturn();
                break;
            case tLong:
                code.lreturn();
                break;
            case tFloat:
                code.freturn();
                break;
            case tDouble:
                code.dreturn();
                break;
            case tVoid:
                code.return_();
                break;
            default:
                code.areturn();
                break;
        }
    }

    public static void doNullReturn(Code code, Class type) throws Exception {
        switch (getType(type)) {
            case tNone:
                break;
            case tCharacter:
            case tBoolean:
            case tByte:
            case tShort:
            case tInteger:
                code.iconst(0);
                code.ireturn();
                break;
            case tLong:
                code.ldc(code.pool.Long(0));
                code.lreturn();
                break;
            case tFloat:
                code.ldc(code.pool.Float((float) 0.));
                code.freturn();
                break;
            case tDouble:
                code.ldc(code.pool.Double(0.));
                code.dreturn();
                break;
            case tVoid:
                code.return_();
                break;
            default:
                code.aconst_null();
                code.areturn();
                break;
        }
    }

    public void callSuper(Code code, String name, String superclass, Class[] parameters, Class ret, String sig)
            throws Exception {
        code.aload(0);
        int local_index;
        int i;
        for (i = 0, local_index = 1; i < parameters.length; i++) {
            switch (getType(parameters[i])) {
                case tCharacter:
                case tBoolean:
                case tByte:
                case tShort:
                case tInteger:
                    code.iload(local_index);
                    local_index += 1;
                    break;
                case tLong:
                    code.lload(local_index);
                    local_index += 2;
                    break;
                case tFloat:
                    code.fload(local_index);
                    local_index += 1;
                    break;
                case tDouble:
                    code.dload(local_index);
                    local_index += 2;
                    break;
                default:
                    code.aload(local_index);
                    local_index += 1;
                    break;
            }
        }
        int meth = code.pool.Methodref(superclass, name, sig);
        code.invokespecial(meth);
        doReturn(code, ret);
    }

    public void doJavaCall(Code code, String name, String type, String jcallName) throws Exception {
        int jcall = code.pool.Methodref("org/python/core/PyObject", jcallName, "(" + $objArr + ")" + $pyObj);

        int py2j = code.pool.Methodref("org/python/core/Py", "py2" + name, "(" + $pyObj + ")" + type);

        code.invokevirtual(jcall);
        code.invokestatic(py2j);
    }

    public void getArgs(Code code, Class[] parameters) throws Exception {
        if (parameters.length == 0) {
            int EmptyObjects = code.pool.Fieldref("org/python/core/Py", "EmptyObjects", $pyObjArr);
            code.getstatic(EmptyObjects);
        } else {
            code.iconst(parameters.length);
            code.anewarray(code.pool.Class("java/lang/Object"));
            int array = code.getLocal("[org/python/core/PyObject");
            code.astore(array);

            int local_index;
            int i;
            for (i = 0, local_index = 1; i < parameters.length; i++) {
                code.aload(array);
                code.iconst(i);

                switch (getType(parameters[i])) {
                    case tBoolean:
                    case tByte:
                    case tShort:
                    case tInteger:
                        code.iload(local_index);
                        local_index += 1;

                        int newInteger = code.pool.Methodref("org/python/core/Py", "newInteger", "(I)" + $pyInteger);
                        code.invokestatic(newInteger);
                        break;
                    case tLong:
                        code.lload(local_index);
                        local_index += 2;

                        int newInteger1 = code.pool.Methodref("org/python/core/Py", "newInteger", "(J)" + $pyObj);
                        code.invokestatic(newInteger1);
                        break;
                    case tFloat:
                        code.fload(local_index);
                        local_index += 1;

                        int newFloat = code.pool.Methodref("org/python/core/Py", "newFloat", "(F)" + $pyFloat);
                        code.invokestatic(newFloat);
                        break;
                    case tDouble:
                        code.dload(local_index);
                        local_index += 2;

                        int newFloat1 = code.pool.Methodref("org/python/core/Py", "newFloat", "(D)" + $pyFloat);
                        code.invokestatic(newFloat1);
                        break;
                    case tCharacter:
                        code.iload(local_index);
                        local_index += 1;
                        int newString = code.pool.Methodref("org/python/core/Py", "newString", "(C)" + $pyStr);
                        code.invokestatic(newString);
                        break;
                    default:
                        code.aload(local_index);
                        local_index += 1;
                        break;
                }
                code.aastore();
            }
            code.aload(array);
        }
    }

    public void callMethod(Code code, String name, Class[] parameters, Class ret, Class[] exceptions) throws Exception {
        Label start = null;
        Label end = null;

        String jcallName = "_jcall";
        int instLocal = 0;

        if (exceptions.length > 0) {
            start = code.getLabel();
            end = code.getLabel();
            jcallName = "_jcallexc";
            instLocal = code.getLocal("org/python/core/PyObject");
            code.astore(instLocal);
            start.setPosition();
            code.aload(instLocal);
        }

        getArgs(code, parameters);

        switch (getType(ret)) {
            case tCharacter:
                doJavaCall(code, "char", "C", jcallName);
                break;
            case tBoolean:
                doJavaCall(code, "boolean", "Z", jcallName);
                break;
            case tByte:
            case tShort:
            case tInteger:
                doJavaCall(code, "int", "I", jcallName);
                break;
            case tLong:
                doJavaCall(code, "long", "J", jcallName);
                break;
            case tFloat:
                doJavaCall(code, "float", "F", jcallName);
                break;
            case tDouble:
                doJavaCall(code, "double", "D", jcallName);
                break;
            case tVoid:
                doJavaCall(code, "void", "V", jcallName);
                break;
            default:
                int jcall = code.pool.Methodref("org/python/core/PyObject", jcallName, "(" + $objArr + ")" + $pyObj);
                code.invokevirtual(jcall);
                /* catching exceptions is not vm mandatory
                Label forname_start =code.getLabel();
                Label forname_end = code.getLabel();
                Label forname_exch_start = code.getLabel();
                Label forname_exch_end = code.getLabel();
                forname_start.setPosition();
                */
                int forname = code.pool.Methodref("java/lang/Class", "forName", "(" + $str + ")" + $clss);
                code.ldc(ret.getName());
                code.invokestatic(forname);
                /*
                forname_end.setPosition();
                code.goto_(forname_exch_end);
                forname_exch_start.setPosition();
                code.stack = 1;
                // never reached, but this code keeps the verifier happy
                code.pop();
                code.aconst_null();
                code.dup();
                forname_exch_end.setPosition();

                code.addExceptionHandler(forname_start,forname_end,
                        forname_exch_start,
                        code.pool.Class("java/lang/ClassNotFoundException"));
                */
                int tojava = code.pool.Methodref("org/python/core/Py", "tojava", "(" + $pyObj + $clss + ")" + $obj);
                code.invokestatic(tojava);
                // I guess I need this checkcast to keep the verifier happy
                code.checkcast(code.pool.Class(mapClass(ret)));
                break;
        }
        if (exceptions.length > 0)
            end.setPosition();

        doReturn(code, ret);

        if (exceptions.length > 0) {
            boolean throwableFound = false;

            Label handlerStart = null;
            for (int i = 0; i < exceptions.length; i++) {
                handlerStart = code.getLabel();
                handlerStart.setPosition();
                code.stack = 1;
                int excLocal = code.getLocal("java/lang/Throwable");
                code.astore(excLocal);

                code.aload(excLocal);
                code.athrow();

                code.addExceptionHandler(start, end, handlerStart, code.pool.Class(mapClass(exceptions[i])));
                doNullReturn(code, ret);

                code.freeLocal(excLocal);
                if (exceptions[i] == Throwable.class)
                    throwableFound = true;
            }

            if (!throwableFound) {
                // The final catch (Throwable)
                handlerStart = code.getLabel();
                handlerStart.setPosition();
                code.stack = 1;
                int excLocal = code.getLocal("java/lang/Throwable");
                code.astore(excLocal);
                code.aload(instLocal);
                code.aload(excLocal);

                int jthrow = code.pool.Methodref("org/python/core/PyObject", "_jthrow", "(" + $throwable + ")V");
                code.invokevirtual(jthrow);

                code.addExceptionHandler(start, end, handlerStart, code.pool.Class("java/lang/Throwable"));
                code.freeLocal(excLocal);
                doNullReturn(code, ret);
            }
            code.freeLocal(instLocal);
        }
    }

    public void addMethod(Method method, int access) throws Exception {
        boolean isAbstract = false;

        if (Modifier.isAbstract(access)) {
            access = access & ~Modifier.ABSTRACT;
            isAbstract = true;
        }

        Class[] parameters = method.getParameterTypes();
        Class ret = method.getReturnType();
        String sig = makeSignature(parameters, ret);

        String name = method.getName();
        //         System.out.println(name+": "+sig);
        names.put(name, name);

        Code code = classfile.addMethod(name, sig, access);

        code.aload(0);
        code.ldc(name);

        if (!isAbstract) {
            int tmp = code.getLocal("org/python/core/PyObject");
            int jfindattr = code.pool
                    .Methodref("org/python/core/Py", "jfindattr", "(" + $pyProxy + $str + ")" + $pyObj);
            code.invokestatic(jfindattr);

            code.astore(tmp);
            code.aload(tmp);

            Label callPython = code.getLabel();

            code.ifnonnull(callPython);

            String superclass = mapClass(method.getDeclaringClass());

            callSuper(code, name, superclass, parameters, ret, sig);
            callPython.setPosition();
            code.aload(tmp);
            callMethod(code, name, parameters, ret, method.getExceptionTypes());

            addSuperMethod("super__" + name, name, superclass, parameters, ret, sig, access);
        } else {
            if (!isAdapter) {
                int jgetattr = code.pool.Methodref("org/python/core/Py", "jgetattr", "(" + $pyProxy + $str + ")"
                        + $pyObj);
                code.invokestatic(jgetattr);
                callMethod(code, name, parameters, ret, method.getExceptionTypes());
            } else {
                int jfindattr = code.pool.Methodref("org/python/core/Py", "jfindattr", "(" + $pyProxy + $str + ")"
                        + $pyObj);
                code.invokestatic(jfindattr);
                code.dup();
                Label returnNull = code.getLabel();
                code.ifnull(returnNull);
                callMethod(code, name, parameters, ret, method.getExceptionTypes());
                returnNull.setPosition();
                code.pop();
                doNullReturn(code, ret);
            }
        }
    }

    private String methodString(Method m) {
        StringBuffer buf = new StringBuffer(m.getName());
        buf.append(":");
        Class[] params = m.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            buf.append(params[i].getName());
            buf.append(",");
        }
        return buf.toString();
    }

    protected void addMethods(Class c, Hashtable t) throws Exception {
        Method[] methods = c.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String s = methodString(method);
            if (t.containsKey(s))
                continue;
            t.put(s, s);

            int access = method.getModifiers();
            if (Modifier.isStatic(access) || Modifier.isPrivate(access)) {
                continue;
            }

            if (Modifier.isNative(access)) {
                access = access & ~Modifier.NATIVE;
            }

            if (Modifier.isProtected(access)) {
                access = (access & ~Modifier.PROTECTED) | Modifier.PUBLIC;
                if (Modifier.isFinal(access)) {
                    addSuperMethod(methods[i], access);
                    continue;
                }
            } else if (Modifier.isFinal(access)) {
                continue;
            }
            addMethod(methods[i], access);
        }

        Class sc = c.getSuperclass();
        if (sc != null)
            addMethods(sc, t);

        Class[] interfaces = c.getInterfaces();
        for (int j = 0; j < interfaces.length; j++) {
            addMethods(interfaces[j], t);
        }
    }

    public void addConstructor(String name, Class[] parameters, Class ret, String sig, int access) throws Exception {
        Code code = classfile.addMethod("<init>", sig, access);
        callSuper(code, "<init>", name, parameters, Void.TYPE, sig);
    }

    public void addConstructors(Class c) throws Exception {
        Constructor[] constructors = c.getDeclaredConstructors();
        String name = mapClass(c);
        for (int i = 0; i < constructors.length; i++) {
            int access = constructors[i].getModifiers();
            if (Modifier.isPrivate(access))
                continue;
            if (Modifier.isNative(access))
                access = access & ~Modifier.NATIVE;
            if (Modifier.isProtected(access))
                access = access & ~Modifier.PROTECTED | Modifier.PUBLIC;
            Class[] parameters = constructors[i].getParameterTypes();
            String sig = makeSignature(parameters, Void.TYPE);
            addConstructor(name, parameters, Void.TYPE, sig, access);
        }
    }

    // Super methods are added for the following three reasons:
    //
    //   1) for a protected non-final method add a public method with no
    //   super__ prefix.  This gives needed access to this method for
    //   subclasses
    //
    //   2) for protected final methods, add a public method with the
    //   super__ prefix.  This avoids the danger of trying to override a
    //   final method
    //
    //   3) For any other method that is overriden, add a method with the
    //   super__ prefix.  This gives access to super. version or the
    //   method.
    //
    public void addSuperMethod(Method method, int access) throws Exception {
        Class[] parameters = method.getParameterTypes();
        Class ret = method.getReturnType();
        String sig = makeSignature(parameters, ret);
        String superclass = mapClass(method.getDeclaringClass());
        String superName = method.getName();
        String methodName = superName;
        if (Modifier.isFinal(access)) {
            methodName = "super__" + superName;
            access &= ~Modifier.FINAL;
        }
        addSuperMethod(methodName, superName, superclass, parameters, ret, sig, access);
    }

    public void addSuperMethod(String methodName, String superName, String declClass, Class[] parameters, Class ret,
            String sig, int access) throws Exception {
        if (methodName.startsWith("super__")) {
            /* rationale: JC java-class, P proxy-class subclassing JC
               in order to avoid infinite recursion P should define super__foo
               only if no class between P and JC in the hierarchy defines
               it yet; this means that the python class needing P is the
               first that redefines the JC method foo.
            */
            try {
                superclass.getMethod(methodName, parameters);
                return;
            } catch (NoSuchMethodException e) {
            } catch (SecurityException e) {
                return;
            }
        }
        supernames.put(methodName, methodName);
        Code code = classfile.addMethod(methodName, sig, access);
        callSuper(code, superName, declClass, parameters, ret, sig);
    }

    public void addProxy() throws Exception {
        // implement PyProxy interface
        classfile.addField("__proxy", "Lorg/python/core/PyInstance;", Modifier.PROTECTED);
        // setProxy method
        Code code = classfile.addMethod("_setPyInstance", "(Lorg/python/core/PyInstance;)V", Modifier.PUBLIC);

        int field = code.pool.Fieldref(classfile.name, "__proxy", "Lorg/python/core/PyInstance;");
        code.aload(0);
        code.aload(1);
        code.putfield(field);
        code.return_();

        // getProxy method
        code = classfile.addMethod("_getPyInstance", "()Lorg/python/core/PyInstance;", Modifier.PUBLIC);
        code.aload(0);
        code.getfield(field);
        code.areturn();

        // implement PyProxy interface
        classfile.addField("__systemState", "Lorg/python/core/PySystemState;", Modifier.PROTECTED | Modifier.TRANSIENT);

        // setProxy method
        code = classfile.addMethod("_setPySystemState", "(Lorg/python/core/PySystemState;)V", Modifier.PUBLIC);

        field = code.pool.Fieldref(classfile.name, "__systemState", "Lorg/python/core/PySystemState;");
        code.aload(0);
        code.aload(1);
        code.putfield(field);
        code.return_();

        // getProxy method
        code = classfile.addMethod("_getPySystemState", "()Lorg/python/core/PySystemState;", Modifier.PUBLIC);
        code.aload(0);
        code.getfield(field);
        code.areturn();
    }

    public void addClassDictInit() throws Exception {
        int n = supernames.size();

        // classDictInit method
        classfile.addInterface(mapClass(org.python.core.ClassDictInit.class));
        Code code = classfile.addMethod("classDictInit", "(" + $pyObj + ")V", Modifier.PUBLIC | Modifier.STATIC);
        code.aload(0);
        code.ldc("__supernames__");

        String[] names = new String[n];
        Enumeration e = supernames.keys();
        for (int i = 0; e.hasMoreElements();)
            names[i++] = (String) e.nextElement();
        CodeCompiler.makeStrings(code, names, n);
        int j2py = code.pool.Methodref("org/python/core/Py", "java2py", "(" + $obj + ")" + $pyObj);
        code.invokestatic(j2py);

        int setitem = code.pool.Methodref("org/python/core/PyObject", "__setitem__", "(" + $str + $pyObj + ")V");
        code.invokevirtual(setitem);
        code.return_();

    }

    public void build() throws Exception {
        names = new Hashtable();
        int access = superclass.getModifiers();
        if ((access & Modifier.FINAL) != 0) {
            throw new InstantiationException("can't subclass final class");
        }
        access = Modifier.PUBLIC | Modifier.SYNCHRONIZED;

        classfile = new ClassFile(myClass, mapClass(superclass), access);
        addProxy();
        addConstructors(superclass);
        classfile.addInterface("org/python/core/PyProxy");

        Hashtable seenmethods = new Hashtable();
        addMethods(superclass, seenmethods);
        for (int i = 0; i < interfaces.length; i++) {
            if (interfaces[i].isAssignableFrom(superclass)) {
                Py.writeWarning("compiler", "discarding redundant interface: " + interfaces[i].getName());
                continue;
            }
            classfile.addInterface(mapClass(interfaces[i]));
            addMethods(interfaces[i], seenmethods);
        }
        doConstants();
        addClassDictInit();
    }

    public static String makeProxy(Class superclass, OutputStream ostream) throws Exception {
        ProxyMaker pm = new ProxyMaker(superclass.getName(), superclass);
        pm.build();
        pm.classfile.write(ostream);
        return pm.myClass;
    }

    public static File makeFilename(String name, File dir) {
        int index = name.indexOf(".");
        if (index == -1)
            return new File(dir, name + ".class");

        return makeFilename(name.substring(index + 1, name.length()), new File(dir, name.substring(0, index)));
    }

    // This is not general enough
    public static OutputStream getFile(String d, String name) throws IOException {
        File dir = new File(d);
        File file = makeFilename(name, dir);
        new File(file.getParent()).mkdirs();
        //System.out.println("proxy file: "+file);
        return new FileOutputStream(file);
    }
}
