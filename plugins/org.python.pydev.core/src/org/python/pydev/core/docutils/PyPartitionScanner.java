/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: July 10, 2003
 */

package org.python.pydev.core.docutils;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.PatternRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.log.Log;

/**
 * Rule-based partition scanner
 * 
 * Simple, fast parsing of the document into partitions.<p>
 * This is like a rough 1st pass at parsing. We only parse
 * out for comments, single-line strings, and multiline strings<p>
 * The results are parsed again inside {@link org.python.pydev.editor.PyEditConfiguration#getPresentationReconciler}
 * and colored there.<p>
 * 
 * "An IPartitionTokenScanner can also start in the middle of a partition,
 * if it knows the type of the partition."
 */
public class PyPartitionScanner extends RuleBasedPartitionScanner implements IPythonPartitions {
    public PyPartitionScanner() {
        super();
        List<IPredicateRule> rules = new ArrayList<IPredicateRule>();

        addMultilineStringRule(rules);
        addSinglelineStringRule(rules);
        addReprRule(rules);
        addCommentRule(rules);

        setPredicateRules(rules.toArray(new IPredicateRule[0]));
    }

    private void addReprRule(List<IPredicateRule> rules) {
        rules.add(new SingleLineRule("`", "`", new Token(IPythonPartitions.PY_BACKQUOTES)));
    }

    private void addSinglelineStringRule(List<IPredicateRule> rules) {
        //        IToken singleLineString = new Token(PY_SINGLELINE_STRING);
        //        rules.add(new SingleLineRule("\"", "\"", singleLineString, '\\'));
        //        rules.add(new SingleLineRule("'", "'", singleLineString, '\\')); -- changed to the construct below because we need to continue on escape

        IToken singleLineString1 = new Token(IPythonPartitions.PY_SINGLELINE_STRING1);
        IToken singleLineString2 = new Token(IPythonPartitions.PY_SINGLELINE_STRING2);
        // deal with "" and '' strings
        boolean breaksOnEOL = true;
        boolean breaksOnEOF = false;
        boolean escapeContinuesLine = true;
        rules.add(new PatternRule("'", "'", singleLineString1, '\\', breaksOnEOL, breaksOnEOF, escapeContinuesLine));
        rules.add(new PatternRule("\"", "\"", singleLineString2, '\\', breaksOnEOL, breaksOnEOF, escapeContinuesLine));
    }

    private void addMultilineStringRule(List<IPredicateRule> rules) {
        IToken multiLineString1 = new Token(IPythonPartitions.PY_MULTILINE_STRING1);
        IToken multiLineString2 = new Token(IPythonPartitions.PY_MULTILINE_STRING2);
        // deal with ''' and """ strings

        boolean breaksOnEOF = true;
        //If we don't add breaksOnEOF = true it won't properly recognize the rule while typing
        //in the following case:
        ///'''<new line>
        //text
        //''' <-- it's already lost at this point and the 'text' will not be in a multiline string partition.

        rules.add(new MultiLineRule("'''", "'''", multiLineString1, '\\', breaksOnEOF));
        rules.add(new MultiLineRule("\"\"\"", "\"\"\"", multiLineString2, '\\', breaksOnEOF));

        //there is a bug in this construct: When parsing a simple document such as:
        //
        //"""ttt"""
        //print 'a'
        //
        //if lines are feed after 'ttt', it ends up considering the whole document as a multiline string.
        //the bug is reported at: http://sourceforge.net/tracker/index.php?func=detail&aid=1402165&group_id=85796&atid=577329
        //
        //some regards on the bug:
        //- it does not happen if the multiline has ''' instead of """
        //- also, if we first add the """ rule and after the ''' rule, the bug happens with ''' and not """
        //- if the user later changes the first line of that multiline or a line above it, it ends up parsing correctly again
        //- if we let just one of the constructs, no problem happens
        //
        //I also tried creating a new token for it, but it had problems too (not the same ones, but had other problems).
    }

    private void addCommentRule(List<IPredicateRule> rules) {
        IToken comment = new Token(IPythonPartitions.PY_COMMENT);
        rules.add(new EndOfLineRule("#", comment));
    }

    /**
     * @return all types recognized by this scanner (used by doc partitioner)
     */
    static public String[] getTypes() {
        return IPythonPartitions.types;
    }

    /**
     * Checks if the partitioner is correctly set in the document.
     * @return the partitioner that is set in the document
     */
    public static IDocumentPartitioner checkPartitionScanner(IDocument document) {
        if (document == null) {
            return null;
        }

        IDocumentExtension3 docExtension = (IDocumentExtension3) document;
        IDocumentPartitioner partitioner = docExtension.getDocumentPartitioner(IPythonPartitions.PYTHON_PARTITION_TYPE);
        if (partitioner == null) {
            addPartitionScanner(document);
            //get it again for the next check
            partitioner = docExtension.getDocumentPartitioner(IPythonPartitions.PYTHON_PARTITION_TYPE);
        }
        if (!(partitioner instanceof PyPartitioner)) {
            Log.log("Partitioner should be subclass of PyPartitioner. It is " + partitioner.getClass());
        }
        return partitioner;
    }

    /**
     * @see http://help.eclipse.org/help31/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/editors_documents.htm
     * @see http://jroller.com/page/bobfoster -  Saturday July 16, 2005
     * @param document the document where we want to add the partitioner
     * @return the added document partitioner (or null)
     */
    public static IDocumentPartitioner addPartitionScanner(IDocument document) {
        if (document != null) {
            IDocumentExtension3 docExtension = (IDocumentExtension3) document;
            IDocumentPartitioner curr = docExtension.getDocumentPartitioner(IPythonPartitions.PYTHON_PARTITION_TYPE);

            if (curr == null) {
                //set the new one
                IDocumentPartitioner partitioner = createPyPartitioner();
                partitioner.connect(document);
                docExtension.setDocumentPartitioner(IPythonPartitions.PYTHON_PARTITION_TYPE, partitioner);
                return partitioner;
            } else {
                return curr;
            }
        }
        return null;
    }

    public static PyPartitioner createPyPartitioner() {
        return new PyPartitioner(new PyPartitionScanner(), getTypes());
    }

}
