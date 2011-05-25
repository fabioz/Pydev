..
    <right_area>
    <p class="italic">"Personally, the pleasure I take in
        developping software is half linked to the language, half to the
        programming tools.
        
        With PyDev, I've got everything."</p>
    <p>
        Franck Perez
    </p>
    <br/>
    <br/>
    <br/>
    <br/>
    <br/>
    <br/>
    
    <p class="italic">
    "PyDev is a core tool in our development process, and is a major reason
    why Python has become viable for us as a production language.  I look
    forward to each new release of PyDev as it is continually evolving into
    a more and more powerful development environment."
    </p>
    <p>
        Eric Wittmann, Zoundry LLC.
    </p>
    <br/>
    <br/>
    <br/>
    <br/>
    </right_area>
    
    
    <image_area></image_area>
    
    
    <quote_area></quote_area>

What is PyDev?
=================

PyDev is a **Python IDE** for **Eclipse**, which may be used in **Python**, **Jython** and **IronPython** development.

.. _Features Matrix: manual_adv_features.html
.. _History for PyDev Extensions: history_pydev_extensions.html
.. _History for PyDev: history_pydev.html
.. _PyDev Blog: http://pydev.blogspot.com/

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
.. _Unittest integration: manual_adv_pyunit.html
.. _Code coverage: manual_adv_coverage.html
.. _video: video_pydev_20.html

It comes with many goodies such as:

+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------------------------------------------------------------------------------------------------------+
| * `Django integration`_                                                                                                                                                                                                                                  |                                                                                                                                                  |
| * `Code completion`_                                                                                                                                                                                                                                     |                                                                                                                                                  |
| * `Code completion with auto import`_                                                                                                                                                                                                                    |                                                                                                                                                  |
| * `Syntax highlighting`_                                                                                                                                                                                                                                 |                                                                                                                                                  |
| * `Code analysis`_                                                                                                                                                                                                                                       | .. raw:: html                                                                                                                                    |
| * `Go to definition`_                                                                                                                                                                                                                                    |                                                                                                                                                  |
| * `Refactoring`_                                                                                                                                                                                                                                         |    <a href="video_pydev_20.html" border=0><img class="link" src="images/video/snap.png" alt="PyDev 2.0 video" title="Click to see video" /></a>  |
| * `Mark occurrences`_                                                                                                                                                                                                                                    |                                                                                                                                                  |
| * `Debugger`_                                                                                                                                                                                                                                            |                                                                                                                                                  |
| * `Remote debugger`_                                                                                                                                                                                                                                     |                                                                                                                                                  |
| * `Tokens browser`_                                                                                                                                                                                                                                      |                                                                                                                                                  |
| * `Interactive console`_                                                                                                                                                                                                                                 |                                                                                                                                                  |
| * `Unittest integration`_                                                                                                                                                                                                                                |                                                                                                                                                  |
| * `Code coverage`_                                                                                                                                                                                                                                       |                                                                                                                                                  |
| * **and many others**:                                                                                                                                                                                                                                   |                                                                                                                                                  |
+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------------------------------------------------------------------------------------------------------+

For more details on the provided features, check the `Features Matrix`_.


.. _`Getting started guide`: manual_101_root.html

Important
==========
First time users are strongly advised to read the `Getting started guide`_  which explains how to properly configure PyDev



Release 2.1
===============


Noteworthy
-----------


**Code Analysis**
    
 * By default, only the currently opened editor will be analyzed.
 * Added action to force the analysis on a given folder or file.
 * Showing error markers for PyDev elements in the tree.
 * New option to remove error markers when the editor is closed (default).

**Editor**

 * Override method completions (Ctrl+Space after a 'def ') .
 * Completions starting with '_' now have lower priority.
 * Fixed major issue when replacing markers which could make errors appear when they shouldn't appear anymore
 * Auto-linking on close parens is now optional (and disabled by default).

**Code coverage**
 
 * No longer looses the selection on a refresh.
 * Fixed issue where coverage was not working properly when running with multiple processes.
 * Added orientation options

**PyUnit**

 * Added feature to relaunch the last launch when file changes (with option to relaunch only errors).
 * setUpClass was not called when running with the pydev test runner
 * F12 makes the editor active even if there's a tooltip active in the PyUnit view.
 * The PyUnit tooltip is now properly restoring the focus of the previous active control.
 * Added orientation options


**Others**

 * Improved the django templates code-completion to better deal with the html/css counterparts.
 * When the interpreter is not configured, detect it and take the proper actions to ask the user to configure it.
 * No longer using StyleRange.data as it's not available for older versions of Eclipse.
 * Fixed issue where references to modules could become obsolete in memory.
 * When a source folder is added/removed, the package explorer will properly update to remove/add errors.
 * Fixed issue where code-formatting could be really slow on unbalanced parenthesis on a big file.
 * Fixed error accessing __builtins__.__import__ when running in the debugger.
 * Fixed issue with wrong code-formatting with numbers.
 * The assist to create a docstring will remove the pass right after it (if there's one).
 * The path of the file that holds the preferences no longer has the same number of chars as the path for the interpreter.
 * Fixed some TDD actions
 * Fixed issue where project references were not being gotten recursively as they should. 
 * Fixed dedent issues on else and elif.
 * Fixed issue with __init__.py not showing the parent package name (when set in the preferences to do so).
 * sys._getframe shouldn't be needed when running unit-tests in IronPython.
 * Showing interpreter information when a given project is also a source folder.






Development Info
====================================

`PyDev Blog`_

Releases History:
==================

`History for PyDev`_

`History for PyDev Extensions`_

 