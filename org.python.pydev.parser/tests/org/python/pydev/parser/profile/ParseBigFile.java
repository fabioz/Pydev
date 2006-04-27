package org.python.pydev.parser.profile;

import java.io.File;
import java.util.List;

import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.NameTokType;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

public class ParseBigFile extends PyParserTestBase {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(ParseBigFile.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}


	/**
	 * Initial times with 5 iterations:
	 * 
	 * Took: 1.625 secs
	 * Took: 0.797 secs
	 * Took: 0.828 secs
	 * Took: 0.766 secs
	 * Took: 0.765 secs
	 * 
	 * After using the FastCharStream:
	 * 
	 * Took: 0.453 secs
	 * Took: 0.14 secs
	 * Took: 0.14 secs
	 * Took: 0.141 secs
	 * Took: 0.14 secs
	 * 
	 * (impressive hum?)
	 * 
	 * @throws Exception
	 */
	public void testBigFileParsing() throws Exception {
        String loc = TestDependent.TEST_PYDEV_PARSER_PLUGIN_LOC+"/tests/pysrc/data_string.py";
        String s = REF.getFileContents(new File(loc));
        for (int i = 0; i < 5; i++) {
        	long curr = System.currentTimeMillis();
        	SimpleNode node = parseLegalDocStr(s);
        	
        	PyParser.USE_FAST_STREAM = true;
        	System.out.println(StringUtils.format("Took: %s secs", (System.currentTimeMillis()-curr)/1000.0));
        	SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(node);
        	
        	ASTEntry entry = visitor.getAsList(Str.class).get(0);
        	String s0 = ((Str)entry.node).s;
        	assertEquals(42, entry.node.beginLine);
        	assertEquals(8, entry.node.beginColumn);
        	assertTrue("Expecting big string. Received"+s0, s0.length() > 1 );
        	
        	List<ASTEntry> names = visitor.getAsList(Name.class);
			entry = names.get(0);
        	assertEquals(10, entry.node.beginLine);
        	assertEquals(5, entry.node.beginColumn);
        	
        	entry = names.get(1);
        	assertEquals(10, entry.node.beginLine);
        	assertEquals(12, entry.node.beginColumn);
        	
        	names = visitor.getAsList(NameTok.class);
        	entry = names.get(0);
        	assertEquals(8, entry.node.beginLine);
        	assertEquals(5, entry.node.beginColumn);
        	
        	entry = names.get(1);
        	assertEquals(9, entry.node.beginLine);
        	assertEquals(12, entry.node.beginColumn);
		}
	}
}
