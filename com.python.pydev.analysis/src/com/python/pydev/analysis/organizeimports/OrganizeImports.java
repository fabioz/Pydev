/*
 * Created on 25/09/2005
 */
package com.python.pydev.analysis.organizeimports;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.IOrganizeImports;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.builder.AnalysisRunner;

public class OrganizeImports implements IOrganizeImports{

    public void performArrangeImports(PySelection ps, PyEdit edit) {
        PySourceViewer s = edit.getPySourceViewer();
        
        Iterable<IMarker> markers = s.getMarkerIteratable();
        for (IMarker marker : markers) {
            try {
                String type = marker.getType();
                if(type != null && type.equals(AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER)){
                    performArrangeImports(ps, marker);
                }
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }

    }

    public void performArrangeImports(PySelection ps, IMarker marker) throws BadLocationException, CoreException {
        Integer attribute = marker.getAttribute(AnalysisRunner.PYDEV_ANALYSIS_TYPE, -1 );
        IDocument doc = ps.getDoc();
        if (attribute != null && attribute.equals(IAnalysisPreferences.TYPE_UNUSED_IMPORT)){
            Integer start = (Integer) marker.getAttribute(IMarker.CHAR_START);
            Integer end = (Integer) marker.getAttribute(IMarker.CHAR_END);
            ps.setSelection(start, end);
            ps.deleteSelection();
        }
    }


}
