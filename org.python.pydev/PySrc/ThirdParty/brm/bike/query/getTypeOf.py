# getTypeOf(scope,fqn) and getTypeOfExpr(scope,ast)

from bike.parsing.fastparserast import Class, Function, Module, Root, getRoot, Package, Instance, getModule
from bike.parsing.parserutils import generateLogicalLines, makeLineParseable,splitLogicalLines, makeLineParseable
from bike.parsing import visitor
from bike import log
from bike.parsing.newstuff import getModuleOrPackageUsingFQN
from bike.parsing.load import Cache
import os
import re
import compiler

# used if an assignment exists, but cant find type
# e.g. a = SomeFunctionNotLoaded()
# (as opposed to 'None' if cant find an assignment)
class UnfoundType: pass


getTypeOfStack = []

# name is the fqn of the reference, scope is the scope ast object from
# which the question is being asked.
# returns an fastparser-ast object representing the type
# or None if type not found
def getTypeOf(scope, fqn):
    if isinstance(scope, Root):
        assert False, "Can't use getTypeOf to resolve from Root. Use getModuleOrPackageUsingFQN instead"


    #print "getTypeOf:"+fqn+" -- "+str(scope)
    #print 
    #print str(getTypeOfStack)
    #print 
    if (fqn,scope) in getTypeOfStack:   # loop protection
        return None

    # this is crap!
    hashcode = str(scope)+fqn

    try:
        getTypeOfStack.append((fqn,scope))

        try:
            type = Cache.instance.typecache[hashcode]
        except KeyError:
            type = getTypeOf_impl(scope, fqn)
            Cache.instance.typecache[hashcode] = type
        return type
    finally:
        del getTypeOfStack[-1]
        


def getTypeOf_impl(scope, fqn):
    #print "getTypeOf_impl",scope,fqn
    if fqn == "None":
        return None

    if "."in fqn:
        rcdr = ".".join(fqn.split(".")[:-1])
        rcar = fqn.split(".")[-1]
        newscope = getTypeOf(scope,rcdr)
        if newscope is not None:
            return getTypeOf(newscope, rcar)
        else: 
            #print "couldnt find "+rcdr+" in "+str(scope)
            pass

    assert scope is not None
    #assert not ("." in fqn) 
    
    if isinstance(scope,UnfoundType):
        return UnfoundType()
    
    if isinstance(scope, Package):
        #assert 0,scope
        return handlePackageScope(scope, fqn)
    elif isinstance(scope,Instance):
        return handleClassInstanceAttribute(scope, fqn)
    else:
        return handleModuleClassOrFunctionScope(scope,fqn)



def handleModuleClassOrFunctionScope(scope,name):
    if name == "self" and isinstance(scope,Function) and \
           isinstance(scope.getParent(),Class):
        return Instance(scope.getParent())
    
    matches = [c for c in scope.getChildNodes()if c.name == name]
    if matches != []:
        return matches[0]
    
    type = scanScopeSourceForType(scope, name)
    if type != None:
        return type
    
    #print "name = ",name,"scope = ",scope
    type = getImportedType(scope, name)   # try imported types
    #print "type=",type
    if type != None:
        return type
    parentScope = scope.getParent()
    while isinstance(parentScope,Class):
        # don't search class scope, since this is not accessible except
        # through self   (is this true?)
        parentScope = parentScope.getParent()

    if not (isinstance(parentScope,Package) or isinstance(parentScope,Root)):
        return getTypeOf(parentScope, name)


def handleClassInstanceAttribute(instance, attrname):
    theClass = instance.getType()

    # search methods and inner classes
    match = theClass.getChild(attrname)
    if match:
        return match

    #search methods for assignments with self.foo getattrs
    for child in theClass.getChildNodes():
        if not isinstance(child,Function):
            continue
        res = scanScopeAST(child,attrname,
                          SelfAttributeAssignmentVisitor(child,attrname))
        if res is not None:
            return res

def handlePackageScope(package, fqn):
    #print "handlePackageScope",package,fqn
    child = package.getChild(fqn)
    if child:
        return child

    if isinstance(package,Root):
        return getModuleOrPackageUsingFQN(fqn)

    # try searching the fs
    node = getModuleOrPackageUsingFQN(fqn,package.path)
    if node:
        return node
    
    


    # try the package init module
    initmod = package.getChild("__init__")
    if initmod is not None:
        type = getImportedType(initmod, fqn)
        if type:
            return type
    # maybe fqn is absolute
    return getTypeOf(getRoot(), fqn)


wordRE = re.compile("\w+")
def isWordInLine(word, line):
    if line.find(word) != -1:
        words = wordRE.findall(line)
        if word in words:
            return 1
    return 0

def getImportedType(scope, fqn):
    lines = scope.module.getSourceNode().getLines()
    for lineno in scope.getImportLineNumbers():
        logicalline = generateLogicalLines(lines[lineno-1:]).next()
        logicalline = makeLineParseable(logicalline)
        ast = compiler.parse(logicalline)
        match = visitor.walk(ast, ImportVisitor(scope,fqn)).match
        if match:
            return match
        #else loop

