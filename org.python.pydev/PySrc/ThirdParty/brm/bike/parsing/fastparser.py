#!/usr/bin/env python
from bike.parsing.fastparserast import *
from bike.parsing.parserutils import *
from parser import ParserError
#import exceptions

indentRE = re.compile("^\s*(\w+)")

# returns a tree of objects representing nested classes and functions
# in the source
def fastparser(src,modulename="",filename=""):
    try:
        return fastparser_impl(src,modulename,filename)
    except RuntimeError, ex:   # if recursive call exceeds maximum depth
        if str(ex) == "maximum recursion limit exceeded":
            raise ParserError,"maximum recursion depth exceeded when fast-parsing src "+filename
        else:
            raise

def fastparser_impl(src,modulename,filename):
    lines = src.splitlines(1)
    maskedSrc = maskPythonKeywordsInStringsAndComments(src)
    maskedLines = maskedSrc.splitlines(1)
    root = Module(filename,modulename,lines,maskedSrc)
    parentnode = root
    lineno = 0
    for line in maskedLines:
        lineno+=1
        #print "line",lineno,":",line
        m = indentRE.match(line)
        if m:
            indent = m.start(1)
            tokenstr = m.group(1)
            if tokenstr == "import" or tokenstr == "from":
                while indent <= parentnode.indent:   # root indent is -TABWIDTH
                    parentnode = parentnode.getParent()
                try:
                    parentnode.importlines.append(lineno)
                except AttributeError:
                    parentnode.importlines = [lineno]
            elif tokenstr == "class":
                m2 = classNameRE.match(line)
                if m2:
                    n = Class(m2.group(1), filename, root, lineno, indent, lines, maskedSrc)
                    root.flattenedNodes.append(n)

                    while indent <= parentnode.indent:
                        parentnode = parentnode.getParent()
                    parentnode.addChild(n)
                    parentnode = n

            elif tokenstr == "def":
                m2 = fnNameRE.match(line)
                if m2:
                    n = Function(m2.group(1), filename, root, lineno, indent, lines, maskedSrc)
                    root.flattenedNodes.append(n)

                    while indent <= parentnode.indent:
                        parentnode = parentnode.getParent()
                    parentnode.addChild(n)
                    parentnode = n

            elif indent <= parentnode.indent and \
                     tokenstr in ['if','for','while','try']:
                parentnode = parentnode.getParent()
                while indent <= parentnode.indent:
                    parentnode = parentnode.getParent()

    return root

