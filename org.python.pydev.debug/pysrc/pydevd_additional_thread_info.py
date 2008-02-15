import sys
from pydevd_constants import * #@UnusedWildImport
import threading
from pydevd_frame import PyDBFrame
import weakref

#=======================================================================================================================
# AbstractPyDBAdditionalThreadInfo
#=======================================================================================================================
class AbstractPyDBAdditionalThreadInfo:
    def __init__(self):
        self.pydev_state = STATE_RUN 
        self.pydev_step_stop = None
        self.pydev_step_cmd = None
        self.pydev_notify_kill = False

        
    def IterFrames(self):
        raise NotImplementedError()
    
    def CreateDbFrame(self, mainDebugger, filename, base, additionalInfo, t, frame):
        raise NotImplementedError()
    
    def __str__(self):
        return 'State:%s Stop:%s Cmd: %s Kill:%s' % (self.pydev_state, self.pydev_step_stop, self.pydev_step_cmd, self.pydev_notify_kill)

    
#=======================================================================================================================
# PyDBAdditionalThreadInfoWithCurrentFramesSupport
#=======================================================================================================================
class PyDBAdditionalThreadInfoWithCurrentFramesSupport(AbstractPyDBAdditionalThreadInfo):
    
    def IterFrames(self):
        #sys._current_frames(): dictionary with thread id -> topmost frame
        return sys._current_frames().values() #return a copy... don't know if it's changed if we did get an iterator

    #just create the db frame directly
    CreateDbFrame = PyDBFrame
    
#=======================================================================================================================
# PyDBAdditionalThreadInfoWithoutCurrentFramesSupport
#=======================================================================================================================
class PyDBAdditionalThreadInfoWithoutCurrentFramesSupport(AbstractPyDBAdditionalThreadInfo):
    
    def __init__(self):
        AbstractPyDBAdditionalThreadInfo.__init__(self)
        #That's where the last frame entered is kept. That's needed so that we're able to 
        #trace contexts that were previously untraced and are currently active. So, the bad thing
        #is that the frame may be kept alive longer than it would if we go up on the frame stack,
        #and is only disposed when some other frame is removed.
        #A better way would be if we could get the topmost frame for each thread, but that's 
        #not possible (until python 2.5 -- which is the PyDBAdditionalThreadInfoWithCurrentFramesSupport version)
        
        #NOT RLock!! (could deadlock if it was)
        self.lock = threading.Lock()
        
        #collection with the refs
        self.pydev_existing_frames = {}
        
    def _OnDbFrameCollected(self, ref):
        '''
            Callback to be called when a given reference is garbage-collected.
        '''
        self.lock.acquire()
        try:
            del self.pydev_existing_frames[ref]
        finally:
            self.lock.release()
        
    
    def _AddDbFrame(self, db_frame):
        self.lock.acquire()
        try:
            #create the db frame with a callback to remove it from the dict when it's garbage-collected
            #(could be a set, but that's not available on all versions we want to target).
            r = weakref.ref(db_frame, self._OnDbFrameCollected)
            self.pydev_existing_frames[r] = r
        finally:
            self.lock.release()
    
        
    def CreateDbFrame(self, mainDebugger, filename, base, additionalInfo, t, frame):
        #the frame must be cached as a weak-ref (we return the actual db frame -- which will be kept
        #alive until its trace_dispatch method is not referenced anymore).
        #that's a large workaround because:
        #1. we can't have weak-references to python frame object
        #2. only from 2.5 onwards we have _current_frames support from the interpreter
        db_frame = PyDBFrame(mainDebugger, filename, base, additionalInfo, t, frame)
        db_frame.frame = frame
        self._AddDbFrame(db_frame)
        return db_frame
    
    def IterFrames(self):
        #we may not have yield, so, lets create a list for the iteration
        self.lock.acquire()
        try:
            ret = []
            
            weak_db_frames = self.pydev_existing_frames.keys()
            
            for weak_db_frame in weak_db_frames:
                try:
                    ret.append(weak_db_frame().frame)
                except AttributeError:
                    pass #ok, garbage-collected already
            return ret
        finally:
            self.lock.release()

    def __str__(self):
        return 'State:%s Stop:%s Cmd: %s Kill:%s Frames:%s' % (self.pydev_state, self.pydev_step_stop, self.pydev_step_cmd, self.pydev_notify_kill, len(self.IterFrames()))

#=======================================================================================================================
# NOW, WE HAVE TO DEFINE WHICH THREAD INFO TO USE
# (whether we have to keep references to the frames or not)
# from version 2.5 onwards, we can use sys._current_frames to get a dict with the threads
# and frames, but to support other versions, we can't rely on that.
#=======================================================================================================================
try:
    sys._current_frames #@UndefinedVariable
    PyDBAdditionalThreadInfo = PyDBAdditionalThreadInfoWithCurrentFramesSupport
except AttributeError:
    PyDBAdditionalThreadInfo = PyDBAdditionalThreadInfoWithoutCurrentFramesSupport
    
