import jintrospect
from debug import debug
from code import InteractiveInterpreter
import sys,string

class Tipper:
    BANNER = "Tipper"
    def __init__(self):

        # command buffer
        self.buffer = []
        self.locals = {}
        self.interp = Interpreter(self, self.locals)
        # add some handles to our objects
        self.locals['console'] = self

    def evalText(self, line):
        """ Triggered when enter is pressed """
        #~ offsets = self.__lastLine()
        #~ text = self.doc.getText(offsets[0], offsets[1]-offsets[0])
        #~ text = text[:-1] # chomp \n
        #~ self.buffer.append(text)
        self.buffer.append(line)
        source = "\n".join(self.buffer)
        more = self.interp.runsource(source)
        if more:
            debug("runSource output:", more)
        else:
            self.buffer = []

    def showCompletions(self, line):
        # get the display point before writing text
        # otherwise magicCaretPosition is None
        debug('showCompletions:line:', line)
        line = line[:-1] # remove \n
        debug("showCompletions:self.locals:", self.locals)
        try:
            list = jintrospect.getAutoCompleteList(line, self.locals)
            debug("showCompletions:list:",list)
            return list
        except Exception, e:
            # TODO handle this gracefully
            print >> sys.stderr, e
            return
    def showClosure(self, line):
        debug('showClosure:line:', line)
        debug("showClosure:self.locals", self.locals)
        line = line[:-1] # remove \n
        (name, argspec, tip) = jintrospect.getCallTipJava(line, self.locals)
        return (name, argspec, tip)



class Interpreter(InteractiveInterpreter):
    def __init__(self, tipper, locals):
        InteractiveInterpreter.__init__(self, locals)
        self.tipper= tipper


    def write(self, data):
        print >> sys.stdout,  data[:-1]


def GenerateTip (theDoc):
    debug("GenerateTip:theDoc:", theDoc)
    t = Tipper()
    theDoc = theDoc.split("\n")

    while len(theDoc) > 0 and theDoc[len(theDoc)-1] == "":  #chomp empty trailers
        theDoc = theDoc[:-1]
    activationString = theDoc[len(theDoc)-1]
    debug("activationString:", activationString)
    theDoc = theDoc[:-1]
    for line in theDoc:
        debug("next line:", line)
        t.evalText(line)
    if activationString[len(activationString)-1] != '\n': activationString += '\n'
    if activationString[len(activationString)-2] == '.':
        list = t.showCompletions(activationString)
        for l in list: print >> sys.stdout,  "tip:",l
        debug("GenerateTip:done!!")
    if activationString[len(activationString)-2] == '(':
        tuple = t.showClosure("a.doSomething(\n")
        #~ debug("showClosure:name", tuple[0])
        #~ debug("showClosure:argspec", tuple[1])
        #~ debug("showClosure:tip", tuple[2])
        tip = str(tuple[2])
        print >> sys.stdout,  "tip:",tip[string.rindex(tip, '(')+1:]

if __name__ == '__main__':
    import sys,os
    #~ printAttributes(sys.argv[1], sys.argv[2])
    f = open (sys.argv[1])
    theDoc = f.read()
    GenerateTip(theDoc)

