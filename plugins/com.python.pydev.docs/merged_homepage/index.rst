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


Release 1.5.3
===============

Fixed bug where an error was being print to the pydev console on a run.


Release 1.5.2
===============

 * Profile to have **much** lower memory requirements (especially on code-analysis rebuilds)
 * Profile for parsing to be faster
 
 * Compare Editor
 
   * Syntax highlighting integrated 
   * Editions use the pydev editor behaviour
   * Code completion works
   
 * Fixed issue where pydev could deadlock
 * No longer reporting import redefinitions and unused variables for the initial parts of imports such as import os.path
 * Fixed issue where pydev was removing __classpath__ from the pythonpath in jython
 * Using M1, M2 and M3 for keys instead of hardcoding Ctrl, Shift and Alt (which should make keybindings right on Mac OS)
 * Fixed some menus and popups
 * Properly categorizing Pydev views
 * Handling binary numbers in the python 2.6 and 3.0 grammar
 * from __future__ import print_function works on python 2.6
 * Added drag support from the pydev package explorer
 * Properly translating slashes on client/server debug
 * Other minor fixes
 


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

 