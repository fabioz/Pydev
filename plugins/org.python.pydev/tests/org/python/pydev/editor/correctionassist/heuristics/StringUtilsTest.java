/**
 * 
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.editor.correctionassist.heuristics.StringUtils;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author André Berg
 */
public class StringUtilsTest extends TestCase {

    /**
     * @param name
     */
    public StringUtilsTest(String name) {
        super(name);
    }

    /**
     * Test method for {@link StringUtils#joinArray(CharSequence[], String)}.
     */
    public void testJoinArray() {
        
        String[] strings = {
            "test",
            "fest",
            "zest"
        };

        Integer[] integers = {
            Integer.valueOf(1),
            Integer.valueOf(2),
            Integer.valueOf(3)
        };

        Object[][] inputs = {
            strings,
            integers
        };

        String[] delimiters = {
            "\\n",
            "+"
        };

        String[] expectedResults = {
            "test\\nfest\\nzest",
            "1+2+3"
        };

        int i = 0;
        for (Object[] input : inputs) {
            String delim = delimiters[i];
            String expectedResult = expectedResults[i];
            String result = StringUtils.joinArray(input, delim);

            assertEquals(result, expectedResult);
            i++;
        }
    }
    
    /**
     * Test method for {@link StringUtils#joinArray(CharSequence[], String)}.
     * @throws Exception
     */
    public void testJoinArrayBogus() throws Exception {
        
        String[] inputs = {
            "nominal",
            null,
            "nominal"
        };

        String delimiter = "\\r\\n";

        try {
            String result = StringUtils.joinArray(null, delimiter);
            System.out.println("result = " + result);
            fail("The exception java.lang.IllegalArgumentException should have been thrown if 'objs' is null.");
        } catch (java.lang.IllegalArgumentException exception) {
            // The test succeeded by throwing the expected exception
        }
        
        try {
            String result = StringUtils.joinArray(inputs, null);
            System.out.println("result = " + result);
            fail("The exception java.lang.IllegalArgumentException should have been thrown if 'delimiter' is null.");
        } catch (java.lang.IllegalArgumentException exception) {
            // The test succeeded by throwing the expected exception
        }
    }

    /**
     * Test method for {@link StringUtils#joinIterable(Iterable, String)}.
     */
    public void testJoinIterable() {

        List<String> strings = new ArrayList<String>(3);
        strings.add("test");
        strings.add("fest");
        strings.add("zest");

        List<Integer> integers = new ArrayList<Integer>(3);
        integers.add(Integer.valueOf(1));
        integers.add(Integer.valueOf(2));
        integers.add(Integer.valueOf(3));

        Iterable<?>[] inputs = {
            strings,
            integers
        };

        String[] delimiters = {
            "\\n",
            "+"
        };

        String[] expectedResults = {
            "test\\nfest\\nzest",
            "1+2+3"
        };

        int i = 0;
        for (Iterable<?> input : inputs) {
            String delim = delimiters[i];
            String expectedResult = expectedResults[i];
            String result = StringUtils.joinIterable(input, delim);

            assertEquals(result, expectedResult);
            i++;
        }
    }

    /**
     * Test method for {@link StringUtils#joinIterable(Iterable, String)}
     * using bogus input.
     * 
     * @throws Exception
     */
    public void testJoinIterableBogus() throws Exception {

        List<String> inputs = new ArrayList<String>(3);
        inputs.add("nominal");
        inputs.add(null);
        inputs.add("nominal");

        String delimiter = "\\r\\n";

        try {
            String result = StringUtils.joinIterable(null, delimiter);
            System.out.println("result = " + result);
            fail("The exception java.lang.IllegalArgumentException should have been thrown if 'objs' is null.");
        } catch (java.lang.IllegalArgumentException exception) {
            // The test succeeded by throwing the expected exception
        }
        try {
            String result = StringUtils.joinIterable(inputs, null);
            System.out.println("result = " + result);
            fail("The exception java.lang.IllegalArgumentException should have been thrown if 'delimiter' is null.");
        } catch (java.lang.IllegalArgumentException exception) {
            // The test succeeded by throwing the expected exception
        }
    }

    /**
     * Test method for {@link StringUtils#repeatString(String, int)}.
     * Already includes some bogus value coverage.
     */
    public void testRepeatString() {
        
        String[] inputs = {"Sun", "Java", "*", " ", "-", "André", null};
        int[] timesList = {  -20,      3,   5,   4,   3,       2,    1};
        
        String[] expectedResults = {
            "",
            "JavaJavaJava",
            "*****",
            "    ",
            "---",
            "AndréAndré",
            "null"
        };
        
        int len = inputs.length;
        for (int i = 0; i < len; i++) {
            String input = inputs[i];
            int times = timesList[i];
            
            String expectedResult = expectedResults[i];
            String result = StringUtils.repeatString(input, times);
            
            assertEquals(expectedResult, result);
        }
    }
    
