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
    <br/>
    <br/>
    <br/>
    
    <br/>
    <strong>Acknowledgements</strong>
    <br/>
    <br/>
    <p class="italic">
    "YourKit kindly supports PyDev (and other open source projects) with its full-featured Java Profiler.
    <br/>
    <br/>
    YourKit, LLC is the creator of innovative and intelligent tools for profiling
    Java and .NET applications. Take a look at YourKit's leading software products:
    <a href="http://www.yourkit.com/java/profiler/index.jsp"><img src="images/yk.png" width="12" height="12" border="0"  /> YourKit Java Profiler</a> and
    <a href="http://www.yourkit.com/.net/profiler/index.jsp"><img src="images/yk.png" width="12" height="12" border="0" /> YourKit .NET Profiler</a>."
    </p>
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


Release 2.7.2
===============


* Updated icons in PyDev to match better a dark theme.
* Minor: improved colors in outline according to theme.
* Improved minimap.
* Fixed issue copying qualified name when editor is not in the PYTHONPATH.
* Removed ping from PyDev.
* Fixed issue on Ctrl+1 assist to ignore some warning.
* Improved comment/uncomment to deal properly with pep8 formatting.
* Added plead so that PyDev does not become unsupported (http://igg.me/at/liclipse)


Release 2.7.0 (and 2.7.1)
===========================


* **Code formatter**:

 * Number of spaces before a comment can be configured (default: 2 spaces as pep-8 recommends)
 * Minimum number of spaces before start of comment may be configured (default: 1 space as pep-8 recommends)
 * Right trim lines now also properly trims comments.
 * When the auto-formatter is enabled, if syntax errors are present the code-formatting is not applied (it could end up getting things wrong in this situation).

* Python 3.3 'yield from' syntax now properly supported.

* Fixed issue when unable to get filesystem encoding when configuring interpreter.
* Debugger: 'Enable Condition' checkbox in break properties dialog no longer ignored.
* Fixed ClassCastException during parse in Python file with yield in global scope.
* Fixed StackOverflowError in fast parser (i.e.: parser used to get only the outline of the code).
* PyDev Mylyn integration can now be installed on Eclipse 4.2.
* Fixed NPE when trying to add interpreter and it detected directory which we could not list() in Java.
* Fixed cache issue in code-completion (nature.startRequests() could end up not having nature.endRequests() called).
* Save a bit faster on big files (i.e.: No longer doing invalidateTextPresentation on each save).


Release 2.6.0
===============

* **Interactive console**:

 * **It's now possible to use the interactive console attached to a debug session.** (patch from Hussain Bohra)
 
   * To use this feature either right-click a frame in the debug view and choosing PyDev > Debug console or create a new Interactive console as usual (Ctrl+Alt+Enter and choose 'PyDev Debug Console' -- but note that this option will only be enabled when in a debug session with a selected frame in the Debug view.
   
 * Fixed issue where completions from the console did not work properly with '%' because quoting was not being properly done.
 * Fixed issue where the **execfile()** redefinition in the PyDev console did not use the proper globals
 * When launching interactive console, PYTHONPATH order is properly kept (patch from James Blackburn).
 * Fix pasting into the middle of the console (patch from James Blackburn).
 * For paste, only go to the end of the line if the cursor isn't in range (patch from James Blackburn).

* **PyUnit**:
 
 * Improved preferences page configuration (links shown to add options).
 * Improved test discovery in PyDev PyUnit runner (exclude/include files/tests options added).
  
* **Jython**:

 * **print** may be used in dotted names as Jython requires for grammars 2.4 and 2.5.
 

* **Others**:

 * In a build, PyDev could end up reading the contents of files unrelated to Python.
 * Django project startup compatible with django 1.4.
 * Assignments to builtins when in the class-level no longer generate a warning.
 * Fixed issue starting new thread in the debugger (fix for **paste/waitress**).
 * Fixed error configuring interpreter if os.path was not present.
 * Fixed issue when configuring interpreter which has unicode characters in the PYTHONPATH.
 * When searching for external files, also take a look at the configured projects, as it may be that the file should actually be found in an external source folder.
 * Fixed issues getting marker on files with a dirty editor and where we could end up getting markers from other files.
 * The scripting output console is not shown unless there's actually some output to show.
 * A bunch of other minor fixes.
        
 

Development Info
====================================

`PyDev Blog`_

Releases History:
==================

`History for PyDev`_

`History for PyDev Extensions`_

 