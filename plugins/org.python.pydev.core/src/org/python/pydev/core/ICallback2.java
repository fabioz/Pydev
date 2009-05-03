package org.python.pydev.core;

public interface ICallback2<Ret, Arg, Arg2> {

    Ret call(Arg arg, Arg2 arg2);
}
