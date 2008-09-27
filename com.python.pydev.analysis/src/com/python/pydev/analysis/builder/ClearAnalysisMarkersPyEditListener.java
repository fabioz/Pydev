package com.python.pydev.analysis.builder;

import java.util.ListResourceBundle;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.IPyEditListener3;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;

/**
 * When the editor is disposed, if needed this class will remove the markers from the related
 * file (if no other editor is still editing the same file) and will remove the hash from the
 * additional info.
 * 
 * @author Fabio
 */
public class ClearAnalysisMarkersPyEditListener implements IPyEditListener, IPyEditListener3{

    @Override
    public void onCreateActions(ListResourceBundle resources, PyEdit edit, IProgressMonitor monitor) {
        
    }

    @Override
    public void onDispose(PyEdit edit, IProgressMonitor monitor) {
        IEditorInput input = edit.getEditorInput();
        //remove the markers if we want problems only in the active editor.
        removeMarkersFromInput(input);
    }


    @Override
    public void onSave(PyEdit edit, IProgressMonitor monitor) {
        
    }

    @Override
    public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor) {
        
    }

    @Override
    public void onInputChanged(PyEdit edit, IEditorInput oldInput, IEditorInput input, IProgressMonitor monitor) {
        removeMarkersFromInput(oldInput);
    }

    
    /**
     * Removes the markers from the given input
     * 
     * @param input the input that has a related resource that should have markers removed
     */
    private void removeMarkersFromInput(IEditorInput input) {
        if(input != null && PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor()){
            IFile relatedFile = (IFile) input.getAdapter(IFile.class);
            
            PythonNature nature = PythonNature.getPythonNature(relatedFile);
            if(nature != null){
                String moduleName = nature.resolveModule(relatedFile);
                if(moduleName != null){
                    AbstractAdditionalInterpreterInfo info = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature);
                    if(info != null){
                        //clear the hash
                        info.setLastModificationHash(moduleName, new byte[0]);
                    }
                }
            }
            if(relatedFile != null && relatedFile.exists()){
                //when disposing, remove all markers
                AnalysisRunner.deleteMarkers(relatedFile);
            }
        }
    }

}
