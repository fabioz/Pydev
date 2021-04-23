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

    public void testMatchStmtSimple() {
        String s = "match command.split():\n"
                + "    case [action, obj]:\n"
                + "        pass";
        parseLegalDocStr(s);
    }

    public void testMatchStmtSpecificValues() {
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
    }

    public void testMatchStmtMultipleValues() {
        String s = "match command.split():\n"
                + "    case [\"drop\", *objects]:\n"
                + "        for obj in objects:\n"
                + "            character.drop(obj, current_room)\n"
                + "";
        parseLegalDocStr(s);
    }

    public void testMatchStmtWildcard() {
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
    }

    public void testMatchStmtOrPattern() {
        String s = "match command.split():\n"
                + "    case [\"north\"] | [\"go\", \"north\"]:\n"
                + "        current_room = current_room.neighbor(\"north\")\n"
                + "    case [\"get\", obj] | [\"pick\", \"up\", obj] | [\"pick\", obj, \"up\"]:\n"
                + "        pass";
        parseLegalDocStr(s);
    }

    public void testMatchStmtSubPatterns() {
        String s = "match command.split():\n"
                + "    case [\"go\", (\"north\" | \"south\" | \"east\" | \"west\")]:\n"
                + "        pass\n";
        parseLegalDocStr(s);
    }

    public void testMatchStmtSubPatterns2() {
        String s = "match command.split():\n"
                + "    case [\"go\", (\"north\" | \"south\" | \"east\" | \"west\") as direction]:\n"
                + "        current_room = current_room.neighbor(direction)";
        parseLegalDocStr(s);
    }

    public void testMatchStmtConditionsToPatterns() {
        String s = "match command.split():\n"
                + "    case [\"go\", direction] if direction in current_room.exits:\n"
                + "        current_room = current_room.neighbor(direction)\n"
                + "    case [\"go\", _]:\n"
                + "        print(\"Sorry, you can't go that way\")";
        parseLegalDocStr(s);
    }

    public void testMatchStmtMatchingObjects() {
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
    }

    public void testMatchStmtMatchingPositionalAttribs() {
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
    }

    public void testMatchStmtMatchingPositionalAttribs2() {
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
    }

    public void testMatchStmtMappings() {
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
    }

    public void testMatchStmtBuiltins() {
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
    }

    public void testMatchStmt() {
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
    }

    public void testMatchStmt2() {
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
    }

    public void testMatchStmt3() {
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
    }

    public void testMatchStmt4() {
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
    }

    public void testMatchStmt5() {
        String s = "match point:\n"
                + "    case Point(x, y) if x == y:\n"
                + "        print(f\"Y=X at {x}\")\n"
                + "    case Point(x, y):\n"
                + "        print(f\"Not on the diagonal\")";
        parseLegalDocStr(s);
    }

    public void testMatchStmt6() {
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
    }
}
