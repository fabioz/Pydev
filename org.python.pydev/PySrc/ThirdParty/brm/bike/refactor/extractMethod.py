import re
import compiler
from bike.parsing import visitor
from bike.query.common import getScopeForLine
from bike.parsing.parserutils import generateLogicalLines, \
                makeLineParseable, maskStringsAndRemoveComments
from parser import ParserError
from bike.parsing.fastparserast import Class
from bike.transformer.undo import getUndoStack
from bike.refactor.utils import getTabWidthOfLine, getLineSeperator, \
                reverseCoordsIfWrongWayRound
from bike.transformer.save import queueFileToSave
from bike.parsing.load import getSourceNode
TABSIZE = 4

class coords:
    def __init__(self, line, column):
        self.column = column
        self.line = line
    def __str__(self):
        return "("+str(self.column)+","+str(self.line)+")"

commentRE = re.compile(r"#.*?$")

class ParserException(Exception): pass

def extractMethod(filename, startcoords, endcoords, newname):
    ExtractMethod(getSourceNode(filename),
                  startcoords, endcoords, newname).execute()

class ExtractMethod(object):
    def __init__(self,sourcenode, startcoords, endcoords, newname):
        self.sourcenode = sourcenode

        startcoords, endcoords = \
               reverseCoordsIfWrongWayRound(startcoords,endcoords)

        self.startline = startcoords.line
        self.endline = endcoords.line
        self.startcol = startcoords.column
        self.endcol= endcoords.column

        self.newfn = NewFunction(newname)

        self.getLineSeperator()
        self.adjustStartColumnIfLessThanTabwidth()
        self.adjustEndColumnIfStartsANewLine()
        self.fn = self.getFunctionObject()
        self.getRegionToBuffer()
        #print "-"*80
        #print self.extractedLines
        #print "-"*80
        self.deduceIfIsMethodOrFunction()

    def execute(self):
        self.deduceArguments()
        getUndoStack().addSource(self.sourcenode.filename,
                                 self.sourcenode.getSource())
        srclines = self.sourcenode.getLines()
        newFnInsertPosition = self.fn.getEndLine()-1
        self.insertNewFunctionIntoSrcLines(srclines, self.newfn,
                                           newFnInsertPosition)
        self.writeCallToNewFunction(srclines)

        src = "".join(srclines)
        queueFileToSave(self.sourcenode.filename,src)        

    def getLineSeperator(self):
        line = self.sourcenode.getLines()[self.startline-1]
        linesep = getLineSeperator(line)
        self.linesep = linesep        

    def adjustStartColumnIfLessThanTabwidth(self):
        tabwidth = getTabWidthOfLine(self.sourcenode.getLines()[self.startline-1])
        if self.startcol < tabwidth: self.startcol = tabwidth

    def adjustEndColumnIfStartsANewLine(self):
        if self.endcol == 0:
            self.endline -=1
            nlSize = len(self.linesep)
            self.endcol = len(self.sourcenode.getLines()[self.endline-1])-nlSize


    def getFunctionObject(self):
        return getScopeForLine(self.sourcenode,self.startline)


    def getTabwidthOfParentFunction(self):
        line = self.sourcenode.getLines()[self.fn.getStartLine()-1]
        match = re.match("\s+",line)
        if match is None:
            return 0
        else:
            return match.end(0)

    # should be in the transformer module
    def insertNewFunctionIntoSrcLines(self,srclines,newfn,insertpos):
        tabwidth = self.getTabwidthOfParentFunction()

        while re.match("\s*"+self.linesep,srclines[insertpos-1]):
            insertpos -= 1

        srclines.insert(insertpos, self.linesep)
        insertpos +=1

        fndefn = "def "+newfn.name+"("

        if self.isAMethod:
            fndefn += "self"
            if newfn.args != []:
                fndefn += ", "+", ".join(newfn.args)
        else:
            fndefn += ", ".join(newfn.args)

        fndefn += "):"+self.linesep


        srclines.insert(insertpos,tabwidth*" "+fndefn)
        insertpos +=1

        tabwidth += TABSIZE


        if self.extractedCodeIsAnExpression(srclines):
            assert len(self.extractedLines) == 1

            fnbody = [tabwidth*" "+ "return "+self.extractedLines[0]]


        else:
            fnbody = [tabwidth*" "+line for line in self.extractedLines]
            if newfn.retvals != []:
                fnbody.append(tabwidth*" "+"return "+
                             ", ".join(newfn.retvals) + self.linesep)

        for line in fnbody:
            srclines.insert(insertpos,line)
            insertpos +=1


    def writeCallToNewFunction(self, srclines):
        startline = self.startline
        endline = self.endline
        startcol = self.startcol
        endcol= self.endcol

        fncall = self.constructFunctionCallString(self.newfn.name, self.newfn.args,
                                                  self.newfn.retvals)

        self.replaceCodeWithFunctionCall(srclines, fncall,
                                         startline, endline, startcol, endcol)


    def replaceCodeWithFunctionCall(self, srclines, fncall,
                                    startline, endline, startcol, endcol):
        if startline == endline:  # i.e. extracted code part of existing line
            line = srclines[startline-1]
            srclines[startline-1] = self.replaceSectionOfLineWithFunctionCall(line,
                                                         startcol, endcol, fncall)
        else:
            self.replaceLinesWithFunctionCall(srclines, startline, endline, fncall)


    def replaceLinesWithFunctionCall(self, srclines, startline, endline, fncall):
        tabwidth = getTabWidthOfLine(srclines[startline-1])
        line = tabwidth*" " + fncall + self.linesep
        srclines[startline-1:endline] = [line]



    def replaceSectionOfLineWithFunctionCall(self, line, startcol, endcol, fncall):
        line = line[:startcol] + fncall + line[endcol:]
        if not line.endswith(self.linesep):
            line+=self.linesep
        return line



    def constructFunctionCallString(self, fnname, fnargs, retvals):
        fncall = fnname + "("+", ".join(fnargs)+")"
        if self.isAMethod:
            fncall = "self." + fncall

        if retvals != []:
            fncall = ", ".join(retvals) + " = "+fncall
        return fncall


    def deduceArguments(self):
        lines = self.fn.getLinesNotIncludingThoseBelongingToChildScopes()

        # strip off comments
        lines = [commentRE.sub(self.linesep,line) for line in lines]
        extractedLines = maskStringsAndRemoveComments("".join(self.extractedLines)).splitlines(1)

        linesbefore = lines[:(self.startline - self.fn.getStartLine())]
        linesafter = lines[(self.endline - self.fn.getStartLine()) + 1:]

        # split into logical lines
        linesbefore = [line for line in generateLogicalLines(linesbefore)]        
        extractedLines = [line for line in generateLogicalLines(extractedLines)]
        linesafter = [line for line in generateLogicalLines(linesafter)]

        if self.startline == self.endline:
            # need to include the line code is extracted from
            line = generateLogicalLines(lines[self.startline - self.fn.getStartLine():]).next()
            linesbefore.append(line[:self.startcol] + "dummyFn()" + line[self.endcol:])
        assigns = getAssignments(linesbefore)
        fnargs = getFunctionArgs(linesbefore)
        candidateArgs = assigns + fnargs            
        refs = getVariableReferencesInLines(extractedLines)
        self.newfn.args = [ref for ref in refs if ref in candidateArgs]

        assignsInExtractedBlock = getAssignments(extractedLines)
        usesAfterNewFunctionCall = getVariableReferencesInLines(linesafter)
        usesInPreceedingLoop = getVariableReferencesInLines(
            self.getPreceedingLinesInLoop(linesbefore,line))
        self.newfn.retvals = [ref for ref in usesInPreceedingLoop+usesAfterNewFunctionCall
                                   if ref in assignsInExtractedBlock]

    def getPreceedingLinesInLoop(self,linesbefore,firstLineToExtract):
        if linesbefore == []: return []
        tabwidth = getTabWidthOfLine(firstLineToExtract)
        rootTabwidth = getTabWidthOfLine(linesbefore[0])
        llines = [line for line in generateLogicalLines(linesbefore)]
        startpos = len(llines)-1
        loopTabwidth = tabwidth
        for idx in range(startpos,0,-1):
            line = llines[idx]
            if re.match("(\s+)for",line) is not None or \
               re.match("(\s+)while",line) is not None:
                candidateLoopTabwidth = getTabWidthOfLine(line)
                if candidateLoopTabwidth < loopTabwidth:
                    startpos = idx
        return llines[startpos:]

    




    def getRegionToBuffer(self):
        startline = self.startline
        endline = self.endline
        startcol = self.startcol
        endcol= self.endcol


        self.extractedLines = self.sourcenode.getLines()[startline-1:endline]

        match = re.match("\s*",self.extractedLines[0])
        tabwidth = match.end(0)

        self.extractedLines = [line[startcol:] for line in self.extractedLines]

        # above cropping can take a blank line's newline off.
        # this puts it back
        for idx in range(len(self.extractedLines)):
            if self.extractedLines[idx] == '':
                self.extractedLines[idx] = self.linesep

        if startline == endline:
            # need to crop the end
            # (n.b. if region is multiple lines, then whole lines are taken)
            self.extractedLines[-1] = self.extractedLines[-1][:endcol-startcol]

        if self.extractedLines[-1][-1] != '\n':
            self.extractedLines[-1] += self.linesep

    def extractedCodeIsAnExpression(self,lines):
        if len(self.extractedLines) == 1:
            charsBeforeSelection = lines[self.startline-1][:self.startcol]
            if re.match("^\s*$",charsBeforeSelection) is not None:
                return 0
            if re.search(":\s*$",charsBeforeSelection) is not None:
                return 0
            return 1
        return 0

    def deduceIfIsMethodOrFunction(self):
        if isinstance(self.fn.getParent(),Class):
            self.isAMethod = 1
        else:
            self.isAMethod = 0


