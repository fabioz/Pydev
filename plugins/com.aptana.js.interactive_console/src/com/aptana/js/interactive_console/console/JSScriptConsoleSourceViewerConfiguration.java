package com.aptana.js.interactive_console.console;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.python.pydev.editor.codecompletion.PyContentAssistant;

/**
 * Configuration for the source viewer.
 */
public class JSScriptConsoleSourceViewerConfiguration extends
		SourceViewerConfiguration {

	public static final String PARTITION_TYPE = IDocument.DEFAULT_CONTENT_TYPE;

	private ITextHover hover;

	private ContentAssistant contentAssist;

	private IQuickAssistAssistant quickAssist;

	public JSScriptConsoleSourceViewerConfiguration(ITextHover hover,
			ContentAssistant contentAssist, IQuickAssistAssistant quickAssist) {
		this.hover = hover;
		this.contentAssist = contentAssist;
		this.quickAssist = quickAssist;
	}

	public int getTabWidth(ISourceViewer sourceViewer) {
		return 4;
	}

	public ITextHover getTextHover(ISourceViewer sv, String contentType) {
		return hover;
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { PARTITION_TYPE };
	}

	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		contentAssist.setInformationControlCreator(this
				.getInformationControlCreator(sourceViewer));
		return contentAssist;
	}

	@Override
	public IQuickAssistAssistant getQuickAssistAssistant(
			ISourceViewer sourceViewer) {
		quickAssist.setInformationControlCreator(this
				.getInformationControlCreator(sourceViewer));
		return quickAssist;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#
	 * getInformationControlCreator(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public IInformationControlCreator getInformationControlCreator(
			ISourceViewer sourceViewer) {
		return PyContentAssistant.createInformationControlCreator(sourceViewer);
	}

}
