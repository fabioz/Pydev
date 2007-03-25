package org.python.pydev.editor.codecompletion.revisited;

import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.structure.CompletionRecursionException;

public class CompletionStateWrapper implements ICompletionState {

    private ICompletionState wrapped;
    
    public CompletionStateWrapper(CompletionState state) {
        this.wrapped = state;
        this.activationToken = state.activationToken;
    }
    
    //things that are not delegated ------------------------------------------------------------------------------------
    private String activationToken;
    private int col;
    private int line;
    
    public String getActivationToken() {
        return activationToken;
    }
    public void setActivationToken(String string) {
        activationToken = string;
    }
    public int getCol() {
        return col;
    }
    public int getLine() {
        return line;
    }
    public void setCol(int i) {
        col = i;
    }
    public void setLine(int i) {
        line = i;
    }


    
    //delegated --------------------------------------------------------------------------------------------------------
    public void checkDefinitionMemory(IModule module, IDefinition definition) throws CompletionRecursionException {
        wrapped.checkDefinitionMemory(module, definition);
    }

    public void checkFindDefinitionMemory(IModule mod, String tok) throws CompletionRecursionException {
        wrapped.checkFindDefinitionMemory(mod, tok);
    }

    public void checkFindMemory(IModule module, String value) throws CompletionRecursionException {
        wrapped.checkFindMemory(module, value);
    }

    public void checkFindModuleCompletionsMemory(IModule mod, String tok) throws CompletionRecursionException {
        wrapped.checkFindModuleCompletionsMemory(mod, tok);
    }

    public void checkFindResolveImportMemory(IToken tok) throws CompletionRecursionException {
        wrapped.checkFindResolveImportMemory(tok);
    }

    public void checkMemory(IModule module, String base) throws CompletionRecursionException {
        wrapped.checkMemory(module, base);
    }

    public void checkResolveImportMemory(IModule module, String value) throws CompletionRecursionException {
        wrapped.checkResolveImportMemory(module, value);
    }

    public void checkWildImportInMemory(IModule current, IModule mod) throws CompletionRecursionException {
        wrapped.checkWildImportInMemory(current, mod);
    }


    public boolean getBuiltinsGotten() {
        return wrapped.getBuiltinsGotten();
    }

    public ICompletionState getCopy() {
        return wrapped.getCopy();
    }

    public ICompletionState getCopyForResolveImportWithActTok(String representation) {
        return wrapped.getCopyForResolveImportWithActTok(representation);
    }

    public ICompletionState getCopyWithActTok(String value) {
        return wrapped.getCopyWithActTok(value);
    }

    public boolean getIsInCalltip() {
        return wrapped.getIsInCalltip();
    }


    public boolean getLocalImportsGotten() {
        return wrapped.getLocalImportsGotten();
    }

    public IPythonNature getNature() {
        return wrapped.getNature();
    }

    public String getQualifier() {
        return wrapped.getQualifier();
    }

    public int isLookingFor() {
        return wrapped.isLookingFor();
    }

    public void raiseNFindTokensOnImportedModsCalled(IModule mod, String tok) throws CompletionRecursionException {
        wrapped.raiseNFindTokensOnImportedModsCalled(mod, tok);
    }

    public void setBuiltinsGotten(boolean b) {
        wrapped.setBuiltinsGotten(b);
    }


    public void setIsInCalltip(boolean isInCalltip) {
        wrapped.setIsInCalltip(isInCalltip);
    }


    public void setLocalImportsGotten(boolean b) {
        wrapped.setLocalImportsGotten(b);
    }

    public void setLookingFor(int b) {
        wrapped.setLookingFor(b);
    }
    
    public void popFindResolveImportMemoryCtx() {
        wrapped.popFindResolveImportMemoryCtx();
    }
    
    public void pushFindResolveImportMemoryCtx() {
        wrapped.pushFindResolveImportMemoryCtx();
    }

}
