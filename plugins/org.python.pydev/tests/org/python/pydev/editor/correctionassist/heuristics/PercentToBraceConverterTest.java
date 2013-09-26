/******************************************************************************
* Copyright (C) 2011  André Berg
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     André Berg <andre.bergmedia@googlemail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.editor.correctionassist.heuristics;

import org.python.pydev.editor.correctionassist.heuristics.PercentToBraceConverter;

import junit.framework.*;

/**
 * The class <code>PercentToBraceConverterTest</code> contains tests for the
 * class <code>{@link PercentToBraceConverter}</code>.
 * 
 * @author André Berg
 */
public class PercentToBraceConverterTest extends TestCase {

    /**
     * Run the PercentToBraceConverter(String) constructor test.
     */
    public void testPercentToBraceConverterCreation() {

        String formatStringToConvert = "";
        PercentToBraceConverter result = new PercentToBraceConverter(formatStringToConvert);

        // add additional test code here
        assertNotNull(result);
        assertEquals("<PercentToBraceConverter@0x1 | source= match= argIndex=0 head= tail=>", result.toString());
        assertEquals(0, result.getLength());
        assertEquals("", result.convert());
        assertEquals(false, result.isSkippingFormatCallReplacement());
    }
    
    /**
     * Run the PercentToBraceConverter(String) constructor test.
     * @throws Exception
     */
    public void testPercentToBraceConverterBogusCreation() throws Exception {
        try {
            String formatStringToConvert = null;
            PercentToBraceConverter fixture = new PercentToBraceConverter(formatStringToConvert);
            System.out.println(fixture);

            // add additional test code here
            fail("The exception java.lang.IllegalArgumentException should have been thrown.");
        } catch (java.lang.IllegalArgumentException exception) {
            // The test succeeded by throwing the expected exception
        }
    }

    /**
     * Run the String convert() method test with an empty string.
     * @post should result in empty string.
     */
    public void testConvertEmptyString() throws Exception {

        PercentToBraceConverter fixture = new PercentToBraceConverter("");
        String result = fixture.convert();

        assertEquals("", result);
    }

    /**
     * Run the String convert() method test with non-format string.
     * @post should result in the input string unchanged.
     */
    public void testConvertNonFormatString() {

        String[] inputs = {
            "some string that should'nt match",
            "x = r'''variable assignment with a double percent sign: %%'''"
        };

        int i = 0;
        for (String input : inputs) {

            PercentToBraceConverter fixture = new PercentToBraceConverter(input);
            String result = fixture.convert();

            assertEquals(input, result);
            i++;
        }
    }

    /**
     * Run the String convert() method test. Post-Condition: converted results
     * equal to expected results.
     */
    public void testConvertFloatFormatStrings() {

        String[] inputs = {
            "'%0.2f' % (2.3334)",
            "x = r'''%#0.3LF''' % (2.7272777)"
        };

        String[] expectedResults = {
            "'{0:>0.2f}'.format(2.3334)",
            "x = r'''{0:>0.3F}'''.format(2.7272777)"
        };

        int i = 0;
        for (String input : inputs) {
            String expectedResult = expectedResults[i];

            PercentToBraceConverter fixture = new PercentToBraceConverter(input);
            String result = fixture.convert();

            assertEquals(expectedResult, result);
            i++;
        }
    }

    /**
     * Run the String convert() method test. Post-Condition: converted results
     * equal to expected results.
     */
    public void testConvertIntFormatStrings() {

        String[] inputs = {
            "'my int: %.2i' % 12222334",
            "'my int: %2.i' % 12222334",
            "'my int: %2.0i' % 12222334",
            "'my oct: %#o' % 9",
            "'my oct: %#+20o' % 9",
            "'my oct: %#-20o' % 9",
            "'my oct: %# 20o' % 9"
        };
        String[] expectedResults = {
            "'my int: {0:.2d}'.format(12222334)",
            "'my int: {0:>2d}'.format(12222334)",
            "'my int: {0:>2.0d}'.format(12222334)",
            "'my oct: {0:o}'.format(9)",
            "'my oct: {0:>+20o}'.format(9)",
            "'my oct: {0:<20o}'.format(9)",
            "'my oct: {0:> 20o}'.format(9)"
        };

        int i = 0;
        for (String input : inputs) {
            String expectedResult = expectedResults[i];

            PercentToBraceConverter fixture = new PercentToBraceConverter(input);
            String result = fixture.convert();

            assertEquals(expectedResult, result);
            i++;
        }
    }

