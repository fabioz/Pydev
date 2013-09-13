/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.parser.jython.Token;

import junit.framework.TestCase;

public class TokensIteratorTest extends TestCase {

    public static void main(String[] args) {
        try {
            TokensIteratorTest test = new TokensIteratorTest();
            test.setUp();
            test.testIterator();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(TokensIteratorTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testIterator() {
        Token firstIterationToken = new Token();
        ITokenManager tokenManager = createTokenManager();
        TokensIterator iterator = new TokensIterator(tokenManager, firstIterationToken, 50, false);
        assertTrue(iterator.hasNext());
        assertSame(firstIterationToken, iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals("if", iterator.next().image);

        assertTrue(iterator.hasNext());
        assertEquals("True", iterator.next().image);

        assertTrue(iterator.hasNext());
        assertEquals("<INDENT>", iterator.next().image);

        assertTrue(iterator.hasNext());
        assertEquals("pass", iterator.next().image);

        assertTrue(iterator.hasNext());
        assertEquals("<EOF>", iterator.next().image);

        assertFalse(iterator.hasNext());
    }

    public void testIterator2() {
        Token firstIterationToken = new Token();
        ITokenManager tokenManager = createTokenManager();
        TokensIterator iterator = new TokensIterator(tokenManager, firstIterationToken, 50, true);
        assertTrue(iterator.hasNext());
        assertSame(firstIterationToken, iterator.next());

        assertTrue(iterator.hasNext());
        assertEquals("if", iterator.next().image);

        assertTrue(iterator.hasNext());
        assertEquals("True", iterator.next().image);

        assertFalse(iterator.hasNext()); //break on indent
    }

    private ITokenManager createTokenManager() {

        final FastCharStream stream = new FastCharStream("if True:\n    pass".toCharArray());

        final Integer[] curr = new Integer[] { 0 };

        return new ITokenManager() {

            public void indenting(int i) {

            }

            public Token getNextToken() {
                final Token ret = new Token();

                if (curr[0] == 0) {
                    ret.kind = getIfId();
                    ret.image = "if";

                } else {
                    if (curr[0] == 1) {
                        ret.kind = 99; //any kind that's not below
                        ret.image = "True";

                    } else if (curr[0] == 2) {
                        ret.kind = getIndentId();
                        ret.image = "<INDENT>";

                    } else if (curr[0] == 3) {
                        ret.kind = 99; //any kind that's not below
                        ret.image = "pass";

                    } else if (curr[0] == 4) {
                        ret.kind = getEofId();
                        ret.image = "<EOF>";
                    } else {
                        throw new RuntimeException("Unexpected");
                    }
                }

                curr[0] += 1;
                return ret;
            }

            public FastCharStream getInputStream() {
                return stream;
            }

            public int getIndentId() {
                return 30;
            }

            public int getIfId() {
                return 31;
            }

            public int getForId() {
                return 32;
            }

            public int getEofId() {
                return 0; //this one is 'special' and must be 0!
            }

            public int getDefId() {
                return 34;
            }

            public int getDedentId() {
                return 35;
            }

            public int getClassId() {
                return 36;
            }

            public int getAtId() {
                return 37;
            }

            public int getWhileId() {
                return 38;
            }

            public int getTryId() {
                return 39;
            }

        };
    }

}
