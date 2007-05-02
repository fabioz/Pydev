package org.python.pydev.refactoring.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.REF;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;

/**
 * This class does depend on PyDev to resolve a module name. The module was resolved (alias etc.) by ModuleAdapter method. Double insertion
 * of ModuleAdapters is detected (represented files are equal)
 * 
 * @author Ueli Kistler
 * 
 */
public class PythonModuleManager {

	private static final String INIT = "__init__.py";

	private IModulesManager moduleManager;

	private transient IPythonNature nature;

	public PythonModuleManager(IPythonNature nature) {
		this.nature = nature;
		this.moduleManager = nature.getAstManager().getModulesManager();
	}

	public Set<ModuleAdapter> resolveModule(File currentFile, FQIdentifier identifier) {

		if (identifier == null || currentFile == null || (!(currentFile.exists())) || identifier.getProbableModuleName().length() == 0)
			return new HashSet<ModuleAdapter>();

		SortedMap<ModulesKey, ModulesKey> modulesStartingWith = resolveModules(identifier);

		return extractModuleAdapter(modulesStartingWith);

	}

	private Set<ModuleAdapter> extractModuleAdapter(SortedMap<ModulesKey, ModulesKey> modulesStartingWith) {
		Set<ModuleAdapter> resolvedModules = new HashSet<ModuleAdapter>();
		for (ModulesKey key : modulesStartingWith.keySet()) {
			try {
				if (key.file != null) {
					ModuleAdapter moduleAdapter = getModuleAdapterFromFile(key.file);
					if (moduleAdapter != null) {
						resolvedModules.add(moduleAdapter);
					}
				}
			} catch (Throwable e) {
			}
		}
		return resolvedModules;
	}

	private SortedMap<ModulesKey, ModulesKey> resolveModules(FQIdentifier identifier) {
		SortedMap<ModulesKey, ModulesKey> modulesStartingWith = moduleManager.getAllModulesStartingWith(identifier.getProbableModuleName());

		if (modulesStartingWith.size() == 0)
			modulesStartingWith = moduleManager.getAllModulesStartingWith(identifier.getModule());
		return modulesStartingWith;
	}
	
	
	/**
	 * Creates an adapter for a given file.
	 */
	private ModuleAdapter getModuleAdapterFromFile(File file) throws Throwable {
		if (file != null && !file.getName().equals(INIT) && file.exists()) {
			IDocument doc = getDocFromFile(file);
			if (doc != null && doc.getLength() > 0) {
				return VisitorFactory.createModuleAdapter(this, file, doc, nature);
			}
		}
		return null;
	}

	/**
	 * If we're testing, we want to return docs only with \n, otherwise, we want it to be based
	 * on the actual document from the user
	 */
	public static boolean TESTING = false;
	
	public static IDocument getDocFromFile(File file)  {
		boolean loadIfNotInWorkspace = true;
		if(TESTING){
			loadIfNotInWorkspace = false;
		}
		IDocument doc = null;
		try {
			doc = REF.getDocFromFile(file, loadIfNotInWorkspace);
		} catch (IOException e1) {
			//ignore (will remain null)
		}
		if(doc == null){
			try {
				doc = new Document(getFileContent(new FileInputStream(file)));
			} catch (FileNotFoundException e) {
				return null;
			}
		}
		return doc;
	}
	
	private static String getFileContent(InputStream stream) {
        if(!TESTING){
            throw new RuntimeException("Should only call this method in tests.");
        }
		try {
			StringBuilder contentBuilder = new StringBuilder();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				contentBuilder.append(line).append('\n');
			}
			return contentBuilder.toString();
		} catch (IOException e) {
		}
		return "";
	}


	public void setIModuleManager(IModulesManager moduleManager) {
		this.moduleManager = moduleManager;
	}


	public IModulesManager getIModuleManager() {
		return moduleManager;
	}
}
