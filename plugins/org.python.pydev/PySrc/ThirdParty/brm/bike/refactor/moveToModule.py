#@PydevCodeAnalysisIgnore
import bike.globals
from bike.parsing.load import getSourceNode
from bike.parsing.fastparserast import Module
from bike.query.common import getScopeForLine, convertNodeToMatchObject
from bike.transformer.save import queueFileToSave, save
from bike.transformer.undo import getUndoStack
from bike.refactor.extractMethod import getVariableReferencesInLines
from bike.refactor.utils import getLineSeperator
from bike.query.findDefinition import findDefinitionFromASTNode
from bike.query.findReferences import findReferences
from bike.parsing.pathutils import filenameToModulePath
from compiler.ast import Name
import re

def moveClassToNewModule(origfile,line,newfile):
    srcnode = getSourceNode(origfile)
    targetsrcnode = getSourceNode(newfile)
    classnode = getScopeForLine(srcnode,line)
    classlines = srcnode.getLines()[classnode.getStartLine()-1:
                                    classnode.getEndLine()-1]
    getUndoStack().addSource(srcnode.filename,
                             srcnode.getSource())
    getUndoStack().addSource(targetsrcnode.filename,
                             targetsrcnode.getSource())

    srcnode.getLines()[classnode.getStartLine()-1:
                       classnode.getEndLine()-1] = []
    
    targetsrcnode.getLines().extend(classlines)

    queueFileToSave(srcnode.filename,srcnode.getSource())
    queueFileToSave(targetsrcnode.filename,targetsrcnode.getSource())


exactFromRE = "(from\s+\S+\s+import\s+%s)(.*)"    
fromRE = "from\s+\S+\s+import\s+(.*)"

def moveFunctionToNewModule(origfile,line,newfile):
    
    srcnode = getSourceNode(origfile)
    targetsrcnode = getSourceNode(newfile)
    scope = getScopeForLine(srcnode,line)

    linesep = getLineSeperator(srcnode.getLines()[0])

    matches = findReferences(origfile, line, scope.getColumnOfName())

    origFileImport = []
    fromline = 'from %s import %s'%(filenameToModulePath(newfile),scope.name)
    
    for match in matches:
        if match.filename == origfile:
            origFileImport = fromline + linesep
        else:
            s = getSourceNode(match.filename)
            m = s.fastparseroot
            if match.lineno in m.getImportLineNumbers():                
                getUndoStack().addSource(s.filename,
                                         s.getSource())

                maskedline = m.getLogicalLine(match.lineno)
                origline = s.getLines()[match.lineno-1]
                reMatch =  re.match(exactFromRE%(scope.name),maskedline)
                if reMatch and not (',' in reMatch.group(2) or \
                                    '\\' in reMatch.group(2)):
                    restOfOrigLine = origline[len(reMatch.group(1)):]
                    s.getLines()[match.lineno-1] = fromline + restOfOrigLine

                elif re.match(fromRE,maskedline):
                    #remove the element from the import stmt
                    line = re.sub('%s\s*?,'%(scope.name),'',origline)
                    s.getLines()[match.lineno-1] = line
                    #and add a new line
                    nextline = match.lineno + maskedline.count('\\') + 1
                    s.getLines()[nextline-1:nextline-1] = [fromline+linesep]
                    
                queueFileToSave(s.filename,s.getSource())
                    
    
    refs = getVariableReferencesInLines(scope.getMaskedLines())

    scopeLines = srcnode.getLines()[scope.getStartLine()-1:
                                    scope.getEndLine()-1]
    importModules = deduceImportsForNewFile(refs, scope)
    importlines = composeNewFileImportLines(importModules, linesep)



    getUndoStack().addSource(srcnode.filename,
                             srcnode.getSource())
    getUndoStack().addSource(targetsrcnode.filename,
                             targetsrcnode.getSource())

    srcnode.getLines()[scope.getStartLine()-1:
                       scope.getEndLine()-1] = origFileImport

    targetsrcnode.getLines().extend(importlines+scopeLines)

    queueFileToSave(srcnode.filename,srcnode.getSource())
    queueFileToSave(targetsrcnode.filename,targetsrcnode.getSource())


def composeNewFileImportLines(importModules, linesep):
    importlines = []
    for mpath in importModules:
        importlines += "from %s import %s"%(mpath,
                                  ', '.join(importModules[mpath]))
        importlines += linesep
    return importlines

def deduceImportsForNewFile(refs, scope):
    importModules = {}
    for ref in refs:
        match = findDefinitionFromASTNode(scope,Name(ref))

        if match.filename == scope.module.filename:
            tgtscope = getScopeForLine(getSourceNode(match.filename),
                                       match.lineno)
            while tgtscope != scope and not isinstance(tgtscope,Module):
                tgtscope = tgtscope.getParent()
            
            if not isinstance(tgtscope,Module):
                continue   # was defined in this function
        
        mpath = filenameToModulePath(match.filename)            
        if mpath in importModules:            
            importModules[mpath].append(ref)
        else:
            importModules[mpath] = [ref]
    return importModules
    
    
