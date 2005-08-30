/*
 * Author: atotic
 * Created: July 10, 2003
 * License: Common Public License v1.0
 */

package org.python.pydev.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

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
public class PyPartitionScanner extends RuleBasedPartitionScanner {
	public final static String PY_COMMENT = "__python_comment";
	public final static String PY_SINGLELINE_STRING = "__python_singleline_string";
	public final static String PY_MULTILINE_STRING = "__python_multiline_string";
	public final static String PY_BACKQUOTES = "__python_backquotes";
    
    public final static String[] types = {PY_COMMENT, PY_SINGLELINE_STRING, PY_MULTILINE_STRING, PY_BACKQUOTES};
    public static final String PYTHON_PARTITION_TYPE = "__PYTHON_PARTITION_TYPE";
    
	public PyPartitionScanner() {
		super();
		List rules = new ArrayList();

		addCommentRule(rules);
		addMultilineStringRule(rules);
		addSinglelineStringRule(rules);
		addReprRule(rules);
		
		IPredicateRule[] result = new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}

	private void addReprRule(List rules) {
		rules.add(new SingleLineRule("`", "`", new Token(PY_BACKQUOTES)));
	}

	private void addSinglelineStringRule(List rules) {
		IToken singleLineString = new Token(PY_SINGLELINE_STRING);
		// deal with "" and '' strings
		rules.add(new SingleLineRule("\"", "\"", singleLineString, '\\'));
		rules.add(new SingleLineRule("'", "'", singleLineString, '\\'));
	}

	private void addMultilineStringRule(List rules) {
		IToken multiLineString = new Token(PY_MULTILINE_STRING);
		// deal with ''' and """ strings
		rules.add(new MultiLineRule("'''", "'''", multiLineString, '\\'));
		rules.add(new MultiLineRule("\"\"\"", "\"\"\"", multiLineString,'\\'));
	}

	private void addCommentRule(List rules) {
		IToken comment = new Token(PY_COMMENT);
		rules.add(new EndOfLineRule("#", comment));
	}
	
	/**
	 * @return all types recognized by this scanner (used by doc partitioner)
	 */
	static public String[] getTypes() {
		return types;
	}

	public static void checkPartitionScanner(IDocument document) {
	    if(document == null){
	        return;
        }
        
        IDocumentExtension3 docExtension= (IDocumentExtension3) document;
	    IDocumentPartitioner partitioner = docExtension.getDocumentPartitioner(PYTHON_PARTITION_TYPE);
	    if (partitioner == null){
            addPartitionScanner(document);
            //get it again for the next check
            partitioner = docExtension.getDocumentPartitioner(PYTHON_PARTITION_TYPE);
        }
	    if (!(partitioner instanceof PyPartitioner)){
	        throw new RuntimeException("Partitioner should be subclass of PyPartitioner. It is "+partitioner.getClass());
	    }
    }
    
    /**
     * @see http://help.eclipse.org/help31/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/editors_documents.htm
     * @see http://jroller.com/page/bobfoster -  Saturday July 16, 2005
     * @param element
     * @param document
     */
    public static void addPartitionScanner(IDocument document) {
        if (document != null) {
            IDocumentExtension3 docExtension= (IDocumentExtension3) document;
            if(docExtension.getDocumentPartitioner(PYTHON_PARTITION_TYPE) == null){
                //set the new one
                FastPartitioner partitioner = new PyPartitioner(new PyPartitionScanner(), getTypes());
                partitioner.connect(document);
                docExtension.setDocumentPartitioner(PYTHON_PARTITION_TYPE,partitioner);
            }
        }
    }
}
