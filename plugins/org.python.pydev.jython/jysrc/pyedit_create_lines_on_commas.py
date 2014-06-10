#===============================================================================
# Pydev Extensions in Jython code protocol
#===============================================================================
if False:
    from org.python.pydev.editor import PyEdit #@UnresolvedImport
    cmd = 'command string'
    editor = PyEdit
    systemGlobals = {}



if cmd == 'onCreateActions':
    # interface: PyEdit object: this is the actual editor that we will act upon
    assert editor is not None

    CreateLinesOnCommas = systemGlobals.get('CreateLinesOnCommas')
    if CreateLinesOnCommas is None:
        Action = editor.getActionClass() #from org.eclipse.jface.action import Action #@UnresolvedImport
    
        class CreateLinesOnCommas(Action):
            ''' Split a line in its commas.
            '''
            
            def __init__(self, editor):
                self.editor = editor
            
            def run(self):
                editor = self.editor
                from split_text_in_commas import SplitTextInCommas
                sel = editor.createPySelection()
                txt = sel.getSelectedText()
                delimiter = sel.getEndLineDelim()
                indent = sel.getIndentationFromLine()
    
                splitted = SplitTextInCommas(txt)
    
                doc = sel.getDoc()
                sel = sel.getTextSelection()
                doc.replace(sel.getOffset(), sel.getLength(), (delimiter + indent).join([x.strip() + ', ' for x in splitted]))

        systemGlobals['CreateLinesOnCommas'] = CreateLinesOnCommas

    # Change these constants if the default does not suit your needs
    ACTIVATION_STRING = 'sl'
    WAIT_FOR_ENTER = False

    # Register the extension as an ActionListener.
    editor.addOfflineActionListener(ACTIVATION_STRING, CreateLinesOnCommas(editor), \
                                    'Create new lines in commas', \
                                    WAIT_FOR_ENTER)

