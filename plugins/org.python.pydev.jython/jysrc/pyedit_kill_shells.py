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
import traceback
import StringIO

if cmd == 'onCreateActions':
    from org.eclipse.jface.action import Action #@UnresolvedImport
    from org.python.pydev.editor.codecompletion.shell import AbstractShell #@UnresolvedImport
    from org.eclipse.jface.dialogs import MessageDialog #@UnresolvedImport
    from org.python.pydev.plugin import PydevPlugin #@UnresolvedImport
    from org.eclipse.core.runtime import NullProgressMonitor #@UnresolvedImport
    from org.python.pydev.core import MisconfigurationException #@UnresolvedImport
    
    class ListCommand(Action):
        def run(self):
            
            for val in AbstractShell.shells.values():
                for val2 in val.values():
                    val2.endIt()
                    
            try:
                managers = [
                    PydevPlugin.getPythonInterpreterManager(), 
                    PydevPlugin.getJythonInterpreterManager(),
                    PydevPlugin.getIronpythonInterpreterManager(),
                ]
                
                for manager in managers:
                    try:
                        for info in manager.getInterpreterInfos():
                            info.modulesManager.clearCache()
                            manager.clearCaches()
                    except MisconfigurationException:
                        pass #that's ok -- it's not configured
            except:
                s = StringIO.StringIO()
                traceback.print_exc(file=s)
                MessageDialog.openInformation(editor.getSite().getShell(), "Error killing the shells", s.getvalue());
            else:    
                MessageDialog.openInformation(editor.getSite().getShell(), "Ok", "Ok, killed all the running shells.\n(They will be recreated on request)");
            
    editor.addOfflineActionListener("kill", ListCommand(), 'Kill all the running shells.', True) 
                    
