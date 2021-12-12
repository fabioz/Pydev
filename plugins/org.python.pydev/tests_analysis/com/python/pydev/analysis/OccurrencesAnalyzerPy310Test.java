package com.python.pydev.analysis;

import org.eclipse.jface.text.Document;
import org.python.pydev.ast.analysis.messages.IMessage;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.ParseException;

public class OccurrencesAnalyzerPy310Test extends AnalysisTestsBase {

    public static void main(String[] args) {
        try {
            OccurrencesAnalyzerPy310Test analyzer2 = new OccurrencesAnalyzerPy310Test();
            analyzer2.setUp();
            analyzer2.testMatchStmtSimple();
            analyzer2.tearDown();
            System.out.println("finished");
            junit.textui.TestRunner.run(OccurrencesAnalyzerPy310Test.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private int initialGrammar;

    @Override
    public void setUp() throws Exception {
        initialGrammar = GRAMMAR_TO_USE_FOR_PARSING;
        GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.GRAMMAR_PYTHON_VERSION_3_10;
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        GRAMMAR_TO_USE_FOR_PARSING = initialGrammar;
        ParseException.verboseExceptions = true;
        super.tearDown();
    }

    @Override
    protected boolean isPython3Test() {
        return true;
    }

    public void testMatchStmtSimple() {
        doc = new Document(""
                + "command = 'foo'\n"
                + "action = 'bar'\n"
                + "obj = dict()\n"
                + "match command.split():\n"
                + "    case [action,obj]:\n"
                + "        pass\n");
        checkNoError();
    }

    public void testMatchStmtSimple2() {
        doc = new Document(""
                + "command = 'foo'\n"
                + "action = 'bar'\n"
                + "obj = dict()\n"
                + "match command.split():\n"
                + "    case (action,obj):\n"
                + "        pass\n");
        checkNoError();
    }

    public void testMatchAsSoftKeyword() {
        doc = new Document(""
                + "match = 10\n"
                + "print(match)");
        checkNoError();
    }

    public void testCaseAsSoftKeyword() {
        doc = new Document(""
                + "case = 10\n"
                + "print(case)");
        checkNoError();
    }

    public void testMatchAndCaseAsSoftKeyword() {
        doc = new Document(""
                + "match = 30\n"
                + "case = 10\n"
                + "result = match + case\n"
                + "print(result)");
        checkNoError();
    }

    public void testMatchingMultiplePatterns() {
        doc = new Document(""
                + "command = \"foo bar\"\n"
                + "action = \"doo\"\n"
                + "obj = {\"key\":\"value\"}\n"
                + "match command.split():\n"
                + "    case [action]:\n"
                + "        pass\n"
                + "    case [action, obj]:\n"
                + "        pass");
        checkNoError();
    }

    public void testMatchingSpecificValues() {
        doc = new Document(""
                + "def quit_game():\n"
                + "    pass\n"
                + "def foo(command, current_room, character):\n"
                + "    match command.split():\n"
                + "        case [\"quit\"]:\n"
                + "            print(\"Goodbye!\")\n"
                + "            quit_game()\n"
                + "        case [\"look\"]:\n"
                + "            current_room.describe()\n"
                + "        case [\"get\", obj]:\n"
                + "            character.get(obj, current_room)\n"
                + "        case [\"go\", direction]:\n"
                + "            current_room = current_room.neighbor(direction)");
        checkNoError();
    }

    public void testMatchingMultipleValues() {
        doc = new Document(""
                + "def foo(command, character, current_room):\n"
                + "    match command.split():\n"
                + "        case [\"drop\", *objects]:\n"
                + "            for obj in objects:\n"
                + "                character.drop(obj, current_room)");
        checkNoError();
    }

    public void testMatchWildcard() {
        doc = new Document(""
                + "def foo(command):\n"
                + "    match command.split():\n"
                + "        case [\"quit\"]:\n"
                + "            pass\n"
                + "        case [\"go\", direction]:\n"
                + "            pass\n"
                + "        case [\"drop\", *objects]:\n"
                + "            pass\n"
                + "        case _:\n"
                + "            print(f\"Sorry, I couldn't understand {command!r}\")");
        checkError("Unused variable: direction", "Unused variable: objects");
    }

    public void testMatchFlexibleBinding() {
        doc = new Document(""
                + "def foo(command):\n"
                + "    match command.split():\n"
                + "        case [\"quit\"]:\n"
                + "            pass\n"
                + "        case [\"go\", direction]:\n"
                + "            print(direction)\n"
                + "        case [\"drop\", *objects]:\n"
                + "            pass");
        checkError("Unused variable: objects");
    }

    public void testMatchFlexibleBinding2() {
        doc = new Document(""
                + "def foo(command):\n"
                + "    match command.split():\n"
                + "        case [\"quit\"]:\n"
                + "            pass\n"
                + "        case [\"go\", direction]:\n"
                + "            pass\n"
                + "        case [\"drop\", *objects]:\n"
                + "            print(objects)");
        checkError("Unused variable: direction");
    }

    public void testMatchStarPattern() {
        doc = new Document(""
                + "def foo(command):\n"
                + "    match command.split():\n"
                + "        case [\"quit\"]:\n"
                + "            pass\n"
                + "        case [\"drop\", *objects]:\n"
                + "            print(objects)");
        checkNoError();
    }

    public void testMatchOrPatterns() {
        doc = new Document(""
                + "def foo(command, current_room):\n"
                + "    match command.split():\n"
                + "        case [\"north\"] | [\"go\", \"north\"]:\n"
                + "            current_room = current_room.neighbor(\"north\")\n"
                + "        case [\"get\", obj] | [\"pick\", \"up\", obj] | [\"pick\", obj, \"up\"]:\n"
                + "            pass");
        checkError(3);
        for (IMessage message : msgs) {
            assertEquals("Unused variable: obj", message.getMessage());
        }
    }

    public void testMatchSubPatterns() {
        doc = new Document(""
                + "def foo(command, current_room):\n"
                + "    match command.split():\n"
                + "        case [\"go\", (\"north\" | \"south\" | \"east\" | \"west\")]:\n"
                + "            current_room = current_room.neighbor()");
        checkNoError();
    }

    public void testMatchSubPatterns2() {
        doc = new Document(""
                + "def foo(command, current_room):\n"
                + "    match command.split():\n"
                + "        case [\"go\", (\"north\" | \"south\" | \"east\" | \"west\") as direction]:\n"
                + "            current_room = current_room.neighbor(direction)");
        checkNoError();
    }

    public void testMatchConditionsPattern() {
        doc = new Document(""
                + "def foo(command, current_room):\n"
                + "    match command.split():\n"
                + "        case [\"go\", direction] if direction in current_room.exits:\n"
                + "            current_room = current_room.neighbor(direction)\n"
                + "        case [\"go\", _]:\n"
                + "            print(\"Sorry, you can't go that way\")");
        checkNoError();
    }

    public void testMatchingObjects() {
        doc = new Document(""
                + "def handle_click_at(x, y):\n"
                + "    pass\n"
                + "def foo(event, Click, KeyPress, Quit, game):\n"
                + "    match event.get():\n"
                + "        case Click(position=(x, y)):\n"
                + "            handle_click_at(x, y)\n"
                + "        case KeyPress(key_name=\"Q\") | Quit():\n"
                + "            game.quit()\n"
                + "        case KeyPress(key_name=\"up arrow\"):\n"
                + "            game.go_north()\n"
                + "        case KeyPress():\n"
                + "            pass\n"
                + "        case other_event:\n"
                + "            raise ValueError(f\"Unrecognized event: {other_event}\")");
        checkNoError();
    }

    public void testMatchPositionalAttrs() {
        doc = new Document(""
                + "class Click:\n"
                + "    position: tuple\n"
                + "    button: str\n"
                + "def handle_click_at(x, y):\n"
                + "    pass\n"
                + "def foo(event):\n"
                + "    match event.get():\n"
                + "        case Click((x, y)):\n"
                + "            handle_click_at(x, y)");
        checkNoError();
    }

    public void testMatchConstantsAndEnums() {
        doc = new Document(""
                + "class Click:\n"
                + "    def __init__(self, coord, button):\n"
                + "        self.coord = coord\n"
                + "        self.button = button\n"
                + "def handle_click_at(x, y):\n"
                + "    pass\n"
                + "def foo(event, Button):\n"
                + "    match event.get():\n"
                + "        case Click((x, y), button=Button.LEFT):  # This is a left click\n"
                + "            handle_click_at(x, y)\n"
                + "        case Click():\n"
                + "            pass");
        checkNoError();
    }

    public void testMatchMappings() {
        doc = new Document(""
                + "def warning(s):\n"
                + "    pass\n"
                + "def foo(actions, message, ui, c, duration, url):\n"
                + "    for action in actions:\n"
                + "        match action:\n"
                + "            case {\"text\": message, \"color\": c}:\n"
                + "                ui.set_text_color(c)\n"
                + "                ui.display(message)\n"
                + "            case {\"sleep\": duration}:\n"
                + "                ui.wait(duration)\n"
                + "            case {\"sound\": url, \"format\": \"ogg\"}:\n"
                + "                ui.play(url)\n"
                + "            case {\"sound\": _, \"format\": _}:\n"
                + "                warning(\"Unsupported audio format\")");
        checkNoError();
    }

    public void testMatchBuiltinClasses() {
        doc = new Document(""
                + "def warning(s):\n"
                + "    pass\n"
                + "def foo(actions, message, ui, c, duration, url):\n"
                + "    for action in actions:\n"
                + "        match action:\n"
                + "            case {\"text\": str(message), \"color\": str(c)}:\n"
                + "                ui.set_text_color(c)\n"
                + "                ui.display(message)\n"
                + "            case {\"sleep\": float(duration)}:\n"
                + "                ui.wait(duration)\n"
                + "            case {\"sound\": str(url), \"format\": \"ogg\"}:\n"
                + "                ui.play(url)\n"
                + "            case {\"sound\": _, \"format\": _}:\n"
                + "                warning(\"Unsupported audio format\")");
        checkNoError();
    }

    public void testMatchStmt() {
        doc = new Document(""
                + "def http_error(status):\n"
                + "    match status:\n"
                + "        case 400:\n"
                + "            return \"Bad request\"\n"
                + "        case 404:\n"
                + "            return \"Not found\"\n"
                + "        case 418:\n"
                + "            return \"I'm a teapot\"\n"
                + "        case _:\n"
                + "            return \"Something's wrong with the Internet\"\n"
                + "        case 401 | 403 | 404:\n"
                + "            return \"Not allowed\"");
        checkNoError();
    }

    public void testMatchStmt2() {
        doc = new Document(""
                + "def foo(point):\n"
                + "    match point:\n"
                + "        case (0, 0):\n"
                + "            print(\"Origin\")\n"
                + "        case (0, y):\n"
                + "            print(f\"Y={y}\")\n"
                + "        case (x, 0):\n"
                + "            print(f\"X={x}\")\n"
                + "        case (x, y):\n"
                + "            print(f\"X={x}, Y={y}\")\n"
                + "        case _:\n"
                + "            raise ValueError(\"Not a point\")");
        checkNoError();
    }

    public void testMatchStmt3() {
        doc = new Document(""
                + "class Point:\n"
                + "    def __init__(self, x, y):\n"
                + "        self.x = x\n"
                + "        self.y = y\n"
                + "def where_is(point):\n"
                + "    match point:\n"
                + "        case Point(x=0, y=0):\n"
                + "            print(\"Origin\")\n"
                + "        case Point(x=0, y=y):\n"
                + "            print(f\"Y={y}\")\n"
                + "        case Point(x=x, y=0):\n"
                + "            print(f\"X={x}\")\n"
                + "        case Point():\n"
                + "            print(\"Somewhere else\")\n"
                + "        case _:\n"
                + "            print(\"Not a point\")");
        checkNoError();
    }

    public void testMatchStmt4() {
        doc = new Document(""
                + "class Point:\n"
                + "    def __init__(self, x, y):\n"
                + "        self.x = x\n"
                + "        self.y = y\n"
                + "def foo(points):\n"
                + "    match points:\n"
                + "        case []:\n"
                + "            print(\"No points\")\n"
                + "        case [Point(0, 0)]:\n"
                + "            print(\"The origin\")\n"
                + "        case [Point(x, y)]:\n"
                + "            print(f\"Single point {x}, {y}\")\n"
                + "        case [Point(0, y1), Point(0, y2)]:\n"
                + "            print(f\"Two on the Y axis at {y1}, {y2}\")\n"
                + "        case _:\n"
                + "            print(\"Something else\")");
        checkNoError();
    }

    public void testMatchStmt5() {
        doc = new Document(""
                + "class Point:\n"
                + "    def __init__(self, x, y):\n"
                + "        self.x = x\n"
                + "        self.y = y\n"
                + "def foo(point):\n"
                + "    match point:\n"
                + "        case Point(x, y) if x == y:\n"
                + "            print(f\"Y=X at {x}\")\n"
                + "        case Point(x, y):\n"
                + "            print(f\"Not on the diagonal\")");
        checkNoError();
    }

    public void testMatchStmt6() {
        doc = new Document(""
                + "class Color:\n"
                + "    RED = 0\n"
                + "    GREEN = 1\n"
                + "    BLUE = 2\n"
                + "def foo(color):\n"
                + "    match color:\n"
                + "        case Color.RED:\n"
                + "            print(\"I see red!\")\n"
                + "        case Color.GREEN:\n"
                + "            print(\"Grass is green\")\n"
                + "        case Color.BLUE:\n"
                + "            print(\"I'm feeling the blues :(\")");
        checkNoError();
    }

    public void testWildcardPattern() {
        doc = new Document(""
                + "match \"foo bar\".split()\n"
                + "    case (\"doo\", \"lee\"):\n"
                + "        pass\n"
                + "    case _:\n"
                + "        pass");
        checkNoError();
    }

    public void testNonPatternWildcard() {
        doc = new Document(""
                + "x = 10\n"
                + "match \"foo bar\".split()\n"
                + "    case (\"doo\", \"lee\"):\n"
                + "        pass\n"
                + "x = _");
        checkError("Undefined variable: _");
    }

    public void testNonPatternWildcard2() {
        doc = new Document(""
                + "x = 10\n"
                + "match \"foo bar\".split()\n"
                + "    case (\"doo\", \"lee\"):\n"
                + "        pass\n"
                + "    case _:\n"
                + "        x = _");
        checkError("Undefined variable: _");
    }

    public void testMatchStmtIgnoreNoStatement() {
        doc = new Document(""
                + "class Color:\n"
                + "    RED = 0\n"
                + "    GREEN = 1\n"
                + "    BLUE = 2\n"
                + "def foo(color):\n"
                + "    a = 1\n"
                + "    match color:\n"
                + "        case Color.RED:\n"
                + "            print(\"I see red!\")\n"
                + "        case Color.GREEN:\n"
                + "            print(\"Grass is green\")\n"
                + "        case Color.BLUE:\n"
                + "            a == 2");
        checkError("Statement apppears to have no effect");
    }

    public void testMatchCaseNameBinding() {
        doc = new Document(""
                + "class Point:\n"
                + "    def __init__(self, x, y):\n"
                + "        self.x = x\n"
                + "        self.y = y\n"
                + "def where_is(point):\n"
                + "    match point:\n"
                + "        case Point(x=0, y=y):\n"
                + "            print('Y=', y)\n"
                + "where_is(Point(0, 0))");
        checkNoError();
    }

    public void testMatchCaseNameBinding2() {
        doc = new Document(""
                + "point = (0, 2)\n"
                + "match point:\n"
                + "    case (0, 0):\n"
                + "        print(\"Origin\")\n"
                + "    case (0, y):\n"
                + "        print(f\"Y={y}\")\n"
                + "    case _:\n"
                + "        raise ValueError(\"Not a point\")");
        checkNoError();
    }

    public void testWithStmt() {
        doc = new Document("from contextlib import nullcontext as f\n"
                + "with (f() as example):\n"
                + "    print(example)");
        checkError("Unresolved import: f");
    }

    public void testWithStmt2() {
        doc = new Document("from contextlib import nullcontext as f\n"
                + "with (f('c') as a,\n"
                + "     f('a') as b):\n"
                + "    print(a)\n"
                + "    print(b)");
        checkError("Unresolved import: f");
    }

    public void testWithStmt3() {
        doc = new Document("from contextlib import nullcontext as f\n"
                + "with f('c') as a, f('a') as b:\n"
                + "    print(a)\n"
                + "    print(b)");
        checkError("Unresolved import: f");
    }
}
