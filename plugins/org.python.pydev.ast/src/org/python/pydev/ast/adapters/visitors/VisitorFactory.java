package org.python.pydev.ast.adapters.visitors;

import java.io.File;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.adapters.AbstractNodeAdapter;
import org.python.pydev.ast.adapters.AbstractScopeNode;
import org.python.pydev.ast.adapters.ModuleAdapter;
import org.python.pydev.ast.adapters.PythonModuleManager;
import org.python.pydev.ast.adapters.context.AbstractContextVisitor;
import org.python.pydev.ast.adapters.visitors.selection.SelectionException;
import org.python.pydev.ast.adapters.visitors.selection.SelectionExtenderVisitor;
import org.python.pydev.ast.adapters.visitors.selection.SelectionValidationVisitor;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.core.BaseModuleRequest;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IGrammarVersionProvider.AdditionalGrammarVersionsToCheck;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISourceModule;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.preferences.FileTypesPreferences;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.VisitorIF;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.ICoreTextSelection;

public final class VisitorFactory {

    private VisitorFactory() {
    }

    /**
     * Unchecked (because if doing Class.cast, it does not work in java 1.4)
     */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractContextVisitor> T createContextVisitor(Class<T> visitorClass, SimpleNode root,
            ModuleAdapter module, AbstractNodeAdapter parent) {
        try {
            T visitor = (T) visitorClass.getConstructors()[0].newInstance(new Object[] { module, parent });
            root.accept(visitor);
            return visitor;
        } catch (Exception e) {
            throw new CannotCreateContextRuntimeException(e);
        }
    }

    /**
     * Provides a way to find duplicates of a given expression.
     */
    public static FindDuplicatesVisitor createDuplicatesVisitor(ICoreTextSelection selection, SimpleNode nodeToVisit,
            exprType expression, AbstractScopeNode node, IDocument doc) {
        FindDuplicatesVisitor visitor = new FindDuplicatesVisitor(selection, expression, doc);
        try {
            nodeToVisit.accept(visitor);
            visitor.finish();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }

    public static <T extends VisitorIF> T createVisitor(Class<T> visitorClass, SimpleNode root) {
        T visitor = null;
        try {
            visitor = visitorClass.getDeclaredConstructor().newInstance();
            root.accept(visitor);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }

    public static ModuleAdapter createModuleAdapter(PythonModuleManager pythonModuleManager, File file, IDocument doc,
            IPythonNature nature, IGrammarVersionProvider versionProvider) throws Throwable {
        if (file != null && file.exists()) {
            if (FileTypesPreferences.isCythonFile(file.getName())) {
                versionProvider = new IGrammarVersionProvider() {
    
                    @Override
                    public int getGrammarVersion() throws MisconfigurationException {
                        return IPythonNature.GRAMMAR_PYTHON_VERSION_CYTHON;
                    }
    
                    @Override
                    public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions()
                            throws MisconfigurationException {
                        return null;
                    }
                };
            }
            if (pythonModuleManager != null) {
                IModulesManager modulesManager = pythonModuleManager.getIModuleManager();
                if (modulesManager != null) {
                    String modName = modulesManager.resolveModule(FileUtils.getFileAbsolutePath(file));
                    if (modName != null) {
                        IModule module = modulesManager.getModule(modName, nature, true, new BaseModuleRequest(false));
                        if (module instanceof ISourceModule) {
                            SourceModule iSourceModule = (SourceModule) module;
                            if (iSourceModule.parseError != null) {
                                throw iSourceModule.parseError;
                            }
                            return new ModuleAdapter(pythonModuleManager, ((ISourceModule) module), nature, doc);
                        }
                    }
                }
            }
        }
        return new ModuleAdapter(pythonModuleManager, file, doc, org.python.pydev.parser.PyParser.parseSimple(doc, versionProvider), nature);
    }

    public static void validateSelection(ModuleAdapter scope) throws SelectionException {
        SelectionValidationVisitor visitor = null;
        try {
            visitor = new SelectionValidationVisitor();
            scope.getASTNode().accept(visitor);
        } catch (SelectionException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static ICoreTextSelection createSelectionExtension(AbstractScopeNode<?> scope,
            ICoreTextSelection selection) {
        SelectionExtenderVisitor visitor = null;
        try {
            visitor = new SelectionExtenderVisitor(scope.getModule(), selection);
            scope.getASTNode().accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor.getSelection();
    }

}
