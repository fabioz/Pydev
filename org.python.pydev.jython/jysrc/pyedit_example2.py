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
    from org.eclipse.jface.dialogs import MessageDialog#@UnresolvedImport
    
    class ExampleCommand2(Action):
        def run(self):
            MessageDialog.openInformation(editor.getSite().getShell(), "Example2", "Activated!!");
            
            
    editor.addOfflineActionListener("ex2", ExampleCommand2()) #the user can activate this action with: Ctrl+2  ex2<ENTER>
            
