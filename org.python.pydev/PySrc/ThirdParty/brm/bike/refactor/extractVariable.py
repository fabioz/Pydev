from bike.parsing.parserutils import maskStringsAndRemoveComments
from bike.transformer.undo import getUndoStack
from parser import ParserError
import compiler
from bike.refactor.extractMethod import coords
from bike.refactor.utils import getTabWidthOfLine, getLineSeperator,\
     reverseCoordsIfWrongWayRound
from bike.transformer.save import queueFileToSave
from bike.parsing.load import getSourceNode


def extractLocalVariable(filename, startcoords, endcoords, varname):
    sourceobj = getSourceNode(filename)
    if startcoords.line != endcoords.line:
        raise "Can't do multi-line extracts yet"
    startcoords, endcoords = \
                 reverseCoordsIfWrongWayRound(startcoords,endcoords)
    line = sourceobj.getLine(startcoords.line)
    tabwidth = getTabWidthOfLine(line)
    linesep = getLineSeperator(line)
    region = line[startcoords.column:endcoords.column]
    
    getUndoStack().addSource(sourceobj.filename,sourceobj.getSource())
    sourceobj.getLines()[startcoords.line-1] = \
          line[:startcoords.column] + varname + line[endcoords.column:]

    defnline = tabwidth*" " + varname + " = " + region + linesep
    
    sourceobj.getLines().insert(startcoords.line-1,defnline)

    queueFileToSave(sourceobj.filename,"".join(sourceobj.getLines()))

