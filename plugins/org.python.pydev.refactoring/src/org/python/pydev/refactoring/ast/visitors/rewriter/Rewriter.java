package org.python.pydev.refactoring.ast.visitors.rewriter;

import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterPrefsV2;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterV2;
import org.python.pydev.refactoring.ast.adapters.AdapterPrefs;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;

/**
 * This class just provides an interface for using the rewriter.
 */
public final class Rewriter  {
    
    
    public static String reparsed(String source, AdapterPrefs adapterPrefs) {
        try {
            SimpleNode root = VisitorFactory.getRootNodeFromString(source);
            return createSourceFromAST(root, adapterPrefs);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    
    public static String createSourceFromAST(SimpleNode root, AdapterPrefs adapterPrefs) {
        return createSourceFromAST(root, false, adapterPrefs);
    }


    public static String createSourceFromAST(
            SimpleNode root, String endLineDelim, IGrammarVersionProvider versionProvider) {
        return createSourceFromAST(root, false, new AdapterPrefs(endLineDelim, versionProvider));
    }
    
    
    public static String createSourceFromAST(SimpleNode root, boolean ignoreComments, AdapterPrefs adapterPrefs) {
        IIndentPrefs indentPrefs = DefaultIndentPrefs.get();
        PrettyPrinterPrefsV2 prettyPrinterPrefs = new PrettyPrinterPrefsV2(
                adapterPrefs.endLineDelim, indentPrefs.getIndentationString(), adapterPrefs.versionProvider);
        
        prettyPrinterPrefs.setSpacesAfterComma(1);
        prettyPrinterPrefs.setSpacesBeforeComment(1);
        prettyPrinterPrefs.setLinesAfterMethod(1);
        prettyPrinterPrefs.setLinesAfterClass(2);
        prettyPrinterPrefs.setLinesAfterSuite(1);
        
        PrettyPrinterV2 printer = new PrettyPrinterV2(prettyPrinterPrefs);
        try {
            return printer.print(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
    }
    

}
