What is Pydev?
=================

.. _Features Matrix: manual_adv_features.html
.. _History for Pydev Extensions: history_pydev_extensions.html
.. _History for Pydev: history_pydev.html
.. _Pydev Blog: http://pydev.blogspot.com/

Pydev is a plugin that enables users to use **Eclipse** for **Python**, **Jython** and **IronPython** development, making Eclipse a first class **Python IDE**.

It comes with many goodies such as:

 * code completion
 * code completion with auto import
 * syntax highlighting
 * syntax analysis
 * code analysis
 * go to definition
 * refactor
 * mark occurrences
 * debug
 * tokens browser
 * interactive console
 * **and many others**:

For more details on the provided features, check the `Features Matrix`_.


Release 1.5.1
===============

 * Improvements in the AST rewriter
 * Improvements on the refactoring engine:
 
   * No longer using BRM
   * Merged with the latest PEPTIC
   * Inline local available
   * Extract method bug-fixes
   * Extract local on multi-line
   * Generating properties using coding style defined in preferences
   * Add after current method option added to extract method
   * A bunch of other corner-case situations were fixed
	
 * Bug-fixes:
 
   * Minor editor improvements
   * Adding default forced builtins on all platforms (e.g.: time, math, etc) which wouldn't be on sys.builtin_module_names on some python installations
   * Adding 'numpy' and 'Image' to the forced builtins always
   * Ctrl+1: Generate docstring minor fixes
   * Ctrl+1: Assign to local now follows coding style preferences properly
   * Exponential with uppercase E working on code-formatting
   * When a set/get method is found in code-completion for a java class an NPE is no longer thrown
   * Backspace properly treated in block mode
   * Setting IRONPYTHONPATH when dealing with IronPython (projects could not be referenced)
   * No longer giving spurious 'statement has no effect' inside of lambda and decorators
   * Fixed new exec in python 3k
   * Fixed NPE when breakpoint is related to a resource in a removed project
   * Fixed import problem on regexp that could lead to a recursion.
   * No longer giving NPE when debugging with the register view open
   * List access be treated as __getitem__() in the list -- patch from Tassilo Barth
   * Fix for invalid auto-self added when typing

Release 1.5.0
===============

**Pydev Extensions is now Open Source!**


What happened to Pydev Extensions?
====================================


Pydev Extensions is now merged with Pydev, and its once closed source code has become open source. Thus,
there is no more Pydev Extensions, only the open source Pydev, with all the capabilities of Pydev Extensions
incorporated.

Development Info
====================================

`Pydev Blog`_

Releases History:
==================

 * `History for Pydev`_
 * `History for Pydev Extensions`_

 