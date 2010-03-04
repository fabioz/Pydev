What is Pydev?
=================

Pydev is a **Python IDE** for **Eclipse**, which may be used in **Python**, **Jython** and **IronPython** development.

.. _Features Matrix: manual_adv_features.html
.. _History for Pydev Extensions: history_pydev_extensions.html
.. _History for Pydev: history_pydev.html
.. _Pydev Blog: http://pydev.blogspot.com/

.. _Code Completion: manual_adv_complctx.html
.. _Code completion with auto import: manual_adv_complnoctx.html
.. _Code Analysis: manual_adv_code_analysis.html
.. _Go to definition: manual_adv_gotodef.html
.. _Refactoring: manual_adv_refactoring.html
.. _Mark occurrences: manual_adv_markoccurrences.html
.. _Debugger: manual_adv_debugger.html
.. _Remote debugger: manual_adv_remote_debugger.html
.. _Tokens browser: manual_adv_open_decl_quick.html
.. _Interactive console: manual_adv_interactive_console.html
.. _Syntax highlighting: manual_adv_editor_prefs.html


It comes with many goodies such as:

 * `Code completion`_
 * `Code completion with auto import`_
 * `Syntax highlighting`_
 * `Code analysis`_
 * `Go to definition`_
 * `Refactoring`_
 * `Mark occurrences`_
 * `Debugger`_
 * `Remote debugger`_
 * `Tokens browser`_
 * `Interactive console`_
 * **and many others**:

For more details on the provided features, check the `Features Matrix`_.

Release 1.5.5
==============

.. _See Predefined Completions in manual for more info: manual_101_interpreter.html

* **Predefined completions available for code completion:**

    * Predefined completions may be created for use when sources are not available 
    * Can also be used for providing better completions for compiled modules (e.g.: PyQt, wx, etc.)
    * Defined in .pypredef files (which are plain Python code)
    * Provides a way for generating those from a QScintilla .api file (experimental)
    * `See Predefined Completions in manual for more info`_
    
* **Pydev Package Explorer:**

    * Showing the contents of the PYTHONPATH from the interpreter for each project
    * Shows the folder containing the python interpreter executable (to browse for docs, scripts, etc)
    * Allows opening files in the interpreter PYTHONPATH (even inside zip files)

* **Editor options:**

    * Find/replace dialog has option to search in currently opened editors
    * Move line up/down can move considering Python indentation (not default)
    * Simple token completions can have a space or a space and colon added when applied. E.g.: print, if, etc (not default)

* **Refactoring:**

    * Fixed InvalidThreadAccess on refactoring
    * Fixed problem doing refactoring on external files (no file was found) 

* **Globals Browser (Ctrl+Shift+T):**

    * No longer throwing NullPointerException when the interpreter is no longer available for a previously found token

* **General:**
    
    * When creating a new pydev project, the user will be asked before changing to the pydev perspective
    * Only files under source folders are analyzed (files in the external source folders would be analyzed if they happened to be in the Eclipse workspace)
    * Interactive console now works properly on non-english systems
    * Hover working over tokens from compiled modules (e.g.: file, file.readlines)
    * JYTHONPATH environment variable is set on Jython (previously only the PYTHONPATH was set)
    * Fixed path translation issues when using remote debugger
    * Fixed issue finding definition for a method of a locally created token



What happened to Pydev Extensions?
====================================


Pydev Extensions is now merged with Pydev, and its once closed source code has become open source (on version 1.5.0). 
Thus, there is no more Pydev Extensions, only the open source Pydev, with all the capabilities of Pydev Extensions
incorporated.

Development Info
====================================

`Pydev Blog`_

Releases History:
==================

`History for Pydev`_

`History for Pydev Extensions`_

 