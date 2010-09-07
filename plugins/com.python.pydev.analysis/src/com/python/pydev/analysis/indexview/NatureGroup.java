package com.python.pydev.analysis.indexview;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;

public class NatureGroup extends ElementWithChildren {

    private PythonNature nature;

    public NatureGroup(ITreeElement parent, PythonNature nature) {
        super(parent);
        this.nature = nature;
    }

    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    protected void calculateChildren() throws MisconfigurationException {
        Tuple<List<AbstractAdditionalInterpreterInfo>, List<IPythonNature>> additionalInfoAndNature = AdditionalProjectInterpreterInfo.getAdditionalInfoAndNature(nature, false, false, false);
        
        ICodeCompletionASTManager astManager = nature.getAstManager();
        IModulesManager projectModulesManager = astManager.getModulesManager();
        Set<String> allAstManagerModuleNames = projectModulesManager.getAllModuleNames(false, "");
        
        Set<String> allAdditionalInfoModuleNames = projectModulesManager.getAllModuleNames(false, "");
        for (AbstractAdditionalInterpreterInfo abstractAdditionalInterpreterInfo : additionalInfoAndNature.o1) {
            abstractAdditionalInterpreterInfo.getAllModulesWithTokens();
        }
        
        Set<String> notInBoth = new TreeSet<String>();
        Set<String> inBoth = new TreeSet<String>();
        
        for(String s:allAdditionalInfoModuleNames){
            if(!allAstManagerModuleNames.contains(s)){
                notInBoth.add(StringUtils.format("Module: %s in additional info but not on AST manager", s));
            }else{
                inBoth.add(s);
            }
        }
        
        for(String s:allAstManagerModuleNames){
            if(!allAdditionalInfoModuleNames.contains(s)){
                notInBoth.add(StringUtils.format("Module: %s in AST manager but not on additional info", s));
            }else{
                inBoth.add(s);
            }
        }
        for(String s:notInBoth){
            addChild(new LeafElement(this, s));
        }
        for(String s:inBoth){
            addChild(new LeafElement(this, s));
        }
        
    }
    
    @Override
    public String toString() {
        IProject project = nature.getProject();
        if(project != null){
            return project.getName();
        }
        return "Project not set";
    }

}
