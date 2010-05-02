if False: 
    from org.python.pydev.editor import PyEdit #@UnresolvedImport
    cmd = 'command string'
    editor = PyEdit

#--------------------------------------------------------------- REQUIRED LOCALS
#interface: String indicating which command will be executed
#As this script will be watching the PyEdit (that is the actual editor in Pydev), and this script
#will be listening to it, this string can indicate any of the methods of org.python.pydev.editor.IPyEditListener
assert cmd is not None 

#interface: PyEdit object: this is the actual editor that we will act upon
assert editor is not None

if cmd == 'onCreateActions':
    from org.eclipse.jface.action import Action #@UnresolvedImport
    from org.python.pydev.editor.codecompletion.shell import AbstractShell #@UnresolvedImport
    from org.eclipse.jface.dialogs import MessageDialog #@UnresolvedImport
    
    class ListCommand(Action):
        def run(self):
            error_msg = AbstractShell.restartAllShells()
            if error_msg:
                MessageDialog.openInformation(editor.getSite().getShell(), "Error killing the shells", error_msg);
            else:    
                MessageDialog.openInformation(editor.getSite().getShell(), "Ok", "Ok, killed all the running shells.\n(They will be recreated on request)");
            
    editor.addOfflineActionListener("kill", ListCommand(), 'Kill all the running shells.', True) 
                    
