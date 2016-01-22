/**
 * Copyright: Fabio Zadrozny
 * License: EPL
 */
package org.python.pydev.shared_core.partitioner;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.rules.IToken;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;

public final class SubRuleToken {

    public IToken token;
    public int offset;
    public int len;

    public SubRuleToken(IToken token, int offset, int len) {
        Assert.isTrue(offset >= 0);
        //System.out.println(token + " offset: " + offset + " len:" + len);
        this.token = token;
        this.offset = offset;
        this.len = len;
    }

    public SubRuleToken createCopy() {
        SubRuleToken copy = copyWithoutChildren();
        if (this.children != null) {
            for (SubRuleToken c : this.children) {
                copy.addChild(c.createCopy());
            }
        }
        return copy;
    }

    @Override
    public String toString() {
        FastStringBuffer ret = new FastStringBuffer("SubRuleToken[", 30)
                .appendObject(token.getData())
                .append(" offset: ")
                .append(offset)
                .append(" len: ")
                .append(len);
        if (children != null && children.size() > 0) {
            ret.append(" children:\n")
                    .append(children.toString());
        }
        ret.append(']')
                .toString();
        return ret.toString();
    }

    public String toStringBetter() {
        return toString(0);
    }

    public String toString(int level) {
        String ident = new FastStringBuffer().appendN("   ", level + 1).toString();
        String ident2 = new FastStringBuffer().appendN("   ", level).toString();
        FastStringBuffer ret = new FastStringBuffer("SubRuleToken: ", 30)
                .appendObject(token.getData())
                .append(" offset: ")
                .append(offset)
                .append(" len: ")
                .append(len);
        if (children != null && children.size() > 0) {
            ret.append(" children:[");
            for (SubRuleToken subRuleToken : children) {
                ret.append("\n");
                ret.append(ident);
                ret.append(subRuleToken.toString(level + 1));
            }
            ret.append("\n");
            ret.append(ident2);
            ret.append("]");
        }
        return ret.toString();
    }

    private LinkedList<SubRuleToken> children;

    public void makeRelativeToOffset(int offset) {
        this.offset -= offset;
        if (this.children != null) {
            for (SubRuleToken c : this.children) {
                c.makeRelativeToOffset(offset);
            }
        }
    }

    public void addOffset(int offset) {
        this.offset += offset;
        if (this.children != null) {
            for (SubRuleToken c : this.children) {
                c.addOffset(offset);
            }
        }
    }

    public void flatten(LinkedList<SubRuleToken> lst) {
        addSubRuleToken(lst, this.copyWithoutChildren(), true);
        if (this.children != null) {
            for (SubRuleToken c : this.children) {
                c.flatten(lst);
            }
        }
    }

    private SubRuleToken copyWithoutChildren() {
        return new SubRuleToken(token, offset, len);
    }

    public List<SubRuleToken> flatten() {
        LinkedList<SubRuleToken> lst = new LinkedListWarningOnSlowOperations<>();
        flatten(lst);
        return lst;
    }

    public void addChildren(List<SubRuleToken> lst) {
        if (lst == null) {
            return;
        }
        if (this.children == null) {
            this.children = new LinkedListWarningOnSlowOperations<>();
        }
        for (SubRuleToken subRuleToken : lst) {
            addSubRuleToken(children, subRuleToken);
        }
    }

    public void addChild(SubRuleToken subRuleToken) {
        if (this.children == null) {
            this.children = new LinkedListWarningOnSlowOperations<>();
        }
        addSubRuleToken(children, subRuleToken);
    }

    public static void addSubRuleToken(LinkedList<SubRuleToken> lst, SubRuleToken subRuleToken) {
        addSubRuleToken(lst, subRuleToken, false);
    }

