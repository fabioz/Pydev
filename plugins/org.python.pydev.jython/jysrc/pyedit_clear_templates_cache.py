
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
    ClearTemplateCache = systemGlobals.get('ClearTemplateCache')
    if ClearTemplateCache is None:
        Action = editor.getActionClass() #from org.eclipse.jface.action import Action #@UnresolvedImport
    
        class ClearTemplateCache(Action):
            
            def __init__(self, editor):
                self.editor = editor
                
            def run(self):
                from org.python.pydev.editor.templates import TemplateHelper #@UnresolvedImport
                editor = self.editor
                
                TemplateHelper.clearTemplateRegistryCache()
                editor.showInformationDialog("Ok", "Ok, cleared templates cache.");
        
        systemGlobals['ClearTemplateCache'] = ClearTemplateCache

    editor.addOfflineActionListener(
        "--clear-templates-cache",
        ClearTemplateCache(editor),
        'Clears the template variables cache.',
        True)

