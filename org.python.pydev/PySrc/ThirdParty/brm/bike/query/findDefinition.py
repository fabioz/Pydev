from __future__ import generators
from bike.query.common import Match, MatchFinder, \
     getScopeForLine, indexToCoordinates, \
     translateSourceCoordsIntoASTNode, scanScopeForMatches, \
     isAMethod, convertNodeToMatchObject, walkLinesContainingStrings
from bike.parsing.parserutils import generateLogicalLines,\
     generateLogicalLinesAndLineNumbers, \
     splitLogicalLines, makeLineParseable
import compiler
from compiler.ast import Getattr, Name, AssName, AssAttr
from bike.parsing.fastparserast import getRoot, Package, Class, \
                                       Module, Function, Instance
import re
from bike.query.getTypeOf import getTypeOfExpr, UnfoundType, \
     isWordInLine, resolveImportedModuleOrPackage
from bike.parsing import visitor
from bike.parsing.visitor import walkAndGenerate

from bike.parsing.parserutils import makeLineParseable,splitLogicalLines
from bike.parsing.newstuff import getSourceNodesContainingRegex
from bike.parsing.load import getSourceNode
from bike import log


class CantFindDefinitionException:
    pass


def findAllPossibleDefinitionsByCoords(filepath,lineno,col):
    
    #try:
    node = translateSourceCoordsIntoASTNode(filepath,lineno,col)
    #except:
    #    import traceback
    #    traceback.print_exc()

    if node is None:
        raise "selected node type not supported"
    scope = getScopeForLine(getSourceNode(filepath),lineno)
    match = findDefinitionFromASTNode(scope,node)
    if match is not None:
        yield match
    if isinstance(node,Getattr) and (match is None or match.confidence != 100):
        root = getRoot()
        name = node.attrname
        for match in scanPythonPathForMatchingMethodNames(name,filepath):
            yield match
    print >>log.progress,"done"


def findDefinitionFromASTNode(scope,node):
    assert node is not None
    if isinstance(node,Name) or isinstance(node,AssName):
        while 1:
            # try scope children
            childscope = scope.getChild(node.name)
            if childscope is not None:
                return convertNodeToMatchObject(childscope,100)

            if isinstance(scope,Package):
                scope = scope.getChild("__init__")

            # try arguments and assignments
            match = scanScopeAST(scope,node.name,
                                 AssignmentAndFnArgsSearcher(node.name))
            if match is not None:
                return match
            
            # try imports
            match = searchImportedModulesForDefinition(scope,node)
            if match is not None:
                return match


            if not isinstance(scope,Module):
                # try parent scope
                scope = scope.getParent()
            else:
                break
        assert isinstance(scope,Module)

    elif isinstance(node,Getattr) or isinstance(node,AssAttr):
        exprtype = getTypeOfExpr(scope,node.expr)
        if not (exprtype is None or isinstance(exprtype,UnfoundType)):
            if isinstance(exprtype,Instance):
                exprtype = exprtype.getType()
                match = findDefinitionOfAttributeFromASTNode(exprtype,
                                                         node.attrname)
            else:
                match = findDefinitionFromASTNode(exprtype,
                                                  Name(node.attrname))
            if match is not None:
                return match

    elif isinstance(node,compiler.ast.Function) or \
             isinstance(node,compiler.ast.Class):
        if isAMethod(scope,node): 
            match = findDefinitionOfAttributeFromASTNode(scope,
                                                        node.name)
        else:
            match = findDefinitionFromASTNode(scope,Name(node.name))
        if match is not None:
            return match


    type = getTypeOfExpr(scope,node)
    if type is not None and (not isinstance(type,UnfoundType)) and \
                             (not isinstance(type,Instance)):
        return  convertNodeToMatchObject(type,100)
    else:
        return None


def findDefinitionOfAttributeFromASTNode(type,name):
    assert isinstance(type,Class)
    attrfinder = AttrbuteDefnFinder([type],name)

    # first scan the method names:
    for child in type.getChildNodes():
        if child.name == name:
            return convertNodeToMatchObject(child,100)
    # then scan the method source for attribues
    for child in type.getChildNodes():
        if isinstance(child,Function):
            try:
                return scanScopeForMatches(child.module.getSourceNode(),
                                        child, attrfinder,
                                        name).next()
            except StopIteration:
                continue


