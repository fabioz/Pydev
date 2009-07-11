package org.python.pydev.editor;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PyPartitionScanner;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.actions.PyFormatStd;
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.ui.ColorCache;

public class FormatAndStyleRangeHelper{
    
    
    
    /**
     * This method will format the passed string with the passed standard and create style ranges for the returned
     * string, so that the code is properly seen by the user in a StyledText.
     */
    public static Tuple<String, StyleRange[]> formatAndGetStyleRanges(FormatStd formatStd, String str, ColorCache colorCache){
        PyFormatStd formatter = new PyFormatStd();
        try{
            str = formatter.formatStr(str, formatStd, "\n", false);
        }catch(SyntaxErrorException e){
        }
        FastStringBuffer buf = new FastStringBuffer();
        for(String line:StringUtils.splitInLines(str)){
            buf.append(line);
            char c = buf.lastChar();
            if(c == '\n'){
                buf.deleteLast();
                buf.append("\\n");
                buf.appendN('|', 8); //Just so that the initial presentation is bigger!
                buf.append(c);
            }
        }
        String result = buf.toString();
        
        String finalResult = result.replace(' ', '.').replace('|', ' ');
        
        PyPartitionScanner pyPartitionScanner = new PyPartitionScanner();
        FastPartitioner fastPartitioner = new FastPartitioner(pyPartitionScanner, IPythonPartitions.types);
        Document doc = new Document(result);
        fastPartitioner.connect(doc);
        
        TextPresentation textPresentation = new TextPresentation();
        try{
            IPreferenceStore prefs = colorCache.getPreferences();
            ITypedRegion[] computePartitioning = fastPartitioner.computePartitioning(0, doc.getLength());
            for(ITypedRegion region:computePartitioning){
                String type = region.getType();
                int offset = region.getOffset();
                int len = region.getLength();
                if(IPythonPartitions.PY_DEFAULT.equals(type) || type == null){
                    createDefaultRanges(textPresentation, colorCache, doc, offset, len);
                    
                }else if(IPythonPartitions.PY_COMMENT.equals(type)){
                    textPresentation.addStyleRange(
                            new StyleRange(
                                    offset, 
                                    len, 
                                    colorCache.getNamedColor(PydevEditorPrefs.COMMENT_COLOR), 
                                    null,
                                    prefs.getInt(PydevEditorPrefs.COMMENT_STYLE))
                            );   
                    
                }else if(IPythonPartitions.PY_BACKQUOTES.equals(type)){
                    textPresentation.addStyleRange(
                            new StyleRange(
                                    offset, 
                                    len, 
                                    colorCache.getNamedColor(PydevEditorPrefs.BACKQUOTES_COLOR), 
                                    null,
                                    prefs.getInt(PydevEditorPrefs.BACKQUOTES_STYLE))
                    );   
                    
                }else if(IPythonPartitions.PY_MULTILINE_STRING1.equals(type)||
                        IPythonPartitions.PY_MULTILINE_STRING2.equals(type)||
                        IPythonPartitions.PY_SINGLELINE_STRING1.equals(type)||
                        IPythonPartitions.PY_SINGLELINE_STRING2.equals(type)
                        ){
                    textPresentation.addStyleRange(
                            new StyleRange(
                                    offset, 
                                    len, 
                                    colorCache.getNamedColor(PydevEditorPrefs.STRING_COLOR), 
                                    null,
                                    prefs.getInt(PydevEditorPrefs.STRING_STYLE))
                    );   
                }
            }
        }finally{
            fastPartitioner.disconnect();
        }
        
        
        for(int i=0;i<result.length();i++){
            char curr = result.charAt(i);
            if(curr == '\\' && i+1<result.length() && result.charAt(i+1) == 'n'){
                textPresentation.mergeStyleRange(
                        new StyleRange(i, 2, colorCache.getColor(new RGB(180,180,180)), null));
                i+=1;
            }else if(curr == ' '){
                int finalI=i;
                for(;finalI<result.length() && result.charAt(finalI) == ' ';finalI++){
                    //just iterate (the finalI will have the right value at the end).
                }
                textPresentation.mergeStyleRange(
                        new StyleRange(i, finalI-i, colorCache.getColor(new RGB(180,180,180)), null));
                
            }
        }

        ArrayList<StyleRange> list = new ArrayList<StyleRange>();
        Iterator<StyleRange> it = textPresentation.getAllStyleRangeIterator();
        while(it.hasNext()){
            list.add(it.next());
        }
        StyleRange[] ranges = list.toArray(new StyleRange[list.size()]);
        return new Tuple<String, StyleRange[]>(finalResult, ranges);
    }

    private static void createDefaultRanges(
            TextPresentation textPresentation, 
            ColorCache colorCache, 
            Document doc, 
            int partitionOffset, 
            int partitionLen
            ){
        
        PyCodeScanner scanner = new PyCodeScanner(colorCache);
        scanner.setRange(doc, partitionOffset, partitionLen);
        
        IToken nextToken = scanner.nextToken();
        while(!nextToken.isEOF()){
            Object data = nextToken.getData();
            if(data instanceof TextAttribute){
                TextAttribute textAttribute = (TextAttribute) data;
                int offset = scanner.getTokenOffset();
                int len = scanner.getTokenLength();
                Color foreground = textAttribute.getForeground();
                Color background = textAttribute.getBackground();
                textPresentation.addStyleRange(
                        new StyleRange(
                                offset, 
                                len, 
                                foreground, 
                                background,
                                textAttribute.getStyle())
                        );
                
            }
            nextToken = scanner.nextToken();
        }
    }

}
