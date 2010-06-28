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

Release 1.5.8
==============

* **Features only available on Aptana Studio 3 (Beta):**

    * Theming support provided by Aptana Studio used
    * Find bar provided by Aptana used (instead of the default find/replace dialog)
    * Aptana App Explorer provides Pydev nodes
    
    
* **Eclipse:**

    * Eclipse 3.6 is now supported
    * Pydev Jars are now signed


* **Django:**

    * DoesNotExist and MultipleObjectsReturned recognized in Django    
    * Added option to make the name of Django models,views,tests editors work as regular editors while still changing the icon


* **Run/Debug:**

    * Ctrl+Shift+B properly working to toggle breakpoint
    * If file is not found in debugger, only warn once (and properly cache the return)
    * Run configuration menus: Only showing the ones that have an available interpreter configured
    
    
* **Outline/Pydev Package Explorer:**

    * Fixed sorting issue in pydev package explorer when comparing elements from the python model with elements from the eclipse resource model
    * Fixed issue when the 'go into' was used in the pydev package explorer (refresh was not automatic)
    * Added decoration to class attributes
    * Added node identifying if __name__ == '__main__'
    
    
* **General:**
    
    * Properly working with editor names when the path would be the same for different editors
    * Fixed issue where aptanavfs appeared in the title for aptana remote files
    * Fixed halting condition
    * Not always applying completion of dot in interactive console on context-insensitive completions
    * Home key properly handled in compare editor
    * Interactive console working with pickle
    * String substitution configuration in interpreter properly works
    * On import completions, full module names are not shown anymore, only the next submodule alternative
    

    
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

 