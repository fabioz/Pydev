from bike import log
from bike.transformer.save import queueFileToSave

_undoStack = None

def getUndoStack(forceNewStack = 0):
    global _undoStack
    if _undoStack is None or forceNewStack:
        _undoStack = UndoStack()
    return _undoStack

class UndoStackEmptyException: pass

class UndoStack(object):
    def __init__(self):
        self.stack = []
        self.stack.append({})
        self.frame = self.stack[-1]
        self.setUndoBufferSize(10)

    def setUndoBufferSize(self, undoBufferSize):
        self.undoBufferSize = undoBufferSize

    def addSource(self, filename, src):
        if filename not in self.frame:
            self.frame[filename] = src

    def commitUndoFrame(self):
        #restrict size of buffer
        while len(self.stack) > self.undoBufferSize:
            #print "clipping undo stack"
            del self.stack[0]

        if len(self.frame) != 0:
            #print "commitUndoFrame"
            self.stack.append({})
            self.frame = self.stack[-1]

    def undo(self, **opts):
        #print "undo called",self.stack
        if len(self.stack) < 2:
            raise UndoStackEmptyException()
        undoframe = self.stack[-2]
        #print "undoframe is",undoframe
        for filename,src in undoframe.iteritems():
            print >>log.progress, "Undoing:",filename
            queueFileToSave(filename,src)
        self.stack = self.stack[:-2]
        self.stack.append({})
        self.frame = self.stack[-1]