    /**
     * Adds a sub rule token to a list with existing sub-rule tokens. It fixes existing sub-rule tokens
     * so that they do not overlap. Note that list is always kept ordered by the offset/len.
     */
    public static void addSubRuleToken(LinkedList<SubRuleToken> lst, SubRuleToken subRuleToken, boolean ignoreEmpty) {
        if (ignoreEmpty) {
            if (subRuleToken.token == null) {
                return;
            }
            if (subRuleToken.token instanceof DummyToken) {
                return;
            }
            Object data = subRuleToken.token.getData();
            if (data == null || "".equals(data)) {
                return;
            }
        }
        for (ListIterator<SubRuleToken> it = lst.listIterator(lst.size()); it.hasPrevious();) {
            SubRuleToken prev = it.previous();
            if (prev.offset + prev.len <= subRuleToken.offset) {
                //This should be 95% of our use-cases (always add a new one non-overlapping
                //at the last position).
                it.next();
                it.add(subRuleToken);
                return;
            } else {
                //Everything from now on is to add it in the proper place properly
                //managing possible overlaps with existing regions.

                if (prev.offset < subRuleToken.offset) {
                    int prevEndOffset = prev.offset + prev.len;
                    int newEndOffset = subRuleToken.offset + subRuleToken.len;
                    prev.len = subRuleToken.offset - prev.offset;
                    it.next();
                    it.add(subRuleToken);

                    if (prevEndOffset > newEndOffset) {
                        // We have to create a new as the newly added was in the middle of the existing one.
                        it.add(new SubRuleToken(prev.token, newEndOffset, prevEndOffset - newEndOffset));
                    }

                    return;

                } else if (prev.offset == subRuleToken.offset) {
                    //Same starting offset, let's see if it has to be broken

                    if (prev.len <= subRuleToken.len) {
                        //Same len (or smaller): the new one overrides it.
                        it.remove();
                        it.add(subRuleToken);
                        return;

                    } else {
                        //The previous is larger than the new one. Let's change its
                        //starting offset/len and add the new one before it.
                        int newOffset = subRuleToken.offset + subRuleToken.len;
                        prev.len = prev.len - (newOffset - prev.offset);
                        prev.offset = newOffset;
                        it.add(subRuleToken);

                        return;
                    }

                } else {
                    // The previous offset is higher than the newly added token
                    it.remove(); //Remove the previous and add it back later.
                    while (it.hasPrevious()) {
                        SubRuleToken beforePrevious = it.previous();
                        int beforePreviousEndOffset = beforePrevious.offset + beforePrevious.len;
                        if (beforePreviousEndOffset < subRuleToken.offset) {
                            //All ok (keep it).
                            it.next();
                            break;
                        } else {
                            if (beforePrevious.offset == subRuleToken.offset) {
                                if (beforePrevious.len <= subRuleToken.len) {
                                    //No need to keep it at all: just remove it.
                                    it.remove();
                                    break;

                                } else {
                                    //We need to keep it (but it'll appear after the one
                                    //we're adding now).
                                    it.remove();
                                    it.add(subRuleToken);

                                    beforePrevious.offset = subRuleToken.offset + subRuleToken.len;
                                    beforePrevious.len = beforePreviousEndOffset - beforePrevious.offset;
                                    subRuleToken = null;
                                    it.add(beforePrevious);
                                    break;

                                }

                            } else if (beforePrevious.offset < subRuleToken.offset) {
                                //Ok, we found one which is lower than the newly added token, so, let's
                                //fix it.
                                if (beforePreviousEndOffset > subRuleToken.offset + subRuleToken.len) {
                                    //It's in the middle of this token.
                                    beforePrevious.len = subRuleToken.offset - beforePrevious.offset;
                                    it.next();
                                    it.add(subRuleToken);

                                    int newEndOffset = subRuleToken.offset + subRuleToken.len;
                                    // We have to create a new as the newly added was in the middle of the existing one.
                                    it.add(new SubRuleToken(beforePrevious.token, newEndOffset, beforePreviousEndOffset
                                            - newEndOffset));
                                    it.add(prev);
                                    return;

                                } else {
                                    if (subRuleToken.offset < beforePreviousEndOffset) {
                                        beforePrevious.len = subRuleToken.offset - beforePrevious.offset;
                                    }
                                    it.next();
                                    break;
                                }

                            } else {
                                if (beforePrevious.offset >= subRuleToken.offset + subRuleToken.len
                                        || beforePreviousEndOffset > subRuleToken.offset + subRuleToken.len) {
                                    it.remove();
                                    it.add(prev);
                                    it.previous();
                                    prev = beforePrevious;
                                    continue;
                                } else {
                                    it.remove();
                                    continue;
                                }
                            }
                        }
                    }

                    if (subRuleToken != null) {
                        int newOffset = subRuleToken.offset + subRuleToken.len;
                        int prevEndOffset = prev.offset + prev.len;
                        if (prev.offset < newOffset) {
                            prev.len = prevEndOffset - newOffset;
                            prev.offset = newOffset;
                        }
                        it.add(subRuleToken);
                    }
                    if (prev.len > 0) {
                        it.add(prev);
                    }
                    return;
                }
            }
        }
        lst.add(subRuleToken);
    }

    public static void fillWithSubToken(IToken contentScope, IRegion contentRegion, LinkedList<SubRuleToken> lst) {
        final int offset = contentRegion.getOffset();
        final int len = contentRegion.getLength();
        fillWithSubToken(contentScope, offset, len, lst);
    }

    public static void fillWithSubToken(IToken contentScope, final int offset, final int len,
            LinkedList<SubRuleToken> lst) {
        int lastOffset = offset;
        int lastLen = 0;
        for (ListIterator<SubRuleToken> it = lst.listIterator(); it.hasNext();) {
            SubRuleToken next = it.next();
            if (next.offset > lastOffset + lastLen) {
                int off = lastOffset + lastLen;
                int l = next.offset - (lastOffset + lastLen);
                it.set(new SubRuleToken(contentScope, off, l));
                it.add(next);
            }
            lastOffset = next.offset;
            lastLen = next.len;
        }
        // To finish, check offset+len

        if (offset + len > lastOffset + lastLen) {
            int off = lastOffset + lastLen;
            int l = (offset + len) - (lastOffset + lastLen);
            lst.add(new SubRuleToken(contentScope, off, l));
        }
    }

    public List<SubRuleToken> getChildren() {
        return this.children;
    }

    public void fillWithTokensAtOffset(int offset, List<IToken> lst) {
        if (offset >= this.offset && offset <= this.offset + this.len) {
            lst.add(token);
            if (this.children != null) {
                for (SubRuleToken c : this.children) {
                    c.fillWithTokensAtOffset(offset, lst);
                }
            }
        }
    }

}