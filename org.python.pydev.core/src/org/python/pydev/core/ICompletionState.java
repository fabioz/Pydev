/*
 * Created on Jan 14, 2006
 */
package org.python.pydev.core;




public interface ICompletionState {

    String getActivationToken();

    IPythonNature getNature();

    ICompletionState getCopy();

    void setActivationToken(String string);

    void setBuiltinsGotten(boolean b);

    void raiseNFindTokensOnImportedModsCalled(IModule mod, String tok);
    
    void setCol(int i);

    void setLine(int i);

    void setLocalImportsGotten(boolean b);

    boolean getLocalImportsGotten();

    int getLine();

    int getCol();

    void checkDefinitionMemory(IModule module, IDefinition definition);

    void checkWildImportInMemory(IModule current, IModule mod);

    boolean getBuiltinsGotten();

    void checkMemory(IModule module, String base);

    void checkFindMemory(IModule module, String value);


}