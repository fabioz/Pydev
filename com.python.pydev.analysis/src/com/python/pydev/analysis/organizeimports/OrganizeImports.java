/*
 * Created on 25/09/2005
 */
package com.python.pydev.analysis.organizeimports;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.IOrganizeImports;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.builder.AnalysisRunner;

public class OrganizeImports implements IOrganizeImports{

    
    public void performArrangeImports(PySelection ps, PyEdit edit) {
        SimpleNode ast = edit.getAST();
        if(ast == null){
            return; //we need it to be correctly parsed with an ast to be able to do it...
        }
        
        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        try {
            ast.accept(visitor);
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
        List<ASTEntry> availableImports = visitor.getAsList(new Class[]{ImportFrom.class, Import.class});
        
        
        
        PySourceViewer s = edit.getPySourceViewer();
        
        Iterable<IMarker> markers = s.getMarkerIteratable();

        ArrayList<Tuple<IMarker, ASTEntry>> unusedImportsMarkers = new ArrayList<Tuple<IMarker, ASTEntry>>();
        ArrayList<Tuple<IMarker, ASTEntry>> unusedWildImportsMarkers =  new ArrayList<Tuple<IMarker, ASTEntry>>();
        ArrayList<Tuple<IMarker, ASTEntry>> unresolvedImportsMarkers =  new ArrayList<Tuple<IMarker, ASTEntry>>();

        ArrayList<IMarker> undefinedVariablesMarkers = new ArrayList<IMarker>();
        
        //get the markers we are interested in and the related ast elements
        
        for (IMarker marker : markers) {
            try {
                String type = marker.getType();
                if(type != null && type.equals(AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER)){
                    Integer attribute = marker.getAttribute(AnalysisRunner.PYDEV_ANALYSIS_TYPE, -1 );
                    if (attribute != null){
                        if(attribute.equals(IAnalysisPreferences.TYPE_UNUSED_IMPORT)){
                            unusedImportsMarkers.add(new Tuple<IMarker, ASTEntry>(marker, getImportEntry(marker, availableImports)));
                            
                        }else if(attribute.equals(IAnalysisPreferences.TYPE_UNUSED_WILD_IMPORT)){
                            unusedWildImportsMarkers.add(new Tuple<IMarker, ASTEntry>(marker, getImportEntry(marker, availableImports)));
                            
                        }else if(attribute.equals(IAnalysisPreferences.TYPE_UNRESOLVED_IMPORT)){
                            unresolvedImportsMarkers.add(new Tuple<IMarker, ASTEntry>(marker, getImportEntry(marker, availableImports)));
                            
                        }else if(attribute.equals(IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE)){
                            undefinedVariablesMarkers.add(marker);
                        }
                    }

                }
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }

    }

    /**
     * This gives an entry correspondent to the marker.
     *  
     * @param marker the marker we want to get the import from
     * @param availableImports the available imports
     * 
     * @return the entry with the node for the import
     */
    private ASTEntry getImportEntry(IMarker marker, List<ASTEntry> availableImports) {
        return null;
    }

    /**
     * In this function we have to pass through all the imports available and make sure that:
     * 
     * - only the used tokens remain in the import
     * 
     * - any import that imports something we don't know is removed
     * 
     * - tokens that are not defined in the document should get the available tokens in the workspace and if it
     *   exists, it should add an import for that token.
     *    
     *   - If more than one token has the same name, it should be presented to the user to choose which should be used
     *     the class to look at when doing the selection is org.eclipse.jdt.internal.ui.dialogs.MultiElementListSelectionDialog
     *     the class that uses it is org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation 
     *     
     *     another interesting selection pane is org.eclipse.ui.dialogs.TwoPaneElementSelector, altough it is probably
     *     not the most applicable in this case.
     *     
     * the action that jdt uses for this is org.eclipse.jdt.ui.actions.OrganizeImportsAction
     * 
     */
    public void performArrangeImports(PySelection ps, IMarker marker, PyEdit edit) throws BadLocationException, CoreException {
        SimpleNode ast = edit.getAST();
        if(ast == null){
            //we need the ast to look for the imports... (the generated markers will be matched against them)
            return;
        }
        
        Integer attribute = marker.getAttribute(AnalysisRunner.PYDEV_ANALYSIS_TYPE, -1 );
//        IDocument doc = ps.getDoc();
        if (attribute != null && attribute.equals(IAnalysisPreferences.TYPE_UNUSED_IMPORT)){
            Integer start = (Integer) marker.getAttribute(IMarker.CHAR_START);
            Integer end = (Integer) marker.getAttribute(IMarker.CHAR_END);
            ps.setSelection(start, end);
            ps.deleteSelection();
        }
    }


}
