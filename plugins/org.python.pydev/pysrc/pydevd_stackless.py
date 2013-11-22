from pydevd_constants import *  #@UnusedWildImport
import sys  #@Reimport
import stackless  #@UnresolvedImport
from pydevd_tracing import SetTrace
from pydevd_custom_frames import replaceCustomFrame, removeCustomFrame, addCustomFrame
from pydevd_comm import GetGlobalDebugger

id_to_tasklet_frame = {}

_application_set_schedule_callback = None

#=======================================================================================================================
# _schedule_callback
#=======================================================================================================================
def _schedule_callback(prev, next):
    '''
    Called when a context is stopped or a new context is made runnable.
    '''
    try:
        if not prev and not next:
            return
        
        if not prev:
            # Ok, making next runnable: set the tracing facility in it.
            debugger = GetGlobalDebugger()
            if debugger is not None:
                next.frame.f_trace = debugger.trace_dispatch
            
        elif not next:
            pass
        
        else:
            # Suspending prev and resuming next
            #print'changing', prev, next  
            frameId = id_to_tasklet_frame.get(id(prev))
            if frameId is not None:
                if prev.frame is not None:
                    replaceCustomFrame(frameId, prev.frame.f_back)
                else:
                    removeCustomFrame(frameId)

            #Check: next.frame.f_trace:
    except:
        import traceback;traceback.print_exc()
        
    if _application_set_schedule_callback is not None:
        return _application_set_schedule_callback(prev, next)


#=======================================================================================================================
# __call__
#=======================================================================================================================
def __call__(self, *args, **kwargs):
    '''
    Called when a new tasklet: at this point we add the frame for the debugger to track.
    '''
    caller_frame = sys._getframe().f_back
    
    f = self.tempval
    def new_f(old_f, args, kwargs):
        debugger = GetGlobalDebugger()
        if debugger is not None:
            SetTrace(debugger.trace_dispatch)
            
        frameId = addCustomFrame(caller_frame, 'Tasklet')
        tasklet_id = id(self)
        id_to_tasklet_frame[tasklet_id] = frameId
        try:
            # Note: if the debugger appears in the line below, it means that a tasklet was created
            # but it's still not running.
    
            # Hover old_f to see the stackless being created and *args and **kwargs to see its parameters.
            old_f(*args, **kwargs)
        finally:
            removeCustomFrame(frameId)
            del id_to_tasklet_frame[tasklet_id]

    #This is the way to tell stackless that the function it should execute is our function, not the original one.
    self.tempval = new_f
    stackless.tasklet.setup(self, f, args, kwargs)


_original_run = stackless.run

def run(*args, **kwargs):
    debugger = GetGlobalDebugger()
    if debugger is not None:
        SetTrace(debugger.trace_dispatch)
        
    f_back = sys._getframe().f_back
    frameId = addCustomFrame(f_back, 'Main Tasklet Run')
    tasklet_id = id(f_back)
    id_to_tasklet_frame[tasklet_id] = frameId
    try:
        return _original_run(*args, **kwargs)
    finally:
        removeCustomFrame(frameId)
        del id_to_tasklet_frame[tasklet_id]



#=======================================================================================================================
# patch_stackless
#=======================================================================================================================
def patch_stackless():
    '''
    This function should be called to patch the stackless module so that new tasklets are properly tracked in the 
    debugger.
    '''
    stackless.set_schedule_callback(_schedule_callback)
    def set_schedule_callback(callable):
        global _application_set_schedule_callback
        _application_set_schedule_callback = callable
    
    set_schedule_callback.__doc__ = stackless.set_schedule_callback.__doc__
    stackless.set_schedule_callback = set_schedule_callback
        
    __call__.__doc__ = stackless.tasklet.__call__.__doc__
    stackless.tasklet.__call__ = __call__
        
    run.__doc__ = stackless.run.__doc__
    stackless.run = run
    
patch_stackless = call_only_once(patch_stackless)
