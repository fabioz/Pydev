package com.python.pydev.analysis.refactoring.quick_fixes;

import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.ReplaceEdit;
import org.python.pydev.core.log.Log;

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

    // test all possibility cases
    public void testAllCases() throws Exception {

        // -- TESTS WITH STANDARD COLS VALUE -- //

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
        checkImport("from math import ceil", "from math import sqrt", "from math import ceil,\\\r\nsqrt", 20);

        // test with multiple spaces and tabs -> style conserves
        checkImport("from math import ceil      ", "from math import sqrt",
                "from math import ceil,\\\r\nsqrt      ", 20);

        // test with "(xxxx)"
        checkImport("from math import (ceil)", "from math import sqrt", "from math import (ceil,\r\nsqrt)", 20);

        // test with "(xxxx)" and spaces after
        checkImport("from math import (ceil)   ", "from math import sqrt", "from math import (ceil,\r\nsqrt)   ", 20);

        //!! test with "(xxxx)" and multiple spaces and tabs
        checkImport("from math import (ceil       )", "from math import sqrt",
                "from math import (ceil,\r\nsqrt       )",
                20);

        //!! test with "(xxxx)", spaces after and multiple spaces and tabs
        checkImport("from math import (ceil       )   ", "from math import sqrt",
                "from math import (ceil,\r\nsqrt       )   ", 20);

        // test with "(xxxx)" and comma
        checkImport("from math import (ceil,)", "from math import sqrt", "from math import (ceil,\r\nsqrt,)", 20);

        // test with "(xxxx)", spaces after and comma
        checkImport("from math import (ceil,)    ", "from math import sqrt", "from math import (ceil,\r\nsqrt,)    ",
                20);

        // -- END OF TESTS WITH COLS LIMIT EXCEDEED -- //

        // -- TESTS WITH COLS LIMIT EXCEDEED WITH COMMENTS INLINE -- //

        // normal situation
        checkImport("from math import ceil#foo", "from math import sqrt", "from math import ceil,\\\r\nsqrt#foo", 20);

        // test with multiple spaces and tabs -> style conserves
        checkImport("from math import ceil      #foo", "from math import sqrt",
                "from math import ceil,\\\r\nsqrt      #foo", 20);

        // test with "(xxxx)"
        checkImport("from math import (ceil)#foo", "from math import sqrt", "from math import (ceil,\r\nsqrt)#foo", 20);

        // test with "(xxxx)" and spaces after
        checkImport("from math import (ceil)   #foo", "from math import sqrt",
                "from math import (ceil,\r\nsqrt)   #foo", 20);

        //!! test with "(xxxx)" and multiple spaces and tabs
        checkImport("from math import (ceil       )#foo", "from math import sqrt",
                "from math import (ceil,\r\nsqrt       )#foo",
                20);

        //!! test with "(xxxx)", spaces after and multiple spaces and tabs
        checkImport("from math import (ceil       )   #foo", "from math import sqrt",
                "from math import (ceil,\r\nsqrt       )   #foo", 20);

        // test with "(xxxx)" and comma
        checkImport("from math import (ceil,)#foo", "from math import sqrt", "from math import (ceil,\r\nsqrt,)#foo",
                20);

        // test with "(xxxx)", spaces after and comma
        checkImport("from math import (ceil,)    #foo", "from math import sqrt",
                "from math import (ceil,\r\nsqrt,)    #foo",
                20);

        // -- TESTS WITH COLS LIMIT EXCEDEED WITH COMMENTS INLINE -- //

        // -- TESTS WITH MULTI-LINE DECLARATIONS -- //

        // normal situation
        checkImport("from math import ceil,\\\r\nsqrt", "from math import fmod",
                "from math import ceil,\\\r\nsqrt,\\\r\nfmod", 80);

        // test with multiple spaces and tabs -> style conserves
        checkImport("from math import ceil,\\\r\nsqrt      ", "from math import fmod",
                "from math import ceil,\\\r\nsqrt,\\\r\nfmod      ", 80);

        // test with "(xxxx)"
        checkImport("from math import (ceil,\r\nsqrt)", "from math import fmod",
                "from math import (ceil,\r\nsqrt,\r\nfmod)", 80);

        // test with "(xxxx)" and spaces after
        checkImport("from math import (ceil,\r\nsqrt)   ", "from math import fmod",
                "from math import (ceil,\r\nsqrt,\r\nfmod)   ", 80);

        //!! test with "(xxxx)" and multiple spaces and tabs
        checkImport("from math import (ceil,\r\nsqrt       )", "from math import fmod",
                "from math import (ceil,\r\nsqrt,\r\nfmod       )",
                80);

        //!! test with "(xxxx)", spaces after and multiple spaces and tabs
        checkImport("from math import (ceil,\r\nsqrt       )   ", "from math import fmod",
                "from math import (ceil,\r\nsqrt,\r\nfmod       )   ", 80);

        // test with "(xxxx)" and comma
        checkImport("from math import (ceil,\r\nsqrt,)", "from math import fmod",
                "from math import (ceil,\r\nsqrt,\r\nfmod,)", 80);

        // test with "(xxxx)", spaces after and comma
        checkImport("from math import (ceil,\r\nsqrt,)    ", "from math import fmod",
                "from math import (ceil,\r\nsqrt,\r\nfmod,)    ",
                80);

        // -- END OF TESTS WITH MULTI-LINE DECLARATIONS -- //

        // -- TESTS WITH MULTI-LINE DECLARATIONS AND COLS LIMIT EXCEDEED -- //

        // normal situation
        checkImport("from math import ceil,\\\r\nsqrt", "from math import fmod",
                "from math import ceil,\\\r\nsqrt,\\\r\nfmod", 20);

        // test with multiple spaces and tabs -> style conserves
        checkImport("from math import ceil,\\\r\nsqrt      ", "from math import fmod",
                "from math import ceil,\\\r\nsqrt,\\\r\nfmod      ", 20);

        // test with "(xxxx)"
        checkImport("from math import (ceil,\r\nsqrt)", "from math import fmod",
                "from math import (ceil,\r\nsqrt,\r\nfmod)", 20);

        // test with "(xxxx)" and spaces after
        checkImport("from math import (ceil,\r\nsqrt)   ", "from math import fmod",
                "from math import (ceil,\r\nsqrt,\r\nfmod)   ", 20);

        //!! test with "(xxxx)" and multiple spaces and tabs
        checkImport("from math import (ceil,\r\nsqrt       )", "from math import fmod",
                "from math import (ceil,\r\nsqrt,\r\nfmod       )",
                20);

        //!! test with "(xxxx)", spaces after and multiple spaces and tabs
        checkImport("from math import (ceil,\r\nsqrt       )   ", "from math import fmod",
                "from math import (ceil,\r\nsqrt,\r\nfmod       )   ", 20);

        // test with "(xxxx)" and comma
        checkImport("from math import (ceil,\r\nsqrt,)", "from math import fmod",
                "from math import (ceil,\r\nsqrt,\r\nfmod,)", 20);

        // test with "(xxxx)", spaces after and comma
        checkImport("from math import (ceil,\r\nsqrt,)    ", "from math import fmod",
                "from math import (ceil,\r\nsqrt,\r\nfmod,)    ",
                20);

        // -- END OF TESTS WITH MULTI-LINE DECLARATIONS AND COLS LIMIT EXCEDEED -- //

    }
}