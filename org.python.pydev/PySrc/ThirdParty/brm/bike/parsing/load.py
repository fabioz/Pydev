from bike.globals import *
import os
from bike.parsing.fastparser import fastparser

class Cache:
    def __init__(self):
        self.reset()
    def reset(self):
        self.srcnodecache = {}
        self.typecache = {}
        self.maskedlinescache = {}

    instance = None

Cache.instance = Cache()

class CantLocateSourceNodeException(Exception): pass

def getSourceNode(filename_path):
    #print "getSourceNode:",filename_path
    sourcenode = None
    
    try:
        sourcenode = Cache.instance.srcnodecache[filename_path]
    except KeyError:
        pass
    
    if sourcenode is None:
        from bike.parsing.newstuff import translateFnameToModuleName
        sourcenode = SourceFile.createFromFile(filename_path,
                              translateFnameToModuleName(filename_path))
    if sourcenode is None:
        raise CantLocateSourceNodeException(filename_path)

    Cache.instance.srcnodecache[filename_path]=sourcenode
    return sourcenode

class SourceFile:

    def createFromString(filename, modulename, src):
        return SourceFile(filename,modulename,src)
    createFromString = staticmethod(createFromString)

    def createFromFile(filename,modulename):
        try:
            f = file(filename)
            src = f.read()
            f.close()
        except IOError:
            return None
        else:
            return SourceFile(filename,modulename,src)
                
    createFromFile = staticmethod(createFromFile)
    
    def __init__(self, filename, modulename, src):

        if os.path.isabs(filename):
            self.filename = filename
        else:
            self.filename = os.path.abspath(filename)
        self.modulename = modulename

        self.resetWithSource(src)

    def resetWithSource(self, source):
        # fastparser ast
        self.fastparseroot = fastparser(source,self.modulename,self.filename)
        self.fastparseroot.setSourceNode(self)
        self._lines = source.splitlines(1)
        self.sourcenode = self

    def __repr__(self):
        return "Source(%s,%s)"%('source', self.filename)

    def getChildNodes(self):
        return self.fastparseroot.getChildNodes()        
        
    def getSource(self):
        return  "".join(self.getLines())

    def getLine(self,linenum):
        return self.getLines()[linenum-1]

    # TODO: rename me!
    def getFlattenedListOfFastParserASTNodes(self):
        return self.fastparseroot.getFlattenedListOfChildNodes()
        
    def getLines(self):
        return self._lines




