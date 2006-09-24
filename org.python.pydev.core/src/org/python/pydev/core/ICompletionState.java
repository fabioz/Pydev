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
    
    public void checkResolveImportMemory(IModule module, String value);

    boolean getBuiltinsGotten();

    void checkMemory(IModule module, String base);

    void checkFindMemory(IModule module, String value);

    void checkFindDefinitionMemory(IModule mod, String tok);
    
    boolean getIsInCalltip();

    public static final int LOOKING_FOR_INSTANCE_UNDEFINED=0;
    public static final int LOOKING_FOR_INSTANCED_VARIABLE=1;
    public static final int LOOKING_FOR_UNBOUND_VARIABLE=2;
    
    /**
     * Identifies if we should be looking for an instance (in which case, self should not
     * be added to the parameters)
     */
    void setLookingForInstance(boolean b);

    ICompletionState getCopyWithActTok(String value);

    String getQualifier();


}