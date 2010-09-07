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

Release 1.6.2
==============

* Pydev is now also distributed with Aptana Studio 3, so it can be gotten in a version that doesn't require installing it as 
  a separate plugin. Get it at: http://aptana.com/products/studio3/download 

* **Django templates editor** (requires Aptana Studio 3)

    * Supports HTML files with HTML, CSS and Javascript
    * Supports CSS files
    * Outline page
    * Code-completion for Django templates based on templates (window > preferences > pydev > django templates editor > templates)
    * Code-completion for HTML, CSS and Javascript 
    * Syntax highlighting based on the templates with the 'Django tags' context
    * Colors based on the Aptana themes
    
* **Python 2.7 grammar** supported

* Fixed indexing issue on contents getting getting stale in the cache

* Fixed issue where the partitioning became wrong when entering a multiline string

* Colors in the compare editor are now correct when using the Aptana themes

* Extract method refactoring now works with "import" and "from ... import" inside a method

* Source folders now appear before other folders

* Fixed False positive on code analysis when using the property decorator


Release 1.6.1
==============

* **Debugger**

    * **Critical Fix: issue that prevented the debugger from working with Python 3 solved**
    * Improving socket connection handling

* **Launching**

    * Restart last launch and terminate all launches actions created
        * Restart last: **Ctrl+Shift+F9** (in pydev editor)
        * Terminate all: **Ctrl+Alt+F9** (in pydev editor)
        * Buttons were also added to pydev consoles 
    
* **Utilities**

    * **2to3**: Right-clicking a folder or file will show an option in the Pydev menu to convert from python 2 to python 3 (note that lib2to3 must available in the python installation).
    * Defining execfile in a Python 3 interactive console so that Ctrl+Alt+Enter works.
    * Fixed issue in the code style preferences page (switched value shown).
    * com.ziclix.python.sql added to the forced builtins in a Jython install by default.
    * Improved some icons when on a dark theme (patch from Kenneth Belitzky)


Release 1.6.0
==============


* **Debugger**

    * Code-completion added to the debug console
    * Entries in the debug console are evaluated on a line-by-line basis (previously an empty line was needed)
    * Threads started with thread.start_new_thread are now properly traced in the debugger
    * Added method -- pydevd.set_pm_excepthook() -- which clients may use to debug uncaught exceptions
    * Printing exception when unable to connect in the debugger
    
* **General**

    * Interactive console may be created using the eclipse vm (which may be used for experimenting with Eclipse) 
    * Apply patch working (Fixed NPE when opening compare editor in a dialog)
    * Added compatibility to Aptana Studio 3 (Beta) -- release from July 12th


    
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

 