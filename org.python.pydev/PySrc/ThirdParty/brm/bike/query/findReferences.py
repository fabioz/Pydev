from __future__ import generators
from bike.globals import *
from bike.parsing.fastparserast import Module, Class, Function, getRoot, Instance
from bike.query.common import Match, MatchFinder,\
     getScopeForLine, indexToCoordinates, \
     translateSourceCoordsIntoASTNode, scanScopeForMatches,\
     globalScanForMatches, isAMethod, convertNodeToMatchObject
from compiler.ast import AssName,Name,Getattr,AssAttr
import compiler
from findDefinition import findDefinitionFromASTNode
from bike.query.getTypeOf import getTypeOfExpr, UnfoundType
from bike.query.relationships import getRootClassesOfHierarchy
from bike import log
from bike.parsing.load import getSourceNode


class CouldntFindDefinitionException(Exception):
    pass

def findReferencesIncludingDefn(filename,lineno,col):
    return findReferences(filename,lineno,col,1)


def findReferences(filename,lineno,col,includeDefn=0):
    sourcenode = getSourceNode(filename)
    node = translateSourceCoordsIntoASTNode(filename,lineno,col)
    assert node is not None
    scope,defnmatch = getDefinitionAndScope(sourcenode,lineno,node)

    try:
        for match in findReferencesIncludingDefn_impl(sourcenode,node,
                                                      scope,defnmatch):
            if not includeDefn and match == defnmatch: 
                continue        # don't return definition
            else:
                yield match
    except CouldntFindDefinitionException:
        raise CouldntFindDefinitionException("Could not find definition. Please locate manually (maybe using find definition) and find references from that")

def findReferencesIncludingDefn_impl(sourcenode,node,scope,defnmatch):
    if isinstance(node,Name) or isinstance(node,AssName):
        return generateRefsToName(node.name,scope,sourcenode,defnmatch)
    elif isinstance(node,Getattr) or isinstance(node,AssAttr):
        exprtype = getTypeOfExpr(scope,node.expr)
        if exprtype is None or isinstance(exprtype,UnfoundType):
            raise CouldntFindDefinitionException()

        if isinstance(exprtype,Instance):
            exprtype = exprtype.getType()
            return generateRefsToAttribute(exprtype,node.attrname)
        
        else:
            targetname = node.attrname
            return globalScanForMatches(sourcenode.filename,
                                        NameRefFinder(targetname, defnmatch),
                                        targetname, )
        if match is not None:
            return match
    elif isinstance(node,compiler.ast.Function) or \
                             isinstance(node,compiler.ast.Class):
        return handleClassOrFunctionRefs(scope, node, defnmatch)
    else:
        assert 0,"Seed to references must be Name,Getattr,Function or Class"

def handleClassOrFunctionRefs(scope, node, defnmatch):
    if isAMethod(scope,node):        
        for ref in generateRefsToAttribute(scope,node.name):
            yield ref
    else:
        #yield convertNodeToMatchObject(node,100)
        yield defnmatch
        for ref in generateRefsToName(node.name,scope,
                                    scope.module.getSourceNode(),
                                    defnmatch):
            yield ref

def getDefinitionAndScope(sourcenode,lineno,node):
    scope = getScopeForLine(sourcenode,lineno)
    if scope.getStartLine() == lineno and \
           scope.matchesCompilerNode(node):  # scope is the node
        return scope.getParent(), convertNodeToMatchObject(scope,100)
    defnmatch = findDefinitionFromASTNode(scope,node)
    if defnmatch is None:
        raise CouldntFindDefinitionException()
    scope = getScopeForLine(sourcenode,defnmatch.lineno)
    return scope,defnmatch

def generateRefsToName(name,scope,sourcenode,defnmatch):
    assert scope is not None
    if isinstance(scope,Function):
        # search can be limited to scope
        return scanScopeForMatches(sourcenode,scope,
                                   NameRefFinder(name,defnmatch),
                                   name)
    else:        
        return globalScanForMatches(sourcenode.filename,
                                    NameRefFinder(name,defnmatch),
                                    name)


