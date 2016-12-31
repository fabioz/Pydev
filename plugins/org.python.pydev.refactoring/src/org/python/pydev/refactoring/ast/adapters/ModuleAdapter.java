/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>    - initial implementation
*     Jonah Graham <jonah@kichwacoders.com> - ongoing maintenance
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISourceModule;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.AbstractASTManager;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.FQIdentifier;
import org.python.pydev.refactoring.ast.PythonModuleManager;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.BeforeCurrentOffset;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.BeginOffset;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.EndOffset;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.InitOffset;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.ast.visitors.info.ImportVisitor;

public class ModuleAdapter extends AbstractScopeNode<Module> {
    private List<FQIdentifier> aliasToFQIdentifier;
    private IDocument doc;
    private File file;
    private SortedMap<String, String> importedModules;
    private IOffsetStrategy offsetStrategy;
    private ISourceModule sourceModule;
    public final IPythonNature nature;

    public String getEndLineDelimiter() {
        return TextUtilities.getDefaultLineDelimiter(doc);
    }

    public ModuleAdapter(PythonModuleManager pm, File file, IDocument doc, Module node, IPythonNature nature) {
        super(null, null, node, new AdapterPrefs(TextUtilities.getDefaultLineDelimiter(doc), nature));
        this.file = file;
        this.doc = doc;
        this.aliasToFQIdentifier = null;
        this.importedModules = null;
        this.nature = nature;
    }

