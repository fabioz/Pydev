import sys
from pydevd_constants import *
import threading
import pydevd_frame
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
        ret = []
        for f in sys._current_frames().values():
            ret.append(f)
        return ret
    
    def CreateDbFrame(self, mainDebugger, filename, base, additionalInfo, t, frame):
        #no need to pass the frame
        return pydevd_frame.PyDBFrame(mainDebugger, filename, base, additionalInfo, t)
    
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
        #A better way would be if we could get the topmost frame for each thread, but that's currently
        #not possible.
        self.lock = threading.RLock()
        self.pydev_last_frame = []
        self.pydev_i_frames_added = 0
        
    def _AddDbFrame(self, db_frame):
        self.lock.acquire()
        try:
            self.pydev_last_frame.append(weakref.ref(db_frame))
            
            #after creating some, make a little cleanup in the list
            self.pydev_i_frames_added += 1
            if self.pydev_i_frames_added > 20:
                self.ClearDeadDbFrames()
                self.pydev_i_frames_added = 0
            self.ClearDeadDbFrames()
        finally:
            self.lock.release()
    
    def ClearDeadDbFrames(self):
        self.lock.acquire()
        try:
            weak_db_frames = self.pydev_last_frame[:]
            weak_db_frames.reverse()
            
            enumerated = range(len(weak_db_frames))
            enumerated.reverse()
            
            for i, weak_db_frame in zip(enumerated, weak_db_frames):
                db_frame = weak_db_frame()
                if db_frame is None:
                    del self.pydev_last_frame[i]
        finally:
            self.lock.release()
        
    def CreateDbFrame(self, mainDebugger, filename, base, additionalInfo, t, frame):
        #the frame must be cached
        db_frame = pydevd_frame.PyDBFrame(mainDebugger, filename, base, additionalInfo, t)
        db_frame.frame = frame
        self._AddDbFrame(db_frame)
        return db_frame
    
    def IterFrames(self):
        self.lock.acquire()
        try:
            ret = []
            
            weak_db_frames = self.pydev_last_frame[:]
            weak_db_frames.reverse()
            
            enumerated = range(len(weak_db_frames))
            enumerated.reverse()
            
            for i, weak_db_frame in zip(enumerated, weak_db_frames):
                db_frame = weak_db_frame()
                if db_frame is None:
                    del self.pydev_last_frame[i]
                else:
                    ret.append(db_frame.frame)
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
    sys._current_frames
    PyDBAdditionalThreadInfo = PyDBAdditionalThreadInfoWithCurrentFramesSupport
except AttributeError:
    PyDBAdditionalThreadInfo = PyDBAdditionalThreadInfoWithoutCurrentFramesSupport
    
