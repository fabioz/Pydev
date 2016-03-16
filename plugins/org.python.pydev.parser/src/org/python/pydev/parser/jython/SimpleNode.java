/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.jython;

import java.util.List;

import org.eclipse.core.runtime.AssertionFailedException;
import org.python.pydev.parser.jython.ast.VisitorIF;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.structure.LowMemoryArrayList;

public class SimpleNode implements Node, ISimpleNode {

    private static final boolean DEBUG = false;

    /**
     * Begins at 1
     */
    public int beginLine;

    /**
     * Begins at 1
     */
    public int beginColumn;

    public SimpleNode parent;

    /**
     * each node may have a number of associated comments, although they are not in the visiting structure by default
     * (it appears separately from that in this attribute, so, when doing a traverse in some node, the comments
     * will NOT be added in the visitor by default, as in the grammar it is not associated).
     * 
     * If they are not of commentType, they should be strings, with things such as colons, parenthesis, etc, to help
     * in the process of pretty-printing the ast.
     * 
     * They are only initialized 'on-demand'
     */
    public List<Object> specialsBefore;
    public List<Object> specialsAfter;

    public SimpleNode() {
    }

    private int nodeId = -1;

    public int getId() {
        return nodeId;
    }

    public void setId(int id) {
        this.nodeId = id;
    }

    public Object getImage() {
        return null;
    }

    public void setImage(Object image) {
        //ignore the image at this point (only subclasses actually have it -- when that's necessary).
    }

    public SimpleNode createCopy() {
        throw new RuntimeException("Subclasses must reimplement");
    }

    public SimpleNode createCopy(boolean copyComments) {
        throw new RuntimeException("Subclasses must reimplement");
    }

    /**
     * @param special The 'special token' added (comment or some literal)
     * @param after defines if it was found before or after the token
     */
    public void addSpecial(Object special, boolean after) {
        if (special != null) {

            if (special instanceof Token) {
                special = ((Token) special).asSpecialStr();
            } else {
                if (special instanceof String) {
                    throw new AssertionFailedException("assertion failed: Special: " + special + " is not valid");
                }
            }

            if (after) {
                if (special instanceof commentType) {
                    getSpecialsAfter().add(special);
                } else {
                    int addAt = countAfter(special, false);
                    getSpecialsAfter().add(addAt, special);
                }

            } else {
                if (special instanceof commentType) {
                    commentType s = (commentType) special;
                    if (s.beginLine < this.beginLine) {
                        getSpecialsBefore().add(countBefore(special, true), special);
                    } else {
                        getSpecialsBefore().add(special);
                    }
                } else {
                    int addAt = countBefore(special, false);
                    getSpecialsBefore().add(addAt, special);
                }
            }
            if (DEBUG) {
                System.out.println("Adding:" + special + " after:" + after + " to:" + this + " class:"
                        + special.getClass());
            }
        }
    }

    public List<Object> getSpecialsBefore() {
        if (specialsBefore == null) {
            specialsBefore = new LowMemoryArrayList<Object>();
        }
        return specialsBefore;
    }

    public List<Object> getSpecialsAfter() {
        if (specialsAfter == null) {
            specialsAfter = new LowMemoryArrayList<Object>();
        }
        return specialsAfter;
    }

    private int countStrings() {
        return doCountStringsAndSpecialStrings(specialsAfter);
    }

    private int countStringsBefore() {
        return doCountStringsAndSpecialStrings(specialsBefore);
    }

    private int countAfter(Object special, boolean beforeRegularStrings) {
        int[] lineCol = getLineCol(special);
        if (lineCol == null) {
            return countStrings();
        }

        return getIndex(lineCol, specialsAfter, beforeRegularStrings);
    }

    private int countBefore(Object special, boolean beforeRegularStrings) {
        int[] lineCol = getLineCol(special);
        if (lineCol == null) {
            return countStringsBefore();
        }

        return getIndex(lineCol, specialsBefore, beforeRegularStrings);
    }

    /**
     * @return the probable index of a given object based on its line and column according to existing
     * objects in the passed list.
     */
    private int getIndex(int[] lineCol, List<Object> l, boolean beforeRegularStrings) {
        if (l == null) {
            return 0;
        }
        int i = 0;
        for (Object o : l) {
            int[] existing = getLineCol(o);
            if (existing == null) {
                //do nothing... (will be after it)
                if (beforeRegularStrings) {
                    return i;
                }

            } else if (existing[0] > lineCol[0]) {
                return i;

            } else if (existing[0] == lineCol[0]) {
                if (existing[1] > lineCol[1]) {
                    return i;
                }

            }
            i++;
        }
        return i;
    }

    /**
     * @param o the object we're interested in (only yields interesting results if it's a SpecialStr or a commentType)
     * @return the line and column where that object starts (or null if it cannot get that information)
     */
    private int[] getLineCol(Object o) {
        if (o instanceof ISpecialStr) {
            ISpecialStr s = (ISpecialStr) o;
            return new int[] { s.getBeginLine(), s.getBeginCol() };
        }
        if (o instanceof commentType) {
            commentType c = (commentType) o;
            return new int[] { c.beginLine, c.beginColumn };
        }
        return null;
    }

