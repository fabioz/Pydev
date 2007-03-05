package org.python.pydev.refactoring.ast.adapters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISourceModule;
import org.python.pydev.core.IToken;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.BeginOffset;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.EndOffset;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.InitOffset;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.ast.visitors.info.ImportVisitor;
import org.python.pydev.refactoring.core.FQIdentifier;
import org.python.pydev.refactoring.core.PythonModuleManager;

public class ModuleAdapter extends AbstractScopeNode<Module> {

	private List<FQIdentifier> aliasToFQIdentifier;

	private IDocument doc;

	private File file;

	private SortedMap<String, String> importedModules;

	private PythonModuleManager moduleManager;

	private IOffsetStrategy offsetStrategy;

	private ISourceModule sourceModule;

	private IPythonNature nature;

	public ModuleAdapter(PythonModuleManager pm, File file, IDocument doc, Module node, IPythonNature nature) {
		super(null, null, node);
//		Assert.isNotNull(pm); TODO: MAKE THIS ASSERTION TRUE
		this.moduleManager = pm;
		this.file = file;
		this.doc = doc;
		this.aliasToFQIdentifier = null;
		this.importedModules = null;
		this.nature = nature;
	}

	public ModuleAdapter(PythonModuleManager pm, ISourceModule module, IPythonNature nature) {
		super(null, null, (Module) (module).getAst());
//		Assert.isNotNull(pm); TODO: MAKE THIS ASSERTION TRUE
		this.sourceModule = module;
		this.moduleManager = pm;
		this.file = module.getFile();
		this.doc = PythonModuleManager.getDocFromFile(this.file);
		this.aliasToFQIdentifier = null;
		this.importedModules = null;
		this.nature = nature;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ModuleAdapter) {
			String otherPath = ((ModuleAdapter) obj).getFile().getAbsolutePath();
			return this.file.getAbsolutePath().compareToIgnoreCase(otherPath) == 0;
		}
		return super.equals(obj);
	}

	public List<FQIdentifier> getAliasToIdentifier() {
		if (aliasToFQIdentifier == null)
			initAliasList();

		return aliasToFQIdentifier;
	}

	public List<IClassDefAdapter> getClassHierarchy(IClassDefAdapter scopeClass) {
		List<IClassDefAdapter> bases = new ArrayList<IClassDefAdapter>();

		resolveClassHierarchy(bases, scopeClass);
		Collections.reverse(bases);
		bases.add(new ObjectAdapter(this, this));

		return bases;
	}

	public String getBaseContextName(IClassDefAdapter contextClass, String originalName) {
		originalName = resolveRealToAlias(originalName);
		for (String baseName : contextClass.getBaseClassNames()) {
			if (baseName.endsWith(originalName)) {
				return baseName;
			}
		}

		return originalName;
	}

	private String resolveRealToAlias(String originalName) {
		for (FQIdentifier identifier : getAliasToIdentifier()) {
			if (identifier.getRealName().compareTo(originalName) == 0) {
				originalName = identifier.getAlias();
			}
		}
		return originalName;
	}

	private File getFile() {
		return this.file;
	}

	public List<String> getGlobarVarNames() {
		List<String> globalNames = new ArrayList<String>();
		if(this.sourceModule != null && nature != null){
			try {
				ICodeCompletionASTManager astManager = nature.getAstManager();
				if(astManager != null){
					IToken[] tokens = astManager.getCompletionsForModule(this.sourceModule, new CompletionState(0, 0, "", nature, ""));
					for (IToken token : tokens) {
						globalNames.add(token.getRepresentation());
					}
				}
			} catch (CompletionRecursionException e) {
				PydevPlugin.log(e);
			}
		}else{
			for (SimpleAdapter adapter : getAssignedVariables()) {
				globalNames.add(adapter.getName());
			}
		}
		return globalNames;
	}

	@Override
	public ModuleAdapter getModule() {
		return this;
	}

	@Override
	public AbstractScopeNode<?> getParent() {
		return this;
	}

	public int getOffset(IASTNodeAdapter adapter, int strategy) {
		int offset = 0;

		setStrategy(adapter, strategy);
		try {
			offset = offsetStrategy.getOffset();
		} catch (BadLocationException e) {
			// fallback :)
			String src = doc.get();
			int nameIndex = src.indexOf(adapter.getName());
			return src.indexOf(":", nameIndex) + 2;
		}
		return offset;
	}

	public SortedMap<String, String> getRegularImportedModules() {
		if (importedModules == null)
			initAliasList();

		return importedModules;
	}

	public IClassDefAdapter getScopeClass(ITextSelection selection) {
        IASTNodeAdapter bestClassScope = null;
		Iterator<IClassDefAdapter> iter = getClasses().iterator();
		while (iter.hasNext()) {
            IASTNodeAdapter classScope = (IASTNodeAdapter) iter.next();
			if (isSelectionInAdapter(selection, classScope)){
				bestClassScope = classScope;
            }

			if (classScope.getNodeFirstLine() > selection.getEndLine()){
				break;
            }

		}

		return (IClassDefAdapter) bestClassScope;
	}

	private void initAliasList() {
		ImportVisitor visitor = VisitorFactory.createVisitor(ImportVisitor.class, getASTNode());
		this.importedModules = visitor.getImportedModules();
		this.aliasToFQIdentifier = visitor.getAliasToFQIdentifier();
	}

	public boolean isGlobal(String name) {
		return getGlobarVarNames().contains(name);
	}

	private boolean isSelectionInAdapter(ITextSelection selection, IASTNodeAdapter adapter) {

		int startOffSet = selection.getOffset();
		int endOffSet = selection.getOffset() + selection.getLength();

		try {
			int lastLine = adapter.getNodeLastLine() - 1;
			int adapterStartOffset = doc.getLineOffset(adapter.getNodeFirstLine() - 1) + adapter.getNodeIndent() - 1;

			int adapterEndOffset = doc.getLineOffset(lastLine) + doc.getLineLength(lastLine);

			return (adapterStartOffset <= startOffSet && adapterEndOffset >= endOffSet);
		} catch (BadLocationException e) {

		}
		return false;
	}

	private boolean isAdapterInSelection(ITextSelection selection, IASTNodeAdapter adapter) {

		int startOffSet = selection.getOffset();
		int endOffSet = selection.getOffset() + selection.getLength();

		try {
			int adapterStartOffset = getStartOffset(adapter);

			return (adapterStartOffset >= startOffSet && adapterStartOffset < endOffSet);
		} catch (BadLocationException e) {

		}
		return false;
	}

	public int getEndOffset(IASTNodeAdapter adapter, int adapterStartOffset) throws BadLocationException {
		int lastLine = adapter.getNodeLastLine() - 1;

		int adapterEndOffset = 0;
		if (adapter.getASTNode() instanceof Str) {
			adapterEndOffset += adapterStartOffset + adapter.getName().length();
		} else {
			adapterEndOffset = doc.getLineOffset(lastLine) + doc.getLineLength(lastLine);
		}
		return adapterEndOffset;
	}

	public int getStartOffset(SimpleNode node) throws BadLocationException {
		return getStartOffset(new SimpleAdapter(this, this, node));
	}

	public int getStartOffset(IASTNodeAdapter adapter) throws BadLocationException {
		return doc.getLineOffset(adapter.getNodeFirstLine() - 1) + adapter.getNodeIndent() - 1;
	}

	public boolean isNodeInSelection(ITextSelection selection, SimpleNode node) {
		return isAdapterInSelection(selection, new SimpleAdapter(this, this, node));
	}

	private IClassDefAdapter resolveClassHierarchy(List<IClassDefAdapter> bases, IClassDefAdapter adap) {
		if (adap.hasBaseClass() && adap.getModule() != null) {
			for (IClassDefAdapter elem : adap.getModule().getBaseClasses(adap)) {
				if (elem != null) {
					bases.add(resolveClassHierarchy(bases, elem));
				}
			}
		}

		return adap;
	}

	public List<IClassDefAdapter> getBaseClasses(IClassDefAdapter clazz) {

		List<String> baseNames = clazz.getBaseClassNames();
		List<IClassDefAdapter> bases = new ArrayList<IClassDefAdapter>();
		Set<String> importedBaseNames = new HashSet<String>();
		for (IClassDefAdapter adapter : getClasses()) {
			for (String baseName : baseNames) {

				if (baseName.compareTo(adapter.getName()) == 0) {
					bases.add(adapter);
				} else {
					importedBaseNames.add(baseName);
				}
			}
		}

		bases.addAll(resolveImportedClass(importedBaseNames));

		return bases;
	}

	public IClassDefAdapter resolveClass(String name) {
		for (IClassDefAdapter classAdapter : getClasses()) {
			if (classAdapter.getName().compareTo(name) == 0) {
				return classAdapter;
			} else if (name.contains(".")) {
				String fileName = file.getName().substring(0, file.getName().indexOf("."));
				if (name.startsWith(fileName + ".")) {
					name = name.substring(name.indexOf(".") + 1);
				}
				if (name.endsWith("." + classAdapter.getName())) {
					return classAdapter;
				}

				IClassDefAdapter current = classAdapter;
				String fullNestedName = classAdapter.getName();
				while (nodeHelper.isClassDef(current.getParent().getASTNode())) {
					current = (IClassDefAdapter) current.getParent();
					fullNestedName = current.getName() + "." + fullNestedName;
				}
				if (fullNestedName.compareTo(name) == 0)
					return classAdapter;
			}
		}
		return null;
	}

	/**
	 * Will resolve module and real identifier if an alias is used. The returned import may be a relative one...
	 * 
	 * @param aliasName
	 *            Identifier/Token (e.g. foo.classname)
	 * @return Array consisting of module and real identifier
	 */
	public List<FQIdentifier> resolveFullyQualified(String aliasName) {
		List<FQIdentifier> qualifiedIdentifiers = new ArrayList<FQIdentifier>();
		String FQPrefix = "";
		String aliasIdentifier = "";
		int longestMatch = 0;

		for (String module : getRegularImportedModules().keySet()) {
			if (aliasName.startsWith(module)) {
				if (module.length() > longestMatch) {
					FQPrefix = getRegularImportedModules().get(module);
					longestMatch = module.length();
				}
			}
		}
		if (longestMatch > 0) {
			if (aliasName.length() > longestMatch)
				aliasIdentifier = aliasName.substring(longestMatch + 1);
			qualifiedIdentifiers.add(new FQIdentifier(FQPrefix, aliasIdentifier, aliasIdentifier));
			return qualifiedIdentifiers;
		}

		for (FQIdentifier identifier : getAliasToIdentifier()) {
			if (aliasName.startsWith(identifier.getAlias())) {
				String attribute = aliasName.substring(identifier.getAlias().length());
				FQIdentifier id = new FQIdentifier(identifier.getModule(), identifier.getRealName() + attribute, identifier.getAlias()
						+ attribute);
				qualifiedIdentifiers.add(id);
				return qualifiedIdentifiers;
			}
		}

		for (String moduleAlias : getRegularImportedModules().keySet()) {
			qualifiedIdentifiers.add(new FQIdentifier(getRegularImportedModules().get(moduleAlias), aliasName, aliasName));
		}
		return qualifiedIdentifiers;

	}

	private Set<ClassDefAdapter> resolveImportedClass(Set<String> importedBase) {
		Set<ClassDefAdapter> bases = new HashSet<ClassDefAdapter>();
		if (moduleManager == null)
			return bases;

		for (String baseName : importedBase) {
            CompletionRequest compReq = new CompletionRequest(file, nature, doc, baseName, 0, 0, new PyCodeCompletion(), "");
		    ArrayList ret = new ArrayList();
            ICompletionState state = new CompletionState(0,0,baseName,nature,"");
            PyCodeCompletion.getSelfOrClsCompletions(compReq, ret, state, true, (SimpleNode) sourceModule.getAst());
            for (Object object : ret) {
                if(object instanceof IToken){
                    IToken token = (IToken) object;
                    System.out.println("TODO: CREATE CLASS DEF ADAPTER FOR TOKENS..."+token);
                    //new ClassDefAdapterFromTokens(ret);
                }
            }
		}
		return bases;
	}

    /**
     * TODO: the current way of getting the base list is 'naive', and can take a lot of time when processing
     * (and does not handle CompiledModules)
     * 
     * This method fills the bases list (out) with asts for the methods that can be overriden.
     * 
     * Still, compiled modules will not have an actual ast, but a list of tokens (that should be used
     * to know what should be overriden), so, this method should actually be changed so that 
     * it works with tokens (that are resolved when a completion is requested), so, if we request a completion
     * for each base class, all the tokens from it will be returned, what's missing in this approach is that currently
     * the tokens returned don't have an associated context, so, after getting them, it may be hard to actually
     * tell the whole class structure above it (but this can be considered secondary for now).
     */
	private void fillBaseList(Set<IClassDefAdapter> bases, List<FQIdentifier> qualifiedBaseName) {
		if (moduleManager != null) {
			for (FQIdentifier identifier : qualifiedBaseName) {

				Set<ModuleAdapter> resolvedModules = moduleManager.resolveModule(file, identifier);
				for (ModuleAdapter module : resolvedModules) {
					IClassDefAdapter base = module.resolveClass(identifier.getRealName());
					if (base != null){
						bases.add(base);
                    }
				}
			}
		}
	}

	public void setStrategy(IASTNodeAdapter adapter, int strategy) {
		switch (strategy) {
		case IOffsetStrategy.AFTERINIT:
			this.offsetStrategy = new InitOffset(adapter, this.doc);
			break;
		case IOffsetStrategy.BEGIN:
			this.offsetStrategy = new BeginOffset(adapter, this.doc);
			break;
		case IOffsetStrategy.END:
			this.offsetStrategy = new EndOffset(adapter, this.doc);
			break;

		default:
			this.offsetStrategy = new BeginOffset(adapter, this.doc);
		}
	}

	public AbstractScopeNode<?> getScopeAdapter(ITextSelection selection) {
		AbstractScopeNode<?> bestScopeNode = null;

		bestScopeNode = getScopeFunction(selection);
		if (bestScopeNode == null) {
			bestScopeNode = (AbstractScopeNode<?>) getScopeClass(selection);
		}

		if (bestScopeNode == null) {
			bestScopeNode = this;
		}

		return bestScopeNode;
	}

	private AbstractScopeNode<?> getScopeFunction(ITextSelection selection) {
		AbstractScopeNode<?> scopeAdapter = null;

		Iterator<FunctionDefAdapter> iter = getFunctions().iterator();
		while (iter.hasNext()) {
			FunctionDefAdapter functionScope = iter.next();
			if (isSelectionInAdapter(selection, functionScope))
				scopeAdapter = functionScope;

			if (functionScope.getNodeFirstLine() > selection.getEndLine())
				break;
		}
		return scopeAdapter;
	}

	@Override
	public int getNodeBodyIndent() {
		return 0;
	}

	@Override
	public int getNodeIndent() {
		return 0;
	}

	public List<SimpleAdapter> getWithinSelection(ITextSelection selection, List<SimpleAdapter> variables) {

		List<SimpleAdapter> withinOffsetAdapters = new ArrayList<SimpleAdapter>();
		for (SimpleAdapter adapter : variables) {
			if (isAdapterInSelection(selection, adapter)) {
				withinOffsetAdapters.add(adapter);
			}
		}
		return withinOffsetAdapters;
	}

	public ITextSelection extendSelection(ITextSelection selection, SimpleNode nodeStart, SimpleNode nodeEnd) {
		if (this.doc != null) {
			try {

				int startOffset = getStartOffset(nodeStart);

				int endOffset = getStartOffset(nodeEnd) - 1;

				if (startOffset > selection.getOffset()) {
					startOffset = selection.getOffset();
				}
				if (endOffset < selection.getOffset() + selection.getLength()) {
					endOffset = selection.getOffset() + selection.getLength();
				}
				selection = new TextSelection(doc, startOffset, endOffset - startOffset);
			} catch (BadLocationException e) {

			}
		}
		return normalizeSelection(selection);
	}

	public ITextSelection normalizeSelection(ITextSelection userSelection) {

		while (userSelection.getText() != null
				&& (userSelection.getText().startsWith(" ") || userSelection.getText().startsWith("\n") || userSelection.getText()
						.startsWith("\r"))) {
			userSelection = new TextSelection(this.doc, userSelection.getOffset() + 1, userSelection.getLength() - 1);
		}
		while (userSelection.getText() != null
				&& (userSelection.getText().endsWith(" ") || userSelection.getText().endsWith("\n") || userSelection.getText().endsWith(
						"\r"))) {
			userSelection = new TextSelection(this.doc, userSelection.getOffset(), userSelection.getLength() - 1);
		}

		return userSelection;
	}

	public ITextSelection extendSelectionToEnd(ITextSelection selection, SimpleNode node) {
		if (this.doc != null) {
			SimpleAdapter adapter = new SimpleAdapter(this, this, node);
			int lastLine = adapter.getNodeLastLine() - 1;
			try {
				int adapterEndOffset = doc.getLineOffset(lastLine);

				adapterEndOffset += doc.getLineLength(lastLine);

				selection = new TextSelection(doc, selection.getOffset(), adapterEndOffset - selection.getOffset());
			} catch (BadLocationException e) {

			}

		}
		return selection;
	}

	public ITextSelection extendSelection(ITextSelection selection, SimpleNode node) {
		if (this.doc != null && (node instanceof Str)) {
			SimpleAdapter adapter = new SimpleAdapter(this, this, node);
			try {
				int startOffset = getStartOffset(adapter);
				if (startOffset > selection.getOffset()) {
					startOffset = selection.getOffset();
				}
				int endOffset = startOffset + adapter.getName().length() + 2;
				if (endOffset < selection.getOffset() + selection.getLength()) {
					endOffset = selection.getOffset() + selection.getLength();
				}

				selection = new TextSelection(doc, startOffset, endOffset - startOffset);
			} catch (BadLocationException e) {

			}

		}
		return selection;

	}

	@Override
	public int getNodeFirstLine() {
		int i = 0;

		while (i < getASTNode().body.length) {
			SimpleNode node = this.getASTNode().body[i];
			if (!nodeHelper.isImport(node))
				return node.beginLine;
			i += 1;
		}
		return 1;
	}

	public boolean isImport(String name) {
		for (String module : getRegularImportedModules().keySet()) {
			if (module.compareTo(name) == 0)
				return true;
		}

		for (FQIdentifier fq : getAliasToIdentifier()) {
			if (fq.getAlias().compareTo(name) == 0)
				return true;
		}
		return false;
	}

}
