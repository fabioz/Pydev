package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.REF;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.util.UIUtils;

/**
 * Checks the integrity of the internal pydev caches.
 * 
 * @author Fabio
 */
public class AdditionalInfoIntegrityChecker implements IPyEditListener{

    private static class Info{
        boolean allOk=true;
    }
    
    public static StringBuffer checkIntegrity(IProject project, IProgressMonitor monitor){
        Info info = new Info();
        
        PythonNature nature = PythonNature.getPythonNature(project);
        IModulesManager modulesManager = nature.getAstManager().getModulesManager();
        PythonPathHelper pythonPathHelper = (PythonPathHelper) modulesManager.getPythonPathHelper();
        List<String> pythonpath = pythonPathHelper.getPythonpath();
        StringBuffer buffer = new StringBuffer();
        buffer.append(StringUtils.format("Checking the integrity of the project: %s\n\n", project.getName()));
        buffer.append("Pythonpath:\n");
        for (String string : pythonpath) {
            buffer.append(string);
            buffer.append("\n");
        }
        buffer.append("\n");
        
        HashSet<ModulesKey> existingModuleNames = new HashSet<ModulesKey>();
        for (String string : pythonpath) {
            File file = new File(string);
            if(file.exists()){
                List<File> modulesBelow = pythonPathHelper.getModulesBelow(file, monitor)[0];
                for (File moduleFile : modulesBelow) {
                    String modName = pythonPathHelper.resolveModule(REF.getFileAbsolutePath(moduleFile), true);
                    if(modName != null){
                        existingModuleNames.add(new ModulesKey(modName, file));
                        buffer.append(StringUtils.format("Found module: %s\n", modName));
                    }else{
                        info.allOk = false;
                        buffer.append(StringUtils.format("Unable to resolve module: %s (gotten null module name)", moduleFile));
                    }
                }
            }else{
                info.allOk = false;
                buffer.append(StringUtils.format("File %s is referenced in the pythonpath but does not exist.", file));
            }
        }
        
        check(existingModuleNames, modulesManager, buffer, info);
        return buffer;
    }

    /**
     * @param existingModuleNames the modules that exist in the disk (an actual file is found and checked for the module it resolves to)
     */
    private static void check(HashSet<ModulesKey> existingModuleNames, IModulesManager modulesManager, StringBuffer buffer, Info info) {
        ModulesKey[] onlyDirectModules = modulesManager.getOnlyDirectModules();
        TreeSet<ModulesKey> inModulesManager = new TreeSet<ModulesKey>(Arrays.asList(onlyDirectModules));
        
        for (ModulesKey key : inModulesManager) {
            if(!existingModuleNames.contains(key)){
                info.allOk = false;
                buffer.append(StringUtils.format("ModulesKey %s exists in memory but not in the disk.\n", key));
            }
        }
        
        for (ModulesKey key : existingModuleNames) {
            if(!inModulesManager.contains(key)){
                info.allOk = false;
                buffer.append(StringUtils.format("ModulesKey %s exists in the disk but not in memory.\n", key));
            }
        }
        
        if(info.allOk){
            buffer.append("All checks OK!\n");
        }
    }

    public void onCreateActions(ListResourceBundle resources, final PyEdit edit, IProgressMonitor monitor) {
        edit.addOfflineActionListener("--d", new Action(){
            @Override
            public void run() {
                List<IPythonNature> allPythonNatures = PythonNature.getAllPythonNatures();
                StringBuffer buf = new StringBuffer();
                for (IPythonNature nature : allPythonNatures) {
                    buf.append(checkIntegrity(nature.getProject(), new NullProgressMonitor()));
                }
                UIUtils.showString(buf.toString());
            }
        });
    }

    public void onDispose(PyEdit edit, IProgressMonitor monitor) {
        
    }

    public void onSave(PyEdit edit, IProgressMonitor monitor) {
        
    }

    public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor) {
        
    }

}
