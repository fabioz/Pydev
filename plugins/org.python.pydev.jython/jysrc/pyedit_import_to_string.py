#===============================================================================
# Pydev Extensions in Jython code protocol
#===============================================================================
if False:
    from org.python.pydev.editor import PyEdit #@UnresolvedImport
    cmd = 'command string'
    editor = PyEdit
    systemGlobals = {}

#---------------------------- REQUIRED LOCALS-----------------------------------
# interface: String indicating which command will be executed As this script
# will be watching the PyEdit (that is the actual editor in Pydev), and this
# script will be listening to it, this string can indicate any of the methods of
# org.python.pydev.editor.IPyEditListener
assert cmd is not None

# interface: PyEdit object: this is the actual editor that we will act upon
assert editor is not None

if cmd == 'onCreateActions':
    
    ImportToString = systemGlobals.get('ImportToString')
    if ImportToString is None:
        Action = editor.getActionClass() #from org.eclipse.jface.action import Action #@UnresolvedImport
    
        class ImportToString(Action):
            ''' Make a string joining the various parts available in the selection (and removing strings 'from' and 'import')        
            '''
            
            def __init__(self, editor):
                self.editor = editor
            
            def run(self):
                editor = self.editor
                import re
                sel = editor.createPySelection()
                txt = sel.getSelectedText()
    
                splitted = re.split('\\.|\\ ', txt)
                new_text = '.'.join([x for x in splitted if x not in ('from', 'import')])
                new_text = splitted[-1] + ' = ' + '\'' + new_text + '\''
                doc = sel.getDoc()
                sel = sel.getTextSelection()
                doc.replace(sel.getOffset(), sel.getLength(), new_text)
        
        systemGlobals['ImportToString'] = ImportToString


    # Change these constants if the default does not suit your needs
    ACTIVATION_STRING = 'is'
    WAIT_FOR_ENTER = False

    # Register the extension as an ActionListener.
    editor.addOfflineActionListener(ACTIVATION_STRING, ImportToString(editor), \
                                    'Import to string', \
                                    WAIT_FOR_ENTER)
