from __future__ import nested_scopes # for Jython 2.1 compatibility

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
    Action = editor.getActionClass() #from org.eclipse.jface.action import Action #@UnresolvedImport

    class ClearTemplateCache(Action):
        def run(self):
            from org.python.pydev.editor.templates import TemplateHelper #@UnresolvedImport
            TemplateHelper.clearTemplateRegistryCache()
            editor.showInformationDialog("Ok", "Ok, cleared templates cache.");

    editor.addOfflineActionListener(
        "--clear-templates-cache",
        ClearTemplateCache(),
        'Clears the template variables cache.',
        True)

