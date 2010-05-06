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

Release 1.5.7
==============

* **Uniquely identifying editors:**

    * Names are never duplicated
    * Special treatment for __init__
    * Special treatment for django on views, models and tests
    * See: http://pydev.blogspot.com/2010/04/identifying-your-editors.html for details

* **Debugger:**

    * **CRITICAL**: Fixed issue which could make the debugger skip breakpoints
    * Properly dealing with varibles that have '<' or '>'
    * Debugging file in python 3 with an encoding works
    * Double-clicking breakpoint opens file from the workspace instead of always forcing an external file
    * Added '* any file' option for file selection during a debug where the file is not found

* **Performance improvements for dealing with really large files:**
    
    * Code folding marks won't be shown on *really large files* for performance reasons
    * Performance improvements in the code-analysis (much faster for *really large files*)
    * Outline tree is also faster

* **Interpreter configuration:**

    * Only restoring the needed interpreter info (so, it's much faster to add a new interpreter)
    * Using an asynchronous progress monitor (which makes it even faster)
    * Interpreter location may not be duplicated (for cases where the same interpreter is used with a different config, virtualenv should be used)
    * Properly refreshing internal caches (which made a ctrl+2+kill or a restart of eclipse needed sometimes after configuring the interpreter)
    * socket added to forced builtins

* **Python 3 grammar:**

    * Code completion and code-analysis work when dealing with keyword only parameters
    * Properly reporting syntax error instead of throwing a NumberFormatException on "1.0L"
    
* **Editor and forcing tabs:**

    * Option to toggle forcing tabs added to the editor context menu
    * Fixed tabs issue which could change the global setting on force tabs
    
* **Indentation:**

    * Added rule so that indentation stops at the level of the next line def or @ (to indent to add a decorator)
    * Auto indent strategy may indent based on next line if the previous is empty

* **General:**
    
    * Django configuration supporting version 1.2 (contribution by Kenneth Belitzky)
    * Fixed encoding problem when pasting encoded text with indentation
    * asthelper.completions no longer created on current directory when project is removed
    * __all__ semantics correct when a tuple is defined (and not only when a list is defined)
    * Fixed issue in extract method (was not creating tuple on caller function with multiple returns)
    * Improved heuristic for assist assign (ctrl+1)
    * On search open files (ctrl+2+s), dialog is opened if nothing is entered and there's no editor selection
    * Fixed issue where ctrl+2 would not work on linux


    
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

 