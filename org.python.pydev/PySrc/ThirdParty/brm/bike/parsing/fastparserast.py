from __future__ import generators
from parserutils import generateLogicalLines, maskStringsAndComments, maskStringsAndRemoveComments
import re
import os
import compiler
from bike.transformer.save import resetOutputQueue

TABWIDTH = 4

classNameRE = re.compile("^\s*class\s+(\w+)")
fnNameRE = re.compile("^\s*def\s+(\w+)")

_root = None

def getRoot():
    global _root
    if _root is None:
        resetRoot()
    return _root 

def resetRoot(root = None):
    global _root
    _root = root or Root()
    _root.unittestmode = False
    resetOutputQueue()


def getModule(filename_path):
    from bike.parsing.load import CantLocateSourceNodeException, getSourceNode
    try:
        sourcenode = getSourceNode(filename_path)
        return sourcenode.fastparseroot
    except CantLocateSourceNodeException:
        return None

def getPackage(directory_path):
    from bike.parsing.pathutils import getRootDirectory
    rootdir = getRootDirectory(directory_path)
    if rootdir == directory_path:
        return getRoot()
    else:
        return Package(directory_path,
                       os.path.basename(directory_path))




            
class Root:
    def __init__(self, pythonpath = None):
        # singleton hack to allow functions in query package to appear
        # 'stateless'
        resetRoot(self)

        # this is to get round a python optimisation which reuses an
        # empty list as a default arg. unfortunately the client of
        # this method may fill that list, so it's not empty
        if not pythonpath:
            pythonpath = []
        self.pythonpath = pythonpath

    def __repr__(self):
        return "Root()"
        #return "Root(%s)"%(self.getChildNodes())


    # dummy method
    def getChild(self,name):
        return None

class Package:
    def __init__(self, path, name):
        self.path = path
        self.name = name

    def getChild(self,name):
        from bike.parsing.newstuff import getModule
        return getModule(os.path.join(self.path,name+".py"))

    def __repr__(self):
        return "Package(%s,%s)"%(self.path, self.name)

# used so that linenum can be an attribute
class Line(str):
    pass

class StructuralNode:
    def __init__(self, filename, srclines, modulesrc):
        self.childNodes = []
        self.filename = filename
        self._parent = None
        self._modulesrc = modulesrc
        self._srclines = srclines
        self._maskedLines = None

    def addChild(self, node):
        self.childNodes.append(node)
        node.setParent(self)

    def setParent(self, parent):
        self._parent = parent

    def getParent(self):
        return self._parent

    def getChildNodes(self):
        return self.childNodes

    def getChild(self,name):
        matches = [c for c in self.getChildNodes() if c.name == name]
        if matches != []:
            return matches[0]

    def getLogicalLine(self,physicalLineno):
        return generateLogicalLines(self._srclines[physicalLineno-1:]).next()

    # badly named: actually returns line numbers of import statements
    def getImportLineNumbers(self):
        try:
            return self.importlines
        except AttributeError:
            return[]

    def getLinesNotIncludingThoseBelongingToChildScopes(self):
        srclines = self.getMaskedModuleLines()
        lines = []
        lineno = self.getStartLine()
        for child in self.getChildNodes():
            lines+=srclines[lineno-1: child.getStartLine()-1]
            lineno = child.getEndLine()
        lines+=srclines[lineno-1: self.getEndLine()-1]
        return lines


    def generateLinesNotIncludingThoseBelongingToChildScopes(self):
        srclines = self.getMaskedModuleLines()
        lines = []
        lineno = self.getStartLine()
        for child in self.getChildNodes():
            for line in srclines[lineno-1: child.getStartLine()-1]:
                yield self.attachLinenum(line,lineno)
                lineno +=1
            lineno = child.getEndLine()
        for line in srclines[lineno-1: self.getEndLine()-1]:
            yield self.attachLinenum(line,lineno)
            lineno +=1

    def generateLinesWithLineNumbers(self,startline=1):
        srclines = self.getMaskedModuleLines()
        for lineno in range(startline,len(srclines)+1):
            yield self.attachLinenum(srclines[lineno-1],lineno)

    def attachLinenum(self,line,lineno):
        line = Line(line)
        line.linenum = lineno
        return line

    def getMaskedModuleLines(self):
        from bike.parsing.load import Cache
        try:
            maskedlines = Cache.instance.maskedlinescache[self.filename]
        except:
            # make sure src is actually masked
            # (could just have keywords masked)
            maskedsrc = maskStringsAndComments(self._modulesrc)
            maskedlines = maskedsrc.splitlines(1)
            Cache.instance.maskedlinescache[self.filename] = maskedlines
        return maskedlines


