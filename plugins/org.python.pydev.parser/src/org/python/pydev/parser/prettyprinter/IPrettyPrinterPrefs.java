package org.python.pydev.parser.prettyprinter;

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
