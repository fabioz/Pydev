import threading
import time
from pydevd_constants import *  #@UnusedWildImport
threadingCurrentThread = threading.currentThread

#=======================================================================================================================
# CustomFramesContainer
#=======================================================================================================================
class CustomFramesContainer:
    
    custom_frames_lock = threading.Lock()
    
    # custom_frames can only be accessed if properly locked with custom_frames_lock! 
    custom_frames = {}
    
    # Only to be used in this module
    _next_frame_id = 0
    
    # This is the event we must set to release an internal process events. It's later set by the actual debugger
    # when we do create the debugger.
    _py_db_command_thread_event = Null()
    

def addCustomFrame(frame):
    CustomFramesContainer.custom_frames_lock.acquire()
    try:
        curr_thread_id = GetThreadId(threadingCurrentThread())
        next_id = CustomFramesContainer._next_frame_id = CustomFramesContainer._next_frame_id + 1
        
        # Note: the frame id kept contains an id and thread information on the thread where the frame was added
        # so that later on we can check if the frame is from the current thread by doing frameId.endswith('|'+thread_id).
        frameId = '__frame__:%s|%s' % (next_id, curr_thread_id)

        CustomFramesContainer.custom_frames[frameId] = ('Tasklet', frame, time.time())
        CustomFramesContainer._py_db_command_thread_event.set()
        return frameId
    finally:
        CustomFramesContainer.custom_frames_lock.release()


def replaceCustomFrame(frameId, frame):
    CustomFramesContainer.custom_frames_lock.acquire()
    try:
        CustomFramesContainer.custom_frames[frameId] = ('Tasklet', frame, time.time())
        CustomFramesContainer._py_db_command_thread_event.set()
    finally:
        CustomFramesContainer.custom_frames_lock.release()


def getCustomFrame(frameId):
    CustomFramesContainer.custom_frames_lock.acquire()
    try:
        return CustomFramesContainer.custom_frames[frameId][1]
    finally:
        CustomFramesContainer.custom_frames_lock.release()
    
    
def removeCustomFrame(frameId):
    CustomFramesContainer.custom_frames_lock.acquire()
    try:
        CustomFramesContainer.custom_frames.pop(frameId, None)
        CustomFramesContainer._py_db_command_thread_event.set()
    finally:
        CustomFramesContainer.custom_frames_lock.release()