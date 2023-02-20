package com.python.pydev.analysis.refactoring.quick_fixes;

import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.ReplaceEdit;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.utils.PlatformUtils;

import com.python.pydev.analysis.refactoring.quick_fixes.AddTokenAndImportStatement.ComputedInfo;

import junit.framework.TestCase;

public class AddTokenAndImportStatementTest extends TestCase {

    public void checkImport(String baseDoc, String importToAdd, String expectedDoc, int maxCols) throws Exception {

        Document document = new Document(baseDoc);

        char trigger = '\n';
        int offset = 0;
        boolean addLocalImport = false;
        boolean addLocalImportsOnTopOfMethod = false;
        boolean groupImports = true;

        AddTokenAndImportStatement stmt = new AddTokenAndImportStatement(document, trigger, offset, addLocalImport,
                addLocalImportsOnTopOfMethod,
                groupImports, maxCols);

        String realImportRep = importToAdd;
        int fReplacementOffset = document.getLineLength(0);
        int fLen = 0;
        String indentString = "";
        String fReplacementString = "";
        boolean appliedWithTrigger = false;
        int importLen = 0;

        ComputedInfo computedInfo = new ComputedInfo(realImportRep, fReplacementOffset, fLen, indentString,
                fReplacementString, appliedWithTrigger, importLen, document);
        stmt.createTextEdit(computedInfo);

        for (ReplaceEdit edit : computedInfo.replaceEdit) {
            try {
                edit.apply(document);
            } catch (Exception e) {
                Log.log(e);
            }
        }

        assertEquals(expectedDoc, document.get());
    }

    public void checkLocalImport(String baseDoc, String importToAdd, String expectedDoc,
            boolean addLocalImportsOnTopOfMethod) throws Exception {

        Document document = new Document(baseDoc);

        char trigger = '\n';
        int offset = baseDoc.length();
        boolean addLocalImport = true;
        boolean groupImports = true;

        AddTokenAndImportStatement stmt = new AddTokenAndImportStatement(document, trigger, offset, addLocalImport,
                addLocalImportsOnTopOfMethod,
                groupImports, 80);

        String realImportRep = importToAdd;
        int fReplacementOffset = offset;
        int fLen = 0;
        String indentString = "    ";
        String fReplacementString = "";
        boolean appliedWithTrigger = false;
        int importLen = 0;

        ComputedInfo computedInfo = new ComputedInfo(realImportRep, fReplacementOffset, fLen, indentString,
                fReplacementString, appliedWithTrigger, importLen, document);
        stmt.createTextEdit(computedInfo);

        for (ReplaceEdit edit : computedInfo.replaceEdit) {
            try {
                edit.apply(document);
            } catch (Exception e) {
                Log.log(e);
            }
        }

        assertEquals(expectedDoc, document.get());
    }

