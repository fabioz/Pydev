..
    <right_area>
    	<div class="section" id="development-info">
		<h1>Development Info</h1>
		<p><a class="reference external" href="http://pydev.blogspot.com/">PyDev Blog</a></p>
		</div>
		<div class="section" id="releases-history">
		<h1>Releases History:</h1>
		<p><a class="reference external" href="history_pydev.html">History for PyDev</a></p>
		<p><a class="reference external" href="history_pydev_extensions.html">History for PyDev Extensions</a></p>
		</div>


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

.. _Type hinting: manual_adv_type_hints.html
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
| * `Type hinting`_                                                                                                                                                                                                                                        |                                                                                                                                                  |
| * `Code analysis`_                                                                                                                                                                                                                                       | .. raw:: html                                                                                                                                    |
| * `Go to definition`_                                                                                                                                                                                                                                    |                                                                                                                                                  |
| * `Refactoring`_                                                                                                                                                                                                                                         |    <a href="video_pydev_20.html" border=0><img class="link" src="images/video/snap.png" alt="PyDev 2.0 video" title="Click to see video" /></a>  |
| * `Debugger`_                                                                                                                                                                                                                                            |                                                                                                                                                  |
| * `Remote debugger`_                                                                                                                                                                                                                                     |                                                                                                                                                  |
| * `Tokens browser`_                                                                                                                                                                                                                                      |                                                                                                                                                  |
| * `Interactive console`_                                                                                                                                                                                                                                 |                                                                                                                                                  |
| * `Unittest integration`_                                                                                                                                                                                                                                |                                                                                                                                                  |
| * `Code coverage`_                                                                                                                                                                                                                                       |                                                                                                                                                  |
| * Find References (Ctrl+Shift+G)                                                                                                                                                                                                                         |                                                                                                                                                  |
| * **and many others**:                                                                                                                                                                                                                                   |                                                                                                                                                  |
+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------------------------------------------------------------------------------------------------------+

For more details on the provided features, check the `Features Matrix`_.


Important
==========
First time users are strongly advised to read the `Getting started guide`_  which explains how to properly configure PyDev.


LiClipse
==========

The recommended way of using PyDev is bundled in `LiClipse <http://brainwy.github.io/liclipse/>`_, which provides PyDev builtin as well as
support for other languages such as Django Templates, Mako, RST, C++, CoffeScript, Dart, HTML, JavaScript, CSS, among others (also, by licensing
LiClipse you directly support the development of PyDev).


Gold Sponsors
==============

.. raw:: html

   <!--Added 2013-07-25-->
   <a href="http://www.kichwacoders.com/" border=0><img class="sponsors" src="images/sponsors/kichwacoders.png" alt="Kichwacoders" title="http://www.kichwacoders.com/" /></a>
   <a href="http://www.tracetronic.com" border=0><img class="sponsors" src="images/sponsors/tracetronic.png" alt="Tracetronic" title="http://www.tracetronic.com/" /></a>
   <a href="http://brainwy.github.io/liclipse/" border=0><img class="sponsors" src="images/sponsors/liclipse.png" alt="LiClipse" title="http://brainwy.github.io/liclipse/" /></a>
   <a href="http://www.squishlist.com/" border=0><img class="sponsors" src="images/sponsors/squishlist.png" alt="Squishlist" title="http://www.squishlist.com/" /></a>

Supporting PyDev
=================

Thank you to all PyDev supporters: https://sw-brainwy.rhcloud.com/supporters/PyDev.


To show your appreciation for PyDev and to help to keep it going too, support it at https://sw-brainwy.rhcloud.com/. Supporter benefits
include having votes to decide the next tackled tickets and space in the homepage.

Companies have the option of sponsoring PyDev through corporate sponsorship. See `About/Sponsorship <about.html>`_ for details.


.. _`Getting started guide`: manual_101_root.html



Release 3.2.0
==========================

* **Important**: PyDev requires Eclipse 3.8 or 4.3 onwards and Java 7! For older versions, keep using PyDev 2.x.


* **General**:

    * Added option to sort imports on save.
    
    * Showing dialog suggesting user to customize settings in Eclipse which are more suitable for PyDev.
    
    * Memory improvements on situations where an OutOfMemoryError could happen.

* **Rename refactoring**:

    * Added option to rename module without updating references.
    
    * Bugfixes.

* **Performance**:

    * Code completion: Builtins gotten from a shell are now cached for subsequent requests.
    
    * Doing a full build (reindex) is faster.
    
    * Search references (Ctrl+Shift+G) when initial is on external works (for matches in workspace).

* **Debugger**:

    * Improvements on stackless integration.
    
    * Providing a view which shows the current caught exception.
    
    * Providing way to ignore current caught exception.
    
    * Providing option to show progress on taskbar when breakpoint is hit to get the users attention (windows 7).
    
    * Fixed issue in while getting referrers when getting __dict__ and having an exception. 




    
