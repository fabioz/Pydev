from bike import log

outputqueue = {}

def getQueuedFile(filename):
    try:
        return outputqueue[filename]
    except:
        pass
        #print "HERE!"
        

def resetOutputQueue():
    global outputqueue
    outputqueue = {}

def queueFileToSave(filename,src):
    outputqueue[filename] = src
    from bike.parsing.load import getSourceNode
    getSourceNode(filename).resetWithSource(src)

def save():
    from bike.transformer.undo import getUndoStack

    global outputqueue
    savedFiles = []
    for filename,src in outputqueue.iteritems():
        print >> log.progress, "Writing:",filename
        f = file(filename, "w+")
        f.write(outputqueue[filename])
        f.close()
        savedFiles.append(filename)
    outputqueue = {}
    #print "stack is "+ str(getUndoStack().stack)
    getUndoStack().commitUndoFrame()    
    return savedFiles
