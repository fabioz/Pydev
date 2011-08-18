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
    <img alt="PyDev" src="images/eclipse_award.png"/>
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



Release 2.2.1
===============


**Quick-outline**

    .. image:: images/index/quick_outline_parent.png
        :class: no_border
        

    * Parent methods may be shown with a 2nd Ctrl+O.
    * The initial node is selected with the current location in the file.

**Extract local refactoring**

    .. image:: images/index/refactor_duplicate.png
        :class: no_border
        
    * Option to replace duplicates.
    * Fixed issue where wrong grammar could be used.
        
**Others**

    * Improved handling of Ctrl+Shift+T so that no keybinding conflict takes place (now it'll be only active on the PyDev views/editor).
    * PyLint markers always removed on a project clean.
    * If the standard library source files are not found, more options are presented.
    * If the completion popup is focused and shift is pressed on a context insensitive completion, a local import is done.
    * Fixed issue where a local import wasn't being added to the correct location.
    * Fixed error message in debugger when there was no caught/uncaught exception set in an empty workspace.
    * Performance improvements on hierarchy view.
    * Django commands may be deleted on dialog with backspace.


Release 2.2
===============


Noteworthy
-----------

**Eclipse 3.7** 

    * Eclipse 3.7 (Indigo) is now supported.

**Break on Exceptions**

    .. image:: images/index/manage_exceptions.png
        :class: no_border
        
    * It's now possible to **break on caught exceptions** in the debugger.
    * There's an UI to break on caught or uncaught exceptions (menu: Run > Manage Python Exception Breakpoints).

**Hierarchy view**

    .. image:: images/index/hierarchy_view.png
        :class: no_border

    * UI improved (now only uses SWT -- access through F4 with the cursor over a class).

**PyPy**: 
    
    * PyDev now supports PyPy (can be configured as a regular Python interpreter).

**Django**

    .. _`Django remote debugging with auto-reload`: manual_adv_remote_debugger.html#django-remote-debugging-with-auto-reload
    
    * Django configuration in project properties page (improved UI for configuration of the django manage.py and django settings module).
    * Improved support for debugging Django with autoreload. Details at: `Django remote debugging with auto-reload`_.

**Code analysis**

    * Fixed issue where a resolution of a token did not properly consider a try..except ImportError (always went for the first match).
    * Fixed issue with relative import with wildcards.
    * Fixed issue with relative import with alias.
    * Fixed issue where binary files would be wrongly parsed (ended up generating errors in the error log).

**Code completion**

    * Improved sorting of proposals (__*__ come at last)

**Others**

    * Improved ctrl+1 quick fix with local import.
    * Fixed issue running with py.test.
    * PyDev test runner working properly with unittest2.
    * Fixed compatibility issue with eclipse 3.2.
    * No longer sorting libraries when adding interpreter/added option to select all not in workspace.
    * Fixed deadlock in the debugger when dealing with multiple threads.
    * Fixed debugger issue (dictionary changing size during thread creation/removal on python 3.x).


**Note**: Java 1.4 is no longer supported (at least Java 5 is required now).



Development Info
====================================

`PyDev Blog`_

Releases History:
==================

`History for PyDev`_

`History for PyDev Extensions`_

 