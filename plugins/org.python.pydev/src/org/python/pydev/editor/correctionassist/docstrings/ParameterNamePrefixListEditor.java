package org.python.pydev.editor.correctionassist.docstrings;

import java.util.LinkedList;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * @author olof
 * Using null ("\0") as string separator
 */
public class ParameterNamePrefixListEditor extends ListEditor {

    public ParameterNamePrefixListEditor(String name, String labelText, Composite parent) {
        super(name, labelText, parent);
    }
    
    @Override
    protected String createList(String[] items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items)
        {
            sb.append(item);
            sb.append("\0");
        }
        return sb.toString();
    }

    @Override
    protected String getNewInputObject() {
        InputDialog d = new InputDialog(getShell(), 
                "Type doctag generation", 
                "Enter a parameter prefix",
                null, 
                null);
        d.open();
        return d.getValue();
    }

    @Override
    protected String[] parseString(String stringList) {
        LinkedList<String> items = new LinkedList<String>();
        StringTokenizer st = new StringTokenizer(stringList, "\0");
        
        while (st.hasMoreTokens())
        {
            items.add(st.nextToken());
        }
        String prefixesList[] = new String[items.size()];
        
        return items.toArray(prefixesList); 
    }
}
         
