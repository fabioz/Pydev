from __future__ import generators
from bike.globals import *
from bike.parsing.fastparserast import getRoot, Function, Class, Module, getModule
from bike.parsing.parserutils import generateLogicalLines, makeLineParseable, UnbalancedBracesException, generateLogicalLinesAndLineNumbers
from bike.parsing.newstuff import getSourceNodesContainingRegex
from bike.parsing import visitor
from bike import log
import compiler
from compiler.ast import Getattr, Name
import re

class Match:
    def __repr__(self):
        return ",".join([self.filename, str(self.lineno), str(self.colno),
                         str(self.confidence)])
    def __eq__(self,other):
        if self is None or other is None:
            return False
        return self.filename == other.filename and \
               self.lineno == other.lineno and \
               self.colno == other.colno

def getScopeForLine(sourceNode, lineno):
    scope = None
    childnodes = sourceNode.getFlattenedListOfFastParserASTNodes()
    if childnodes == []:
        return sourceNode.fastparseroot #module node

    scope = sourceNode.fastparseroot

    for node in childnodes:
        if node.linenum > lineno: break
        scope = node

    if scope.getStartLine() != scope.getEndLine(): # is inline
        while scope.getEndLine() <= lineno:
            scope = scope.getParent()
    return scope



# global from the perspective of 'contextFilename'
def globalScanForMatches(contextFilename, matchFinder, targetname):
    for sourcenode in getSourceNodesContainingRegex(targetname, contextFilename):
        print >> log.progress, "Scanning", sourcenode.filename
        searchscope = sourcenode.fastparseroot
        for match in scanScopeForMatches(sourcenode,searchscope,
                                         matchFinder,targetname):
            yield match


def scanScopeForMatches(sourcenode,scope,matchFinder,targetname):
    lineno = scope.getStartLine()
    for line in generateLogicalLines(scope.getMaskedLines()):
        if line.find(targetname) != -1:
            doctoredline = makeLineParseable(line)
            ast = compiler.parse(doctoredline)
            scope = getScopeForLine(sourcenode, lineno)
            matchFinder.reset(line)
            matchFinder.setScope(scope)
            matches = visitor.walk(ast, matchFinder).getMatches()
            for index, confidence in matches:
                match = Match()
                match.filename = sourcenode.filename
                match.sourcenode = sourcenode
                x, y = indexToCoordinates(line, index)
                match.lineno = lineno+y
                match.colno = x
                match.colend = match.colno+len(targetname)
                match.confidence = confidence
                yield match
        lineno+=line.count("\n")
    

def walkLinesContainingStrings(scope,astWalker,targetnames):
    lineno = scope.getStartLine()
    for line in generateLogicalLines(scope.getMaskedLines()):
        if lineContainsOneOf(line,targetnames):
            doctoredline = makeLineParseable(line)
            ast = compiler.parse(doctoredline)
            astWalker.lineno = lineno
            matches = visitor.walk(ast, astWalker)
        lineno+=line.count("\n")


def lineContainsOneOf(line,targetnames):
    for name in targetnames:
        if line.find(name) != -1:
            return True
    return False


# translates an idx in a logical line into physical line coordinates
# returns x and y coords
def indexToCoordinates(src, index):
    y = src[: index].count("\n")
    startOfLineIdx = src.rfind("\n", 0, index)+1
    x = index-startOfLineIdx
    return x, y



# interface for MatchFinder classes
# implement the visit methods
class MatchFinder:
    def setScope(self, scope):
        self.scope = scope

    def reset(self, line):
        self.matches = []
        self.words = re.split("(\w+)", line) # every other one is a non word
        self.positions = []
        i = 0
        for word in self.words:
            self.positions.append(i)
            #if '\n' in word:  # handle newlines
            #    i = len(word[word.index('\n')+1:])
            #else:
            i+=len(word)
        self.index = 0

    def getMatches(self):
        return self.matches

    # need to visit childnodes in same order as they appear
    def visitPrintnl(self,node):
        if node.dest:
            self.visit(node.dest)
        for n in node.nodes:
            self.visit(n)
    
    def visitName(self, node):
        self.popWordsUpTo(node.name)

    def visitClass(self, node):
        self.popWordsUpTo(node.name)
        for base in node.bases:
            self.visit(base)

    def zipArgs(self, argnames, defaults):
        """Takes a list of argument names and (possibly a shorter) list of
        default values and zips them into a list of pairs (argname, default).
        Defaults are aligned so that the last len(defaults) arguments have
        them, and the first len(argnames) - len(defaults) pairs have None as a
        default.
        """
        fixed_args = len(argnames) - len(defaults)
        defaults = [None] * fixed_args + list(defaults)
        return zip(argnames, defaults)

    def visitFunction(self, node):
        self.popWordsUpTo(node.name)
        for arg, default in self.zipArgs(node.argnames, node.defaults):
            self.popWordsUpTo(arg)
            if default is not None:
                self.visit(default)
        self.visit(node.code)

    def visitGetattr(self,node):
        self.visit(node.expr)
        self.popWordsUpTo(node.attrname)

    def visitAssName(self, node):
        self.popWordsUpTo(node.name)

    def visitAssAttr(self, node):
        self.visit(node.expr)
        self.popWordsUpTo(node.attrname)

    def visitImport(self, node):
        for name, alias in node.names:
            for nameelem in name.split("."):
                self.popWordsUpTo(nameelem)
            if alias is not None:
                self.popWordsUpTo(alias)

    def visitFrom(self, node):
        for elem in node.modname.split("."):
            self.popWordsUpTo(elem)
        for name, alias in node.names:
            self.popWordsUpTo(name)
            if alias is not None:
                self.popWordsUpTo(alias)

    def visitLambda(self, node):
        for arg, default in self.zipArgs(node.argnames, node.defaults):
            self.popWordsUpTo(arg)
            if default is not None:
                self.visit(default)
        self.visit(node.code)

    def visitGlobal(self, node):
        for name in node.names:
            self.popWordsUpTo(name)

    def popWordsUpTo(self, word):
        if word == "*":
            return        # won't be able to find this
        posInWords = self.words.index(word)
        idx = self.positions[posInWords]
        self.words = self.words[posInWords+1:]
        self.positions = self.positions[posInWords+1:]

    def appendMatch(self,name,confidence=100):
        idx = self.getNextIndexOfWord(name)
        self.matches.append((idx, confidence))

    def getNextIndexOfWord(self,name):
        return self.positions[self.words.index(name)]

