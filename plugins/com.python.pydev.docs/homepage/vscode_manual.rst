
Settings
======================================

python.pydev.java.home
--------------------------------------

If the extension can't find the java executable, this setting can be used to specify the java home folder (which has **/bin/java** inside it).

**Note**: requires restarting vscode

python.pydev.ls.vmargs
--------------------------------------

vmargs to the java executable used to launch PyDev.

**Note**: requires restarting vscode

python.pydev.pythonExecutable
--------------------------------------

The python executable to be used (if not specified, **python** is expected to be in the **PATH**).

python.pydev.pythonPath
--------------------------------------

A list of folders that should be added to the **PYTHONPATH** and should be considered source folders
(i.e.: folders with the sources meant to be edited by the user).

If not specified, the folders which are in the **PYTHONPATH** and are available below a workspace
in vscode will be considered as source folders (and if there's no match, each workspace folder in
vscode will be considered a source folder).

python.pydev.forcedBuiltins
--------------------------------------

A  list of additional modules to be inspected through a shell
(see http://www.pydev.org/manual_101_interpreter.html#PyDevInterpreterConfiguration-ForcedBuiltins for more information).


python.pydev.preferredImportLocation
--------------------------------------

The preferred import location to be used on auto-imports and quick-fix to determine the location to add an import. Must be one of:

**global**: Adds the import to the imports in the top of the module.

**topOfMethod**: Adds the import to the top of the current method.

**lineBeforeUsage**: Adds the import just before the line where the import was requested.

editor.formatOnType
-----------------------

When format on type is turned on, PyDev will automatically add some of its auto-editions (such as adding **self** on a method automatically
or closing a method with **):**).
