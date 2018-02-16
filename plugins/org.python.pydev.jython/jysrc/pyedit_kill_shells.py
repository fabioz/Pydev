if False:
    from org.python.pydev.editor import PyEdit #@UnresolvedImport
    cmd = 'command string'
    editor = PyEdit
    systemGlobals = {}

#--------------------------------------------------------------- REQUIRED LOCALS
#interface: String indicating which command will be executed
#As this script will be watching the PyEdit (that is the actual editor in Pydev), and this script
#will be listening to it, this string can indicate any of the methods of org.python.pydev.editor.IPyEditListener
assert cmd is not None

#interface: PyEdit object: this is the actual editor that we will act upon
assert editor is not None

if cmd == 'onCreateActions':
    ListCommand = systemGlobals.get('ListCommand')
    if ListCommand is None:
        Action = editor.getActionClass() #from org.eclipse.jface.action import Action #@UnresolvedImport
    
        class ListCommand(Action):
            
            def __init__(self, editor):
                self.editor = editor
                
            def run(self):
                editor = self.editor
                from org.python.pydev.ast.codecompletion.shell import AbstractShell  # @UnresolvedImport
                error_msg = AbstractShell.restartAllShells()
                if error_msg:
                    editor.showInformationDialog("Error killing the shells", error_msg);
                else:
                    editor.showInformationDialog("Ok", "Ok, killed all the running shells.\n(They will be recreated on request)");
                    
        systemGlobals['ListCommand'] = ListCommand

    editor.addOfflineActionListener("kill", ListCommand(editor), 'Kill all the running shells.', True)
