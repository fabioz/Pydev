package org.python.pydev.refactoring.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ModulesKey;
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

	private static final String NL = "\n";

	private static final String INIT = "__init__.py";

	private IModulesManager moduleManager;

	public PythonModuleManager(IPythonNature nature) {
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

	private ModuleAdapter getModuleAdapterFromFile(File file) throws Throwable {
		if (file != null && file.getName().compareTo(INIT) != 0 && file.exists()) {
			Document doc = new Document(getFileContent(new FileInputStream(file)));
			if (doc != null && doc.getLength() > 0) {
				return VisitorFactory.createModuleAdapter(this, file, doc);
			}
		}
		return null;
	}

	private String getFileContent(InputStream stream) {
		try {
			StringBuilder contentBuilder = new StringBuilder();
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				contentBuilder.append(line).append(NL);
			}
			return contentBuilder.toString();
		} catch (IOException e) {
		}
		return "";
	}
}
