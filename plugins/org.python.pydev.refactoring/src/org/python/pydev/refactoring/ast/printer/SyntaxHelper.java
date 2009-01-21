/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.printer;

import org.python.pydev.core.structure.FastStringBuffer;


public class SyntaxHelper {
    private static final String AT_SYMBOL = "@";

    private static final String COMMA = ",";

    private static final String DICT_CLOSE = "}";

    private static final String DICT_OPEN = "{";

    private static final String DOT = ".";

    private static final String DOUBLEDOT = ":";

    private static final String ELLIPSIS = "...";

    private static final String empty = "";

    private static final String EQUAL = "=";

    private static final String LIST_CLOSE = "]";

    private static final String LIST_OPEN = "[";

    private static final String NUM_COMP = "j";

    private static final String NUM_LONG = "L";

    private static final String oneSpace = " ";

    private static final String OP_ADD = "+";

    private static final String OP_BITWISE_AND = "&";

    private static final String OP_BITWISE_OR = "|";

    private static final String OP_BITWISE_XOR = "^";

    private static final String OP_BOOL_AND = "and";

    private static final String OP_BOOL_OR = "or";

    private static final String OP_DIV = "/";

    private static final String OP_EQUAL = "=";

    private static final String OP_FLOORDIV = "//";

    private static final String OP_GT = ">";

    private static final String OP_IN = "in";

    private static final String OP_INVERT = "!";

    private static final String OP_IS = "is";

    private static final String OP_LSHIFT = "<<";

    private static final String OP_LT = "<";

    private static final String OP_MOD = "%";

    private static final String OP_NOT = "not";

    private static final String OP_RSHIFT = ">>";

    private static final String OP_SUB = "-";

    private static final String OP_UADD = "+";

    private static final String OP_UINVERT = "~";

    private static final String OP_UNOT = "not";

    private static final String OP_USUB = "-";

    private static final String PARENTHESE_CLOSE = ")";

    private static final String PARENTHESE_OPEN = "(";

    private static final String QUOTE_DOUBLE = "\"";

    private static final String QUOTE_SINGLE = "'";

    private static final String REPR_QUOTE = "`";

    private static final String STAR = "*";

    private AlignHelper alignHelper;

    private String defaultLineDelimiter;

    public SyntaxHelper(String newLineDelim) {
        defaultLineDelimiter = newLineDelim;
        alignHelper = new AlignHelper();
    }

    public void setAlignHelper(AlignHelper alignHelper) {
        this.alignHelper = alignHelper;
    }

    public String afterCall() {
        return PARENTHESE_CLOSE;
    }

    public String afterDict() {
        return DICT_CLOSE;
    }

    public String afterList() {
        return LIST_CLOSE;
    }

    public String afterMethodArguments() {
        return empty;
    }

    public String afterStatement() {
        return oneSpace;
    }

    public String afterTuple() {
        return PARENTHESE_CLOSE;
    }

    public String beforeCall() {
        return PARENTHESE_OPEN;
    }

    public String beforeDict() {
        return DICT_OPEN;
    }

    public String beforeList() {
        return LIST_OPEN;
    }

    public String beforeMethodArguments() {
        return oneSpace;
    }

    public String beforeStatement() {
        return oneSpace;
    }

    public String beforeTuple() {
        return PARENTHESE_OPEN;
    }

    public String getAlignment() {
        return alignHelper.getAlignment();
    }

    public void indent() {
        alignHelper.indent();
    }

    public void outdent() {
        alignHelper.outdent();
    }

    public String getAtSymbol() {
        return AT_SYMBOL;
    }

    public String getAttributeSeparator() {
        return DOT;
    }

    public String getComma() {
        return COMMA;
    }

    public String getDoubleDot() {
        return DOUBLEDOT;
    }

    public String getDoubleQuote() {
        return QUOTE_DOUBLE;
    }

    public String getEllipsis() {
        return ELLIPSIS;
    }

    public String getListSeparator() {
        return COMMA + oneSpace;
    }

    public String getNewLine() {
        return defaultLineDelimiter;
    }

    public String getNumComp() {
        return NUM_COMP;
    }

    public String getNumLong() {
        return NUM_LONG;
    }

    public String getOperatorAdd() {
        return OP_ADD;
    }

    public String getOperatorAssignment() {
        return EQUAL;
    }

    public String getOperatorBitAnd() {
        return OP_BITWISE_AND;
    }

    public String getOperatorBitOr() {
        return OP_BITWISE_OR;
    }

    public String getOperatorBitXor() {
        return OP_BITWISE_XOR;
    }

    public String getOperatorBoolAnd() {
        return OP_BOOL_AND;
    }

    public String getOperatorBoolOr() {
        return OP_BOOL_OR;
    }

    public String getOperatorDestination() {
        return OP_RSHIFT;
    }

    public String getOperatorDiv() {
        return OP_DIV;
    }

    public String getOperatorEqual() {
        return OP_EQUAL + OP_EQUAL;
    }

    public String getOperatorFloorDiv() {
        return OP_FLOORDIV;
    }

    public String getOperatorGt() {
        return OP_GT;
    }

    public String getOperatorGtEqual() {
        return OP_GT + OP_EQUAL;
    }

    public String getOperatorIn() {
        return OP_IN;
    }

    public String getOperatorInvert() {
        return OP_UINVERT;
    }

    public String getOperatorIs() {
        return OP_IS;
    }

    public String getOperatorIsNot() {
        return OP_IS + oneSpace + OP_NOT;
    }

    public String getOperatorLt() {
        return OP_LT;
    }

    public String getOperatorLtEqual() {
        return OP_LT + OP_EQUAL;
    }

    public String getOperatorMod() {
        return OP_MOD;
    }

    public String getOperatorMult() {
        return STAR;
    }

    public String getOperatorNot() {
        return OP_UNOT;
    }

    public String getOperatorNotEqual() {
        return OP_INVERT + OP_EQUAL;
    }

    public String getOperatorNotIn() {
        return OP_NOT + oneSpace + OP_IN;
    }

    public String getOperatorPow() {
        return STAR + STAR;
    }

    public String getOperatorShiftLeft() {
        return OP_LSHIFT;
    }

    public String getOperatorShiftRight() {
        return OP_RSHIFT;
    }

    public String getOperatorSub() {
        return OP_SUB;
    }

    public String getOperatorUAdd() {
        return OP_UADD;
    }

    public String getOperatorUSub() {
        return OP_USUB;
    }

    public String getReprQuote() {
        return REPR_QUOTE;
    }

    public String getSingleQuote() {
        return QUOTE_SINGLE;
    }

    public String getSpace() {
        return oneSpace;
    }

    public String getStar(int i) {
        FastStringBuffer buf = new FastStringBuffer(i+1);
        for (int j = 0; j < i; j++)
            buf.append(STAR);

        return buf.toString();
    }

}
