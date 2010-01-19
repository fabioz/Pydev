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

Release 1.5.4
==============

 * **Actions**:
 
   * Go to matching bracket (Ctrl + Shift + P)
   * Copy the qualified name of the current context to the clipboard.
   * Ctrl + Shift + T keybinding is resolved to show globals in any context (**note**: a conflict may occur if JDT is present -- it can be fixed at the keys preferences if wanted).
   * Ctrl + 2 shows a dialog with the list of available options.
   * Wrap paragraph is available in the source menu.
   * Globals browser will start with the current word if no selection is available (if possible).
 
 * **Templates**:
 
   * Scripting engine can be used to add template variables to Pydev.
   * New template variables for next, previous class or method, current module, etc.
   * New templates for super and super_raw.
   * print is now aware of Python 3.x or 2.x
   
 * **Code analysis and code completion**:
 
   * Fixed problem when getting builtins with multiple Python interpreters configured.
   * If there's a hasattr(obj, 'attr), 'attr' will be considered in the code completion and code analysis.
   * Fixed issue where analysis was only done once when set to only analyze open editor.
   * Proper namespace leakage semantic in list comprehension.
   * Better calltips in IronPython.
   * Support for code-completion in Mac OS (interpreter was crashing if _CF was not imported in the main thread).
 
 * **Grammar**:
 
   * Fixed issues with 'with' being used as name or keyword in 2.5.
   * Fixed error when using nested list comprehension.
   * Proper 'as' and 'with' handling in 2.4 and 2.5.
   * 'with' statement accepts multiple items in python 3.0.
 
 * **Improved hover**:
 
   * Showing the actual contents of method or class when hovering.
   * Link to the definition of the token being hovered (if class or method).
   
 * **Others**:
 
   * Completions for [{( are no longer duplicated when on block mode.
   * String substitution can now be configured in the interpreter.
   * Fixed synchronization issue that could make Pydev halt.
   * Fixed problem when editing with collapsed code.
   * Import wasn't found for auto-import location if it import started with 'import' (worked with 'from')
   * Fixed interactive console problem with help() function in Python 3.1
   * NullPointerException fix in compare editor.


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

 