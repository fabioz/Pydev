/*
 * Created on Sep 23, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codefolding;

import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.PyEditConfiguration;
import org.python.pydev.editor.correctionassist.PyCorrectionAssistant;


public class PySourceViewer extends ProjectionViewer {

    private PyEditProjection projection;
    private PyCorrectionAssistant fCorrectionAssistant;

    public PySourceViewer(Composite parent, IVerticalRuler ruler, IOverviewRuler overviewRuler, boolean showsAnnotationOverview, int styles, PyEditProjection projection) {
        super(parent, ruler, overviewRuler, showsAnnotationOverview, styles);
        this.projection = projection;
        
    }
    
    public void configure(SourceViewerConfiguration configuration) {
        super.configure(configuration);
        if (configuration instanceof PyEditConfiguration) {
            PyEditConfiguration pyConfiguration = (PyEditConfiguration) configuration;
            fCorrectionAssistant = pyConfiguration.getCorrectionAssistant(this);
            fCorrectionAssistant.install(this);
        }
    }
        
    /* (non-Javadoc)
    }
     * @see org.eclipse.jface.text.source.projection.ProjectionViewer#canDoOperation(int)
     */
    public boolean canDoOperation(int operation) {
        
        if(operation == PyEdit.CORRECTIONASSIST_PROPOSALS){
            return true;
        }
        
        return super.canDoOperation(operation);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.source.projection.ProjectionViewer#doOperation(int)
     */
    public void doOperation(int operation) {
        super.doOperation(operation);
		if (getTextWidget() == null)
			return;
		
		switch (operation) {
			case PyEdit.CORRECTIONASSIST_PROPOSALS:
				String msg= fCorrectionAssistant.showPossibleCompletions();
				projection.setStatusLineErrorMessage(msg);
				return;
		}
    }
}