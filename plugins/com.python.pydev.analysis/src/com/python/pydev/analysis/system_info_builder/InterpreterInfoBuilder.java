package com.python.pydev.analysis.system_info_builder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.ModulesFoundStructure;
import org.python.pydev.editor.codecompletion.revisited.ModulesKeyTreeMap;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.SystemModulesManager;
import org.python.pydev.ui.pythonpathconf.IInterpreterInfoBuilder;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;


/**
 * @author fabioz
 */
public class InterpreterInfoBuilder implements IInterpreterInfoBuilder{
    
    static class InterpreterBuilderJob extends Job{
        
        public InterpreterBuilderJob() {
            super("InterpreterBuilderJob");
            this.setPriority(Job.BUILD);
        }

        private volatile Set<InterpreterInfoBuilder> buildersToCheck = new HashSet<InterpreterInfoBuilder>();
        
        @Override
        protected IStatus run(IProgressMonitor monitor) {
            Set<InterpreterInfoBuilder> builders = buildersToCheck;
            buildersToCheck = new HashSet<InterpreterInfoBuilder>();
            
            for (InterpreterInfoBuilder builder : builders) {
                if(builder.isDisposed() || monitor.isCanceled()){
                    return Status.OK_STATUS;
                }
                
                PythonPathHelper pythonPathHelper = new PythonPathHelper();
                pythonPathHelper.setPythonPath(builder.info.libs);
                ModulesFoundStructure modulesFound = pythonPathHelper.getModulesFoundStructure(monitor);
                if(builder.isDisposed() || monitor.isCanceled()){
                    return Status.OK_STATUS;
                }
                
                SystemModulesManager modulesManager = (SystemModulesManager) builder.info.getModulesManager();
                ModulesKeyTreeMap<ModulesKey, ModulesKey> keysFound = modulesManager.buildKeysFromModulesFound(monitor, modulesFound);
                if(builder.isDisposed() || monitor.isCanceled()){
                    return Status.OK_STATUS;
                }
                
                Tuple<List<ModulesKey>, List<ModulesKey>> diffModules = modulesManager.diffModules(keysFound);
                if(diffModules.o1.size() > 0 || diffModules.o2.size() > 0){
                    
                    //Update the modules manager itself (just pass all the keys as that should be fast)
                    modulesManager.updateKeysAndSave(keysFound);
                    
                    //Now, the additional info can be slower, so, let's work only on the deltas...
                    IInterpreterManager manager = builder.info.getModulesManager().getInterpreterManager();
                    try {
                        AbstractAdditionalDependencyInfo additionalSystemInfo = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(
                                manager, builder.info.getExecutableOrJar());
                        additionalSystemInfo.updateKeysIfNeededAndSave(keysFound);
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
            }
            
            
            return Status.OK_STATUS;
        }
        
    }

    
    private InterpreterInfo info;

    private boolean disposed;
    
    private static final InterpreterBuilderJob builderJob = new InterpreterBuilderJob();
    
    
    boolean isDisposed(){
        return this.disposed;
    }

    public void dispose() {
        disposed = true;
    }

    public void setInfo(InterpreterInfo info) {
        setInfo(info, 2000);
    }
    
    public void setInfo(InterpreterInfo info, int schedule) {
        this.info = info;
        builderJob.buildersToCheck.add(this);
        builderJob.schedule(schedule);
    }

}