class ImportVisitor:
    def __init__(self,scope,fqn):
        self.match = None
        self.targetfqn = fqn
        self.scope = scope

    def visitImport(self, node):
        # if target fqn is an import, then it must be a module or package
        for name, alias in node.names:
            if name == self.targetfqn:
                self.match = resolveImportedModuleOrPackage(self.scope,name)
            elif alias is not None and alias == self.targetfqn:
                self.match = resolveImportedModuleOrPackage(self.scope,name)

    def visitFrom(self, node):
        if node.names[0][0] == '*': # e.g. from foo import *
            if not "."in self.targetfqn:
                module = resolveImportedModuleOrPackage(self.scope,
                                                        node.modname)
                if module:
                    self.match = getTypeOf(module, self.targetfqn)
        else:
            for name, alias in node.names:
                if alias == self.targetfqn or \
                   (alias is None and name == self.targetfqn):
                    scope = resolveImportedModuleOrPackage(self.scope,
                                                            node.modname)
                    if scope is not None:
                        if isinstance(scope,Package):
                            self.match = getModuleOrPackageUsingFQN(name,scope.path)
                        else:  
                            assert isinstance(scope,Module)
                            self.match = getTypeOf(scope, name)




class TypeNotSupportedException:
    def __init__(self,msg):
        self.msg = msg

    def __str__(self):
        return self.msg

# attempts to evaluate the type of the expression
def getTypeOfExpr(scope, ast):
    if isinstance(ast, compiler.ast.Name):
        return getTypeOf(scope, ast.name)

    elif isinstance(ast, compiler.ast.Getattr) or \
             isinstance(ast, compiler.ast.AssAttr):

        # need to do this in order to match foo.bah.baz as
        # a string in import statements
        fqn = attemptToConvertGetattrToFqn(ast)
        if fqn is not None:
            return getTypeOf(scope,fqn)

        expr = getTypeOfExpr(scope, ast.expr)
        if expr is not None:
            attrnametype = getTypeOf(expr, ast.attrname)
            return attrnametype
        return None

    elif isinstance(ast, compiler.ast.CallFunc):
        node = getTypeOfExpr(scope,ast.node)
        if isinstance(node,Class):
            return Instance(node)
        elif isinstance(node,Function):
            return getReturnTypeOfFunction(node)
    else:
        #raise TypeNotSupportedException, \
        #      "Evaluation of "+str(ast)+" not supported. scope="+str(scope)
        print >> log.warning, "Evaluation of "+str(ast)+" not supported. scope="+str(scope)
        return None


def attemptToConvertGetattrToFqn(ast):
    fqn = ast.attrname
    ast = ast.expr
    while isinstance(ast,compiler.ast.Getattr):
        fqn = ast.attrname + "." + fqn
        ast = ast.expr
    if isinstance(ast,compiler.ast.Name):
        return ast.name + "." + fqn
    else:
        return None


getReturnTypeOfFunction_stack = []
def getReturnTypeOfFunction(function):
    if function in getReturnTypeOfFunction_stack:   # loop protection
        return None
    try:
        getReturnTypeOfFunction_stack.append(function)
        return getReturnTypeOfFunction_impl(function)
    finally:
        del getReturnTypeOfFunction_stack[-1]

def getReturnTypeOfFunction_impl(function):
    return scanScopeAST(function,"return",ReturnTypeVisitor(function))
    

# does parse of scope sourcecode to deduce type
def scanScopeSourceForType(scope, name):
    return scanScopeAST(scope,name,AssignmentVisitor(scope,name))


# scans for lines containing keyword, and then runs the visitor over
# the parsed AST for that line
def scanScopeAST(scope,keyword,astvisitor):
    lines = scope.getLinesNotIncludingThoseBelongingToChildScopes()
    src = ''.join(lines)
    match = None
    #print "scanScopeAST:"+str(scope)
    for line in splitLogicalLines(src):
        if isWordInLine(keyword, line):
            #print "scanning for "+keyword+" in line:"+line[:-1]
            doctoredline = makeLineParseable(line)
            ast = compiler.parse(doctoredline)
            match = visitor.walk(ast,astvisitor).getMatch()
            if match:
                return match
    return match
    

class AssignmentVisitor:
    def __init__(self,scope,targetName):
        self.match=None
        self.scope = scope
        self.targetName = targetName

    def getMatch(self):
        return self.match

    def visitAssign(self,node):
        if isinstance(node.expr,compiler.ast.CallFunc):
            for assnode in node.nodes:
                if isinstance(assnode,compiler.ast.AssName) and \
                   assnode.name == self.targetName:
                    self.match = getTypeOfExpr(self.scope,node.expr)
                    if self.match is None:
                        self.match = UnfoundType()
                    
                    

class SelfAttributeAssignmentVisitor:
    def __init__(self,scope,targetName):
        self.match=None
        self.scope = scope
        self.targetName = targetName

    def getMatch(self):
        return self.match

    def visitAssign(self,node):
        if isinstance(node.expr,compiler.ast.CallFunc):
            for assnode in node.nodes:
                if isinstance(assnode,compiler.ast.AssAttr) and \
                   isinstance(assnode.expr,compiler.ast.Name) and \
                   assnode.expr.name == "self" and \
                   assnode.attrname == self.targetName:
                    self.match = getTypeOfExpr(self.scope,node.expr)
                    #print "here!",self.match.getType().fqn


class ReturnTypeVisitor:
    def __init__(self,fn):
        self.match=None
        self.fn = fn

    def getMatch(self):
        return self.match
    
    def visitReturn(self,node):
        try:
            self.match = getTypeOfExpr(self.fn,node.value)
        except TypeNotSupportedException, ex:
            pass


def resolveImportedModuleOrPackage(scope,fqn):
    # try searching from directory containing scope module
    path = os.path.dirname(scope.module.filename)
    node = getModuleOrPackageUsingFQN(fqn,path)
    if node is not None:
        return node
    # try searching from the root
    node = getModuleOrPackageUsingFQN(fqn)
    if node is not None:
        return node
    