    /**
     * Run the String convert() method test. Post-Condition: converted results
     * equal to expected results.
     */
    public void testConvertStringFormatStrings() {

        String[] inputs = {
            "'%s%s' % (key, transform)",
        };
        String[] expectedResults = {
            "'{0!s}{1!s}'.format(key, transform)",
        };

        int i = 0;
        for (String input : inputs) {
            String expectedResult = expectedResults[i];

            PercentToBraceConverter fixture = new PercentToBraceConverter(input);
            String result = fixture.convert();

            assertEquals(expectedResult, result);
            i++;
        }
    }
    
    /**
     * Run the String convert() method test. Post-Condition: converted results
     * equal to expected results.
     */
    public void testConvertKeyMappingFormatStrings() {

        String[] inputs = {
            "r'''%(test)s''' % ({'test': \\\"fest\\\"})",
            "r'''%(test)s%(foo)s''' % ({'test': \\\"fest\\\", 'foo': \\\"bar\\\"})"
        };
        String[] expectedResults = {
            "r'''{[test]!s}'''.format({'test': \\\"fest\\\"})",
            "r'''{[test]!s}{[foo]!s}'''.format({'test': \\\"fest\\\", 'foo': \\\"bar\\\"})"
        };

        int i = 0;
        for (String input : inputs) {
            String expectedResult = expectedResults[i];

            PercentToBraceConverter fixture = new PercentToBraceConverter(input);
            String result = fixture.convert();

            assertEquals(expectedResult, result);
            i++;
        }
    }

    /**
     * Run the String convert() method test. Post-Condition: converted results
     * equal to expected results.
     */
    public void testConvertFormatStringsWithHeadAndTail() {

        String[] inputs = {
            "x = r'''%(test)s''' % ({'test': \"fest\"})  # this is a comment",
            "    print(\"test %s\" % \"fest\") #test"
        };
        String[] expectedResults = {
            "x = r'''{[test]!s}'''.format({'test': \"fest\"})  # this is a comment",
            "    print(\"test {0!s}\".format(\"fest\")) #test"
        };

        int i = 0;
        for (String input : inputs) {
            String expectedResult = expectedResults[i];

            PercentToBraceConverter fixture = new PercentToBraceConverter(input);
            String result = fixture.convert();

            assertEquals(expectedResult, result);
            i++;
        }
    }
    
    /**
     * Run the String convert() method test. Post-Condition: converted results
     * equal to expected results.
     */
    public void testConvertFormatCallReplacementSkipFalse() {

        // test with skipFormatCallReplacement == false (default)
        String[] inputs = {
            "\"test %s %0.2f\" % (\"fest\", 2.3393)",
            "\"test %s %0.2f\"% (\"fest\", 2.3393)",
            "\"test %s %0.2f\" %(\"fest\", 2.3393)",
            "\"test %s %0.2f\"%(\"fest\", 2.3393)"
        };
        String[] expectedResults = {
            "\"test {0!s} {1:>0.2f}\".format(\"fest\", 2.3393)",
            "\"test {0!s} {1:>0.2f}\".format(\"fest\", 2.3393)",
            "\"test {0!s} {1:>0.2f}\".format(\"fest\", 2.3393)",
            "\"test {0!s} {1:>0.2f}\".format(\"fest\", 2.3393)"
        };

        int i = 0;
        for (String input : inputs) {
            String expectedResult = expectedResults[i];

            PercentToBraceConverter fixture = new PercentToBraceConverter(input);
            String result = fixture.convert();

            assertEquals(expectedResult, result);
            i++;
        }
    }

