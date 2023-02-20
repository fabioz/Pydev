package org.python.pydev.core;

public interface ITypeInfo {

    String getActTok();

    Object getNode();

    ITypeInfo getPackedType();

    ITypeInfo getUnpacked(UnpackInfo unpackInfo);

}