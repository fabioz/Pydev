from __future__ import generators
from bike.query.common import Match, globalScanForMatches, getScopeForLine, MatchFinder
from getTypeOf import getTypeOf, getTypeOfExpr
import compiler
import re

def getReferencesToModule(root, fqn):
    
    modulename = fqn.split(".")[-1]
    moduleobj = getTypeOf(root, fqn)
    moduleRefFinder = ModuleRefFinder(moduleobj)
    
    for ref in globalScanForMatches(moduleRefFinder, modulename):
        yield ref

        
class ModuleRefFinder(MatchFinder):
    def __init__(self, targetmodule):
        self.targetmodule = targetmodule

    def visitName(self, node):
        if node.name == self.targetmodule.name:
            if getTypeOfExpr(self.scope, node) == self.targetmodule:
                self.appendMatch(node.name)
        self.popWordsUpTo(node.name)

    def visitImport(self, node):
        for name, alias in node.names:
            if name.split(".")[-1] == self.targetmodule.name:
                if getTypeOf(self.scope, name) == self.targetmodule:
                    self.appendMatch(self.targetmodule.name)
            for nameelem in name.split("."):
                self.popWordsUpTo(nameelem)
            if alias is not None:
                self.popWordsUpTo(alias)

    def visitGetattr(self, node):
        for c in node.getChildNodes():
            self.visit(c)
        if node.attrname == self.targetmodule.name:
            if getTypeOfExpr(self.scope, node) == self.targetmodule:
                self.appendMatch(self.targetmodule.name)
        self.popWordsUpTo(node.attrname)

    def visitFrom(self, node):
        for elem in node.modname.split("."):
            if elem == self.targetmodule.name:
                getTypeOf(self.scope, elem) == self.targetmodule
                self.appendMatch(self.targetmodule.name)
            self.popWordsUpTo(elem)
            
        for name, alias in node.names:
            if name == self.targetmodule.name:
                if alias and \
                   getTypeOf(self.scope, alias) == self.targetmodule:
                    self.appendMatch(self.targetmodule.name)
                elif getTypeOf(self.scope, name) == self.targetmodule:
                    self.appendMatch(self.targetmodule.name)
            if name != "*":
                self.popWordsUpTo(name)
            if alias is not None:
                self.popWordsUpTo(alias)
