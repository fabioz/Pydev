package org.python.pydev.dltk.console.ui;

import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;

/**
 * Subclassed simply to make the hide available to the external world.
 * 
 * @author fabioz
 */
public class ScriptConsoleContentAssistant extends ContentAssistant implements IContentAssistant{

	/**
	 * Available for stopping the completion.
	 */
	@Override
	public void hide(){
		super.hide();
	}
}
