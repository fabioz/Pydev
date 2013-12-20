from __future__ import nested_scopes
from pydevd_constants import *  #@UnusedWildImport
import stackless  #@UnresolvedImport
from pydevd_tracing import SetTrace
from pydevd_custom_frames import replaceCustomFrame, removeCustomFrame, addCustomFrame
from pydevd_comm import GetGlobalDebugger
import weakref
from pydevd_file_utils import GetFilenameAndBase
from pydevd import DONT_TRACE



#=======================================================================================================================
# _TaskletInfo
#=======================================================================================================================
class _TaskletInfo:

    _last_id = 0

    def __init__(self, tasklet_weakref):
        self.frame_id = None
        self.tasklet_weakref = tasklet_weakref

        _TaskletInfo._last_id += 1
        self.tasklet_name = 'Tasklet %s (%s)' % (_TaskletInfo._last_id, id(tasklet_weakref()))


_weak_tasklet_registered_to_info = {}

#=======================================================================================================================
# get_tasklet_info
#=======================================================================================================================
def get_tasklet_info(tasklet):
    return register_tasklet_info(tasklet)


#=======================================================================================================================
# register_tasklet_info
#=======================================================================================================================
def register_tasklet_info(tasklet):
    r = weakref.ref(tasklet)
    info = _weak_tasklet_registered_to_info.get(r)
    if info is None:
        info = _weak_tasklet_registered_to_info[r] = _TaskletInfo(r)

    return info


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
        
        if next:
            register_tasklet_info(next)

            # Ok, making next runnable: set the tracing facility in it.
            debugger = GetGlobalDebugger()
            if debugger is not None and next.frame:
                next.frame.f_trace = debugger.trace_dispatch
            debugger = None

        if prev:
            register_tasklet_info(prev)

        try:
            for tasklet_ref, tasklet_info in list(_weak_tasklet_registered_to_info.items()):  #Make sure it's a copy!
                tasklet = tasklet_ref()
                if tasklet is None or not tasklet.alive:
                    #Garbage-collected already!
                    if tasklet_info.frame_id is not None:
                        removeCustomFrame(tasklet_info.frame_id)
                    del _weak_tasklet_registered_to_info[tasklet_ref]
                else:
                    if tasklet.paused or tasklet.blocked or tasklet.scheduled:
                        if tasklet.frame and tasklet.frame.f_back:
                            f_back = tasklet.frame.f_back
                            _filename, base = GetFilenameAndBase(f_back)
                            is_file_to_ignore = DictContains(DONT_TRACE, base)
                            if not is_file_to_ignore:
                                if tasklet_info.frame_id is None:
                                    tasklet_info.frame_id = addCustomFrame(f_back, tasklet_info.tasklet_name)
                                else:
                                    replaceCustomFrame(tasklet_info.frame_id, f_back)

                    elif tasklet.is_current:
                        if tasklet_info.frame_id is not None:
                            #Remove info about stackless suspended when it starts to run.
                            removeCustomFrame(tasklet_info.frame_id)
                            tasklet_info.frame_id = None


        finally:
            tasklet = None
            tasklet_info = None
            f_back = None

    except:
        import traceback;traceback.print_exc()

    if _application_set_schedule_callback is not None:
        return _application_set_schedule_callback(prev, next)



_original_setup = stackless.tasklet.setup

#=======================================================================================================================
# setup
#=======================================================================================================================
def setup(self, *args, **kwargs):
    '''
    Called to run a new tasklet: rebind the creation so that we can trace it.
    '''

    f = self.tempval
    def new_f(old_f, args, kwargs):

        debugger = GetGlobalDebugger()
        if debugger is not None:
            SetTrace(debugger.trace_dispatch)

        debugger = None

        #Remove our own traces :)
        self.tempval = old_f
        register_tasklet_info(self)

        # Hover old_f to see the stackless being created and *args and **kwargs to see its parameters.
        return old_f(*args, **kwargs)

    
    #This is the way to tell stackless that the function it should execute is our function, not the original one. Note:
    #setting tempval is the same as calling bind(new_f), but it seems that there's no other way to get the currently
    #bound function, so, keeping on using tempval instead of calling bind (which is actually the same thing in a better
    #API).
    
    self.tempval = new_f
    
    return _original_setup(self, f, args, kwargs)


#=======================================================================================================================
# __call__
#=======================================================================================================================
def __call__(self, *args, **kwargs):
    '''
    Called to run a new tasklet: rebind the creation so that we can trace it.
    '''

    return setup(self, *args, **kwargs)


_original_run = stackless.run


#=======================================================================================================================
# run
#=======================================================================================================================
def run(*args, **kwargs):
    debugger = GetGlobalDebugger()
    if debugger is not None:
        SetTrace(debugger.trace_dispatch)
    debugger = None

    return _original_run(*args, **kwargs)



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

    setup.__doc__ = stackless.tasklet.setup.__doc__
    stackless.tasklet.setup = setup

    run.__doc__ = stackless.run.__doc__
    stackless.run = run

patch_stackless = call_only_once(patch_stackless)
