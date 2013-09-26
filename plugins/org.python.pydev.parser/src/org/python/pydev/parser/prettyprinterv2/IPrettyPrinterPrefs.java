/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

import org.python.pydev.core.IGrammarVersionProvider;

public interface IPrettyPrinterPrefs extends IGrammarVersionProvider {

    void setSpacesAfterComma(int i);

    String getReplacement(String string);

    void setReplacement(String original, String replacement);

    String getNewLine();

    String getIndent();

    int getLinesAfterMethod();

    int getLinesAfterClass();

    String getSpacesBeforeComment();

    String getOperatorMapping(int op);

    String getUnaryopOperatorMapping(int op);

    String getBoolOperatorMapping(int op);

    String getAssignPunctuation();

    String getCmpOp(int op);

    String getAugOperatorMapping(int op);

    void setLinesAfterMethod(int i);

    int getLinesAfterSuite();

}
