
package com.rc.retroweaver.runtime;

import java.util.Iterator;

/**
 * A version of the 1.5 java.lang.Iterable class for the 1.4 VM.
 *
 * @author Toby Reyelts
 *
 */
public interface Iterable_<E> {
  public abstract Iterator<E> iterator();
}

