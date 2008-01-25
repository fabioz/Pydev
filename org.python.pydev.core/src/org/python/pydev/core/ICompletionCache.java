package org.python.pydev.core;

import org.python.pydev.core.cache.Cache;

/**
 * This interface defines a behavior for a cache that can be used to store info collected during a code-completion
 * operation (or other related operations) 
 *
 * @author Fabio
 */
public interface ICompletionCache extends Cache<Object, Object>{

}