    public ModuleAdapter(PythonModuleManager pm, ISourceModule module, IPythonNature nature, IDocument doc) {
        super();
        this.file = module.getFile();
        if (doc != null) {
            this.doc = doc;
        } else {
            this.doc = PythonModuleManager.getDocFromFile(this.file);
        }
        init(null, null, (Module) module.getAst(), new AdapterPrefs(TextUtilities.getDefaultLineDelimiter(this.doc),
                nature));
        this.sourceModule = module;
        this.aliasToFQIdentifier = null;
        this.importedModules = null;
        this.nature = nature;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ModuleAdapter) {
            ModuleAdapter other = (ModuleAdapter) obj;
            String otherPath = other.getFile().getAbsolutePath();
            return file.getAbsolutePath().equalsIgnoreCase(otherPath);
        }
        return super.equals(obj);
    }

    /* 
     * Not sure if this will ever be used, but as the .equals() method has been overwritten
     * a corresponding hashCode() method is considered good style
     */
    @Override
    public int hashCode() {
        return this.file.getAbsolutePath().toLowerCase().hashCode();
    }

    public List<FQIdentifier> getAliasToIdentifier() {
        if (aliasToFQIdentifier == null) {
            initAliasList();
        }

        return aliasToFQIdentifier;
    }

    public List<IClassDefAdapter> getClassHierarchy(IClassDefAdapter scopeClass) throws MisconfigurationException {
        List<IClassDefAdapter> bases = new ArrayList<IClassDefAdapter>();

        resolveClassHierarchy(bases, scopeClass, new HashSet<String>());
        Collections.reverse(bases);

        return bases;
    }

    public String getBaseContextName(IClassDefAdapter contextClass, String originalName) {
        originalName = resolveRealToAlias(originalName);
        if (originalName.startsWith("__builtin__.")) {
            originalName = originalName.substring(12);

        } else if (originalName.startsWith("builtins.")) {
            originalName = originalName.substring(9);

        }
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

    public List<String> getGlobalVariableNames() {
        List<String> globalNames = new ArrayList<String>();
        if (this.sourceModule != null && nature != null) {
            try {
                ICodeCompletionASTManager astManager = nature.getAstManager();
                if (astManager != null) {
                    IToken[] tokens = astManager.getCompletionsForModule(this.sourceModule, new CompletionState(-1, -1,
                            "", nature, ""));
                    for (IToken token : tokens) {
                        globalNames.add(token.getRepresentation());
                    }
                }
            } catch (CompletionRecursionException e) {
                Log.log(e);
            }
        } else {
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

    public int getOffset(IASTNodeAdapter<? extends SimpleNode> adapter, int strategy,
            AbstractScopeNode<?> scopeAdapter) {
        int offset = 0;

        setStrategy(adapter, strategy, scopeAdapter);
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
        if (importedModules == null) {
            initAliasList();
        }

        return importedModules;
    }

    public IClassDefAdapter getScopeClass(ITextSelection selection) {
        IASTNodeAdapter<? extends SimpleNode> bestClassScope = null;

        for (IClassDefAdapter classScope : getClasses()) {
            if (isSelectionInAdapter(selection, classScope)) {
                bestClassScope = classScope;
            }

            if (classScope.getNodeFirstLine(false) > selection.getEndLine()) {
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
        return getGlobalVariableNames().contains(name);
    }

    private boolean isSelectionInAdapter(ITextSelection selection, IASTNodeAdapter<? extends SimpleNode> adapter) {
        int startOffSet = selection.getOffset();
        int endOffSet = selection.getOffset() + selection.getLength();

        try {
            int lastLine = adapter.getNodeLastLine() - 1;
            int adapterStartOffset = doc.getLineOffset(adapter.getNodeFirstLine(false) - 1) + adapter.getNodeIndent();
            int adapterEndOffset = doc.getLineOffset(lastLine) + doc.getLineLength(lastLine);

            return (adapterStartOffset <= startOffSet && adapterEndOffset >= endOffSet);
        } catch (BadLocationException e) {
            throw new RuntimeException("Internal error, bad location exception" + e.getMessage());
        }
    }

    private boolean isAdapterInSelection(ITextSelection selection, IASTNodeAdapter<? extends SimpleNode> adapter) {

        int selectionStart = selection.getOffset();
        int selectionEnd = selection.getOffset() + selection.getLength();

        try {
            int adapterStart = getStartOffset(adapter);

            return (adapterStart >= selectionStart && adapterStart < selectionEnd);
        } catch (BadLocationException e) {
            return false;
        }
    }

    public int getEndOffset(IASTNodeAdapter<? extends SimpleNode> adapter, int adapterStartOffset)
            throws BadLocationException {
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
        return getStartOffset(new SimpleAdapter(this, this, node, getAdapterPrefs()));
    }

    public int getStartOffset(IASTNodeAdapter<? extends SimpleNode> adapter) throws BadLocationException {
        return doc.getLineOffset(adapter.getNodeFirstLine(false) - 1) + adapter.getNodeIndent();
    }

    public boolean isNodeInSelection(ITextSelection selection, SimpleNode node) {
        return isAdapterInSelection(selection, new SimpleAdapter(this, this, node, getAdapterPrefs()));
    }

    private IClassDefAdapter resolveClassHierarchy(List<IClassDefAdapter> bases, IClassDefAdapter adap,
            Set<String> memo)
            throws MisconfigurationException {
        if (adap.hasBaseClass() && adap.getModule() != null) {

            List<IClassDefAdapter> baseClasses = adap.getModule().getBaseClasses(adap);
            for (IClassDefAdapter elem : baseClasses) {
                if (elem != null && !memo.contains(elem.getName())) {
                    memo.add(elem.getName());
                    bases.add(resolveClassHierarchy(bases, elem, memo));
                }
            }
        }

        return adap;
    }

    /**
     * @param clazz the class from where we want to get the bases.
     * 
     * @return a list of adapters for the base classes of the given class.
     */
    public List<IClassDefAdapter> getBaseClasses(IClassDefAdapter clazz) {
        CompletionCache completionCache = new CompletionCache();

        List<String> baseNames = clazz.getBaseClassNames();
        Set<String> classesToResolve = new HashSet<String>(baseNames);

        Set<IClassDefAdapter> resolved;
        try {
            resolved = resolveImportedClass(classesToResolve, completionCache);
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
        return new ArrayList<IClassDefAdapter>(resolved);
    }

    /**
     * Get a class adapter for a given class contained in this module.
     * 
     * @param name the name of the class we want to resolve.
     * 
     * @return an adapter to the class.
     */
    public IClassDefAdapter resolveClass(String name) {
        CompletionCache completionCache = new CompletionCache();

        HashSet<String> toResolve = new HashSet<String>();
        toResolve.add(name);
        Set<IClassDefAdapter> resolved;
        try {
            resolved = resolveImportedClass(toResolve, completionCache);
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
        if (toResolve.size() == 1) {
            return resolved.iterator().next();
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
        String fqPrefix = "";
        String aliasIdentifier = "";
        int longestMatch = 0;

        for (String module : getRegularImportedModules().keySet()) {
            if (aliasName.startsWith(module)) {
                if (module.length() > longestMatch) {
                    fqPrefix = getRegularImportedModules().get(module);
                    longestMatch = module.length();
                }
            }
        }
        if (longestMatch > 0) {
            if (aliasName.length() > longestMatch) {
                aliasIdentifier = aliasName.substring(longestMatch + 1);
            }
            qualifiedIdentifiers.add(new FQIdentifier(fqPrefix, aliasIdentifier, aliasIdentifier));
            return qualifiedIdentifiers;
        }

        for (FQIdentifier identifier : getAliasToIdentifier()) {
            if (aliasName.startsWith(identifier.getAlias())) {
                String attribute = aliasName.substring(identifier.getAlias().length());
                FQIdentifier id = new FQIdentifier(identifier.getModule(), identifier.getRealName() + attribute,
                        identifier.getAlias() + attribute);
                qualifiedIdentifiers.add(id);
                return qualifiedIdentifiers;
            }
        }

        for (String moduleAlias : getRegularImportedModules().keySet()) {
            qualifiedIdentifiers.add(new FQIdentifier(getRegularImportedModules().get(moduleAlias), aliasName,
                    aliasName));
        }
        return qualifiedIdentifiers;

    }

    /**
     * This method fills the bases list (out) with asts for the methods that can be overridden.
     * 
     * Still, compiled modules will not have an actual ast, but a list of tokens (that should be used
     * to know what should be overridden), so, this method should actually be changed so that 
     * it works with tokens (that are resolved when a completion is requested), so, if we request a completion
     * for each base class, all the tokens from it will be returned, what's missing in this approach is that currently
     * the tokens returned don't have an associated context, so, after getting them, it may be hard to actually
     * tell the whole class structure above it (but this can be considered secondary for now).
     * @throws MisconfigurationException 
     */
    private Set<IClassDefAdapter> resolveImportedClass(Set<String> importedBase, CompletionCache completionCache)
            throws MisconfigurationException {
        Set<IClassDefAdapter> bases = new HashSet<IClassDefAdapter>();
        Set<ClassDef> alreadyTreated = new HashSet<ClassDef>();

        //let's create the module only once (this way the classdefs will be the same as reparses should not be needed).
        IModule module;
        try {
            module = AbstractASTManager.createModule(file, doc, nature);
        } catch (MisconfigurationException e1) {
            throw new RuntimeException(e1);
        }

        for (String baseName : importedBase) {
            ICompletionState state = new CompletionState(-1, -1, baseName, nature, "", completionCache);
            IToken[] ret = null;
            try {
                ret = nature.getAstManager().getCompletionsForModule(module, state);
            } catch (CompletionRecursionException e) {
                throw new RuntimeException(e);
            }

            Map<String, List<IToken>> map = new HashMap<String, List<IToken>>();
            Set<ClassDef> classDefAsts = new HashSet<ClassDef>();

            for (IToken tok : ret) {
                if (tok instanceof SourceToken) {
                    SourceToken token = (SourceToken) tok;
                    SimpleNode ast = token.getAst();
                    if (ast instanceof ClassDef || ast instanceof FunctionDef) {
                        if (ast.parent instanceof ClassDef) {
                            ClassDef classDefAst = (ClassDef) ast.parent;
                            if (!alreadyTreated.contains(classDefAst)) {
                                classDefAsts.add(classDefAst);
                                alreadyTreated.add(classDefAst);
                            }
                        }
                    }

                } else if (tok instanceof CompiledToken) {
                    CompiledToken token = (CompiledToken) tok;
                    List<IToken> toks = map.get(token.getParentPackage());
                    if (toks == null) {
                        toks = new ArrayList<IToken>();
                        map.put(token.getParentPackage(), toks);
                    }
                    toks.add(token);
                } else {
                    throw new RuntimeException("Unexpected token:" + tok.getClass());
                }
            }

            for (Map.Entry<String, List<IToken>> entry : map.entrySet()) {
                //TODO: The module adapter should probably not be 'this' (make test to break it!)
                bases.add(new ClassDefAdapterFromTokens(this, entry.getKey(), entry.getValue(), getAdapterPrefs()));
            }
            for (ClassDef classDef : classDefAsts) {
                //TODO: The module adapter should probably not be 'this' (make test to break it!)
                bases.add(new ClassDefAdapterFromClassDef(this, classDef, getAdapterPrefs()));
            }
        }
        return bases;
    }

    public void setStrategy(IASTNodeAdapter<? extends SimpleNode> adapter, int strategy,
            AbstractScopeNode<?> scopeAdapter) {
        switch (strategy) {
            case IOffsetStrategy.AFTERINIT:
                this.offsetStrategy = new InitOffset(adapter, this.doc, this.getAdapterPrefs());
                break;
            case IOffsetStrategy.BEFORECURRENT:
                this.offsetStrategy = new BeforeCurrentOffset(adapter, this.doc, this.getAdapterPrefs(), scopeAdapter);
                break;
            case IOffsetStrategy.BEGIN:
                this.offsetStrategy = new BeginOffset(adapter, this.doc, this.getAdapterPrefs());
                break;
            case IOffsetStrategy.END:
                this.offsetStrategy = new EndOffset(adapter, this.doc, this.getAdapterPrefs());
                break;

            default:
                this.offsetStrategy = new BeginOffset(adapter, this.doc, this.getAdapterPrefs());
        }
    }

    public AbstractScopeNode<?> getScopeAdapter(ITextSelection selection) {
        AbstractScopeNode<?> bestScopeNode = null;

        bestScopeNode = getScopeFunction(selection);

        if (bestScopeNode != null) {
            return bestScopeNode;
        }

        bestScopeNode = (AbstractScopeNode<?>) getScopeClass(selection);

        if (bestScopeNode != null) {
            return bestScopeNode;
        }

        return this;
    }

    private AbstractScopeNode<?> getScopeFunction(ITextSelection selection) {
        AbstractScopeNode<?> scopeAdapter = null;

        Iterator<FunctionDefAdapter> iter = getFunctions().iterator();
        while (iter.hasNext()) {
            FunctionDefAdapter functionScope = iter.next();
            if (isSelectionInAdapter(selection, functionScope)) {
                scopeAdapter = functionScope;
            }

            if (functionScope.getNodeFirstLine(false) > selection.getEndLine()) {
                break;
            }
        }
        return scopeAdapter;
    }

    @Override
    public String getNodeBodyIndent() {
        return "";
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
                Log.log(e);
            }
        }
        return normalizeSelection(selection);
    }

    public ITextSelection normalizeSelection(ITextSelection userSelection) {

        String txt = userSelection.getText();
        while (txt != null && (txt.startsWith(" ") || txt.startsWith("\n") || txt.startsWith("\r"))) {
            userSelection = new TextSelection(this.doc, userSelection.getOffset() + 1, userSelection.getLength() - 1);
            txt = userSelection.getText();
        }
        while (txt != null && (txt.endsWith(" ") || txt.endsWith("\n") || txt.endsWith("\r"))) {
            userSelection = new TextSelection(this.doc, userSelection.getOffset(), userSelection.getLength() - 1);
            txt = userSelection.getText();
        }

        return userSelection;
    }

    public ITextSelection extendSelectionToEnd(ITextSelection selection, SimpleNode node) {
        if (this.doc != null) {
            SimpleAdapter adapter = new SimpleAdapter(this, this, node, getAdapterPrefs());
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
            SimpleAdapter adapter = new SimpleAdapter(this, this, node, getAdapterPrefs());
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

    /**
     * Note that the line returned is 1-based.
     */
    @Override
    public int getNodeFirstLine(boolean considerDecorators) {
        Module astNode = getASTNode();
        for (int i = 0; i < astNode.body.length; i++) {
            SimpleNode node = astNode.body[i];
            if (!nodeHelper.isImport(node) && !nodeHelper.isStr(node)) {
                if (!considerDecorators) {
                    return node.beginLine;
                } else {
                    return nodeHelper.getFirstLineConsideringDecorators(node);
                }
            }
        }
        return 1;
    }

    public boolean isImport(String name) {
        for (String module : getRegularImportedModules().keySet()) {
            if (module.compareTo(name) == 0) {
                return true;
            }
        }

        for (FQIdentifier fq : getAliasToIdentifier()) {
            if (fq.getAlias().compareTo(name) == 0) {
                return true;
            }
        }
        return false;
    }

    public int getStartLineBefore(int selectionOffset) throws Exception {
        int lineOfOffset = this.doc.getLineOfOffset(selectionOffset);
        return this.doc.getLineOffset(lineOfOffset);
    }

    /**
     * @return the doc
     */
    public IDocument getDoc() {
        return doc;
    }

    public String getIndentationFromAst(SimpleNode node) {
        PySelection pySelection = new PySelection(doc);
        return PySelection.getIndentationFromLine(pySelection.getLine(node.beginLine - 1));
    }
}
