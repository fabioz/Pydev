import sys
import stackless
from pydevd_comm import GetGlobalDebugger
from pydevd_tracing import SetTrace

import weakref
tasklet_to_id = {}


def contextDispatch(prev, next):
    try:
        if not prev:
            print "Creating ", next
        elif not next:
            print "Destroying ", prev
        else:
            print'changing', prev, next  #suspending prev, resuming next
            frameId = tasklet_to_id.get(id(prev))
            if frameId is not None:
                debugger = GetGlobalDebugger()
                if prev.frame is not None:
                    debugger.replaceCustomFrame(frameId, prev.frame.f_back)
                else:
                    debugger.removeCustomFrame(frameId)

            #Check: next.frame.f_trace:
    except:
        import traceback;traceback.print_exc()

stackless.set_schedule_callback(contextDispatch)

def __call__(self, *args, **kwargs):
    f = self.tempval
    def new_f(old_f, args, kwargs):
        debugger = GetGlobalDebugger()
        SetTrace(debugger.trace_dispatch)
        frameId = debugger.addCustomFrame(sys._getframe())
        tasklet_to_id[id(self)] = frameId

        # Note: if the debugger appears in the line below, it means that a tasklet was created
        # but it's still not running.
        old_f(*args, **kwargs)

        debugger.removeCustomFrame(frameId)

    self.tempval = new_f
    stackless.tasklet.setup(self, f, args, kwargs)

stackless.tasklet.__call__ = __call__
