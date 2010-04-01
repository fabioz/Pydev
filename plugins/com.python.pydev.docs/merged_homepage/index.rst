What is Pydev?
=================

Pydev is a **Python IDE** for **Eclipse**, which may be used in **Python**, **Jython** and **IronPython** development.

.. _Features Matrix: manual_adv_features.html
.. _History for Pydev Extensions: history_pydev_extensions.html
.. _History for Pydev: history_pydev.html
.. _Pydev Blog: http://pydev.blogspot.com/

.. _Django Integration: manual_adv_django.html
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

 * `Django integration`_
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

Release 1.5.6
==============


* **Django integration:**

    * New Django project can be created through wizards
    * Can set an existing project as a Django project (right-click project > pydev > set as django project)
    * Can remove Django project config (right-click project > django > remove django project config)
    * Custom actions can be passed to the configured manage.py through **ctrl+2+dj django_action** -- if no action is passed, will open dialog to choose from a list of previously used commands.
    * Predefined/custom actions can be used through right-clicking the project > django > select custom action
    * manage.py location and settings module configured
    * Django shell (with code-completion, history, etc) available
    * Run/Debug as Django available
    * See: `Django Integration`_ for more details

* **Find/Replace:**

    * The search in open files is no longer added in the find/replace dialog and now works through **Ctrl+2+s word_to_find** (in the Pydev editor) and if no word is passed, the editor selection is used
    
* **Go to definiton:**

    * Properly works with unsaved files (so, it will work when searching for a definition on an unsaved file)
    * Properly working with eclipse 3.6 (having FileStoreEditorInput as the editor input)

* **Editor:**

    * Automatically closing literals.
    * Removing closing pair on backspace on literal
    * Improved heuristics for automatically closing (, [ and {
    * Removing closing pairs on backspace on (,[ and {
    * **ctrl+2+sl** (sl comes from 'split lines' -- can be used to add a new line after each comma in the selection
    * **ctrl+2+is** (is comes from 'import string' -- can be used to transform the selected import into a string with dots
    
* **General:**

    * Code-completion properly working on relative import with an alias.
    * Fixed racing issue that could deadlock pydev (under really hard to reproduce circumstances)
    * Removing reloading code while debugging until (if) it becomes more mature in the python side
    * Fixed issue where a new project created didn't have the source folder correctly set
    * Text selection in double click no longer has weird behavior
    * Local refactoring working on files not in the PYTHONPATH
    * Edit properly working on string substitution variables
    * Using with statement on python 2.5 no longer makes lines wrong in the AST
    
    
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

 