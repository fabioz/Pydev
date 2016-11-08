package org.python.pydev.editor;

import org.eclipse.jface.text.TextAttribute;
import org.python.pydev.ui.ColorAndStyleCache;

public class ColorCacheAndStyleForTesting extends ColorAndStyleCache {

    public ColorCacheAndStyleForTesting() {
        super(null);
    }

    public static class TextAttr extends TextAttribute {

        public String data;

        public TextAttr(String data) {
            super(null);
            this.data = data;
        }

    }

    /*[[[cog
    import cog
    
    template = '''
    @Override
    public TextAttribute get%sTextAttribute() {
        return new TextAttr("%s");
    }'''
    
    for s in ('self', 'code', 'decorator', 'number', 'class_name', 'func_name', 'comment', 'backquotes', 'string', 'unicode', 'keyword', 'parens', 'operators', 'docstring_markup'):
        
        cog.outl(template % (s.title().replace('_', ''), s))
    
    ]]]*/

    @Override
    public TextAttribute getSelfTextAttribute() {
        return new TextAttr("self");
    }

    @Override
    public TextAttribute getCodeTextAttribute() {
        return new TextAttr("code");
    }

    @Override
    public TextAttribute getDecoratorTextAttribute() {
        return new TextAttr("decorator");
    }

    @Override
    public TextAttribute getNumberTextAttribute() {
        return new TextAttr("number");
    }

    @Override
    public TextAttribute getClassNameTextAttribute() {
        return new TextAttr("class_name");
    }

    @Override
    public TextAttribute getFuncNameTextAttribute() {
        return new TextAttr("func_name");
    }

    @Override
    public TextAttribute getCommentTextAttribute() {
        return new TextAttr("comment");
    }

    @Override
    public TextAttribute getBackquotesTextAttribute() {
        return new TextAttr("backquotes");
    }

    @Override
    public TextAttribute getStringTextAttribute() {
        return new TextAttr("string");
    }

    @Override
    public TextAttribute getUnicodeTextAttribute() {
        return new TextAttr("unicode");
    }

    @Override
    public TextAttribute getKeywordTextAttribute() {
        return new TextAttr("keyword");
    }

    @Override
    public TextAttribute getParensTextAttribute() {
        return new TextAttr("parens");
    }

    @Override
    public TextAttribute getOperatorsTextAttribute() {
        return new TextAttr("operators");
    }

    @Override
    public TextAttribute getDocstringMarkupTextAttribute() {
        return new TextAttr("docstring_markup");
    }
    //[[[end]]]

}
