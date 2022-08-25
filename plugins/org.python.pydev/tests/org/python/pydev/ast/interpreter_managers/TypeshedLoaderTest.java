package org.python.pydev.ast.interpreter_managers;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterPrefsV2;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterV2;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.utils.PlatformUtils;

public class TypeshedLoaderTest extends PyParserTestBase {

    public void testFixBuiltinsKeepElse() throws Exception {
        List<String> l = new ArrayList<String>();
        InterpreterInfo info = new InterpreterInfo("3.8", "test", l);

        String doc = "import sys\n"
                + "if sys.version_info == (3, 7):\n"
                + "    a = 'remove'\n"
                + "else:\n"
                + "    a = 'keep'\n"
                + "\n"
                + "if sys.version_info < (3, 7):\n"
                + "    a = 'remove'\n"
                + "elif sys.version_info < (3, 8):\n"
                + "    a = 'remove'\n"
                + "else:\n"
                + "    a = 'keep'\n"
                + "\n"
                + "if sys.version_info < (3, 7):\n"
                + "    a = 'remove'\n"
                + "elif sys.version_info > (3, 8):\n"
                + "    a = 'keep'\n"
                + "else:\n"
                + "    a = 'remove'\n"
                + "";

        SimpleNode ast = parseLegalDocStr(doc);
        TypeshedLoader.fixBuiltinsAST(ast, info.getModulesManager(), info);

        PrettyPrinterPrefsV2 prefs = new PrettyPrinterPrefsV2("\n", "    ", versionProvider);
        PrettyPrinterV2 printer = new PrettyPrinterV2(prefs);
        String result = printer.print(ast);
        int count = StringUtils.count(result, "'keep'");
        assertTrue(count >= 3);
        assertEquals(StringUtils.count(doc, "'keep'"), count);

        assertTrue(result.indexOf("'remove") == -1);
        assertTrue(result.contains("None = None"));
        assertTrue(result.contains("False = False"));
        assertTrue(result.contains("True = True"));
        assertTrue(result.contains("__builtins__ = Any"));

    }

    public void testFixBuiltinsRemoveTypeCheckOnly() throws Exception {
        List<String> l = new ArrayList<String>();
        InterpreterInfo info = new InterpreterInfo("3.8", "test", l);

        String doc = "@type_check_only\n"
                + "class remove: ...\n"
                + "\n"
                + "class keep: pass\n"
                + "";

        SimpleNode ast = parseLegalDocStr(doc);
        TypeshedLoader.fixBuiltinsAST(ast, info.getModulesManager(), info);

        PrettyPrinterPrefsV2 prefs = new PrettyPrinterPrefsV2("\n", "    ", versionProvider);
        PrettyPrinterV2 printer = new PrettyPrinterV2(prefs);
        String result = printer.print(ast);
        int count = StringUtils.count(result, "keep");
        assertTrue(count >= 1);
        assertEquals(StringUtils.count(doc, "keep"), count);
        assertTrue(result.indexOf("remove") == -1);
    }

    public void testFixCheckPlatform() throws Exception {
        if (!PlatformUtils.isWindowsPlatform()) {
            return;
        }
        List<String> l = new ArrayList<String>();
        InterpreterInfo info = new InterpreterInfo("3.8", "test", l);

        String doc = "import sys\n"
                + "if sys.platform == \"win32\":\n"
                + "    a = 'keep'\n"
                + "else:\n"
                + "    a = 'remove'\n"
                + "\n"
                + "if sys.platform != \"win32\":\n"
                + "    a = 'remove'\n"
                + "else:\n"
                + "    a = 'keep'\n"
                + "\n"
                + "";

        SimpleNode ast = parseLegalDocStr(doc);
        TypeshedLoader.fixAST(ast, info.getModulesManager(), info);

        PrettyPrinterPrefsV2 prefs = new PrettyPrinterPrefsV2("\n", "    ", versionProvider);
        PrettyPrinterV2 printer = new PrettyPrinterV2(prefs);
        String result = printer.print(ast);
        int count = StringUtils.count(result, "'keep'");
        assertTrue(count >= 1);
        assertEquals(StringUtils.count(doc, "'keep'"), count);
        assertTrue(result.indexOf("'remove") == -1);
    }

