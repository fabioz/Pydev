from __future__ import generators
import re

escapedQuotesRE = re.compile(r"(\\\\|\\\"|\\\')")

# changess \" \' and \\ into ** so that text searches
# for " and ' won't hit escaped ones
def maskEscapedQuotes(src):
    return escapedQuotesRE.sub("**", src)

stringsAndCommentsRE =  \
      re.compile("(\"\"\".*?\"\"\"|'''.*?'''|\"[^\"]*\"|\'[^\']*\'|#.*?\n)", re.DOTALL)

import string
#transtable = string.maketrans('classdefifforwhiletry', "*********************")

# performs a transformation on all of the comments and strings so that
# text searches for python keywords won't accidently find a keyword in
# a string or comment
def maskPythonKeywordsInStringsAndComments(src):
    src = escapedQuotesRE.sub("**", src)
    allstrings = stringsAndCommentsRE.split(src)
    # every odd element is a string or comment
    for i in xrange(1, len(allstrings), 2):
        allstrings[i] = allstrings[i].upper()
        #allstrings[i] = allstrings[i].translate(transtable)
    return "".join(allstrings)


allchars = string.maketrans("", "")
allcharsExceptNewline = allchars[: allchars.index('\n')]+allchars[allchars.index('\n')+1:]
allcharsExceptNewlineTranstable = string.maketrans(allcharsExceptNewline, '*'*len(allcharsExceptNewline))


# replaces all chars in a string or a comment with * (except newlines).
# this ensures that text searches don't mistake comments for keywords, and that all
# matches are in the same line/comment as the original
def maskStringsAndComments(src):
    src = escapedQuotesRE.sub("**", src)
    allstrings = stringsAndCommentsRE.split(src)
    # every odd element is a string or comment
    for i in xrange(1, len(allstrings), 2):
        if allstrings[i].startswith("'''")or allstrings[i].startswith('"""'):
            allstrings[i] = allstrings[i][:3]+ \
                           allstrings[i][3:-3].translate(allcharsExceptNewlineTranstable)+ \
                           allstrings[i][-3:]
        else:
            allstrings[i] = allstrings[i][0]+ \
                           allstrings[i][1:-1].translate(allcharsExceptNewlineTranstable)+ \
                           allstrings[i][-1]

    return "".join(allstrings)


# replaces all chars in a string or a comment with * (except newlines).
# this ensures that text searches don't mistake comments for keywords, and that all
# matches are in the same line/comment as the original
def maskStringsAndRemoveComments(src):
    src = escapedQuotesRE.sub("**", src)
    allstrings = stringsAndCommentsRE.split(src)
    # every odd element is a string or comment
    for i in xrange(1, len(allstrings), 2):
        if allstrings[i].startswith("'''")or allstrings[i].startswith('"""'):
            allstrings[i] = allstrings[i][:3]+ \
                           allstrings[i][3:-3].translate(allcharsExceptNewlineTranstable)+ \
                           allstrings[i][-3:]
        elif allstrings[i].startswith("#"):
            allstrings[i] = '\n'
        else:
            allstrings[i] = allstrings[i][0]+ \
                           allstrings[i][1:-1].translate(allcharsExceptNewlineTranstable)+ \
                           allstrings[i][-1]
    return "".join(allstrings)
        

implicitContinuationChars = (('(', ')'), ('[', ']'), ('{', '}'))
emptyHangingBraces = [0,0,0,0,0]
linecontinueRE = re.compile(r"\\\s*(#.*)?$")
multiLineStringsRE =  \
      re.compile("(^.*?\"\"\".*?\"\"\".*?$|^.*?'''.*?'''.*?$)", re.DOTALL)

#def splitLogicalLines(src):
#    src = multiLineStringsRE.split(src)

# splits the string into logical lines.  This requires the comments to
# be removed, and strings masked (see other fns in this module)
def splitLogicalLines(src):
    physicallines = src.splitlines(1)
    return [x for x in generateLogicalLines(physicallines)]


class UnbalancedBracesException: pass

# splits the string into logical lines.  This requires the strings
# masked (see other fns in this module)
# Physical Lines *Must* start on a non-continued non-in-a-comment line
# (although detects unbalanced braces)
def generateLogicalLines(physicallines):
    tmp = []
    hangingBraces = list(emptyHangingBraces)
    hangingComments = 0
    for line in physicallines:
        # update hanging braces
        for i in range(len(implicitContinuationChars)):
            contchar = implicitContinuationChars[i]
            numHanging = hangingBraces[i]
            hangingBraces[i] = numHanging+line.count(contchar[0]) - \
                               line.count(contchar[1])

        hangingComments ^= line.count('"""') % 2
        hangingComments ^= line.count("'''") % 2

        if hangingBraces[0] < 0 or \
           hangingBraces[1] < 0 or \
           hangingBraces[2] < 0:
            raise UnbalancedBracesException()
        
        if linecontinueRE.search(line):
            tmp.append(line)
        elif hangingBraces != emptyHangingBraces:
            tmp.append(line)
        elif hangingComments:
            tmp.append(line)
        else:
            tmp.append(line)
            yield "".join(tmp)
            tmp = []
    

# see above but yields (line,linenum)
#   needs physicallines to have linenum attribute
#   TODO: refactor with previous function
def generateLogicalLinesAndLineNumbers(physicallines):
    tmp = []
    hangingBraces = list(emptyHangingBraces)
    hangingComments = 0
    linenum = None
    for line in physicallines:
        if tmp == []:
            linenum = line.linenum

        # update hanging braces
        for i in range(len(implicitContinuationChars)):
            contchar = implicitContinuationChars[i]
            numHanging = hangingBraces[i]
            hangingBraces[i] = numHanging+line.count(contchar[0]) - \
                               line.count(contchar[1])

        hangingComments ^= line.count('"""') % 2
        hangingComments ^= line.count("'''") % 2
            
        if linecontinueRE.search(line):
            tmp.append(line)
        elif hangingBraces != emptyHangingBraces:
            tmp.append(line)
        elif hangingComments:
            tmp.append(line)
        else:
            tmp.append(line)
            yield "".join(tmp),linenum
            tmp = []
        



# takes a line of code, and decorates it with noops so that it can be
# parsed by the python compiler.
# e.g.  "if foo:"  -> "if foo: pass"
# returns the line, and the adjustment made to the column pos of the first char
# line must have strings and comments masked
#
# N.B. it only inserts keywords whitespace and 0's
notSpaceRE = re.compile("\s*(\S)")
commentRE = re.compile("#.*$")

def makeLineParseable(line):
    return makeLineParseableWhenCommentsRemoved(commentRE.sub("",line))

def makeLineParseableWhenCommentsRemoved(line):
    line = line.strip()
    if ":" in line:
        if line.endswith(":"):
            line += " pass"
        if line.startswith("try"):
            line += "\nexcept: pass"
        elif line.startswith("except") or line.startswith("finally"):
            line = "try: pass\n" + line
            return line
        elif line.startswith("else") or line.startswith("elif"):
            line = "if 0: pass\n" + line
            return line
    elif line.startswith("yield"):
        return ("return"+line[5:])
    return line
