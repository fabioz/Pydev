import threading
import time
from pydevd_constants import *  #@UnusedWildImport
from pydevd_file_utils import GetFilenameAndBase
threadingCurrentThread = threading.currentThread

DEBUG = False

#=======================================================================================================================
# CustomFramesContainer
#=======================================================================================================================
class CustomFramesContainer:
    
    custom_frames_lock = threading.Lock()
    
    # custom_frames can only be accessed if properly locked with custom_frames_lock! 
    # key is a string identifying the frame (as well as the thread it belongs too) 
    # value is a tuple(string, frame, int), where:
    #
    # 0 = string with the representation of that frame
    # 1 = the frame to show
    # 2 = an integer identifying the last time the frame was changed.
    custom_frames = {}
    
    # Only to be used in this module
    _next_frame_id = 0
    
    # This is the event we must set to release an internal process events. It's later set by the actual debugger
    # when we do create the debugger.
    _py_db_command_thread_event = Null()
    

def addCustomFrame(frame, name):
    CustomFramesContainer.custom_frames_lock.acquire()
    try:
        curr_thread_id = GetThreadId(threadingCurrentThread())
        next_id = CustomFramesContainer._next_frame_id = CustomFramesContainer._next_frame_id + 1
        
        # Note: the frame id kept contains an id and thread information on the thread where the frame was added
        # so that later on we can check if the frame is from the current thread by doing frameId.endswith('|'+thread_id).
        frameId = '__frame__:%s|%s' % (next_id, curr_thread_id)
        if DEBUG:
            sys.stderr.write('addCustomFrame: %s (%s) %s %s\n' % (frameId, GetFilenameAndBase(frame)[1], frame.f_lineno, frame.f_code.co_name))

        CustomFramesContainer.custom_frames[frameId] = (name, frame, 0)
        CustomFramesContainer._py_db_command_thread_event.set()
        return frameId
    finally:
        CustomFramesContainer.custom_frames_lock.release()


def replaceCustomFrame(frameId, frame, name=None):
    CustomFramesContainer.custom_frames_lock.acquire()
    try:
        if DEBUG:
            sys.stderr.write('replaceCustomFrame: %s\n' % frameId)
        try:
            old = CustomFramesContainer.custom_frames[frameId]
            name = name or old[0]
            old_change_i = old[2]
        except:
            sys.stderr.write('Unable to get frame to replace: %s\n' % (frameId,))
            import traceback;traceback.print_exc()
            name = 'Unknown'
            old_change_i = 0
            
        CustomFramesContainer.custom_frames[frameId] = (name, frame, old_change_i + 1)
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
        if DEBUG:
            sys.stderr.write('removeCustomFrame: %s\n' % frameId)
        DictPop(CustomFramesContainer.custom_frames, frameId, None)
        CustomFramesContainer._py_db_command_thread_event.set()
    finally:
        CustomFramesContainer.custom_frames_lock.release()
