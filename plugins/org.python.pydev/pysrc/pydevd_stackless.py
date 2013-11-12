import sys
import stackless
from pydevd_tracing import SetTrace
from pydevd_custom_frames import replaceCustomFrame, removeCustomFrame, addCustomFrame
from pydevd_comm import GetGlobalDebugger

tasklet_to_id = {}

_additionalContextDispatch = None

#=======================================================================================================================
# _contextDispatch
#=======================================================================================================================
def _contextDispatch(prev, next):
    '''
    Called when a context is stopped.
    '''
    try:
        if not prev and not next:
            return
        
        if not prev:
            debugger = GetGlobalDebugger()
            if debugger is not None:
                next.frame.f_trace = debugger.trace_dispatch
            
        elif not next:
            pass
        
        else:
            #print'changing', prev, next  #suspending prev, resuming next
            frameId = tasklet_to_id.get(id(prev))
            if frameId is not None:
                if prev.frame is not None:
                    replaceCustomFrame(frameId, prev.frame.f_back)
                else:
                    removeCustomFrame(frameId)

            #Check: next.frame.f_trace:
    except:
        import traceback;traceback.print_exc()
        
    if _additionalContextDispatch is not None:
        return _additionalContextDispatch(prev, next)


try:
    def set_schedule_callback(callable):
        _additionalContextDispatch = callable
    
    set_schedule_callback.__doc__ = stackless.set_schedule_callback.__doc__
    stackless.set_schedule_callback = set_schedule_callback
except:
    import traceback;traceback.print_exc()


#=======================================================================================================================
# __call__
#=======================================================================================================================
def __call__(self, *args, **kwargs):
    f = self.tempval
    def new_f(old_f, args, kwargs):
        debugger = GetGlobalDebugger()
        if debugger is not None:
            SetTrace(debugger.trace_dispatch)
            
        frameId = addCustomFrame(sys._getframe())
        tasklet_to_id[id(self)] = frameId
        try:
            # Note: if the debugger appears in the line below, it means that a tasklet was created
            # but it's still not running.
    
            # Hover old_f to see the stackless being created and *args and **kwargs to see its parameters.
            old_f(*args, **kwargs)
        finally:
            removeCustomFrame(frameId)

    #This is the way to tell stackless that the function it should execute is our function, not the original one.
    self.tempval = new_f
    stackless.tasklet.setup(self, f, args, kwargs)


def patch_stackless():
    stackless.set_schedule_callback(_contextDispatch)
    stackless.tasklet.__call__ = __call__
