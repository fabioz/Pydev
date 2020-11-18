package org.python.pydev.editor.actions;

import org.eclipse.jface.text.Document;
import org.python.pydev.ast.formatter.PyFormatter;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.core.formatter.FormatStd.FormatterEnum;
import org.python.pydev.shared_core.resource_stubs.AbstractIFileStub;

import junit.framework.TestCase;

public class PyFormatBlackTest extends TestCase {
    private final class MockIFile extends AbstractIFileStub {
        private final String ext;

        private MockIFile(String ext) {
            this.ext = ext;
        }

        @Override
        public String getFileExtension() {
            return ext;
        }
    }

    static final String SRC = "from typing import (\n"
            + " Union, Dict\n"
            + "  )\n"
            + "def a()->None: \n"
            + " ...\n"
            + "def b(_:Union[Dict[str,str],str])->int: \n"
            + " ...\n"
            + "d = [1, 2, 3]";

    private static boolean DEBUG = false;

    public static void main(String[] args) {
        try {
            PyFormatBlackTest n = new PyFormatBlackTest();
            junit.textui.TestRunner.run(PyFormatBlackTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    private FormatStd black;

    private void checkFormat(String ext, String expected) {
        try {
            Document doc = new Document(SRC);
            PyFormatter.formatAll(doc, new MockPyEdit(new MockIFile(ext)), true, black, false, true);
            String formattedStr = doc.get();

            assertEquals(expected, formattedStr);
        } catch (SyntaxErrorException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        black = new FormatStd();
        black.formatterStyle = FormatterEnum.BLACK;
        black.searchBlackInInterpreter = false;

        //TODO: get black location from config?
        black.blackExecutableLocation = System.getenv("LOCALAPPDATA")
                + "\\Programs\\Python\\Python37\\Scripts\\black.exe";

        System.out.println(black);
    }

    /**
     * Check that py file is formatted using Black's standard format
     * @throws Exception
     */
    public void testPyFormat() throws Exception {
        checkFormat("py", "from typing import Union, Dict\n"
                + "\n"
                + "\n"
                + "def a() -> None:\n"
                + "    ...\n"
                + "\n"
                + "\n"
                + "def b(_: Union[Dict[str, str], str]) -> int:\n"
                + "    ...\n"
                + "\n"
                + "\n"
                + "d = [1, 2, 3]\n");
    }

    /**
     * Check that pyi (python stub) file is formatted using Black's more compact format
     * @throws Exception
     */
    public void testPyiFormat() throws Exception {
        checkFormat("py", "from typing import Union, Dict\n"
                + "\n"
                + "def a() -> None: ...\n"
                + "def b(_: Union[Dict[str, str], str]) -> int: ...\n"
                + "\n"
                + "d = [1, 2, 3]\n");
    }

}