class Module(StructuralNode):
    def __init__(self, filename, name, srclines, maskedsrc):
        StructuralNode.__init__(self, filename, srclines, maskedsrc)
        self.name = name
        self.indent = -TABWIDTH
        self.flattenedNodes = []
        self.module = self

    def getMaskedLines(self):
        return self.getMaskedModuleLines()

    def getFlattenedListOfChildNodes(self):
        return self.flattenedNodes

    def getStartLine(self):
        return 1

    def getEndLine(self):
        return len(self.getMaskedModuleLines())+1

    def getSourceNode(self):
        return self.sourcenode

    def setSourceNode(self, sourcenode):
        self.sourcenode = sourcenode

    def matchesCompilerNode(self,node):
        return isinstance(node,compiler.ast.Module) and \
               node.name == self.name

    def getParent(self):
        if self._parent is not None:
            return self._parent
        else:
            from newstuff import getPackage
            return getPackage(os.path.dirname(self.filename))


    def __str__(self):
        return "bike:Module:"+self.filename

indentRE = re.compile("^(\s*)\S")
class Node:
    # module = the module node
    # linenum = starting line number
    def __init__(self, name, module, linenum, indent):
        self.name = name
        self.module = module
        self.linenum = linenum
        self.endline = None
        self.indent = indent

    def getMaskedLines(self):
        return self.getMaskedModuleLines()[self.getStartLine()-1:self.getEndLine()-1]

    def getStartLine(self):
        return self.linenum

    def getEndLine(self):
        if self.endline is None:
            physicallines = self.getMaskedModuleLines()
            lineno = self.linenum
            logicallines = generateLogicalLines(physicallines[lineno-1:])

            # skip the first line, because it's the declaration
            line = logicallines.next()
            lineno+=line.count("\n")

            # scan to the end of the fn
            for line in logicallines:
                #print lineno,":",line,
                match = indentRE.match(line)
                if match and match.end()-1 <= self.indent:
                    break
                lineno+=line.count("\n")
            self.endline = lineno
        return self.endline

    # linenum starts at 0
    def getLine(self, linenum):
        return self._srclines[(self.getStartLine()-1) + linenum]


baseClassesRE = re.compile("class\s+[^(]+\(([^)]+)\):")

class Class(StructuralNode, Node):
    def __init__(self, name, filename, module, linenum, indent, srclines, maskedmodulesrc):
        StructuralNode.__init__(self, filename, srclines, maskedmodulesrc)
        Node.__init__(self, name, module, linenum, indent)
        self.type = "Class"

    
    def getBaseClassNames(self):
        #line = self.getLine(0)
        line = self.getLogicalLine(self.getStartLine())
        match = baseClassesRE.search(line)
        if match:
            return [s.strip()for s in match.group(1).split(",")]
        else:
            return []

    def getColumnOfName(self):
        match = classNameRE.match(self.getLine(0))
        return match.start(1)

    def __repr__(self):
        return "<bike:Class:%s>" % self.name

    def __str__(self):
        return "bike:Class:"+self.filename+":"+\
               str(self.getStartLine())+":"+self.name

    def matchesCompilerNode(self,node):
        return isinstance(node,compiler.ast.Class) and \
               node.name == self.name

    def __eq__(self,other):
        return isinstance(other,Class) and \
               self.filename == other.filename and \
               self.getStartLine() == other.getStartLine()

# describes an instance of a class
class Instance:
    def __init__(self, type):
        assert type is not None
        self._type = type

    def getType(self):
        return self._type

    def __str__(self):
        return "Instance(%s)"%(self.getType())


class Function(StructuralNode, Node):
    def __init__(self, name, filename, module, linenum, indent,
                 srclines, maskedsrc):
        StructuralNode.__init__(self, filename, srclines, maskedsrc)
        Node.__init__(self, name, module, linenum, indent)
        self.type = "Function"

    def getColumnOfName(self):
        match = fnNameRE.match(self.getLine(0))
        return match.start(1)

    def __repr__(self):
        return "<bike:Function:%s>" % self.name

    def __str__(self):
        return "bike:Function:"+self.filename+":"+\
               str(self.getStartLine())+":"+self.name

    def matchesCompilerNode(self,node):
        return isinstance(node,compiler.ast.Function) and \
               node.name == self.name