    /**
     * Run the String convert() method test. Post-Condition: converted results
     * equal to expected results.
     */
    public void testConvertFormatCallReplacementSkipTrue() {

        // test with skipFormatCallReplacement == true
        String[] inputs = {
            "\"test %.2s %0.2f\" % (\"fest\", 2.3393)",
            "\"test %s %0.2f\"% (\"fest\", 2.3393)",
            "\"test %s %0.2f\" %(\"fest\", 2.3393)",
            "\"test %s %0.2f\"%(\"fest\", 2.3393)"
        };
        String[] expectedResults = {
            "\"test {0!s:.2} {1:>0.2f}\" % (\"fest\", 2.3393)",
            "\"test {0!s} {1:>0.2f}\"% (\"fest\", 2.3393)",
            "\"test {0!s} {1:>0.2f}\" %(\"fest\", 2.3393)",
            "\"test {0!s} {1:>0.2f}\"%(\"fest\", 2.3393)"
        };

        int i = 0;
        for (String input : inputs) {
            String expectedResult = expectedResults[i];

            PercentToBraceConverter fixture = new PercentToBraceConverter(input);
            fixture.setSkipFormatCallReplacement(true);
            String result = fixture.convert();

            assertEquals(expectedResult, result);
            i++;
        }
    }

    /**
     * Run the boolean equals(Object) method test for converters 
     * constructed from diverse inputs.
     */
    public void testEqualsWithVaryingInputs() {

        String[] inputs = {
            "",
            "'%s%s' % (key, transform)",
            "'%s%s' % (key, transform)",
            null
        };

        String[] inputsOther = {
            "",
            "'%s%s' & (key, transform)",
            "'%s%s' & (key, transfrom)",
            null
        };

        boolean[] expectedResults = {
            true,
            true,
            false
        };

        for (int i = 0; i < inputs.length; i++) {

            String input = inputs[i];
            String inputOther = inputsOther[i];

            PercentToBraceConverter fixture = new PercentToBraceConverter(input);
            PercentToBraceConverter fixtureOther = new PercentToBraceConverter(inputOther);

            boolean expectedResult = expectedResults[i];

            assertEquals(expectedResult, fixture.equals(fixtureOther));

            i++;
        }
    }

    /**
     * Run the boolean equals(Object) method test for converters 
     * constructed from same inputs but with varying object state.
     */
    public void testEqualsWithVaryingState() {
        
        PercentToBraceConverter fixture = new PercentToBraceConverter("");
        PercentToBraceConverter other = new PercentToBraceConverter("");
        
        fixture.setSkipFormatCallReplacement(true);
        other.setSkipFormatCallReplacement(false); // false is the default, but this makes it explicit

        boolean result = fixture.equals(other);
        assertEquals(false, result);
        
        // now the inverse test
        fixture.setSkipFormatCallReplacement(false);
        result = fixture.equals(other);
        assertEquals(true, result);
    }

    /**
     * Run the boolean equals(Object) method test with "bogus" input.
     */
    public void testEqualsBogus() {
        PercentToBraceConverter fixture = new PercentToBraceConverter("");
        
        boolean result = fixture.equals(null);
        assertEquals(false, result);
        
        result = fixture.equals("");
        assertEquals(false, result);
        
        result = fixture.equals(Integer.valueOf(3));
        assertEquals(false, result);
    }

    /**
     * Run the int getLength() method test.
     */
    public void testGetLength() {

        String[] inputs = {
            "",
            "'%s%s' % (key, transform)"
        };

        int[] expectedResults = {
            0,
            35
        };

        int i = 0;
        for (String input : inputs) {
            int expectedResult = expectedResults[i];

            PercentToBraceConverter fixture = new PercentToBraceConverter(input);
            int result = fixture.getLength();

            assertEquals(0, result);

            fixture.convert();
            result = fixture.getLength();

            assertEquals(expectedResult, result);
            i++;
        }

    }

