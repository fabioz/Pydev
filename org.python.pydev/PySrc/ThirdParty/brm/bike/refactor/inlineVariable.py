from bike.query.findDefinition import findAllPossibleDefinitionsByCoords
from bike.query.findReferences import findReferences
from bike.parsing.parserutils import maskStringsAndRemoveComments, linecontinueRE
from bike.transformer.undo import getUndoStack
from bike.transformer.save import queueFileToSave
from parser import ParserError
from bike.parsing.load import getSourceNode
import compiler
import re


def inlineLocalVariable(filename, lineno,col):
    sourceobj = getSourceNode(filename)
    return inlineLocalVariable_old(sourceobj, lineno,col)

def inlineLocalVariable_old(sourcenode,lineno,col):
    definition, region, regionlinecount = getLocalVariableInfo(sourcenode, lineno, col)
    addUndo(sourcenode)
    replaceReferences(sourcenode, findReferences(sourcenode.filename, definition.lineno, definition.colno), region)
    delLines(sourcenode, definition.lineno-1, regionlinecount)
    updateSource(sourcenode)

def getLocalVariableInfo(sourcenode, lineno, col):
    definition = findDefinition(sourcenode, lineno, col)
    region, linecount = getRegionToInline(sourcenode, definition)
    return definition, region, linecount

def findDefinition(sourcenode, lineno, col):
    definition = findAllPossibleDefinitionsByCoords(sourcenode.filename,
                                                    lineno,col).next()
    assert definition.confidence == 100    
    return definition

def getRegionToInline(sourcenode, defn):
    line, linecount = getLineAndContinues(sourcenode, defn.lineno)
    start, end = findRegionToInline(maskStringsAndRemoveComments(line))
    return line[start:end], linecount

def findRegionToInline(maskedline):
    match = re.compile("[^=]+=\s*(.+)$\n", re.DOTALL).match(maskedline)
    assert match
    return match.start(1), match.end(1)

# Possible refactoring: move to class of sourcenode
def getLineAndContinues(sourcenode, lineno):
    line = sourcenode.getLine(lineno)
    
    linecount = 1
    while linecontinueRE.search(line):
        line += sourcenode.getLine(lineno + linecount)
        linecount += 1

    return line, linecount

def addUndo(sourcenode):
    getUndoStack().addSource(sourcenode.filename,sourcenode.getSource())

def replaceReferences(sourcenode, references, replacement):
    for reference in safeReplaceOrder( references ):
        replaceReference(sourcenode, reference, replacement)

def safeReplaceOrder( references ):
    """ 
    When inlining a variable, if multiple instances occur on the line, then the
    last reference must be replaced first. Otherwise the remaining intra-line
    references will be incorrect.
    """
    def safeReplaceOrderCmp(self, other):
        return -cmp(self.colno, other.colno)

    result = list(references)
    result.sort(safeReplaceOrderCmp)
    return result


def replaceReference(sourcenode, ref, replacement):
    """ sourcenode.getLines()[ref.lineno-1][ref.colno:ref.colend] = replacement
    But strings don't support slice assignment as they are immutable. :(
    """
    sourcenode.getLines()[ref.lineno-1] = \
        replaceSubStr(sourcenode.getLines()[ref.lineno-1],
            ref.colno, ref.colend, replacement)

def replaceSubStr(str, start, end, replacement):
    return str[:start] + replacement + str[end:]

# Possible refactoring: move to class of sourcenode
def delLines(sourcenode, lineno, linecount=1):
    del sourcenode.getLines()[lineno:lineno+linecount]
    
def updateSource(sourcenode):
    queueFileToSave(sourcenode.filename,"".join(sourcenode.getLines()))
    
                    