class CouldNotLocateNodeException(Exception): pass

def translateSourceCoordsIntoASTNode(filename,lineno,col):
    module = getModule(filename)
    maskedlines = module.getMaskedModuleLines()
    lline,backtrackchars = getLogicalLine(module, lineno)
    doctoredline = makeLineParseable(lline)
    ast = compiler.parse(doctoredline)
    idx = backtrackchars+col
    nodefinder = ASTNodeFinder(lline,idx)
    node = compiler.walk(ast, nodefinder).node
    if node is None:
        raise CouldNotLocateNodeException("Could not translate editor coordinates into source node")
    return node

def getLogicalLine(module, lineno):
    # we know that the scope is the start of a logical line, so
    # we search from there
    scope = getScopeForLine(module.getSourceNode(), lineno)
    linegenerator = \
            module.generateLinesWithLineNumbers(scope.getStartLine())
    for lline,llinenum in \
            generateLogicalLinesAndLineNumbers(linegenerator):
        if llinenum > lineno:
            break
        prevline = lline
        prevlinenum = llinenum

    backtrackchars = 0
    for i in range(prevlinenum,lineno):
        backtrackchars += len(module.getSourceNode().getLines()[i-1])
    return prevline, backtrackchars



class ASTNodeFinder(MatchFinder):
    # line is a masked line of text
    # lineno and col are coords
    def __init__(self,line,col):
        self.line = line
        self.col = col
        self.reset(line)
        self.node = None

    def visitName(self,node):
        if self.checkIfNameMatchesColumn(node.name):
            self.node = node
        self.popWordsUpTo(node.name)

    def visitGetattr(self,node):
        self.visit(node.expr)
        if self.checkIfNameMatchesColumn(node.attrname):
            self.node = node
        self.popWordsUpTo(node.attrname)

    def visitFunction(self, node):
        if self.checkIfNameMatchesColumn(node.name):
            self.node = node
        self.popWordsUpTo(node.name)

        for arg, default in self.zipArgs(node.argnames, node.defaults):
            if self.checkIfNameMatchesColumn(arg):
                self.node = Name(arg)
            self.popWordsUpTo(arg)
            if default is not None:
                self.visit(default)
        self.visit(node.code)


    visitAssName = visitName
    visitAssAttr = visitGetattr

    def visitClass(self, node):
        if self.checkIfNameMatchesColumn(node.name):
            self.node = node
        self.popWordsUpTo(node.name)
        for base in node.bases:
            self.visit(base)


    def checkIfNameMatchesColumn(self,name):
        idx = self.getNextIndexOfWord(name)
        #print "name",name,"idx",idx,"self.col",self.col
        if idx <= self.col and idx+len(name) > self.col:
            return 1
        return 0

    def visitFrom(self, node):
        for elem in node.modname.split("."):
            self.popWordsUpTo(elem)
        for name, alias in node.names:
            if self.checkIfNameMatchesColumn(name):
                self.node = self._manufactureASTNodeFromFQN(name)
                return
            self.popWordsUpTo(name)
            if alias is not None:
                self.popWordsUpTo(alias)

    # gets round the fact that imports etc dont contain nested getattr
    # nodes for fqns (e.g. import a.b.bah) by converting the fqn
    # string into a getattr instance
    def _manufactureASTNodeFromFQN(self,fqn):
        if "." in fqn:
            assert 0, "getattr not supported yet"
        else:
            return Name(fqn)

def isAMethod(scope,node):
    return isinstance(node,compiler.ast.Function) and \
           isinstance(scope,Class)

def convertNodeToMatchObject(node,confidence=100):
    m = Match()
    m.sourcenode = node.module.getSourceNode()
    m.filename = node.filename
    if isinstance(node,Module):
        m.lineno = 1
        m.colno = 0
    elif isinstance(node,Class) or isinstance(node,Function):
        m.lineno = node.getStartLine()
        m.colno = node.getColumnOfName()
    m.confidence = confidence
    return m
