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
    from org.eclipse.jface.dialogs import MessageDialog #@UnresolvedImport
    
    class ClearTemplateCache(Action):
        def run(self):
            from org.python.pydev.editor.templates import TemplateHelper
            TemplateHelper.clearTemplateRegistryCache()
            MessageDialog.openInformation(editor.getSite().getShell(), "Ok", "Ok, cleared templates cache.");
            
    editor.addOfflineActionListener(
        "--clear-templates-cache", 
        ClearTemplateCache(), 
        'Clears the template variables cache.', 
        True) 
                    