    public void testFixBuiltinsRemoveIf() throws Exception {
        List<String> l = new ArrayList<String>();
        InterpreterInfo info = new InterpreterInfo("3.8", "test", l);

        String doc = "import sys\n"
                // Eq (never matches)
                // Note: version_info is something as (3, 8, 1, 'final', 0), so, this doesn't really match.
                + "if sys.version_info == (3, 7):\n"
                + "    a = 'remove'\n"
                + "if sys.version_info == (3, 8):\n"
                + "    a = 'remove'\n"
                // NotEq (matches)
                + "if sys.version_info != (3, 7):\n"
                + "    a = 'keep'\n"
                + "if sys.version_info != (3, 8):\n"
                + "    a = 'keep'\n"
                // Gte
                + "if sys.version_info >= (3, 7):\n"
                + "    a = 'keep'\n"
                + "if sys.version_info >= (3, 8):\n"
                + "    a = 'keep'\n"
                + "if sys.version_info >= (3, 9):\n"
                + "    a = 'remove'\n"
                // Gt
                + "if sys.version_info > (3, 7):\n"
                + "    a = 'keep'\n"
                // Note: version_info is something as (3, 8, 1, 'final', 0), so, this actually matches.
                + "if sys.version_info > (3, 8):\n"
                + "    a = 'keep'\n"
                + "if sys.version_info > (3, 9):\n"
                + "    a = 'remove'\n"
                // Lte
                + "if sys.version_info <= (3, 7):\n"
                + "    a = 'remove'\n"
                + "if sys.version_info <= (3, 8):\n"
                + "    a = 'remove'\n"
                + "if sys.version_info <= (3, 9):\n"
                + "    a = 'keep'\n"
                // Lt
                + "if sys.version_info < (3, 7):\n"
                + "    a = 'remove'\n"
                + "if sys.version_info < (3, 8):\n"
                + "    a = 'remove'\n"
                + "if sys.version_info < (3, 9):\n"
                + "    a = 'keep'\n"

                + "class Foo:\n"
                + "    if sys.version_info < (3, 8):\n"
                + "        a = 'remove'\n"
                + "    def method():\n"
                + "        if sys.version_info < (3, 8):\n"
                + "            a = 'remove'\n"

                + "\n";

        SimpleNode ast = parseLegalDocStr(doc);
        TypeshedLoader.fixBuiltinsAST(ast, info.getModulesManager(), info);

        PrettyPrinterPrefsV2 prefs = new PrettyPrinterPrefsV2("\n", "    ", versionProvider);
        PrettyPrinterV2 printer = new PrettyPrinterV2(prefs);
        String result = printer.print(ast);
        // System.out.println(result);
        int count = StringUtils.count(result, "'keep'");
        assertTrue(count > 3);
        assertEquals(StringUtils.count(doc, "'keep'"), count);

        assertTrue(result.indexOf("'remove") == -1);
        assertTrue(result.contains("None = None"));
        assertTrue(result.contains("False = False"));
        assertTrue(result.contains("True = True"));
        assertTrue(result.contains("__builtins__ = Any"));
    }

    public void testFixTypingAlias() throws Exception {
        List<String> l = new ArrayList<String>();
        InterpreterInfo info = new InterpreterInfo("3.8", "test", l);

        String doc = "Dict = _Alias()\n"
                + "Set = _Alias()\n"
                + "DefaultDict = _Alias()\n"
                + "List = _Alias()\n"
                + "";

        SimpleNode ast = parseLegalDocStr(doc);
        TypeshedLoader.fixTypingAST(ast, info.getModulesManager(), info);

        PrettyPrinterPrefsV2 prefs = new PrettyPrinterPrefsV2("\n", "    ", versionProvider);
        PrettyPrinterV2 printer = new PrettyPrinterV2(prefs);
        String result = printer.print(ast);
        assertEquals("import collections\n"
                + "Dict = dict\n"
                + "Set = set\n"
                + "DefaultDict = collections.defaultdict\n"
                + "List = list\n", result);

    }
}
