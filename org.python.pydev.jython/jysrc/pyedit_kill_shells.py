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
    from org.python.pydev.plugin import PydevPlugin
    from org.eclipse.core.runtime import NullProgressMonitor
    
    class ListCommand(Action):
        def run(self):
            
            for val in AbstractShell.shells.values():
                for val2 in val.values():
                    val2.endIt()
                    
            try:
                managers = [PydevPlugin.getPythonInterpreterManager(), PydevPlugin.getJythonInterpreterManager()]
                
                for manager in managers:
                    info = manager.getInterpreterInfo(manager.getDefaultInterpreter(), NullProgressMonitor())
                    info.modulesManager.clearCache()
            except:
                import traceback;traceback.print_exc()
                
            MessageDialog.openInformation(editor.getSite().getShell(), "Ok", "Ok, killed all the running shells.\n(They will be recreated on request)");
            
    editor.addOfflineActionListener("kill", ListCommand(), 'Kill all the running shells.', True) 
                    
