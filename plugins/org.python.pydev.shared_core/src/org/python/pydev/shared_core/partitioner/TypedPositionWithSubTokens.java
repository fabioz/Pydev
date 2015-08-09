/**
 * Copyright: Fabio Zadrozny
 * License: EPL
 */
package org.python.pydev.shared_core.partitioner;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.TypedPosition;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class TypedPositionWithSubTokens extends TypedPosition {

    /**
     * Note: offsets should be relative to the offset in this position.
     */
    private SubRuleToken subRuleToken;

    public TypedPositionWithSubTokens(int offset, int length, String type, SubRuleToken subRuleToken,
            boolean fixRelativeOffset) {
        super(offset, length, type);
        if (fixRelativeOffset) {
            this.setSubRuleToken(subRuleToken);
        } else {
            this.subRuleToken = subRuleToken;
        }
    }

    public TypedPositionWithSubTokens(int offset, int length, String type, SubRuleToken subRuleToken) {
        this(offset, length, type, subRuleToken, true);
    }

    public void clearSubRuleToken() {
        subRuleToken = null;
    }

    public void setSubRuleToken(SubRuleToken subRuleToken) {
        if (subRuleToken != null) {
            subRuleToken.makeRelativeToOffset(this.getOffset());
        }
        this.subRuleToken = subRuleToken;
    }

    public SubRuleToken getSubRuleToken() {
        return subRuleToken;
    }

    public String toStringTest() {
        FastStringBuffer buf = new FastStringBuffer();
        buf.append(getType() + ":" + getOffset() + ":" + getLength()).append(" [\n ")
                .append(subRuleToken != null ? subRuleToken.toStringBetter() : "null")
                .append("\n]");
        return buf.toString();
    }

    /**
     * Important: the object passed (and this object) may be mutated, thus, a copy should be passed
     * if the original ones should not be changed.
     */
    public void mergeSubRuleToken(TypedPositionWithSubTokens withSub) {
        if (withSub == null || withSub.subRuleToken == null) {
            return;
        }
        if (subRuleToken == null) {
            subRuleToken = withSub.subRuleToken;
            return;
        }

        // We only implement for this situation.
        Assert.isTrue(withSub.offset >= this.offset);

        // Already make it relative to the offset in this position.
        withSub.subRuleToken.addOffset(withSub.offset);
        withSub.subRuleToken.makeRelativeToOffset(offset);

        // Both exist, let's see if the token/token data matches
        Object d0 = subRuleToken.token.getData();
        Object d1 = withSub.subRuleToken.token.getData();
        if (d0 == d1 || (d0 != null && d1 != null && d0.equals(d1))) {
            // Matches: just add the children and fix its len
            subRuleToken.addChildren(withSub.subRuleToken.getChildren());
        } else {
            // The data doesn't match. Create a SubToken with null data with the proper size (or reuse the
            // current one if that's it's type already).
            SubRuleToken newSub;
            if (subRuleToken.token.getData() != null) {
                newSub = new SubRuleToken(new DummyToken(null), subRuleToken.offset, subRuleToken.len);
                newSub.addChild(subRuleToken);
                this.subRuleToken = newSub;
            }
            this.subRuleToken.addChild(withSub.subRuleToken);
        }

        int finalOffset = withSub.subRuleToken.offset + withSub.subRuleToken.len;
        int currentFinalOffset = subRuleToken.offset + subRuleToken.len;
        if (currentFinalOffset < finalOffset) {
            subRuleToken.len += finalOffset - currentFinalOffset;
        }

    }

    public TypedPositionWithSubTokens createCopy() {
        return new TypedPositionWithSubTokens(this.offset, this.length, this.getType(),
                this.subRuleToken != null ? this.subRuleToken.createCopy() : null,
                false);
    }

}
