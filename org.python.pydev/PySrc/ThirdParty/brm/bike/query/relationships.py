# queries to do with module/class/function relationships
from __future__ import generators
from bike.globals import *
from getTypeOf import getTypeOf, getTypeOfExpr
from bike.parsing.newstuff import generateModuleFilenamesInPythonPath, generateModuleFilenamesInPackage, getPythonPath
from bike.parsing.pathutils import getPackageBaseDirectory
from bike.query.common import MatchFinder, walkLinesContainingStrings, getScopeForLine
from bike import log
from bike.parsing.fastparserast import Module
import re

def getRootClassesOfHierarchy(klass):
    if klass is None:  # i.e. dont have base class in our ast
        return None
    if klass.getBaseClassNames() == []:  # i.e. is a root class
        return [klass]
    else:
        rootclasses = []
        for base in klass.getBaseClassNames():
            baseclass = getTypeOf(klass,base)
            rootclass = getRootClassesOfHierarchy(baseclass)
            if rootclass is None:  # base class not in our ast
                rootclass = [klass]
            rootclasses+=rootclass
        return rootclasses

