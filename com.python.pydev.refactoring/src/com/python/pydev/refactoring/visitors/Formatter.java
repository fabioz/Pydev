/*
 * Created on Feb 17, 2006
 */
package com.python.pydev.refactoring.visitors;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.IFormatter;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;

public class Formatter implements IFormatter{

    public void formatAll(IDocument doc, PyEdit edit) {
        try {
            Tuple<SimpleNode, Throwable> objects = PyParser.reparseDocument(new PyParser.ParserInfo(doc, true, edit.getPythonNature()));
            if(objects.o2!= null){
              edit.setStatusLineErrorMessage("Format not done: Unable to parse the document correctly");
                
            } else if (objects.o1 != null) {
                SimpleNode n = objects.o1;
                final WriterEraser stringWriter = new WriterEraser();
                PrettyPrinterPrefs prefs = new PrettyPrinterPrefs("\n");
                PrettyPrinter printer = new PrettyPrinter(prefs, stringWriter);
                n.accept(printer);
                doc.set(stringWriter.getBuffer().toString());
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    public void formatSelection(IDocument doc, int startLine, int endLineIndex, PyEdit edit, PySelection ps) {
    }

}