# holds information about the new function
class NewFunction:
    def __init__(self,name):
        self.name = name


# lines = list of lines.
# Have to have strings masked and comments removed
def getAssignments(lines):
    class AssignVisitor:
        def __init__(self):
            self.assigns = []

        def visitAssTuple(self, node):
            for a in node.nodes:
                if a.name not in self.assigns:
                    self.assigns.append(a.name)

        def visitAssName(self, node):
            if node.name not in self.assigns:
                self.assigns.append(node.name)

        def visitAugAssign(self, node):
            if isinstance(node.node, compiler.ast.Name):
                if node.node.name not in self.assigns:
                    self.assigns.append(node.node.name)

    assignfinder = AssignVisitor()
    for line in lines:
        doctoredline = makeLineParseable(line)
        try:
            ast = compiler.parse(doctoredline)
        except ParserError:
            raise ParserException("couldnt parse:"+doctoredline)
        visitor.walk(ast, assignfinder)
    return assignfinder.assigns


# lines = list of lines.
# Have to have strings masked and comments removed
def getFunctionArgs(lines):
    if lines == []: return []

    class FunctionVisitor:
        def __init__(self):
            self.result = []
        def visitFunction(self, node):
            for n in node.argnames:
                if n != "self":
                    self.result.append(n)
    fndef = generateLogicalLines(lines).next()
    doctoredline = makeLineParseable(fndef)
    try:
        ast = compiler.parse(doctoredline)
    except ParserError:
        raise ParserException("couldnt parse:"+doctoredline)
    return visitor.walk(ast, FunctionVisitor()).result



# lines = list of lines. Have to have strings masked and comments removed
def getVariableReferencesInLines(lines):
    class NameVisitor:
        def __init__(self):
            self.result = []
        def visitName(self, node):
            if node.name not in self.result:
                self.result.append(node.name)
    reffinder = NameVisitor()
    for line in lines:
        doctoredline = makeLineParseable(line)
        try:
            ast = compiler.parse(doctoredline)
        except ParserError:
            raise ParserException("couldnt parse:"+doctoredline)
        visitor.walk(ast, reffinder)
    return reffinder.result