class AttrbuteDefnFinder(MatchFinder):
    def __init__(self,targetClasses,targetAttribute):
        self.targetClasses = targetClasses
        self.targetAttributeName = targetAttribute

    def visitAssAttr(self, node):
        for c in node.getChildNodes():
            self.visit(c)

        if node.attrname == self.targetAttributeName:
            exprtype = getTypeOfExpr(self.scope,node.expr)
            if isinstance(exprtype,Instance) and \
               exprtype.getType() in self.targetClasses:
                self.appendMatch(self.targetAttributeName)
            #else:
            #    self.appendMatch(self.targetAttributeName,50)
        self.popWordsUpTo(node.attrname)


    

def searchImportedModulesForDefinition(scope,node):
    lines = scope.module.getSourceNode().getLines()
    for lineno in scope.getImportLineNumbers():
        logicalline = getLogicalLine(lines,lineno)
        logicalline = makeLineParseable(logicalline)
        ast = compiler.parse(logicalline)
        class ImportVisitor:
            def __init__(self,node):
                self.target = node
                self.match = None
                assert isinstance(self.target,Name), \
                       "Getattr not supported"
                
            def visitFrom(self, node):
                module = resolveImportedModuleOrPackage(scope,node.modname)
                if module is None: # couldn't find module
                    return 
                
                if node.names[0][0] == '*': # e.g. from foo import *
                    match = findDefinitionFromASTNode(module,self.target)
                    if match is not None:
                        self.match = match
                    return
                    
                for name, alias in node.names:
                    if alias is None and name == self.target.name:
                        match = findDefinitionFromASTNode(module,self.target)
                        if match is not None:
                            self.match = match
                        return


        match = visitor.walk(ast, ImportVisitor(node)).match
        if match:
            return match
    # loop


def getLogicalLine(lines,lineno):
    return generateLogicalLines(lines[lineno-1:]).next()

class AssignmentAndFnArgsSearcher(MatchFinder):
    def __init__(self,name):
        self.targetname = name
        self.match = None

    def visitAssName(self, node):
        if node.name == self.targetname:
            idx = self.getNextIndexOfWord(self.targetname)
            self.match = idx
            return 

    def visitFunction(self, node):
        self.popWordsUpTo(node.name)
        for arg, default in self.zipArgs(node.argnames, node.defaults):
            if arg == self.targetname:
                idx = self.getNextIndexOfWord(self.targetname)
                self.match = idx
                return
            self.popWordsUpTo(arg)
            if default is not None:
                self.visit(default)
        self.visit(node.code)

    def getMatch(self):
        return self.match



# scans for lines containing keyword, and then runs the visitor over
# the parsed AST for that line
def scanScopeAST(scope,keyword,matchfinder):
    lines = scope.generateLinesNotIncludingThoseBelongingToChildScopes()
    match = None
    for line,linenum in generateLogicalLinesAndLineNumbers(lines):
        if isWordInLine(keyword, line):
            doctoredline = makeLineParseable(line)
            ast = compiler.parse(doctoredline)
            matchfinder.reset(line)
            match = visitor.walk(ast,matchfinder).getMatch()
            if match is not None:
                column,yoffset = indexToCoordinates(line,match)
                m = createMatch(scope,linenum + yoffset,column)
                return m
    return None

def createMatch(scope,lineno,x):
    m = Match()
    m.sourcenode = scope.module.getSourceNode()
    m.filename = m.sourcenode.filename
    m.lineno = lineno
    m.colno = x
    m.confidence = 100
    return m

# scan for methods globally (from perspective of 'perspectiveFilename')
def scanPythonPathForMatchingMethodNames(name, contextFilename):
    class MethodFinder:
        def __init__(self,srcnode):
            self.matches = []
            self.srcnode = srcnode
        def visitFunction(self,node):
            node = getScopeForLine(self.srcnode, self.lineno)
            if isinstance(node.getParent(),Class):
                if node.name == name:
                    self.matches.append(convertNodeToMatchObject(node,50))

    for srcnode in getSourceNodesContainingRegex(name,contextFilename):
        m = MethodFinder(srcnode)
        walkLinesContainingStrings(srcnode.fastparseroot,m,[name])
        for match in m.matches:
            yield match


def getIndexOfWord(line,targetword):
    words = re.split("(\w+)", line)
    idx = 0
    for word in words:
        if word == targetword:
            break
        idx += len(word)
    return idx


