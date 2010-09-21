package com.python.pydev.analysis.indexview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.editor.codecompletion.revisited.SystemModulesManager;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class ForcedLibGroup extends ElementWithChildren{

    private InterpreterInfo interpreterInfo;
    private String forcedLib;

    public ForcedLibGroup(ITreeElement parent, InterpreterInfo interpreterInfo, String forcedLib) {
        super(parent);
        this.interpreterInfo = interpreterInfo;
        this.forcedLib = forcedLib;
    }

    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    protected void calculateChildren() throws MisconfigurationException {
        SystemModulesManager m = (SystemModulesManager) this.interpreterInfo.getModulesManager();
        AbstractModule builtinModule = m.getBuiltinModule(forcedLib, m.getNature(), true);
        IToken[] globalTokens = builtinModule.getGlobalTokens();
        
        ArrayList<LeafElement> lst = new ArrayList<LeafElement>();

        for (IToken iToken : globalTokens) {
            lst.add(new LeafElement(this, iToken.getRepresentation()));
        }
        Collections.sort(lst, new Comparator<LeafElement>() {

            @Override
            public int compare(LeafElement o1, LeafElement o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
        for (LeafElement leafElement : lst) {
            addChild(leafElement);
        }
    }
    
    @Override
    public String toString() {
        return "Forced builtin: "+forcedLib;
    }
}
