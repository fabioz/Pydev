/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.printer;

public class SyntaxHelper {
    public static final String AT_SYMBOL = "@";
    public static final String COMMA = ",";
    public static final String DICT_CLOSE = "}";
    public static final String DICT_OPEN = "{";
    public static final String DOT = ".";
    public static final String DOUBLEDOT = ":";
    public static final String ELLIPSIS = "...";
    public static final String EMPTY = "";
    public static final String EQUAL = "=";
    public static final String LIST_CLOSE = "]";
    public static final String LIST_OPEN = "[";
    public static final String NUM_COMP = "j";
    public static final String NUM_LONG = "L";
    public static final String ONE_SPACE = " ";
    public static final String LIST_SEPERATOR = COMMA + ONE_SPACE;
    public static final String OP_ADD = "+";
    public static final String OP_BITWISE_AND = "&";
    public static final String OP_BITWISE_OR = "|";
    public static final String OP_BITWISE_XOR = "^";
    public static final String OP_BOOL_AND = "and";
    public static final String OP_BOOL_OR = "or";
    public static final String OP_DIV = "/";
    public static final String OP_EQUAL = "=";
    public static final String OP_FLOORDIV = "//";
    public static final String OP_GT = ">";
    public static final String OP_IN = "in";
    public static final String OP_INVERT = "!";
    public static final String OP_LSHIFT = "<<";
    public static final String OP_LT = "<";
    public static final String STAR = "*";
    public static final String OP_GT_EQUAL = OP_GT + OP_EQUAL;
    public static final String OP_LT_EQUAL = OP_LT + OP_EQUAL;
    public static final String OP_EQUAL_VALUE = OP_EQUAL + OP_EQUAL;
    public static final String OP_NOT_EQUAL = OP_INVERT + OP_EQUAL;
    public static final String OP_MOD = "%";
    public static final String OP_NOT = "not";
    public static final String OP_IS = "is";
    public static final String OP_IS_NOT = OP_IS + ONE_SPACE + OP_NOT;
    public static final String OP_NOT_IN = OP_NOT + ONE_SPACE + OP_IN;
    public static final String OP_RSHIFT = ">>";
    public static final String OP_SUB = "-";
    public static final String OP_UADD = "+";
    public static final String OP_UINVERT = "~";
    public static final String OP_UNOT = "not";
    public static final String OP_USUB = "-";
    public static final String OP_POWER = STAR + STAR;
    public static final String PARENTHESE_CLOSE = ")";
    public static final String PARENTHESE_OPEN = "(";
    public static final String QUOTE_DOUBLE = "\"";
    public static final String QUOTE_SINGLE = "'";
    public static final String REPR_QUOTE = "`";
    public AlignHelper alignHelper;
    public String defaultLineDelimiter;

    public SyntaxHelper(String newLineDelim) {
        defaultLineDelimiter = newLineDelim;
        alignHelper = new AlignHelper();
    }

    public void setAlignHelper(AlignHelper alignHelper) {
        this.alignHelper = alignHelper;
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

    public String getStar(int i) {
        StringBuffer buf = new StringBuffer();
        for(int j = 0; j < i; j++){
            buf.append(STAR);
        }

        return buf.toString();
    }

}