    /**
     * Test method for {@link StringUtils#lastIndexOf(String, String)}. 
     */
    public void testLastIndexOf() {
        
        String[] inputs = {
            "if ((method(\"test %s\" % name))):\n    print \"True\"",
            "\"\"\"test \\\"%s\"\"\" % \"\"\"fest\"\"\") # comment",
            "\"\"\"test \\\"%s\"\"\" % \"\"\"fest\"\"\") # comment # another comment?!",
            null,
            "André",
            "André",
            "for (Enumeration el=v.elements(); el.hasMoreElements(); ) {"
        };
        
        String[] regexes = {
            "\\%",
            "\\\\\"",
            "#",
            "\\B",
            null,
            "\u00e9",
            "\\;"
        };
        
        int[] expectedResults = { 22, -1, 40, -1, -1, 4, 54 };
        
        int len = inputs.length;
        for (int i = 0; i < len; i++) {
            String input = inputs[i];
            String regex = regexes[i];
            
            int expectedResult = expectedResults[i];
            int result = StringUtils.lastIndexOf(input, regex);
            
            assertEquals(expectedResult, result);
        }
    }
    
    /**
     * Test method for {@link StringUtils#indexOf(String, char, boolean)}. 
     * @throws Exception
     */
    public void testIndexOf() throws Exception {
        
        String[] inputs = {
            "if ((method(\"test %s\" % name))):\n    print \"True\"",
            "if ((method(\"test %s\" % name))):\n    print \"True\"",
            "\"\"\"test #\\\"%s\"\"\" % \"\"\"fest\"\"\") # comment # another comment?!",
            null,
            "André",
            "André",
            "for (Enumeration el=v.elements(); el.hasMoreElements(); ) {",
            "\"whitespace =     \"# the string has ended"
        };
        
        char[] chars = {
            '%',
            '"',
            '#',
            '\0',
            (char)-1,
            '\u00e9',
            ';',
            ' '
        };
        
        // results for ignoreInStringLiteral == true
        int[] expectedResults1 = { 22, 12, 31, -1, -1, 4, 32, 20 };
        
        // results for ignoreInStringLiteral == false
        int[] expectedResults2 = { 18, 12, 8, -1, -1, 4, 32, 11 };
        
        int len = inputs.length;
        for (int i = 0; i < len; i++) {
            String input = inputs[i];
            char character = chars[i];
            
            int expectedResult1 = expectedResults1[i];
            int result = StringUtils.indexOf(input, character, true);
            assertEquals(expectedResult1, result);
            
            result = StringUtils.indexOf(input, character, false);
            int expectedResult2 = expectedResults2[i];
            assertEquals(expectedResult2, result);
        }
    }

    /**
     * Test method for {@link StringUtils#findSubstring(String, char, boolean)}. 
     * @throws Exception
     */
    public void testFindSubstring() throws Exception {
        
        String[] inputs = {
            "if ((method(\"test %s\" % name))):\n    print \"True\"",
            "if ((method(\"test %s\" % name))):\n    print \"True\"",
            "\"\"\"test #\\\"%s\"\"\" % \"\"\"fest\"\"\") # comment # another comment?!",
            null,
            "André",
            "André",
            "for (Enumeration el=v.elements(); el.hasMoreElements(); ) {",
            "\"whitespace =     \"# the string has ended"
        };
        
        char[] chars = {
            '%',
            '"',
            '#',
            '\0',
            (char)-1,
            '\u00e9',
            ';',
            ' '
        };
        
        // results for ignoreInStringLiteral == true
        String[] expectedResults1 = {
            " name))):\n    print \"True\"",
            "test %s\" % name))):\n    print \"True\"",
            " comment # another comment?!",
            null,
            null,
            "",
            " el.hasMoreElements(); ) {",
            "the string has ended"
        };
        
        // results for ignoreInStringLiteral == false
        String[] expectedResults2 = {
            "s\" % name))):\n    print \"True\"",
            "test %s\" % name))):\n    print \"True\"",
            "\\\"%s\"\"\" % \"\"\"fest\"\"\") # comment # another comment?!",
            null,
            null,
            "",
            " el.hasMoreElements(); ) {",
            "=     \"# the string has ended"
        };
        
        int len = inputs.length;
        for (int i = 0; i < len; i++) {
            String input = inputs[i];
            char character = chars[i];
            
            String expectedResult1 = expectedResults1[i];
            String result = StringUtils.findSubstring(input, character, true);
            assertEquals(expectedResult1, result);
            
            result = StringUtils.findSubstring(input, character, false);
            String expectedResult2 = expectedResults2[i];
            assertEquals(expectedResult2, result);
        }
    }
    
    /**
     * Launch the test.
     * 
     * @param args
     *            the command line arguments
     * 
     * @generatedBy CodePro at 25.01.11 11:00
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            // Run all of the tests
            junit.textui.TestRunner.run(StringUtilsTest.class);
        } else {
            // Run only the named tests
            TestSuite suite = new TestSuite("Selected tests");
            for (int i = 0; i < args.length; i++) {
                TestCase test = new PercentToBraceConverterTest();
                test.setName(args[i]);
                suite.addTest(test);
            }
            junit.textui.TestRunner.run(suite);
        }
    }
}
