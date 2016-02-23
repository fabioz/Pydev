/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.string;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitionerExtension2;

public abstract class BaseParsingUtils {

    protected boolean throwSyntaxError;

    public BaseParsingUtils(boolean throwSyntaxError) {
        this.throwSyntaxError = throwSyntaxError;
    }

    /**
     * Class that handles char[]
     *
     * @author Fabio
     */
    private static final class FixedLenCharArrayParsingUtils extends BaseParsingUtils {
        private final char[] cs;
        private final int len;

        public FixedLenCharArrayParsingUtils(char[] cs, boolean throwSyntaxError, int len) {
            super(throwSyntaxError);
            this.cs = cs;
            this.len = len;
        }

        @Override
        public int len() {
            return len;
        }

        @Override
        public char charAt(int i) {
            return cs[i];
        }
    }

    /**
     * Class that handles FastStringBuffer
     *
     * @author Fabio
     */
    private static final class FixedLenFastStringBufferParsingUtils extends BaseParsingUtils {
        private final FastStringBuffer cs;
        private final int len;

        public FixedLenFastStringBufferParsingUtils(FastStringBuffer cs, boolean throwSyntaxError, int len) {
            super(throwSyntaxError);
            this.cs = cs;
            this.len = len;
        }

        @Override
        public int len() {
            return len;
        }

        @Override
        public char charAt(int i) {
            return cs.charAt(i);
        }
    }

    /**
     * Class that handles StringBuffer
     *
     * @author Fabio
     */
    private static final class FixedLenStringBufferParsingUtils extends BaseParsingUtils {
        private final StringBuffer cs;
        private final int len;

        public FixedLenStringBufferParsingUtils(StringBuffer cs, boolean throwSyntaxError, int len) {
            super(throwSyntaxError);
            this.cs = cs;
            this.len = len;
        }

        @Override
        public int len() {
            return len;
        }

        @Override
        public char charAt(int i) {
            return cs.charAt(i);
        }
    }

    /**
     * Class that handles String
     *
     * @author Fabio
     */
    private static final class FixedLenStringParsingUtils extends BaseParsingUtils {
        private final String cs;
        private final int len;

        public FixedLenStringParsingUtils(String cs, boolean throwSyntaxError, int len) {
            super(throwSyntaxError);
            this.cs = cs;
            this.len = len;
        }

        @Override
        public int len() {
            return len;
        }

        @Override
        public char charAt(int i) {
            return cs.charAt(i);
        }
    }

    /**
     * Class that handles String
     *
     * @author Fabio
     */
    private static final class FixedLenIDocumentParsingUtils extends BaseParsingUtils {
        private final IDocument cs;
        private final int len;

        public FixedLenIDocumentParsingUtils(IDocument cs, boolean throwSyntaxError, int len) {
            super(throwSyntaxError);
            this.cs = cs;
            this.len = len;
        }

        @Override
        public int len() {
            return len;
        }

        @Override
        public char charAt(int i) {
            try {
                return cs.getChar(i);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Class that handles FastStringBuffer
     *
     * @author Fabio
     */
    private static final class FastStringBufferParsingUtils extends BaseParsingUtils {
        private final FastStringBuffer cs;

        public FastStringBufferParsingUtils(FastStringBuffer cs, boolean throwSyntaxError) {
            super(throwSyntaxError);
            this.cs = cs;
        }

        @Override
        public int len() {
            return cs.length();
        }

        @Override
        public char charAt(int i) {
            return cs.charAt(i);
        }
    }

    /**
     * Class that handles StringBuffer
     *
     * @author Fabio
     */
    private static final class StringBufferParsingUtils extends BaseParsingUtils {
        private final StringBuffer cs;

        public StringBufferParsingUtils(StringBuffer cs, boolean throwSyntaxError) {
            super(throwSyntaxError);
            this.cs = cs;
        }

        @Override
        public int len() {
            return cs.length();
        }

        @Override
        public char charAt(int i) {
            return cs.charAt(i);
        }
    }

    /**
     * Class that handles String
     *
     * @author Fabio
     */
    private static final class IDocumentParsingUtils extends BaseParsingUtils {
        private final IDocument cs;

        public IDocumentParsingUtils(IDocument cs, boolean throwSyntaxError) {
            super(throwSyntaxError);
            this.cs = cs;
        }

        @Override
        public int len() {
            return cs.getLength();
        }

        @Override
        public char charAt(int i) {
            try {
                return cs.getChar(i);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Factory method to create it (and by default doesn't throw any errors).
     */
    public static BaseParsingUtils create(Object cs) {
        return create(cs, false);
    }

    /**
     * Factory method to create it. Object len may not be changed afterwards.
     */
    public static BaseParsingUtils create(Object cs, boolean throwSyntaxError, int len) {
        if (cs instanceof char[]) {
            char[] cs2 = (char[]) cs;
            return new FixedLenCharArrayParsingUtils(cs2, throwSyntaxError, len);
        }
        if (cs instanceof FastStringBuffer) {
            FastStringBuffer cs2 = (FastStringBuffer) cs;
            return new FixedLenFastStringBufferParsingUtils(cs2, throwSyntaxError, len);
        }
        if (cs instanceof StringBuffer) {
            StringBuffer cs2 = (StringBuffer) cs;
            return new FixedLenStringBufferParsingUtils(cs2, throwSyntaxError, len);
        }
        if (cs instanceof String) {
            String cs2 = (String) cs;
            return new FixedLenStringParsingUtils(cs2, throwSyntaxError, len);
        }
        if (cs instanceof IDocument) {
            IDocument cs2 = (IDocument) cs;
            return new FixedLenIDocumentParsingUtils(cs2, throwSyntaxError, len);
        }
        throw new RuntimeException("Don't know how to create instance for: " + cs.getClass());
    }

    /**
     * Factory method to create it.
     */
    public static BaseParsingUtils create(Object cs, boolean throwSyntaxError) {
        if (cs instanceof char[]) {
            char[] cs2 = (char[]) cs;
            return new FixedLenCharArrayParsingUtils(cs2, throwSyntaxError, cs2.length);
        }
        if (cs instanceof FastStringBuffer) {
            FastStringBuffer cs2 = (FastStringBuffer) cs;
            return new FastStringBufferParsingUtils(cs2, throwSyntaxError);
        }
        if (cs instanceof StringBuffer) {
            StringBuffer cs2 = (StringBuffer) cs;
            return new StringBufferParsingUtils(cs2, throwSyntaxError);
        }
        if (cs instanceof String) {
            String cs2 = (String) cs;
            return new FixedLenStringParsingUtils(cs2, throwSyntaxError, cs2.length());
        }
        if (cs instanceof IDocument) {
            IDocument cs2 = (IDocument) cs;
            return new IDocumentParsingUtils(cs2, throwSyntaxError);
        }
        throw new RuntimeException("Don't know how to create instance for: " + cs.getClass());
    }

    //Abstract interfaces -------------------------------------------------------------

    /**
     * @return the char at a given position of the object
     */
    public abstract char charAt(int i);

    /**
     * @return the length of the contained object
     */
    public abstract int len();

    public static String getContentType(IDocument document, int i) {
        IDocumentPartitionerExtension2 extension = (IDocumentPartitionerExtension2) document.getDocumentPartitioner();
        return extension.getContentType(i, true);
    }

    /**
     * Finds the next char that matches the passed char. If not found, returns -1.
     */
    public int findNextChar(int offset, char findChar) {
        char c;
        int l = len();

        for (int i = offset; i < l; i++) {
            c = charAt(i);
            if (c == findChar) {
                return i;
            }
        }
        return -1;
    }
}
