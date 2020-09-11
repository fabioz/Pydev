package org.python.pydev.core;

import java.util.Set;

import org.python.pydev.shared_core.structure.Tuple;

public interface ITypeInfo {

    Set<Tuple<Integer, Integer>> getChildPositions();

    String getActTok();

    ITypeInfo getPackedType();

    ITypeInfo getUnpacked(UnpackInfo unpackInfo);

}