    // test all possibility cases
    public void testAllCases() throws Exception {

        // -- TESTS WITH STANDARD COLS VALUE -- //

        String delimiter = PlatformUtils.isWindowsPlatform() ? "\r\n" : "\n";

        // normal situation
        checkImport("from math import ceil", "from math import sqrt", "from math import ceil, sqrt", 80);

        // test with multiple spaces and tabs -> style conserves
        checkImport("from math import ceil          ", "from math import sqrt",
                "from math import ceil, sqrt          ", 80);

        // test with "(xxxx)"
        checkImport("from math import (ceil)", "from math import sqrt", "from math import (ceil, sqrt)", 80);

        // test with "(xxxx)" and spaces after
        checkImport("from math import (ceil)   ", "from math import sqrt", "from math import (ceil, sqrt)   ", 80);

        //!! test with "(xxxx)" and multiple spaces and tabs
        checkImport("from math import (ceil       )", "from math import sqrt", "from math import (ceil, sqrt       )",
                80);

        //!! test with "(xxxx)", spaces after and multiple spaces and tabs
        checkImport("from math import (ceil       )   ", "from math import sqrt",
                "from math import (ceil, sqrt       )   ", 80);

        // test with "(xxxx)" and comma
        checkImport("from math import (ceil,)", "from math import sqrt", "from math import (ceil, sqrt,)", 80);

        // test with "(xxxx)", spaces after and comma
        checkImport("from math import (ceil,)    ", "from math import sqrt", "from math import (ceil, sqrt,)    ", 80);

        // -- END OF TESTS WITH STANDARD COLS VALUE -- //

        // -- TESTS WITH COLS LIMIT EXCEDEED WITH COMMENTS NEXT LINE -- //

        // normal situation
        checkImport("from math import ceil\n#foo", "from math import sqrt", "from math import ceil,\\\nsqrt\n#foo", 20);

        // normal situation and spaces after
        checkImport("from math import ceil\n     #foo", "from math import sqrt",
                "from math import ceil,\\\nsqrt\n     #foo",
                20);

        // test with multiple spaces and tabs
        checkImport("from math import ceil          \n#foo", "from math import sqrt",
                "from math import ceil,\\\nsqrt          \n#foo", 20);

        // test with multiple spaces and tabs and spaces after
        checkImport("from math import ceil          \n     #foo", "from math import sqrt",
                "from math import ceil,\\\nsqrt          \n     #foo", 20);

        // test with comma
        checkImport("from math import ceil,\n#foo", "from math import sqrt", "from math import ceil,\\\nsqrt,\n#foo",
                20);

        // test with comma and spaces after
        checkImport("from math import ceil,\n     #foo", "from math import sqrt",
                "from math import ceil,\\\nsqrt,\n     #foo",
                20);

        // test with "(xxxx)"
        checkImport("from math import (ceil)\n#foo", "from math import sqrt", "from math import (ceil,\nsqrt)\n#foo",
                20);

        // test with "(xxxx)" and spaces after
        checkImport("from math import (ceil)\n     #foo", "from math import sqrt",
                "from math import (ceil,\nsqrt)\n     #foo",
                20);

        //!! test with "(xxxx)" and multiple spaces and tabs
        checkImport("from math import (ceil       )\n#foo", "from math import sqrt",
                "from math import (ceil,\nsqrt       )\n#foo",
                20);

        // test with "(xxxx)" and multiple spaces and tabs and spaces after
        checkImport("from math import (ceil       )\n     #foo", "from math import sqrt",
                "from math import (ceil,\nsqrt       )\n     #foo",
                20);

        // test with "(xxxx)" and comma
        checkImport("from math import (ceil,)\n#foo", "from math import sqrt", "from math import (ceil,\nsqrt,)\n#foo",
                20);

        // test with "(xxxx)" and comma and spaces after
        checkImport("from math import (ceil,)\n     #foo", "from math import sqrt",
                "from math import (ceil,\nsqrt,)\n     #foo",
                20);

        // -- END OF TESTS WITH COLS EXCEDEED WITH COMMENTS NEXT LINE -- //

        // -- TESTS WITH AN IMPORT COMMENTED AND A LINE BREAK -- //

        // normal situation
        checkImport("#from math import ceil\nfrom math import sqrt", "from math import fmod",
                "#from math import ceil\nfrom math import sqrt, fmod",
                80);

        // test with multiple spaces and tabs
        checkImport("#from math import ceil  \nfrom math import sqrt   ", "from math import fmod",
                "#from math import ceil  \nfrom math import sqrt, fmod   ",
                80);

        // test with comma
        checkImport("#from math import ceil\nfrom math import sqrt,", "from math import fmod",
                "#from math import ceil\nfrom math import sqrt, fmod,",
                80);

        // test with "(xxxx)"
        checkImport("#from math import ceil\nfrom math import (sqrt)", "from math import fmod",
                "#from math import ceil\nfrom math import (sqrt, fmod)",
                80);

        // test with "(xxxx)" and spaces after
        checkImport("#from math import ceil\nfrom math import (sqrt)  ", "from math import fmod",
                "#from math import ceil\nfrom math import (sqrt, fmod)  ",
                80);

        // test with "(xxxx)" and multiple spaces and tabs
        checkImport("#from math import ceil\nfrom math import (sqrt    )", "from math import fmod",
                "#from math import ceil\nfrom math import (sqrt, fmod    )",
                80);

        // test with "(xxxx)", spaces after and multiple spaces and tabs
        checkImport("#from math import ceil\nfrom math import (sqrt   )    ", "from math import fmod",
                "#from math import ceil\nfrom math import (sqrt, fmod   )    ",
                80);

        // test with "(xxxx)" and comma
        checkImport("#from math import ceil\nfrom math import (sqrt,)", "from math import fmod",
                "#from math import ceil\nfrom math import (sqrt, fmod,)",
                80);

        // test with "(xxxx)", spaces after and comma
        checkImport("#from math import ceil\nfrom math import (sqrt,)    ", "from math import fmod",
                "#from math import ceil\nfrom math import (sqrt, fmod,)    ",
                80);

        // -- END OF TESTS WITH AN IMPORT COMMENTED AND LINE BREAK -- //

        // -- TESTS WITH COMMENTS INLINE -- //

        // normal situation
        checkImport("from math import ceil#foo", "from math import sqrt", "from math import ceil, sqrt#foo", 80);

        // test with multiple spaces or tabs
        checkImport("from math import ceil          #foo", "from math import sqrt",
                "from math import ceil, sqrt          #foo", 80);

        // test with comma
        checkImport("from math import ceil,#foo", "from math import sqrt", "from math import ceil, sqrt,#foo", 80);

        // test with "(xxxx)"
        checkImport("from math import (ceil)#foo", "from math import sqrt", "from math import (ceil, sqrt)#foo", 80);

        // test with "(xxxx)", spaces and tabs before comment and spaces and tabs before comment
        checkImport("from math import (ceil)     #foo", "from math import sqrt",
                "from math import (ceil, sqrt)     #foo",
                80);

        //!! test with "(xxxx)" and multiple spaces and tabs
        checkImport("from math import (ceil       )#foo", "from math import sqrt",
                "from math import (ceil, sqrt       )#foo",
                80);

        //!! test with "(xxxx)", spaces and tabs before comment and multiple spaces and tabs
        checkImport("from math import (ceil       )   #foo", "from math import sqrt",
                "from math import (ceil, sqrt       )   #foo",
                80);

        // test with "(xxxx)" and comma
        checkImport("from math import (ceil,)#foo", "from math import sqrt", "from math import (ceil, sqrt,)#foo", 80);

        // test with "(xxxx)", spaces and tabs before comment and comma
        checkImport("from math import (ceil,)   #foo", "from math import sqrt", "from math import (ceil, sqrt,)   #foo",
                80);

        // -- END OF TESTS WITH COMMENTS INLINE -- //

        // -- TESTS WITH COLS LIMIT EXCEDEED -- //

        // normal situation
        checkImport("from math import ceil", "from math import sqrt", "from math import ceil,\\" + delimiter + "sqrt",
                20);

        // test with multiple spaces and tabs -> style conserves
        checkImport("from math import ceil      ", "from math import sqrt",
                "from math import ceil,\\" + delimiter + "sqrt      ", 20);

        // test with "(xxxx)"
        checkImport("from math import (ceil)", "from math import sqrt", "from math import (ceil," + delimiter + "sqrt)",
                20);

        // test with "(xxxx)" and spaces after
        checkImport("from math import (ceil)   ", "from math import sqrt",
                "from math import (ceil," + delimiter + "sqrt)   ", 20);

        //!! test with "(xxxx)" and multiple spaces and tabs
        checkImport("from math import (ceil       )", "from math import sqrt",
                "from math import (ceil," + delimiter + "sqrt       )",
                20);

        //!! test with "(xxxx)", spaces after and multiple spaces and tabs
        checkImport("from math import (ceil       )   ", "from math import sqrt",
                "from math import (ceil," + delimiter + "sqrt       )   ", 20);

        // test with "(xxxx)" and comma
        checkImport("from math import (ceil,)", "from math import sqrt",
                "from math import (ceil," + delimiter + "sqrt,)", 20);

        // test with "(xxxx)", spaces after and comma
        checkImport("from math import (ceil,)    ", "from math import sqrt",
                "from math import (ceil," + delimiter + "sqrt,)    ",
                20);

        // -- END OF TESTS WITH COLS LIMIT EXCEDEED -- //

        // -- TESTS WITH COLS LIMIT EXCEDEED WITH COMMENTS INLINE -- //

        // normal situation
        checkImport("from math import ceil#foo", "from math import sqrt",
                "from math import ceil,\\" + delimiter + "sqrt#foo", 20);

        // test with multiple spaces and tabs -> style conserves
        checkImport("from math import ceil      #foo", "from math import sqrt",
                "from math import ceil,\\" + delimiter + "sqrt      #foo", 20);

        // test with "(xxxx)"
        checkImport("from math import (ceil)#foo", "from math import sqrt",
                "from math import (ceil," + delimiter + "sqrt)#foo", 20);

        // test with "(xxxx)" and spaces after
        checkImport("from math import (ceil)   #foo", "from math import sqrt",
                "from math import (ceil," + delimiter + "sqrt)   #foo", 20);

        //!! test with "(xxxx)" and multiple spaces and tabs
        checkImport("from math import (ceil       )#foo", "from math import sqrt",
                "from math import (ceil," + delimiter + "sqrt       )#foo",
                20);

        //!! test with "(xxxx)", spaces after and multiple spaces and tabs
        checkImport("from math import (ceil       )   #foo", "from math import sqrt",
                "from math import (ceil," + delimiter + "sqrt       )   #foo", 20);

        // test with "(xxxx)" and comma
        checkImport("from math import (ceil,)#foo", "from math import sqrt",
                "from math import (ceil," + delimiter + "sqrt,)#foo",
                20);

        // test with "(xxxx)", spaces after and comma
        checkImport("from math import (ceil,)    #foo", "from math import sqrt",
                "from math import (ceil," + delimiter + "sqrt,)    #foo",
                20);

        // -- TESTS WITH COLS LIMIT EXCEDEED WITH COMMENTS INLINE -- //

        // -- TESTS WITH MULTI-LINE DECLARATIONS -- //

        // normal situation
        checkImport("from math import ceil,\\" + delimiter + "sqrt", "from math import fmod",
                "from math import ceil,\\" + delimiter + "sqrt, fmod", 80);

        // test with multiple spaces and tabs -> style conserves
        checkImport("from math import ceil,\\" + delimiter + "sqrt      ", "from math import fmod",
                "from math import ceil,\\" + delimiter + "sqrt, fmod      ", 80);

        // test with "(xxxx)"
        checkImport("from math import (ceil," + delimiter + "sqrt)", "from math import fmod",
                "from math import (ceil," + delimiter + "sqrt, fmod)", 80);

        // test with "(xxxx)" and spaces after
        checkImport("from math import (ceil," + delimiter + "sqrt)   ", "from math import fmod",
                "from math import (ceil," + delimiter + "sqrt, fmod)   ", 80);

        //!! test with "(xxxx)" and multiple spaces and tabs
        checkImport("from math import (ceil," + delimiter + "sqrt       )", "from math import fmod",
                "from math import (ceil," + delimiter + "sqrt, fmod       )",
                80);

        //!! test with "(xxxx)", spaces after and multiple spaces and tabs
        checkImport("from math import (ceil," + delimiter + "sqrt       )   ", "from math import fmod",
                "from math import (ceil," + delimiter + "sqrt, fmod       )   ", 80);

        // test with "(xxxx)" and comma
        checkImport("from math import (ceil," + delimiter + "sqrt,)", "from math import fmod",
                "from math import (ceil," + delimiter + "sqrt, fmod,)", 80);

        // test with "(xxxx)", spaces after and comma
        checkImport("from math import (ceil," + delimiter + "sqrt,)    ", "from math import fmod",
                "from math import (ceil," + delimiter + "sqrt, fmod,)    ",
                80);

        // -- END OF TESTS WITH MULTI-LINE DECLARATIONS -- //

        // -- TESTS WITH MULTI-LINE DECLARATIONS AND COLS LIMIT EXCEDEED -- //

        // normal situation
        checkImport("from math import ceil,\\" + delimiter + "sqrt", "from math import fmod",
                "from math import ceil,\\" + delimiter + "sqrt, fmod", 20);

        // test with multiple spaces and tabs -> style conserves
        checkImport("from math import ceil,\\" + delimiter + "sqrt      ", "from math import fmod",
                "from math import ceil,\\" + delimiter + "sqrt, fmod      ", 20);

        // test with "(xxxx)"
        checkImport("from math import (ceil," + delimiter + "sqrt)", "from math import fmod",
                "from math import (ceil," + delimiter + "sqrt, fmod)", 20);

        // test with "(xxxx)" and spaces after
        checkImport("from math import (ceil," + delimiter + "sqrt)   ", "from math import fmod",
                "from math import (ceil," + delimiter + "sqrt, fmod)   ", 20);

        //!! test with "(xxxx)" and multiple spaces and tabs
        checkImport("from math import (ceil," + delimiter + "sqrt       )", "from math import fmod",
                "from math import (ceil," + delimiter + "sqrt, fmod       )",
                20);

        //!! test with "(xxxx)", spaces after and multiple spaces and tabs
        checkImport("from math import (ceil," + delimiter + "sqrt       )   ", "from math import fmod",
                "from math import (ceil," + delimiter + "sqrt, fmod       )   ", 20);

        // test with "(xxxx)" and comma
        checkImport("from math import (ceil," + delimiter + "sqrt,)", "from math import fmod",
                "from math import (ceil," + delimiter + "sqrt, fmod,)", 20);

        // test with "(xxxx)", spaces after and comma
        checkImport("from math import (ceil," + delimiter + "sqrt,)    ", "from math import fmod",
                "from math import (ceil," + delimiter + "sqrt, fmod,)    ",
                20);

        // -- END OF TESTS WITH MULTI-LINE DECLARATIONS AND COLS LIMIT EXCEDEED -- //

    }

