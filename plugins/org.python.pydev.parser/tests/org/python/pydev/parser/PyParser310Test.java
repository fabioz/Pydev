/**
 * Copyright (c) 2020 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import org.python.pydev.core.IPythonNature;

public class PyParser310Test extends PyParserTestBase {

    public static void main(String[] args) {
        try {
            PyParser310Test test = new PyParser310Test();
            test.setUp();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParser310Test.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setDefaultVersion(IPythonNature.GRAMMAR_PYTHON_VERSION_3_10);
    }

    public void testMatchStmtSimple() throws Throwable {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match command.split():\n"
                    + "    case [action, obj]:\n"
                    + "        pass";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchStmtSpecificValues() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match command.split():\n"
                    + "    case [\"quit\"]:\n"
                    + "        print(\"Goodbye!\")\n"
                    + "        quit_game()\n"
                    + "    case [\"look\"]:\n"
                    + "        current_room.describe()\n"
                    + "    case [\"get\", obj]:\n"
                    + "        character.get(obj, current_room)\n"
                    + "    case [\"go\", direction]:\n"
                    + "        current_room = current_room.neighbor(direction)"
                    + "";
            parseLegalDocStr(s);
            return true;
        });

    }

    public void testMatchStmtMultipleValues() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match command.split():\n"
                    + "    case [\"drop\", *objects]:\n"
                    + "        for obj in objects:\n"
                    + "            character.drop(obj, current_room)\n"
                    + "";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchStmtWildcard() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match command.split():\n"
                    + "    case [\"quit\"]:\n"
                    + "        pass\n"
                    + "    case [\"go\", direction]:\n"
                    + "        pass\n"
                    + "    case [\"drop\", *objects]:\n"
                    + "        pass\n"
                    + "    case _:\n"
                    + "        print(f\"Sorry, I couldn't understand {command!r}\")";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchStmtOrPattern() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match command.split():\n"
                    + "    case [\"north\"] | [\"go\", \"north\"]:\n"
                    + "        current_room = current_room.neighbor(\"north\")\n"
                    + "    case [\"get\", obj] | [\"pick\", \"up\", obj] | [\"pick\", obj, \"up\"]:\n"
                    + "        pass";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchStmtSubPatterns() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match command.split():\n"
                    + "    case [\"go\", (\"north\" | \"south\" | \"east\" | \"west\")]:\n"
                    + "        pass\n";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchStmtSubPatterns2() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match command.split():\n"
                    + "    case [\"go\", (\"north\" | \"south\" | \"east\" | \"west\") as direction]:\n"
                    + "        current_room = current_room.neighbor(direction)";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchStmtConditionsToPatterns() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match command.split():\n"
                    + "    case [\"go\", direction] if direction in current_room.exits:\n"
                    + "        current_room = current_room.neighbor(direction)\n"
                    + "    case [\"go\", _]:\n"
                    + "        print(\"Sorry, you can't go that way\")";
            parseLegalDocStr(s);
            return true;
        });

    }

    public void testMatchStmtMatchingObjects() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match event.get():\n"
                    + "    case Click(position=(x, y)):\n"
                    + "        handle_click_at(x, y)\n"
                    + "    case KeyPress(key_name=\"Q\") | Quit():\n"
                    + "        game.quit()\n"
                    + "    case KeyPress(key_name=\"up arrow\"):\n"
                    + "        game.go_north()\n"
                    + "    case KeyPress():\n"
                    + "        pass # Ignore other keystrokes\n"
                    + "    case other_event:\n"
                    + "        raise ValueError(f\"Unrecognized event: {other_event}\")";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchStmtMatchingPositionalAttribs() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "from dataclasses import dataclass\n"
                    + "\n"
                    + "@dataclass\n"
                    + "class Click:\n"
                    + "    position: tuple\n"
                    + "    button: Button\n"
                    + "\n"
                    + "match event.get():\n"
                    + "    case Click((x, y)):\n"
                    + "        handle_click_at(x, y)";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchStmtMatchingPositionalAttribs2() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "from dataclasses import dataclass\n"
                    + "\n"
                    + "@dataclass\n"
                    + "class Click:\n"
                    + "    position: tuple\n"
                    + "    button: Button\n"
                    + "\n"
                    + "match event.get():\n"
                    + "    case Click((x, y), button=Button.LEFT):  # This is a left click\n"
                    + "        handle_click_at(x, y)\n"
                    + "    case Click():\n"
                    + "        pass  # ignore other clicks";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchStmtMappings() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "for action in actions:\n"
                    + "    match action:\n"
                    + "        case {\"text\": message, \"color\": c}:\n"
                    + "            ui.set_text_color(c)\n"
                    + "            ui.display(message)\n"
                    + "        case {\"sleep\": duration}:\n"
                    + "            ui.wait(duration)\n"
                    + "        case {\"sound\": url, \"format\": \"ogg\"}:\n"
                    + "            ui.play(url)\n"
                    + "        case {\"sound\": _, \"format\": _}:\n"
                    + "            warning(\"Unsupported audio format\")";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchStmtBuiltins() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "for action in actions:\n"
                    + "    match action:\n"
                    + "        case {\"text\": str(message), \"color\": str(c)}:\n"
                    + "            ui.set_text_color(c)\n"
                    + "            ui.display(message)\n"
                    + "        case {\"sleep\": float(duration)}:\n"
                    + "            ui.wait(duration)\n"
                    + "        case {\"sound\": str(url), \"format\": \"ogg\"}:\n"
                    + "            ui.play(url)\n"
                    + "        case {\"sound\": _, \"format\": _}:\n"
                    + "            warning(\"Unsupported audio format\")";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchStmt() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "def http_error(status):\n"
                    + "    match status:\n"
                    + "        case 400:\n"
                    + "            return \"Bad request\"\n"
                    + "        case 404:\n"
                    + "            return \"Not found\"\n"
                    + "        case 418:\n"
                    + "            return \"I'm a teapot\"\n"
                    + "        case _:\n"
                    + "            return \"Something's wrong with the Internet\"";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchStmt2() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "# point is an (x, y) tuple\n"
                    + "match point:\n"
                    + "    case (0, 0):\n"
                    + "        print(\"Origin\")\n"
                    + "    case (0, y):\n"
                    + "        print(f\"Y={y}\")\n"
                    + "    case (x, 0):\n"
                    + "        print(f\"X={x}\")\n"
                    + "    case (x, y):\n"
                    + "        print(f\"X={x}, Y={y}\")\n"
                    + "    case _:\n"
                    + "        raise ValueError(\"Not a point\")";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchStmt3() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "from dataclasses import dataclass\n"
                    + "\n"
                    + "@dataclass\n"
                    + "class Point:\n"
                    + "    x: int\n"
                    + "    y: int\n"
                    + "\n"
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
                    + "            print(\"Not a point\")";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchStmt4() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match points:\n"
                    + "    case []:\n"
                    + "        print(\"No points\")\n"
                    + "    case [Point(0, 0)]:\n"
                    + "        print(\"The origin\")\n"
                    + "    case [Point(x, y)]:\n"
                    + "        print(f\"Single point {x}, {y}\")\n"
                    + "    case [Point(0, y1), Point(0, y2)]:\n"
                    + "        print(f\"Two on the Y axis at {y1}, {y2}\")\n"
                    + "    case _:\n"
                    + "        print(\"Something else\")";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchStmt5() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match point:\n"
                    + "    case Point(x, y) if x == y:\n"
                    + "        print(f\"Y=X at {x}\")\n"
                    + "    case Point(x, y):\n"
                    + "        print(f\"Not on the diagonal\")";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchStmt6() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "from enum import Enum\n"
                    + "class Color(Enum):\n"
                    + "    RED = 0\n"
                    + "    GREEN = 1\n"
                    + "    BLUE = 2\n"
                    + "\n"
                    + "match color:\n"
                    + "    case Color.RED:\n"
                    + "        print(\"I see red!\")\n"
                    + "    case Color.GREEN:\n"
                    + "        print(\"Grass is green\")\n"
                    + "    case Color.BLUE:\n"
                    + "        print(\"I'm feeling the blues :(\")";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testLiteralPattern() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match something:\n"
                    + "    case -10 + 10:\n"
                    + "        pass";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testLiteralPattern2() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match something:\n"
                    + "    case 10 + 10:\n"
                    + "        pass";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testLiteralPattern3() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match something:\n"
                    + "    case 10 - 10:\n"
                    + "        pass";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testLiteralPattern4() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match something:\n"
                    + "    case -10 - 10:\n"
                    + "        pass";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testLiteralPattern5() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match something:\n"
                    + "    case -10 + 10:\n"
                    + "        pass";
            parseLegalDocStr(s);
            return true;
        });

    }

    public void testMatchAsSoftKeyword() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match = 10\n"
                    + "print(match)";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testCaseAsSoftKeyword() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "case = 10\n"
                    + "print(case)";
            parseLegalDocStr(s);
            return true;
        });
    }

    public void testMatchAndCaseAsSoftKeyword() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "match = 30\n"
                    + "case = 10\n"
                    + "result = match + case\n"
                    + "print(result)";
            parseLegalDocStr(s);
            return true;
        });

    }

    public void testWithStmt() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "from contextlib import nullcontext as f\n"
                    + "with (f() as example):\n"
                    + "    pass";
            parseLegalDocStr(s);
            return true;
        });

    }

    public void testWithStmt2() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "with (f('c') as a,\n"
                    + "     f('a') as b):\n"
                    + "    pass";
            parseLegalDocStr(s);
            return true;
        });

    }

    public void testWithStmt3() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            String s = "with f('c') as a, f('a') as b:\n"
                    + "    pass\n";
            parseLegalDocStr(s);
            return true;
        });

    }

    public void testWithStmt4() {
        checkWithAllGrammars310Onwards((grammarVersion) -> {
            // Note that the parens of the `with` in this case is not a part of the with
            // and is actually used to create an expression.
            String s = "with (sys.stdin if args.flist=='-' else open(args.flist)) as f:\n"
                    + "    for line in f:\n"
                    + "        compile_dests.append(line.strip())\n"
                    + "";
            parseLegalDocStr(s);
            return true;
        });

    }
}
