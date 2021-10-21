package com.python.pydev.analysis.visitors;

import java.util.List;

import org.python.pydev.ast.codecompletion.revisited.CompletionState;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.FastStack;

public class ZopeInterfaceComputer {

    public final ClassDef classDef;
    private final FastStack<ZopeInterfaceComputer> zopeInterfaceComputers;
    private final IModule module;
    private boolean isZopeInterface;
    private boolean hasCachedIsZopeInterface;

    public ZopeInterfaceComputer(ClassDef classDef, FastStack<ZopeInterfaceComputer> zopeInterfaceComputers,
            IModule module) {
        this.classDef = classDef;
        this.zopeInterfaceComputers = zopeInterfaceComputers;
        this.module = module;
        this.isZopeInterface = false;
        this.hasCachedIsZopeInterface = false;
    }

    public boolean isZopeInterface() {
        if (hasCachedIsZopeInterface) {
            return isZopeInterface;
        }
        for (exprType base : classDef.bases) {
            if (!isBaseValidForZopeInterfaceCheck(base)) {
                return cacheIsZopeInterface(false);
            }
            if (isZopeInterfaceIndirectlyInherited(base)) {
                return cacheIsZopeInterface(true);
            }
            IDefinition[] definitions = findDefinitionsForBase(base);
            for (IDefinition value : definitions) {
                if (value instanceof Definition) {
                    if (isDefinitionZopeInterface((Definition) value, base)) {
                        return cacheIsZopeInterface(true);
                    }
                }
            }
        }
        return cacheIsZopeInterface(false);
    }

    private boolean isBaseValidForZopeInterfaceCheck(exprType base) {
        String baseRep = NodeUtils.getFullRepresentationString(base);
        if (baseRep == null) {
            return false;
        }
        return true;
    }

    private boolean cacheIsZopeInterface(boolean value) {
        hasCachedIsZopeInterface = true;
        isZopeInterface = value;
        return value;
    }

    private boolean isZopeInterfaceIndirectlyInherited(exprType base) {
        String baseRep = NodeUtils.getFullRepresentationString(base);
        for (ZopeInterfaceComputer zopeInterfaceComputer : zopeInterfaceComputers) {
            ClassDef classDef = zopeInterfaceComputer.classDef;
            String classRep = NodeUtils.getFullRepresentationString(classDef);
            if (baseRep.equals(classRep)) {
                return zopeInterfaceComputer.isZopeInterface();
            }
        }
        return false;
    }

    private IDefinition[] findDefinitionsForBase(exprType base) {
        ICompletionState state = new CompletionState(base.beginLine, base.beginColumn,
                NodeUtils.getFullRepresentationString(base), module.getNature(), "");
        try {
            return module.findDefinition(state, state.getLine(), state.getCol(), module.getNature());
        } catch (Exception e) {
            Log.log(e);
        }
        return new IDefinition[0];
    }

    private boolean isDefinitionZopeInterface(Definition definition, exprType base) {
        if (definition.ast instanceof Import) {
            return isBaseZopeInterfaceAtImport(base, (Import) definition.ast);
        } else if (definition.ast instanceof ImportFrom) {
            return isBaseZopeInterfaceAtImportFrom(base, (ImportFrom) definition.ast);
        }
        return false;
    }

    private boolean isBaseZopeInterfaceAtImport(exprType base, Import importNode) {
        final String baseRep = NodeUtils.getFullRepresentationString(base);
        final String[] baseParts = extractBaseParts(baseRep);
        final String zopeInterface = "zope.interface.Interface";
        for (aliasType alias : importNode.names) {
            if (alias.name instanceof NameTok) {
                NameTok nameTok = (NameTok) alias.name;
                if (nameTok.id == null) {
                    continue;
                }
                if (alias.asname instanceof NameTok) {
                    FastStringBuffer compareBuf = new FastStringBuffer(nameTok.id, baseRep.length());
                    String asname = NodeUtils.getNameFromNameTok(alias.asname);
                    if (baseParts[0].equals(asname)) {
                        for (int i = 1; i < baseParts.length; i++) {
                            compareBuf.append('.').append(baseParts[i]);
                        }
                    }
                    if (zopeInterface.equals(compareBuf.toString())) {
                        return true;
                    }
                } else if (zopeInterface.equals(baseRep)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String[] extractBaseParts(String baseRep) {
        List<String> dotSplit = StringUtils.dotSplit(baseRep);
        return dotSplit.toArray(new String[dotSplit.size()]);
    }

    private boolean isBaseZopeInterfaceAtImportFrom(exprType base, ImportFrom importNode) {
        if (importNode.module instanceof NameTok) {
            NameTok moduleNameTok = (NameTok) importNode.module;
            return "zope.interface.interface".equals(moduleNameTok.id);
        }
        return false;
    }

}