    public void testGroupImport() throws Exception {
        String baseDoc = "from mod2 import (ActivityInfoDict, WorkspaceInfoDict, PackageInfoDict,\r\n" +
                "    ActionResultDict, UploadActivityParamsDict,\r\n" +
                "    CloudLoginParamsDict,\r\n" +
                "    ActionResult)";
        String expectedDoc = "from mod2 import (ActivityInfoDict, WorkspaceInfoDict, PackageInfoDict,\r\n" +
                "    ActionResultDict, UploadActivityParamsDict,\r\n" +
                "    CloudLoginParamsDict,\r\n" +
                "    ActionResult, UploadNewActivityParamsDict)";
        checkLocalImport(baseDoc, "from mod2 import UploadNewActivityParamsDict", expectedDoc, true);
    }

    public void testGroupImport2() throws Exception {
        String baseDoc = "from mod2 import (\r\n" +
                "    ActivityInfoDict,\r\n" +
                "    WorkspaceInfoDict,\r\n" +
                "    PackageInfoDict,\r\n" +
                "    ActionResultDict,\r\n" +
                "    UploadActivityParamsDict,\r\n" +
                "    CloudLoginParamsDict,\r\n" +
                "    ActionResult\r\n" +
                "    )";
        String expectedDoc = "from mod2 import (\r\n" +
                "    ActivityInfoDict,\r\n" +
                "    WorkspaceInfoDict,\r\n" +
                "    PackageInfoDict,\r\n" +
                "    ActionResultDict,\r\n" +
                "    UploadActivityParamsDict,\r\n" +
                "    CloudLoginParamsDict,\r\n" +
                "    ActionResult, UploadNewActivityParamsDict\r\n" +
                "    )";
        checkLocalImport(baseDoc, "from mod2 import UploadNewActivityParamsDict", expectedDoc, true);
    }

