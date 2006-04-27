package org.python.pydev.parser.profile;

import java.io.File;

import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.parser.PyParserTestBase;

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
	 * @throws Exception
	 */
	public void testBigFileParsing() throws Exception {
        String loc = TestDependent.TEST_PYDEV_PARSER_PLUGIN_LOC+"/tests/pysrc/data_string.py";
        String s = REF.getFileContents(new File(loc));
        for (int i = 0; i < 5; i++) {
        	long curr = System.currentTimeMillis();
        	parseLegalDocStr(s);
        	
        	System.out.println(StringUtils.format("Took: %s secs", (System.currentTimeMillis()-curr)/1000.0));
		}
	}
}
