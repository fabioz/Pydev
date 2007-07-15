package com.python.pydev.analysis.tabnanny;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.autoedit.IIndentPrefs;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.messages.IMessage;
import com.python.pydev.analysis.messages.Message;

public class TabNanny{

    private IDocument doc;
    private IAnalysisPreferences analysisPrefs;
    private IIndentPrefs indentPrefs;

    public TabNanny(IDocument doc, IAnalysisPreferences analysisPrefs, String moduleName, IIndentPrefs indentPrefs) {
        this.doc = doc;
        this.analysisPrefs = analysisPrefs;
        this.indentPrefs = indentPrefs;
    }
    
    public List<IMessage> analyzeDoc() {
        ArrayList<IMessage> ret = new ArrayList<IMessage>();
        
        int tabWidth = indentPrefs.getTabWidth();
        boolean useSpaces = indentPrefs.getUseSpaces();
        
        TabNannyDocIterator it = new TabNannyDocIterator(doc);
        while(it.hasNext()){
            Tuple<String, Integer> indentation = it.next();
            if(useSpaces){
                if(indentation.o1.indexOf('\t') != -1){
                    PySelection sel = new PySelection(doc, indentation.o2);
                    
                    int startLine = sel.getLineOfOffset()+1;
                    int startCol = sel.getCursorColumn()+1;
                    ret.add(new Message(IAnalysisPreferences.TYPE_INDENTATION_PROBLEM, "Mixed Indentation: Tab found", 
                            startLine,
                            startLine,
                            startCol,
                            startCol+indentation.o1.length(),
                            analysisPrefs));
                }
            }
        }
        return ret;
    }
    
}