    public void testGroupImport3() throws Exception {
        String baseDoc = "from mod2 import \\\r\n" +
                "    ActivityInfoDict,\\\r\n" +
                "    WorkspaceInfoDict,\\\r\n" +
                "    PackageInfoDict,\\\r\n" +
                "    ActionResultDict,\\\r\n" +
                "    UploadActivityParamsDict,\\\r\n" +
                "    CloudLoginParamsDict,\\\r\n" +
                "    ActionResult\r\n" +
                "";
        String expectedDoc = "from mod2 import \\\r\n" +
                "    ActivityInfoDict,\\\r\n" +
                "    WorkspaceInfoDict,\\\r\n" +
                "    PackageInfoDict,\\\r\n" +
                "    ActionResultDict,\\\r\n" +
                "    UploadActivityParamsDict,\\\r\n" +
                "    CloudLoginParamsDict,\\\r\n" +
                "    ActionResult, UploadNewActivityParamsDict\r\n" +
                "";
        checkLocalImport(baseDoc, "from mod2 import UploadNewActivityParamsDict", expectedDoc, true);
    }

    public void testGroupImport4() throws Exception {
        String baseDoc = "from mod2 import \\\r\n" +
                "    ActivityInfoDict,\\\r\n" +
                "    WorkspaceInfoDict,\\\r\n" +
                "    PackageInfoDict,\\\r\n" +
                "    ActionResultDict,\\\r\n" +
                "    UploadActivityParamsDict,\\\r\n" +
                "    CloudLoginParamsDict,\\\r\n" +
                "    ActionResult,\\";
        String expectedDoc = "from mod2 import \\\r\n" +
                "    ActivityInfoDict,\\\r\n" +
                "    WorkspaceInfoDict,\\\r\n" +
                "    PackageInfoDict,\\\r\n" +
                "    ActionResultDict,\\\r\n" +
                "    UploadActivityParamsDict,\\\r\n" +
                "    CloudLoginParamsDict,\\\r\n" +
                "    ActionResult, UploadNewActivityParamsDict,\\";
        checkLocalImport(baseDoc, "from mod2 import UploadNewActivityParamsDict", expectedDoc, true);
    }

