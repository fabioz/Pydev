from __future__ import generators 
# Holding module for scaffolding needed to transition parsing package
# into stateless design
import os
import re
from bike.parsing.pathutils import getRootDirectory, getPackageBaseDirectory, \
     filenameToModulePath, getPathOfModuleOrPackage, getFilesForName
from bike.parsing.fastparserast import Module, Package, getRoot, getPackage, getModule 
import sys
from bike.parsing.load import getSourceNode, CantLocateSourceNodeException


def translateFnameToModuleName(filename_path):
    return filenameToModulePath(filename_path)



# scope is the scope to search from
def getModuleOrPackageUsingFQN(fqn, dirpath=None):
    pythonpath = getPythonPath()
    #print "getModuleOrPackageUsingFQN",pythonpath,fqn
    if dirpath is not None:
        assert os.path.isdir(dirpath)
        pythonpath = [dirpath] + pythonpath
    filename = getPathOfModuleOrPackage(fqn,pythonpath)
    #print "getModuleOrPackageUsingFQN - filename",filename
    if filename is not None:
        if os.path.isdir(filename):
            return getPackage(filename)
        else:
            return getModule(filename)
    else:
        return None

def getPythonPath():
    return getRoot().pythonpath


def generateModuleFilenamesInPythonPath(contextFilename):
    files = []
    rootdir = getRootDirectory(contextFilename)
    if rootdir in getPythonPath():
        # just search the pythonpath
        for path in getPythonPath():
            for file in getFilesForName(path):
                if file not in files:   # check for duplicates
                    files.append(file)
                    yield file
    else:
        # search the package hierarchy containing contextFilename
        # in addition to pythonpath
        basedir = getPackageBaseDirectory(contextFilename)
        for path in [basedir] + getPythonPath():
            for file in getFilesForName(path):
                if file not in files:   # check for duplicates
                    files.append(file)
                    yield file

        # and search the files immediately above the package hierarchy
        for file in getFilesForName(os.path.join(rootdir,"*.py")):
            if file not in files:   # check for duplicates
                files.append(file)
                yield file

def generateModuleFilenamesInPackage(filenameInPackage):
    basedir = getPackageBaseDirectory(filenameInPackage)
    for file in getFilesForName(basedir):
        yield file



# search all sourcenodes globally from the perspective of file 'contextFilename'
def getSourceNodesContainingRegex(regexstr,contextFilename):
    regex = re.compile(regexstr)
    for fname in generateModuleFilenamesInPythonPath(contextFilename):
        try:
            f = file(fname)
            src = f.read()
        finally:
            f.close()
        if regex.search(src) is not None:
            yield getSourceNode(fname)




fromRegex = re.compile("^\s*from\s+(\w+)\s+import")
importregex = re.compile("^\s*import\s+(\w+)")

# fileInPackage is the filename of a file in the package hierarchy
# generates file and directory paths
def generatePackageDependencies(fileInPackage):
    rejectPackagePaths = [getPackageBaseDirectory(fileInPackage)]
    for fname in generateModuleFilenamesInPackage(fileInPackage):

        try:
            f = file(fname)
            src = f.read()
        finally:
            f.close()

        packagepath = None

        for line in src.splitlines():
            match = fromRegex.search(line) or importregex.search(line)
            if match is not None:
                modulepath = match.group(1)
                packagename = modulepath.split('.')[0]
                packagepath = getPathOfModuleOrPackage(packagename,
                                                       getPythonPath())
            if packagepath is not None and \
                   packagepath not in rejectPackagePaths:
                rejectPackagePaths.append(packagepath) # avoid duplicates
                yield packagepath