    /**
     * @param l the list we want to find strings on
     * @return the number of strings / special strings found.
     */
    private int doCountStringsAndSpecialStrings(List<Object> l) {
        if (l == null) {
            return 0;
        }
        int i = 0;
        for (Object o : l) {
            if (o instanceof String || o instanceof ISpecialStr) {
                i++;
            }
        }
        return i;
    }

    /* You can override these two methods in subclasses of SimpleNode to
       customize the way the node appears when the tree is dumped.  If
       your output uses more than one line you should override
       toString(String), otherwise overriding toString() is probably all
       you need to do. */

    @Override
    public String toString() {
        return super.toString() + " at line " + beginLine;
    }

    public String toString(String prefix) {
        return prefix + toString();
    }

    public Object accept(VisitorIF visitor) throws Exception {
        throw new ParseException("Unexpected node: " + this);
    }

    public void traverse(VisitorIF visitor) throws Exception {
        throw new ParseException("Unexpected node: " + this);
    }

    /* Override this method if you want to customize how the node dumps
       ut its children. */

    protected String dumpThis(String s) {
        return s;
    }

    protected String dumpThis(Object o) {
        return String.valueOf(o);
    }

    protected String dumpThis(Object[] s) {
        StringBuffer sb = new StringBuffer();
        if (s == null) {
            sb.append("null");
        } else {
            sb.append("[");
            for (int i = 0; i < s.length; i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(String.valueOf(s[i]));
            }
            sb.append("]");
        }

        return sb.toString();
    }

    protected String dumpThis(int i) {
        return Integer.toString(i);
    }

    protected String dumpThis(int i, String[] names) {
        if (i >= names.length || i < 0) {
            return "Unknown (out of bounds)";
        }
        return names[i];
    }

    protected String dumpThis(int[] arr, String[] names) {
        StringBuffer sb = new StringBuffer();
        if (arr == null) {
            sb.append("null");
        } else {
            sb.append("[");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0)
                    sb.append(", ");
                // XXX Verify bounds.
                sb.append(names[arr[i]]);
            }
            sb.append("]");
        }
        return sb.toString();
    }

    protected String dumpThis(boolean b) {
        return String.valueOf(b);
    }

    //We don't actually make any pickles in Pydev, so, let's leave that code commented out 

    //
    //
    //    public void pickle(DataOutputStream ostream) throws IOException {
    //        throw new IOException("Pickling not implemented");
    //    }
    //
    //    protected void pickleThis(String s, DataOutputStream ostream)
    //        throws IOException
    //    {
    //        if (s == null) {
    //            ostream.writeInt(-1);
    //        } else {
    //            ostream.writeInt(s.length());
    //            ostream.writeBytes(s);
    //        }
    //    }
    //
    //    protected void pickleThis(String[] s, DataOutputStream ostream)
    //        throws IOException
    //    {
    //        if (s == null) {
    //            ostream.writeInt(-1);
    //        } else {
    //            ostream.writeInt(s.length);
    //            for (int i = 0; i < s.length; i++) {
    //                pickleThis(s[i], ostream);
    //            }
    //        }
    //    }
    //
    //    protected void pickleThis(SimpleNode o, DataOutputStream ostream)
    //        throws IOException
    //    {
    //        if (o == null) {
    //            ostream.writeInt(-1);
    //        } else {
    //            o.pickle(ostream);
    //        }
    //    }
    //
    //    protected void pickleThis(SimpleNode[] s, DataOutputStream ostream)
    //        throws IOException
    //    {
    //        if (s == null) {
    //            ostream.writeInt(-1);
    //        } else {
    //            ostream.writeInt(s.length);
    //            for (int i = 0; i < s.length; i++) {
    //                pickleThis(s[i], ostream);
    //            }
    //        }
    //    }
    //
    //    protected void pickleThis(int i, DataOutputStream ostream)
    //        throws IOException
    //    {
    //        ostream.writeInt(i);
    //    }
    //
    //    protected void pickleThis(int[] arr, DataOutputStream ostream)
    //        throws IOException
    //    {
    //        if (arr == null) {
    //            ostream.writeInt(-1);
    //        } else {
    //            ostream.writeInt(arr.length);
    //            for (int i = 0; i < arr.length; i++) {
    //                ostream.writeInt(arr[i]);
    //            }
    //        }
    //    }
    //
    //    protected void pickleThis(boolean b, DataOutputStream ostream)
    //        throws IOException
    //    {
    //        ostream.writeBoolean(b);
    //    }
    //
    //    protected void pickleThis(Object n, DataOutputStream ostream)
    //        throws IOException
    //    {
    //        String s = n.toString();
    //        ostream.writeInt(s.length());
    //        ostream.writeBytes(s);
    //    }
}