    public void testGroupImport5() throws Exception {
        String baseDoc = "from mod2 import \\\r\n" +
                "    ActivityInfoDict,\\\r\n" +
                "    WorkspaceInfoDict,\\\r\n" +
                "    PackageInfoDict,\\\r\n" +
                "    ActionResultDict,\\\r\n" +
                "    UploadActivityParamsDict,\\\r\n" +
                "    CloudLoginParamsDict,\\\r\n" +
                "    ActionResult,\\\r\n" +
                "";
        String expectedDoc = "from mod2 import \\\r\n" +
                "    ActivityInfoDict,\\\r\n" +
                "    WorkspaceInfoDict,\\\r\n" +
                "    PackageInfoDict,\\\r\n" +
                "    ActionResultDict,\\\r\n" +
                "    UploadActivityParamsDict,\\\r\n" +
                "    CloudLoginParamsDict,\\\r\n" +
                "    ActionResult, UploadNewActivityParamsDict,\\\r\n" +
                "";
        checkLocalImport(baseDoc, "from mod2 import UploadNewActivityParamsDict", expectedDoc, true);
    }

    public void testGroupImport6() throws Exception {
        String baseDoc = "from mod2 import ActivityInfoDict  # comment \\";
        String expectedDoc = "from mod2 import ActivityInfoDict, UploadNewActivityParamsDict  # comment \\";
        checkLocalImport(baseDoc, "from mod2 import UploadNewActivityParamsDict", expectedDoc, true);
    }

