package org.python.pydev.editor.codecompletion.revisited;

import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.cache.LRUCache;

/**
 * Default completion cache implementation
 *
 * @author Fabio
 */
public class CompletionCache extends LRUCache<Object, Object> implements ICompletionCache {

    public CompletionCache() {
        super(250);
    }

}
