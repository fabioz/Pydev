package org.python.pydev.dltk.console.codegen;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.viewers.IStructuredSelection;

@SuppressWarnings("rawtypes")
public class ScriptConsoleCodeGeneratorFactory implements IAdapterFactory {
	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType == IScriptConsoleCodeGenerator.class) {
			if (adaptableObject instanceof IStructuredSelection) {
				IStructuredSelection selection = (IStructuredSelection) adaptableObject;
				return new StructuredSelectionScriptConsoleCodeGenerator(selection); 				
			}
		}
		return null;
	}

	public Class[] getAdapterList() {
		return new Class[] { IScriptConsoleCodeGenerator.class };
	}
}