    public void testGroupImport7() throws Exception {
        String baseDoc = "from mod2 import ActivityInfoDict,\\\r\n" +
                "    WorkspaceInfoDict,\\\r\n" +
                "    PackageInfoDict,\\\r\n" +
                "    ActionResultDict,\\\r\n" +
                "    UploadActivityParamsDict,\\\r\n" +
                "    CloudLoginParamsDict,\\\r\n" +
                "    ActionResult  # comment \\";
        String expectedDoc = "from mod2 import ActivityInfoDict,\\\r\n" +
                "    WorkspaceInfoDict,\\\r\n" +
                "    PackageInfoDict,\\\r\n" +
                "    ActionResultDict,\\\r\n" +
                "    UploadActivityParamsDict,\\\r\n" +
                "    CloudLoginParamsDict,\\\r\n" +
                "    ActionResult, UploadNewActivityParamsDict  # comment \\";
        checkLocalImport(baseDoc, "from mod2 import UploadNewActivityParamsDict", expectedDoc, true);
    }

    public void testGroupImport8() throws Exception {
        String baseDoc = "from mod2 import ActivityInfoDict,\\\r\n" +
                "    WorkspaceInfoDict,\\\r\n" +
                "    PackageInfoDict,\\\r\n" +
                "    ActionResultDict,\\\r\n" +
                "    UploadActivityParamsDict,\\\r\n" +
                "    CloudLoginParamsDict,\\\r\n" +
                "    ActionResult  # comment \\\r\n"
                + "";
        String expectedDoc = "from mod2 import ActivityInfoDict,\\\r\n" +
                "    WorkspaceInfoDict,\\\r\n" +
                "    PackageInfoDict,\\\r\n" +
                "    ActionResultDict,\\\r\n" +
                "    UploadActivityParamsDict,\\\r\n" +
                "    CloudLoginParamsDict,\\\r\n" +
                "    ActionResult, UploadNewActivityParamsDict  # comment \\\r\n" +
                "";
        checkLocalImport(baseDoc, "from mod2 import UploadNewActivityParamsDict", expectedDoc, true);
    }

    public void testGroupImport9() throws Exception {
        String baseDoc = "from mod2 import ActivityInfoDict,\\\r\n" +
                "    WorkspaceInfoDict,\\\r\n" +
                "    PackageInfoDict,\\\r\n" +
                "    ActionResultDict,\\\r\n" +
                "    UploadActivityParamsDict,\\\r\n" +
                "    CloudLoginParamsDict,\\\r\n" +
                "    ActionResult,\\  # some comment\r\n" +
                "";
        String expectedDoc = "from mod2 import ActivityInfoDict,\\\r\n" +
                "    WorkspaceInfoDict,\\\r\n" +
                "    PackageInfoDict,\\\r\n" +
                "    ActionResultDict,\\\r\n" +
                "    UploadActivityParamsDict,\\\r\n" +
                "    CloudLoginParamsDict,\\\r\n" +
                "    ActionResult, UploadNewActivityParamsDict,\\  # some comment\r\n" +
                "";
        checkLocalImport(baseDoc, "from mod2 import UploadNewActivityParamsDict", expectedDoc, true);
    }

