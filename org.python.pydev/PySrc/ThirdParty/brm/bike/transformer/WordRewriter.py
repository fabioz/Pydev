from bike.parsing.load import getSourceNode
from bike.transformer.undo import getUndoStack
from bike.transformer.save import queueFileToSave
import re

# This class maintains a set of changed lines to the original source
# nodes. This is important because the act of changing a line messes
# up the coordinates on which renames are done.
# Commit writes the changes back to the source nodes
class WordRewriter:
    def __init__(self):
        self.modifiedsrc = {}

    def rewriteString(self, srcnode, lineno, colno, newname):
        filename = srcnode.filename
        if not self.modifiedsrc.has_key(filename):
            getUndoStack().addSource(filename,srcnode.getSource())
            self.modifiedsrc[filename] = {}
        if not self.modifiedsrc[filename].has_key(lineno):
            line = srcnode.getLines()[lineno-1]
            self.modifiedsrc[filename][lineno] = self._lineToDict(line)
        self.modifiedsrc[filename][lineno][colno] = newname


    # writes all the changes back to the src nodes
    def commit(self):
        for filename in self.modifiedsrc.keys():
            srcnode = getSourceNode(filename)
            for lineno in self.modifiedsrc[filename]:
                lines = srcnode.getLines()
                lines[lineno-1] = self._dictToLine(self.modifiedsrc[filename][lineno])
            queueFileToSave(filename,"".join(srcnode.getLines()))


    # this function creates a dictionary with each word referenced by
    # its column position in the original line
    def _lineToDict(self, line):
        words = re.split("(\w+)", line)
        h = {};i = 0
        for word in words:
            h[i] = word
            i+=len(word)
        return h

    def _dictToLine(self, d):
        cols = d.keys()
        cols.sort()
        return "".join([d[colno]for colno in cols])
