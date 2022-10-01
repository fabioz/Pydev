/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.parser.jython.Token;

public interface ITokenManager {

    int getIndentId();

    int getDedentId();

    int getIfId();

    int getWhileId();

    int getForId();

    int getTryId();

    int getDefId();

    int getClassId();

    int getAtId();

    int getEofId();

    int getNewline1Id();

    int getNewlineId();

    int getNewline2Id();

    int getCrlf1Id();

    void indenting(int i);

    Token getNextToken();

    FastCharStream getInputStream();

}