    /**
     * Run the int hashCode() method test.
     */
    public void testHashCode() {
        
        PercentToBraceConverter fixture = new PercentToBraceConverter("");
        fixture.setSkipFormatCallReplacement(false);

        int result = fixture.hashCode();
        assertEquals(1, result);
        
        fixture.setSkipFormatCallReplacement(true);
        result = fixture.hashCode();
        assertEquals(2, result);
        
        String stringToConvert1 = "this is just a test";
        fixture = new PercentToBraceConverter(stringToConvert1);
        result = fixture.hashCode();
        assertEquals(1 + stringToConvert1.hashCode(), result);
    }

    /**
     * Run the boolean isSkippingFormatCallReplacement() method test.
     */
    public void testIsSkippingFormatCallReplacement() {
        
        PercentToBraceConverter fixture = new PercentToBraceConverter("this is string");
        fixture.setSkipFormatCallReplacement(true);

        boolean result = fixture.isSkippingFormatCallReplacement();

        // add additional test code here
        assertEquals(true, result);

        fixture.setSkipFormatCallReplacement(false);
        result = fixture.isSkippingFormatCallReplacement();

        assertEquals(false, result);
    }

    /**
     * Run the boolean isValidPercentFormatString(String) method test.
     */
    public void testIsValidPercentFormatString() {

        String[] inputs = {
            "",
            "\"test %s %0.2f\"% (\"fest\", 2.3393)",
            "        return 'odict.odict(%r)' % self.items()",
            
            "    def setdefault(self, key, default=None):\n" + 
            "        if key not in self:\n" + 
            "            self._keys.append(key)\n" + 
            "        dict.setdefault(self, key, default)",

            "print u\"Processing '%s'\" % inpath",
            "               raise Error(u\"Error: inpath (%s) doesn't exist!\" % inpath)\n",
            "pat = re.compile(ur'^(\\d+): \\(\\s*0,\\s*0,\\s*0\\) #0{6} black(\\n\\nconvert:.*?)?$')"
        };
        
        String[] multilineInputs = {
            "\t\tif len(result) == 0:\n\t\t\treturn False\n\t\telse:\n\t\t\tpat = re.compile(ur'^(\\d+): \\(\\s*0,\\s*0,\\s*0\\) #0{6} black(\\n\\nconvert:.*?)?$')\n\t\t\tmat = re.match(pat, result)\n",
            "cmd = u\"%s \\\"%s\\\"%s -depth %s -format %%c histogram:info:-\" % (convertpath, imgfile, resize, depth)",
            "help_message = u'''%s\nDetermine if an image is completely black, using ImageMagicks\nhistogram:info construct. Given one or many paths, filters and\nprint the paths containing fully black images to stdout.\n''' % license\n",
            "program_name = u\"imageisblack\"\nprogram_version = u\"v0.1\"\nprogram_build_date = u\"2010-10-16\"\n\nversion_message = u'%%(prog)s %s (%s)' % (program_version, program_build_date)\n"
        };
        
        boolean[] expectedResults = {
            false,
            true,
            true,
            false,
            true,
            true,
            false
        };
        boolean[] mlExpectedResults = {
            false,
            true,
            false, // FIXME: format string spanning multiple lines should be supported
            true
        };

        String input = null;

        for (int i = 0; i < inputs.length; i++) {
            input = inputs[i];
            boolean expectedResult = expectedResults[i];
            boolean result = PercentToBraceConverter.isValidPercentFormatString(input, false);

            assertEquals(expectedResult, result);
            i++;
        }
        
        for (int i = 0; i < multilineInputs.length; i++) {
            input = multilineInputs[i];
            boolean mlExpectedResult = mlExpectedResults[i];
            boolean result = PercentToBraceConverter.isValidPercentFormatString(input, true);
            
            assertEquals(mlExpectedResult, result);
            i++;
        }
    }

    /**
     * Run the String toString() method test.
     */
    public void testToString() {
        
        PercentToBraceConverter fixture = new PercentToBraceConverter("");
        fixture.setSkipFormatCallReplacement(true);

        String result = fixture.toString();

        assertEquals("<PercentToBraceConverter@0x2 | source= match= argIndex=0 head= tail=>", result);
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
            junit.textui.TestRunner.run(PercentToBraceConverterTest.class);
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