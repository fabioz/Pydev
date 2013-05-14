from __future__ import nested_scopes # for Jython 2.1 compatibility

# Do the right thing with boolean values for all known Python versions (so this
# module can be copied to projects that don't depend on Python 2.3, e.g. Optik
# and Docutils).
try:
    True, False #@UndefinedVariable
except NameError:
    (True, False) = (1, 0)

#===============================================================================
# Pydev Extensions in Jython code protocol
#===============================================================================
True, False = 1, 0
if False:
    from org.python.pydev.editor import PyEdit #@UnresolvedImport
    cmd = 'command string'
    editor = PyEdit

#===================================================================================================
# SplitTextInCommas
#===================================================================================================
def SplitTextInCommas(txt):
    '''
    Splits the text in the commas, considering it's python code (right now, only takes into account
    tuples, but it should be extended for lists, dicts, strings, etc)
    '''
    splitted = []

    parens_level = 0

    buf = ''
    for c in txt:
        if c == '(':
            parens_level += 1

        if c == ')':
            parens_level -= 1


        if parens_level == 0:
            if c == ',':
                splitted.append(buf)
                buf = ''
            else:
                buf += c
        else:
            buf += c


    if buf:
        splitted.append(buf)

    return splitted

#===================================================================================================
# main
#===================================================================================================
if __name__ == '__main__':
    #Not run when it comes from the editor
    import unittest

    class Test(unittest.TestCase):

        def testIt(self):
            self.assertEqual(SplitTextInCommas('a,b,c'), ['a', 'b', 'c'])
            self.assertEqual(SplitTextInCommas('(a,b),c'), ['(a,b)', 'c'])

    unittest.main()


elif cmd == 'onCreateActions':
    # interface: PyEdit object: this is the actual editor that we will act upon
    assert editor is not None

    Action = editor.getActionClass() #from org.eclipse.jface.action import Action #@UnresolvedImport

    class ImportToString(Action):
        ''' Make a string joining the various parts available in the selection (and removing strings 'from' and 'import')        
        '''
        def run(self):
            sel = editor.createPySelection()
            txt = sel.getSelectedText()
            delimiter = sel.getEndLineDelim()
            indent = sel.getIndentationFromLine()

            splitted = SplitTextInCommas(txt)


            doc = sel.getDoc()
            sel = sel.getTextSelection()
            doc.replace(sel.getOffset(), sel.getLength(), (delimiter + indent).join([x.strip() + ', ' for x in splitted]))



    # Change these constants if the default does not suit your needs
    ACTIVATION_STRING = 'sl'
    WAIT_FOR_ENTER = False

    # Register the extension as an ActionListener.
    editor.addOfflineActionListener(ACTIVATION_STRING, ImportToString(), \
                                    'Create new lines in commas', \
                                    WAIT_FOR_ENTER)