class NameRefFinder(MatchFinder):
    def __init__(self, targetstr,targetMatch):
        self.targetstr = targetstr
        self.targetMatch = targetMatch
        
    def visitName(self, node):
        if node.name == self.targetstr:
            potentualMatch = findDefinitionFromASTNode(self.scope, node)
            if  potentualMatch is not None and \
                   potentualMatch == self.targetMatch:
                self.appendMatch(node.name)
        self.popWordsUpTo(node.name)

    visitAssName = visitName

    def visitFunction(self, node):
        self.popWordsUpTo(node.name)
        for arg, default in self.zipArgs(node.argnames, node.defaults):
            if arg == self.targetstr:
                self.appendMatch(arg)
            self.popWordsUpTo(arg)
            if default is not None:
                self.visit(default)
        self.visit(node.code)


    def visitFrom(self, node):
        for elem in node.modname.split("."):
            self.popWordsUpTo(elem)
            
        for name, alias in node.names:
            if name == self.targetstr:
                if alias is not None:
                    pretendNode = Name(alias)
                else:
                    pretendNode = Name(name)
                if findDefinitionFromASTNode(self.scope, pretendNode) \
                                                    == self.targetMatch:
                    self.appendMatch(name)
            self.popWordsUpTo(name)
            if alias is not None:
                self.popWordsUpTo(alias)


    def visitGetattr(self, node):        
        for c in node.getChildNodes():
            self.visit(c)
        if node.attrname == self.targetstr:
            defn = findDefinitionFromASTNode(self.scope, node)
            if defn is not None and defn == self.targetMatch:
                self.appendMatch(node.attrname)
        self.popWordsUpTo(node.attrname)


    def visitImport(self, node):
        for name, alias in node.names:
            if name.split(".")[-1] == self.targetstr:
                getattr = self.createGetattr(name)
                if findDefinitionFromASTNode(self.scope, getattr) == self.targetMatch:
                    self.appendMatch(self.targetstr)
            for nameelem in name.split("."):
                self.popWordsUpTo(nameelem)
            if alias is not None:
                self.popWordsUpTo(alias)

    
    def createGetattr(self,fqn):
        node = Name(fqn[0])
        for name in fqn.split(".")[1:]:
            node = Getattr(node,name)
        return node
                           
def generateRefsToAttribute(classobj,attrname):
    rootClasses = getRootClassesOfHierarchy(classobj)
    attrRefFinder = AttrbuteRefFinder(rootClasses,attrname)
    for ref in globalScanForMatches(classobj.filename, attrRefFinder, attrname):
        yield ref
    print >>log.progress,"Done"
    

class AttrbuteRefFinder(MatchFinder):
    def __init__(self,rootClasses,targetAttribute):
        self.rootClasses = rootClasses
        self.targetAttributeName = targetAttribute

    
    def visitGetattr(self, node):
        for c in node.getChildNodes():
            self.visit(c)

        if node.attrname == self.targetAttributeName:
            exprtype = getTypeOfExpr(self.scope,node.expr)            

            if isinstance(exprtype,Instance) and \
                 self._isAClassInTheSameHierarchy(exprtype.getType()):
                self.appendMatch(self.targetAttributeName)
            elif isinstance(exprtype,UnfoundType) or \
                 exprtype is None:   # couldn't find type, so not sure
                self.appendMatch(self.targetAttributeName,50)
            else:
                pass # definately not a match
        self.popWordsUpTo(node.attrname)

    visitAssAttr = visitGetattr

    def visitFunction(self,node):  # visit methods
        if node.name == self.targetAttributeName:
            parentScope = self.scope.getParent()
            #print parentScope
            #print self.targetClasses
            if isinstance(parentScope,Class) and \
                   self._isAClassInTheSameHierarchy(parentScope):
                self.appendMatch(node.name)

        for c in node.getChildNodes():
            self.visit(c)
        
    def _isAClassInTheSameHierarchy(self,classobj):
        #return classobj in self.targetClasses
        targetRootClasses = getRootClassesOfHierarchy(classobj)
        for rootclass in self.rootClasses:
            if rootclass in targetRootClasses:
                return True
        return False