    public void testGroupImport10() throws Exception {
        String baseDoc = "from mod2 import (\r\n" +
                "    ActivityInfoDict,\r\n" +
                "    WorkspaceInfoDict,\r\n" +
                "    PackageInfoDict,\r\n" +
                "    ActionResultDict,\r\n" +
                "    UploadActivityParamsDict,\r\n" +
                "    CloudLoginParamsDict,\r\n" +
                "    ActionResult) #some comment";
        String expectedDoc = "from mod2 import (\r\n" +
                "    ActivityInfoDict,\r\n" +
                "    WorkspaceInfoDict,\r\n" +
                "    PackageInfoDict,\r\n" +
                "    ActionResultDict,\r\n" +
                "    UploadActivityParamsDict,\r\n" +
                "    CloudLoginParamsDict,\r\n" +
                "    ActionResult, UploadNewActivityParamsDict) #some comment";
        checkLocalImport(baseDoc, "from mod2 import UploadNewActivityParamsDict", expectedDoc, true);
    }

    public void testGroupImport11() throws Exception {
        String baseDoc = "from mod2 import (\r\n" +
                "    ActivityInfoDict,\r\n" +
                "    WorkspaceInfoDict,\r\n" +
                "    PackageInfoDict,\r\n" +
                "    ActionResultDict,\r\n" +
                "    UploadActivityParamsDict,\r\n" +
                "    CloudLoginParamsDict,\r\n" +
                "    ActionResult\r\n" +
                "    ) #some comment";
        String expectedDoc = "from mod2 import (\r\n" +
                "    ActivityInfoDict,\r\n" +
                "    WorkspaceInfoDict,\r\n" +
                "    PackageInfoDict,\r\n" +
                "    ActionResultDict,\r\n" +
                "    UploadActivityParamsDict,\r\n" +
                "    CloudLoginParamsDict,\r\n" +
                "    ActionResult, UploadNewActivityParamsDict\r\n" +
                "    ) #some comment";
        checkLocalImport(baseDoc, "from mod2 import UploadNewActivityParamsDict", expectedDoc, true);
    }

    public void testGroupImport12() throws Exception {
        String baseDoc = "from mod2 import (ActivityInfoDict #some comment\r\n" +
                ")";
        String expectedDoc = "from mod2 import (ActivityInfoDict, UploadNewActivityParamsDict #some comment\r\n" +
                ")";
        checkLocalImport(baseDoc, "from mod2 import UploadNewActivityParamsDict", expectedDoc, true);
    }

    public void testLocalImport() throws Exception {
        String baseDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "\r\n" +
                "def method2():\r\n" +
                "    x = \"\"\"\r\n" +
                "some string\r\n" +
                "\"\"\"\r\n" +
                "    sys";
        String expectedDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "\r\n" +
                "def method2():\r\n" +
                "    import sys\r\n" +
                "    x = \"\"\"\r\n" +
                "some string\r\n" +
                "\"\"\"\r\n" +
                "    sys";

        checkLocalImport(baseDoc, "import sys", expectedDoc, true);
    }

    public void testLocalImport2() throws Exception {
        String baseDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "\r\n" +
                "def method2():\r\n" +
                "\r\n" +
                "    s = '''\r\n" +
                "testing %(x)s\r\n" +
                "''' % (dict(x=10))\r\n" +
                "\r\n" +
                "    sys";
        String expectedDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "\r\n" +
                "def method2():\r\n" +
                "\r\n" +
                "    import sys\r\n" +
                "    s = '''\r\n" +
                "testing %(x)s\r\n" +
                "''' % (dict(x=10))\r\n" +
                "\r\n" +
                "    sys";

        checkLocalImport(baseDoc, "import sys", expectedDoc, true);
    }

    public void testLocalImport3() throws Exception {
        String baseDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "def method2(): \r\n" +
                "    \r\n" +
                "    \r\n" +
                "    s = '''\r\n" +
                "\\''' something\r\n" +
                "'''\r\n" +
                "\r\n" +
                "    sys";
        String expectedDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "def method2(): \r\n" +
                "    \r\n" +
                "    \r\n" +
                "    import sys\r\n" +
                "    s = '''\r\n" +
                "\\''' something\r\n" +
                "'''\r\n" +
                "\r\n" +
                "    sys";

        checkLocalImport(baseDoc, "import sys", expectedDoc, true);
    }

