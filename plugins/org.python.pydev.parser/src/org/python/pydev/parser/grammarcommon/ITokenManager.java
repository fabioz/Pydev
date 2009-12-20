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

    void indenting(int i);

    Token getNextToken();

    FastCharStream getInputStream();

}
