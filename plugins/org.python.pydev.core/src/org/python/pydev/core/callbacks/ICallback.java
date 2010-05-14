/*
 * Created on 13/10/2005
 */
package org.python.pydev.core.callbacks;

public interface ICallback<Ret, Arg> {

    Ret call(Arg arg);
}
