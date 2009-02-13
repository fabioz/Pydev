package com.python.pydev.analysis.builder;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.plugin.PydevPlugin;

public abstract class AbstractAnalysisBuilderRunnable implements IAnalysisBuilderRunnable{

    
    // -------------------------------------------------------------------------------------------- ATTRIBUTES

    final protected IProgressMonitor internalCancelMonitor = new NullProgressMonitor();
    final protected String moduleName;
    final protected boolean isFullBuild;
    final protected boolean forceAnalysis;
    final protected int analysisCause;
    final private Object lock = new Object();
    
    protected IPythonNature nature;
    protected volatile boolean runFinished = false;
    private IAnalysisBuilderRunnable oldAnalysisBuilderThread;
    
    // ---------------------------------------------------------------------------------------- END ATTRIBUTES
    
    public AbstractAnalysisBuilderRunnable(boolean isFullBuild, String moduleName, boolean forceAnalysis, 
            int analysisCause, IAnalysisBuilderRunnable oldAnalysisBuilderThread, IPythonNature nature) {
        this.isFullBuild = isFullBuild;
        this.moduleName = moduleName;
        this.forceAnalysis = forceAnalysis;
        this.analysisCause = analysisCause;
        this.oldAnalysisBuilderThread = oldAnalysisBuilderThread;
        this.nature = nature;
    }
    
    
    public int getAnalysisCause() {
        return analysisCause;
    }
    
    public boolean getForceAnalysis() {
        return forceAnalysis;
    }
    
    public synchronized boolean getRunFinished() {
        return runFinished;
    }

    public String getModuleName() {
        return moduleName;
    }
    
    public void run() {
        try{
            try{
                if(oldAnalysisBuilderThread != null){
                    if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                        Log.toLogFile(this, "Waiting for other to be finished...");
                    }
                    int attempts = 0;
                    while(!oldAnalysisBuilderThread.getRunFinished()){
                        attempts += 1;
                        synchronized (lock) {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                //ignore
                                e.printStackTrace();
                            }
                        }
                    }                    
                    if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                        Log.toLogFile(this, "Starting analysis after attempts: "+attempts);
                    }
                }
                //that's all we need it for... we can already dispose of it.
                this.oldAnalysisBuilderThread = null;
                
                doAnalysis();
            }catch(NoClassDefFoundError e){
                //ignore, plugin finished and thread still active
            }
            

        } catch (OperationCanceledException e) {
            //ok, ignore it
            if(DebugSettings.DEBUG_ANALYSIS_REQUESTS){
                Log.toLogFile(this, "OperationCanceledException: cancelled by new runnable");
            }
        } catch (Exception e){
            PydevPlugin.log(e);
        } finally{
            try{
                AnalysisBuilderRunnableFactory.removeFromThreads(moduleName, this);
            }catch (Throwable e){
                PydevPlugin.log(e);
            }finally{
                runFinished=true;
            }
            
            dispose();
        }
    }

    protected void dispose(){
        this.nature = null;
        this.oldAnalysisBuilderThread = null;
    }

    protected abstract void doAnalysis();
    
    

    public synchronized void stopAnalysis() {
        this.internalCancelMonitor.setCanceled(true);
    }
    
    protected void checkStop(){
        if(this.internalCancelMonitor.isCanceled()){
            throw new OperationCanceledException();
        }
    }
    
}
