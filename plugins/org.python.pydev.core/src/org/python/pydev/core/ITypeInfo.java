package org.python.pydev.core;

public interface ITypeInfo {

    String getActTok();

    ITypeInfo getPackedType();

    ITypeInfo getUnpacked(UnpackInfo unpackInfo);

}