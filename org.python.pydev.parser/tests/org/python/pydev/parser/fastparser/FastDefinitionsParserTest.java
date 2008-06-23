package org.python.pydev.parser.fastparser;

import junit.framework.TestCase;

public class FastDefinitionsParserTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    
    public static void main(String[] args) {
        try {
            FastDefinitionsParserTest test = new FastDefinitionsParserTest();
            test.setUp();
            test.testDefinitionsParser();
            test.tearDown();
            junit.textui.TestRunner.run(FastDefinitionsParserTest.class);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    
    public void testDefinitionsParser() {
        FastDefinitionsParser.parse("class Bar:pass");
    }

}