    public void testLocalImport4() throws Exception {
        String baseDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "def method2():\r\n" +
                "\r\n" +
                "\r\n" +
                "    s = '''\r\n" +
                "\"\"\" something\r\n" +
                "'''\r\n" +
                "\r\n" +
                "    sys";
        String expectedDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "def method2():\r\n" +
                "\r\n" +
                "\r\n" +
                "    import sys\r\n" +
                "    s = '''\r\n" +
                "\"\"\" something\r\n" +
                "'''\r\n" +
                "\r\n" +
                "    sys";

        checkLocalImport(baseDoc, "import sys", expectedDoc, true);
    }

    public void testLocalImport5() throws Exception {
        String baseDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "def method2():\r\n" +
                "\r\n" +
                "\r\n" +
                "    s = '''\r\n" +
                "# this is a literal'''\r\n" +
                "\r\n" +
                "    sys";
        String expectedDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "def method2():\r\n" +
                "\r\n" +
                "\r\n" +
                "    import sys\r\n" +
                "    s = '''\r\n" +
                "# this is a literal'''\r\n" +
                "\r\n" +
                "    sys";

        checkLocalImport(baseDoc, "import sys", expectedDoc, true);
    }

    public void testLocalImportAfterDocString1() throws Exception {
        String baseDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "\r\n" +
                "def method2():\r\n" +
                "    \"\"\"Some docstring here\"\"\"\r\n" +
                "\r\n" +
                "    sys";
        String expectedDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "\r\n" +
                "def method2():\r\n" +
                "    \"\"\"Some docstring here\"\"\"\r\n" +
                "    import sys\r\n" +
                "\r\n" +
                "    sys";

        checkLocalImport(baseDoc, "import sys", expectedDoc, true);
    }

    public void testLocalImportAfterDocString2() throws Exception {
        String baseDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "\r\n" +
                "def method2():\r\n" +
                "    \"\"\"Some docstring here\"\"\"\r\n" +
                "\r\n" +
                "    x = \"\"\"\r\n" +
                "some string\r\n" +
                "\"\"\"\r\n" +
                "    sys";
        String expectedDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "\r\n" +
                "def method2():\r\n" +
                "    \"\"\"Some docstring here\"\"\"\r\n" +
                "    import sys\r\n" +
                "\r\n" +
                "    x = \"\"\"\r\n" +
                "some string\r\n" +
                "\"\"\"\r\n" +
                "    sys";

        checkLocalImport(baseDoc, "import sys", expectedDoc, true);
    }

    public void testLocalImportAfterDocString3() throws Exception {
        String baseDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "\r\n" +
                "def method2():\r\n" +
                "    \"\"\"\r\n" +
                "    Some\r\n" +
                "    docstring\r\n" +
                "    here\r\n" +
                "    \"\"\"\r\n" +
                "\r\n" +
                "    x = \"\"\"\r\n" +
                "some string\r\n" +
                "\"\"\"\r\n" +
                "    sys";
        String expectedDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "\r\n" +
                "def method2():\r\n" +
                "    \"\"\"\r\n" +
                "    Some\r\n" +
                "    docstring\r\n" +
                "    here\r\n" +
                "    \"\"\"\r\n" +
                "    import sys\r\n" +
                "\r\n" +
                "    x = \"\"\"\r\n" +
                "some string\r\n" +
                "\"\"\"\r\n" +
                "    sys";

        checkLocalImport(baseDoc, "import sys", expectedDoc, true);
    }

    public void testLocalImportAfterSpacesAndTabs() throws Exception {
        String baseDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "\r\n" +
                "def method2():\r\n" +
                "\r\n" +
                "\r\n" +
                "\r\n" +
                "    x = \"\"\"\r\n" +
                "some string\r\n" +
                "\"\"\"\r\n" +
                "    sys";
        String expectedDoc = "def method():\r\n" +
                "    pass\r\n" +
                "\r\n" +
                "\r\n" +
                "def method2():\r\n" +
                "\r\n" +
                "\r\n" +
                "\r\n" +
                "    import sys\r\n" +
                "    x = \"\"\"\r\n" +
                "some string\r\n" +
                "\"\"\"\r\n" +
                "    sys";

        checkLocalImport(baseDoc, "import sys", expectedDoc, true);
    }
}