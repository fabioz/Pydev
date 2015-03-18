/**
 * Copyright: Fabio Zadrozny
 * License: EPL
 */
package org.python.pydev.shared_core.partitioner;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.rules.IToken;
import org.python.pydev.shared_core.string.FastStringBuffer;

public final class SubRuleToken {

    public final IToken token;
    public final int offset;
    public final int len;

    public SubRuleToken(IToken token, int offset, int len) {
        Assert.isTrue(len >= 0);
        this.token = token;
        this.offset = offset;
        this.len = len;
    }

    @Override
    public String toString() {
        return new FastStringBuffer("SubRuleToken[", 30)
                .appendObject(token.getData())
                .append(" offset: ")
                .append(offset)
                .append(" len: ")
                .append(len)
                .append(']')
                .toString();
    }

}