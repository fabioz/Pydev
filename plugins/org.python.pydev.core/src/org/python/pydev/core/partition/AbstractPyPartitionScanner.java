/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.partition;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.shared_core.partitioner.CustomRuleBasedPartitionScanner;

/**
 * This class should partition Python files in major partitions (code, strings, unicode, comments, backquotes)
 */
public class AbstractPyPartitionScanner extends CustomRuleBasedPartitionScanner implements IPythonPartitions {

    private final Token multiLineBytesToken1;
    private final Token multiLineBytesToken2;
    private final Token multiLineUnicodeToken1;
    private final Token multiLineUnicodeToken2;
    private final Token singleLineUnicodeToken1;
    private final Token singleLineUnicodeToken2;
    private final Token singleLineBytesToken1;
    private final Token singleLineBytesToken2;
    private Token singleLineBytesOrUnicodeToken1;
    private Token singleLineBytesOrUnicodeToken2;
    private Token multiLineBytesOrUnicodeToken1;
    private Token multiLineBytesOrUnicodeToken2;

    /**
     * Note: the formats supported for strings are:
     * 
     * br''
     * b''
     * ur''
     * u''
     * r''
     * ''
     * 
     * For matching only unicode we care about:
     * 
     * u''
     * ur''
     * 
     * For matching only bytes we care about:
     * 
     * b''
     * br''
     * 
     * For matching dependent on defaultIsUnicode we care about:
     * 
     * ''
     * r''
     *
     */
    public AbstractPyPartitionScanner() {
        IPredicateRule reprRule = new SingleLineRule("`", "`", new Token(IPythonPartitions.PY_BACKQUOTES));

        // Single Line
        singleLineUnicodeToken1 = new Token(IPythonPartitions.PY_SINGLELINE_UNICODE1);
        singleLineUnicodeToken2 = new Token(IPythonPartitions.PY_SINGLELINE_UNICODE2);
        singleLineBytesToken1 = new Token(IPythonPartitions.PY_SINGLELINE_BYTES1);
        singleLineBytesToken2 = new Token(IPythonPartitions.PY_SINGLELINE_BYTES2);
        singleLineBytesOrUnicodeToken1 = new Token(IPythonPartitions.PY_SINGLELINE_BYTES_OR_UNICODE1);
        singleLineBytesOrUnicodeToken2 = new Token(IPythonPartitions.PY_SINGLELINE_BYTES_OR_UNICODE2);

        //        boolean breaksOnEOL = true;
        //        boolean breaksOnEOF = false;
        //        boolean escapeContinuesLine = true;
        //        IPredicateRule singlelineUnicodeRule1 = new PatternRule("'", "'", singleLineUnicodeToken1, '\\', breaksOnEOL,
        //                breaksOnEOF, escapeContinuesLine);
        //        IPredicateRule singlelineUnicodeRule2 = new PatternRule("\"", "\"", singleLineUnicodeToken2, '\\', breaksOnEOL,
        //                breaksOnEOF, escapeContinuesLine);

        SingleLineRuleWithMultipleStarts singlelineBytes1 = new SingleLineRuleWithMultipleStarts(
                new String[] { "b'", "br'" }, "'", singleLineBytesToken1, '\\', true);
        SingleLineRuleWithMultipleStarts singlelineBytes2 = new SingleLineRuleWithMultipleStarts(
                new String[] { "b\"", "br\"" }, "\"", singleLineBytesToken2, '\\', true);

        SingleLineRuleWithMultipleStarts singlelineUnicode1 = new SingleLineRuleWithMultipleStarts(
                new String[] { "u\'", "ur\'" }, "'", singleLineUnicodeToken1, '\\', true);
        SingleLineRuleWithMultipleStarts singlelineUnicode2 = new SingleLineRuleWithMultipleStarts(
                new String[] { "u\"", "ur\"" }, "\"", singleLineUnicodeToken2, '\\', true);

        SingleLineRuleWithMultipleStarts singlelineBytesOrUnicode1 = new SingleLineRuleWithMultipleStarts(
                new String[] { "\'", "r\'" }, "'", singleLineBytesOrUnicodeToken1, '\\', true);
        SingleLineRuleWithMultipleStarts singlelineBytesOrUnicode2 = new SingleLineRuleWithMultipleStarts(
                new String[] { "\"", "r\"" }, "\"", singleLineBytesOrUnicodeToken2, '\\', true);

        // multiline
        multiLineBytesToken1 = new Token(IPythonPartitions.PY_MULTILINE_BYTES1);
        multiLineBytesToken2 = new Token(IPythonPartitions.PY_MULTILINE_BYTES2);
        multiLineUnicodeToken1 = new Token(IPythonPartitions.PY_MULTILINE_UNICODE1);
        multiLineUnicodeToken2 = new Token(IPythonPartitions.PY_MULTILINE_UNICODE2);
        multiLineBytesOrUnicodeToken1 = new Token(IPythonPartitions.PY_MULTILINE_BYTES_OR_UNICODE1);
        multiLineBytesOrUnicodeToken2 = new Token(IPythonPartitions.PY_MULTILINE_BYTES_OR_UNICODE2);
        // deal with ''' and """ strings

        //        breaksOnEOF = true;
        //If we don't add breaksOnEOF = true it won't properly recognize the rule while typing
        //in the following case:
        ///'''<new line>
        //text
        //''' <-- it's already lost at this point and the 'text' will not be in a multiline string partition.

        // IPredicateRule multilineBytes1 = new MultiLineRule("'''", "'''", multiLineBytesToken1, '\\', breaksOnEOF);
        // IPredicateRule multilineBytes2 = new MultiLineRule("\"\"\"", "\"\"\"", multiLineBytesToken2, '\\', breaksOnEOF);

        MultiLineRuleWithMultipleStarts multilineBytes1 = new MultiLineRuleWithMultipleStarts(
                new String[] { "b'''", "br'''" }, "'''", multiLineBytesToken1, '\\');
        MultiLineRuleWithMultipleStarts multilineBytes2 = new MultiLineRuleWithMultipleStarts(
                new String[] { "b\"\"\"", "br\"\"\"" }, "\"\"\"", multiLineBytesToken2, '\\');

        MultiLineRuleWithMultipleStarts multilineUnicode1 = new MultiLineRuleWithMultipleStarts(
                new String[] { "u'''", "ur'''" }, "'''", multiLineUnicodeToken1, '\\');
        MultiLineRuleWithMultipleStarts multilineUnicode2 = new MultiLineRuleWithMultipleStarts(
                new String[] { "u\"\"\"", "ur\"\"\"" }, "\"\"\"", multiLineUnicodeToken2, '\\');

        MultiLineRuleWithMultipleStarts multilineBytesOrUnicode1 = new MultiLineRuleWithMultipleStarts(
                new String[] { "'''", "r'''" }, "'''", multiLineBytesOrUnicodeToken1, '\\');
        MultiLineRuleWithMultipleStarts multilineBytesOrUnicode2 = new MultiLineRuleWithMultipleStarts(
                new String[] { "\"\"\"", "r\"\"\"" }, "\"\"\"", multiLineBytesOrUnicodeToken2, '\\');

        IPredicateRule commentRule = new EndOfLineRule("#", new Token(IPythonPartitions.PY_COMMENT));

        setPredicateRules(new IPredicateRule[] {
                reprRule,
                multilineBytes1,
                multilineBytes2,
                multilineUnicode1,
                multilineUnicode2,
                multilineBytesOrUnicode1,
                multilineBytesOrUnicode2,

                //Note: the order is important (so, single lines after multi lines)
                singlelineBytes1,
                singlelineBytes2,
                singlelineUnicode1,
                singlelineUnicode2,
                singlelineBytesOrUnicode1,
                singlelineBytesOrUnicode2,
                commentRule
        });
    }

}
