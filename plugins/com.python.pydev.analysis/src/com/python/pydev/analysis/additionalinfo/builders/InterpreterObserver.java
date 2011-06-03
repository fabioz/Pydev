/*
 * Created on 07/09/2005
 */
package com.python.pydev.analysis.additionalinfo.builders;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.parser.ErrorDescription;
import org.python.pydev.parser.PyParser;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.interpreters.IInterpreterObserver;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;
import org.python.pydev.utils.JobProgressComunicator;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;


public class InterpreterObserver implements IInterpreterObserver {

    private static final boolean DEBUG_INTERPRETER_OBSERVER = false;
    
    private Object lock = new Object();
    
    /**
     * @see org.python.pydev.ui.interpreters.IInterpreterObserver#notifyDefaultPythonpathRestored(org.python.pydev.ui.interpreters.AbstractInterpreterManager, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void notifyDefaultPythonpathRestored(IInterpreterManager manager, String interpreter, IProgressMonitor monitor){
        if(DEBUG_INTERPRETER_OBSERVER){
            System.out.println("notifyDefaultPythonpathRestored "+ interpreter);
        }
        synchronized(lock){
	        try {
	            final IInterpreterInfo interpreterInfo = manager.getInterpreterInfo(interpreter, new NullProgressMonitor());
	            int grammarVersion = interpreterInfo.getGrammarVersion();
	            AbstractAdditionalTokensInfo currInfo = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(manager, interpreter);
	            if(currInfo != null){
	                currInfo.clearAllInfo();
	            }
	            InterpreterInfo defaultInterpreterInfo = (InterpreterInfo) manager.getInterpreterInfo(interpreter, monitor);
	            ISystemModulesManager m = defaultInterpreterInfo.getModulesManager();
	            AbstractAdditionalTokensInfo additionalSystemInfo = restoreInfoForModuleManager(monitor, m, 
	                    "(system: " + manager.getManagerRelatedName() + " - " + interpreter + ")",
	                    new AdditionalSystemInterpreterInfo(manager, interpreter), null, grammarVersion);
	
	            if (additionalSystemInfo != null) {
	                //ok, set it and save it
	                AdditionalSystemInterpreterInfo.setAdditionalSystemInfo(manager, interpreter, additionalSystemInfo);
	                AbstractAdditionalTokensInfo.saveAdditionalSystemInfo(manager, interpreter);
	            }
	        } catch (Throwable e) {
	            Log.log(e);
	        }
        }
    }

    /**
     * received when the interpreter manager is restored
     *  
     * this means that we have to restore the additional interpreter information we stored
     *  
     * @see org.python.pydev.ui.interpreters.IInterpreterObserver#notifyInterpreterManagerRecreated(org.python.pydev.ui.interpreters.AbstractInterpreterManager)
     */
    public void notifyInterpreterManagerRecreated(final IInterpreterManager iManager) {
        for(final IInterpreterInfo interpreterInfo:iManager.getInterpreterInfos()){
            Job j = new Job("PyDev... Restoring indexes for: "+interpreterInfo.getNameForUI()) {

                @Override
                protected IStatus run(IProgressMonitor monitorArg) {
                	synchronized(lock){
	                    boolean loadedAdditionalSystemInfo;
	        			try {
	        				loadedAdditionalSystemInfo = AdditionalSystemInterpreterInfo.loadAdditionalSystemInfo(iManager, interpreterInfo.getExecutableOrJar());
	        			} catch (MisconfigurationException e1) {
	        				loadedAdditionalSystemInfo = false;
	        			}
	        			if (!loadedAdditionalSystemInfo) {
	                	
		                    try {
		                        JobProgressComunicator jobProgressComunicator = new JobProgressComunicator(
		                        		monitorArg, "PyDev... Restoring indexes for: "+interpreterInfo.getNameForUI(), 
		                        		IProgressMonitor.UNKNOWN, this);
		                        notifyDefaultPythonpathRestored(iManager, interpreterInfo.getExecutableOrJar(), jobProgressComunicator);
		                        jobProgressComunicator.done();
		                    } catch (Exception e) {
		                        Log.log(e);
		                    }
	        			}
        			}
                    return Status.OK_STATUS;
                }

            };
            j.setPriority(Job.SHORT);
            j.schedule();
        }
    }

