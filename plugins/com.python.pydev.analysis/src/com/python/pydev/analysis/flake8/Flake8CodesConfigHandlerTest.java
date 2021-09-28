package com.python.pydev.analysis.flake8;

import static org.junit.Assert.assertNotEquals;

import java.util.Optional;

import org.python.pydev.json.eclipsesource.JsonValue;
import org.python.pydev.shared_core.structure.Tuple;

import junit.framework.TestCase;

public class Flake8CodesConfigHandlerTest extends TestCase {

    public void testJsonFormatError() {
        String str = ""
                + "[\n"
                + "    {\n"
                + "        \"something\":\"anything\"\n"
                + "    }\n"
                + "]\n"
                + "\n"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError2() {
        String str = ""
                + "{\n"
                + "\"foo[\": \"ignore\"\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError3() {
        String str = ""
                + "{\n"
                + "\"foo%!$!@\": \"ignore\"\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError4() {
        String str = ""
                + "{\n"
                + "\"foo[]\": \"ignore\"\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError5() {
        String str = ""
                + "{\n"
                + "\"foo]\": \"ignore\"\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError6() {
        String str = ""
                + "{\n"
                + "\"foo[-1, 100]\": 0,\n"
                + "\"foo[-1, 300]\": 1\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError7() {
        String str = ""
                + "{\n"
                + "\"foo[100, 300]\": 0,\n"
                + "\"foo[200, 400]\": 1\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError8() {
        String str = ""
                + "{\n"
                + "\"foo[100, 300]\": 0,\n"
                + "\"foo[200, -1]\": 1\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError9() {
        String str = ""
                + "{\n"
                + "\"foo[100, -1]\": 0,\n"
                + "\"foo[200, 300]\": 1\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError10() {
        String str = ""
                + "{\n"
                + "\"foo[-1, -1]\": 0,\n"
                + "\"foo[-1, -1]\": 1\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError11() {
        String str = ""
                + "{\n"
                + "\"foo[200, 300]\": 0,\n"
                + "\"foo[300, 400]\": 1\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError12() {
        String str = ""
                + "{\n"
                + "\"foo[200, 200]\": 0,\n"
                + "\"foo200\": 1\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError13() {
        String str = ""
                + "{\n"
                + "\"foo[200, 300]\": 0,\n"
                + "\"foo250\": 1\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError14() {
        String str = ""
                + "{\n"
                + "\"foo-1\": 0\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError15() {
        String str = ""
                + "{\n"
                + "\"foo\": 0,\n"
                + "\"foo\": 1\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError16() {
        String str = ""
                + "{\n"
                + "\"foo\": 10\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError17() {
        String str = ""
                + "{\n"
                + "\"foo\": -2\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError18() {
        String str = ""
                + "{\n"
                + "\"foo\": \"ingnore\"\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError19() {
        String str = ""
                + "{\n"
                + "\"foo[200, 100]\": 0\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError20() {
        String str = ""
                + "{\n"
                + "\"foo[200, 100]\": 0\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatError21() {
        String str = ""
                + "{\n"
                + "\"foo[200, 100]\": \"\"\n"
                + "}"
                + "";
        Optional<String> errorMessage = Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str));
        System.out.println(errorMessage.get());
        assertNotEquals(Optional.empty(), errorMessage);
    }

    public void testJsonFormatNoError() {
        String str = ""
                + "{\n"
                + "\"foo[100, -1]\": 0,\n"
                + "\"foo[-1, 99]\": 1\n"
                + "}"
                + "";
        assertEquals(Optional.empty(), Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str)));
    }

    public void testJsonFormatNoError2() {
        String str = ""
                + "{\n"
                + "\"foo[-1, 100]\": 0,\n"
                + "\"foo[101, -1]\": 1\n"
                + "}"
                + "";
        assertEquals(Optional.empty(), Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str)));
    }

    public void testJsonFormatNoError3() {
        String str = ""
                + "{\n"
                + "\"foo[100, 200]\": 0,\n"
                + "\"foo[201, 400]\": 1\n"
                + "}"
                + "";
        assertEquals(Optional.empty(), Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str)));
    }

    public void testJsonFormatNoError4() {
        String str = ""
                + "{\n"
                + "\"foo[-1, -1]\": 0,\n"
                + "\"foo[100, 400]\": 1\n"
                + "}"
                + "";
        assertEquals(Optional.empty(), Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str)));
    }

    public void testJsonFormatNoError5() {
        String str = ""
                + "{\n"
                + "\"foo99\": 0,\n"
                + "\"foo[100, 400]\": 1\n"
                + "}"
                + "";
        assertEquals(Optional.empty(), Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str)));
    }

    public void testJsonFormatNoError6() {
        String str = ""
                + "{\n"
                + "\"foo\": 0,\n"
                + "\"foo[100, 400]\": 1\n"
                + "}"
                + "";
        assertEquals(Optional.empty(), Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str)));
    }

    public void testJsonFormatNoError7() {
        String str = ""
                + "{\n"
                + "\"foo\": 1,\n"
                + "\"foo[100, 400]\": 2\n"
                + "}"
                + "";
        assertEquals(Optional.empty(), Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str)));
    }

    public void testJsonFormatNoError8() {
        String str = ""
                + "{\n"
                + "\"foo\": \"ignore\",\n"
                + "\"foo[100, 400]\": \"info\"\n"
                + "}"
                + "";
        assertEquals(Optional.empty(), Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str)));
    }

    public void testJsonFormatNoError9() {
        String str = ""
                + "{\n"
                + "\"foo\": \"warning\",\n"
                + "\"foo[100, 400]\": \"error\"\n"
                + "}"
                + "";
        assertEquals(Optional.empty(), Flake8CodesConfigHandler.checkJsonFormat(JsonValue.readFrom(str)));
    }

    public void testRangeOverlap() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(0, 100);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(0, 100);
        assertTrue(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeOverlap2() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(100, 200);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(-1, 300);
        assertTrue(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeOverlap3() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(-1, 300);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(100, 200);
        assertTrue(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeOverlap4() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(-1, -1);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(100, 200);
        assertTrue(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeOverlap5() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(100, 200);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(-1, -1);
        assertTrue(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeOverlap6() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(100, 200);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(150, 180);
        assertTrue(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeOverlap7() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(150, 180);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(100, 200);
        assertTrue(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeOverlap8() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(50, 150);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(100, 200);
        assertTrue(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeOverlap9() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(100, 200);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(50, 150);
        assertTrue(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeOverlap10() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(100, -1);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(101, 150);
        assertTrue(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeOverlap11() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(-1, 100);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(50, 50);
        assertTrue(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeOverlap12() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(50, 50);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(-1, 100);
        assertTrue(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeNoOverlap() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(0, 50);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(51, 100);
        assertFalse(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeNoOverlap2() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(51, 100);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(0, 50);
        assertFalse(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeNoOverlap3() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(-1, 50);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(51, 100);
        assertFalse(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeNoOverlap4() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(51, 100);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(-1, 50);
        assertFalse(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeNoOverlap5() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(0, 50);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(51, -1);
        assertFalse(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }

    public void testRangeNoOverlap6() {
        Tuple<Integer, Integer> range1 = new Tuple<Integer, Integer>(51, -1);
        Tuple<Integer, Integer> range2 = new Tuple<Integer, Integer>(0, 50);
        assertFalse(Flake8CodesConfigHandler.checkRangeOverlap(range1, range2));
    }
}
