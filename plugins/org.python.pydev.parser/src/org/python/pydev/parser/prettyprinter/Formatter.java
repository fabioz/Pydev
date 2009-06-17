/*
 * Created on Feb 17, 2006
 */
package org.python.pydev.parser.prettyprinter;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;

public class Formatter implements IFormatter{

    public void formatAll(IDocument doc, IPyEdit edit, boolean isOpenedFile, boolean throwSyntaxError) throws SyntaxErrorException {
            Tuple<SimpleNode, Throwable> objects;
            try{
                objects = PyParser.reparseDocument(new PyParser.ParserInfo(doc, true, edit.getPythonNature()));
            }catch(MisconfigurationException e1){
                Log.log(e1);
                return;
            }
            
            if(objects.o2!= null){
                if(throwSyntaxError){
                    throw new SyntaxErrorException();
                }
                
            } else if (objects.o1 != null) {
                SimpleNode n = objects.o1;
                final WriterEraser stringWriter = new WriterEraser();
                PrettyPrinterPrefs prefs = new PrettyPrinterPrefs("\n");
                PrettyPrinter printer = new PrettyPrinter(prefs, stringWriter);
                try{
                    n.accept(printer);
                    doc.set(stringWriter.getBuffer().toString());
                }catch(Exception e){
                    Log.log(e);
                }
            }
    }

    public void formatSelection(IDocument doc, int startLine, int endLineIndex, IPyEdit edit, PySelection ps) {
    }

}