    /**
     * Restores the info for a module manager
     * 
     * @param monitor a monitor to keep track of the progress
     * @param m the module manager
     * @param nature the associated nature (may be null if there is no associated nature -- as is the case when
     * restoring system info).
     * 
     * @return the info generated from the module manager
     */
    private AbstractAdditionalTokensInfo restoreInfoForModuleManager(IProgressMonitor monitor, IModulesManager m, String additionalFeedback, 
            AbstractAdditionalTokensInfo info, PythonNature nature, int grammarVersion) {
        if(monitor == null){
            monitor = new NullProgressMonitor();
        }
        //TODO: Check if keeping a zip file open makes things faster...
        //Timer timer = new Timer();
        ModulesKey[] allModules = m.getOnlyDirectModules();
        int i = 0;
        
        FastStringBuffer msgBuffer = new FastStringBuffer();

        for (ModulesKey key : allModules) {
            if(monitor.isCanceled()){
                return null;
            }
            i++;

            if (PythonPathHelper.canAddAstInfoFor(key)) { //otherwise it should be treated as a compiled module (no ast generation)

                if(i % 17 == 0){
                    msgBuffer.clear();
                    msgBuffer.append("Creating ");
                    msgBuffer.append(additionalFeedback);
                    msgBuffer.append(" additional info (" );
                    msgBuffer.append(i );
                    msgBuffer.append(" of " );
                    msgBuffer.append(allModules.length );
                    msgBuffer.append(") for " );
                    msgBuffer.append(key.file.getName());
                    monitor.setTaskName(msgBuffer.toString());
                    monitor.worked(1);
                }

                try {
                    if (info.addAstInfo(key, false) == null) {
                        String str = "Unable to generate ast -- using %s.\nError:%s";
                        ErrorDescription errorDesc = null;
                        throw new RuntimeException(StringUtils.format(str, 
                                PyParser.getGrammarVersionStr(grammarVersion),
                                (errorDesc!=null && errorDesc.message!=null)?
                                        errorDesc.message:"unable to determine"));
                    }

                } catch (Throwable e) {
                    Log.log(IStatus.ERROR, "Problem parsing the file :" + key.file + ".", e);
                }
            }
        }
        //timer.printDiff("Time to restore additional info");
        return info;
    }


    public void notifyProjectPythonpathRestored(final PythonNature nature, IProgressMonitor monitor) {
        try {
        	//Note: at this point we're 100% certain that the ast manager is there.
            IModulesManager m = nature.getAstManager().getModulesManager();
            IProject project = nature.getProject();
            
            AbstractAdditionalDependencyInfo currInfo = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature);
            if(currInfo != null){
                currInfo.clearAllInfo();
            }
            
            AdditionalProjectInterpreterInfo newProjectInfo = new AdditionalProjectInterpreterInfo(project);
            String feedback = "(project:" + project.getName() + ")";
            synchronized(m){
                AbstractAdditionalDependencyInfo info = (AbstractAdditionalDependencyInfo) restoreInfoForModuleManager(
                        monitor, m, feedback, newProjectInfo, nature, nature.getGrammarVersion());
    
                if (info != null) {
                    //ok, set it and save it
                    AdditionalProjectInterpreterInfo.setAdditionalInfoForProject(project, info);
                    AdditionalProjectInterpreterInfo.saveAdditionalInfoForProject(nature);
                }
            }
        } catch (Exception e) {
            Log.log(e);
            throw new RuntimeException(e);
        }
    }

    public void notifyNatureRecreated(final PythonNature nature, IProgressMonitor monitor) {
        boolean loadAdditionalInfoForProject;
		try {
			loadAdditionalInfoForProject = AdditionalProjectInterpreterInfo.loadAdditionalInfoForProject(nature);
		} catch (MisconfigurationException e) {
		    Log.log(e);
			loadAdditionalInfoForProject = false;
		}
		if(!loadAdditionalInfoForProject){
            if(DEBUG_INTERPRETER_OBSERVER){
                System.out.println("Unable to load the info correctly... restoring info from the pythonpath");
            }
            notifyProjectPythonpathRestored(nature, monitor);
        }
    }

}
