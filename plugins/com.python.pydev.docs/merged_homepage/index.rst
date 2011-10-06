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



Release 2.2.3
===============

* Performance improvements

* Major: Fixed critical issue when dealing with zip files.

* Added option to create method whenever a field would be created in quick fixes (and vice-versa), to properly deal with functional programming styles.

* Fixed issue where PyDev was changing the image from another plugin in the Project Explorer (i.e.: removing error decorations from JSP).

* Fixed issue: if the django models was opened in PyDev, the 'objects' object was not found in the code analysis.

* Test runner no longer leaves exception visible.

* Fixed issue on Py3: Relative imports are only relative if they have a leading dot (otherwise it always goes to the absolute).

* Default is now set to create project with the projects itself as the source folder.

* Handling deletion of .class files.

* Fixed issue where loading class InterpreterInfo in AdditionalSystemInterpreterInfo.getPersistingFolder ended up raising a BundleStatusException in the initialization. 

* Fixed some code formatting issues


Release 2.2.2
===============

**IPython / Interactive console**

    .. image:: images/index/ipython_console.png
        :class: no_border

    * IPython (0.10 or 0.11) is now used as the interactive console backend if PyDev can detect it in the PYTHONPATH.
    * While waiting for the output of a command, intermediary results are printed in the console.
    * ANSI color codes are supported in the interactive console.

**Code Analysis**

    .. image:: images/index/assignment_to_builtin.png
        :class: no_border

    * Reporting variables that shadow builtins as warnings.
    * Fixed issue where __dict__ was not found.
    
**Code completion**

    * Aliases have a better treatment (i.e.: unittest.assertEqual will show the proper type/parameters).
    * Improved support for analyzing function builtins where the return type is known (i.e.: open, str.split, etc).
    
**Debugger**

    * When doing a remote debug session, if the files cannot be found in the local filesystem, PyDev will ask for files in the remote debugger.

**Editor**
    
    * Files without extension that have a python shebang (e.g.: #!/usr/bin/python in the first line) are automatically opened with the PyDev editor (in the PyDev Package Explorer).

**Django**

    * When the shell command is used in the django custom commands, PyDev no longer uses 100% cpu while it doesn't complete.

**Others** 
    
    * Fixed issue where the * operator was not properly formatted.
    * When the quick outline dialog is deactivated, it's closed.
    * Fixed heuristic for finding position for local import. 
    * Fixed compare editor issue with Eclipse 3.2.
    * Fixed integration issue with latest PyLint.
    * Fixed deadlock issue on app engine manage window.
    * More options added to configure the automatic deletion of .pyc files (delete always, never delete, delete only on .py delete).


Development Info
====================================

`PyDev Blog`_

Releases History:
==================

`History for PyDev`_

`History for PyDev Extensions`_

 