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
| * **and many others**:                                                                                                                                                                                                                                   |                                                                                                                                                  |
+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------------------------------------------------------------------------------------------------------+

For more details on the provided features, check the `Features Matrix`_.


Important
==========
First time users are strongly advised to read the `Getting started guide`_  which explains how to properly configure PyDev

Gold Sponsors
==============

.. raw:: html                        
                                                                                                          
   <!--Added 2013-07-25-->                                                                                                          
   <a href="http://www.kichwacoders.com/" border=0><img class="sponsors" src="images/sponsors/kichwacoders.png" alt="Kichwacoders" title="http://www.kichwacoders.com/" /></a>
   <a href="http://www.tracetronic.com" border=0><img class="sponsors" src="images/sponsors/tracetronic.png" alt="Tracetronic" title="http://www.tracetronic.com/" /></a>
   <a href="http://www.squishlist.com/" border=0><img class="sponsors" src="images/sponsors/squishlist.png" alt="Squishlist" title="http://www.squishlist.com/" /></a>
   
Supporting PyDev
=================

Thank you to all PyDev supporters: https://sw-brainwy.rhcloud.com/supporters/PyDev. 

   

To show your appreciation for PyDev and to help to keep it going too, support it at https://sw-brainwy.rhcloud.com/. Supporter benefits
include having votes to decide the next tackled tickets and space in the homepage.
 

.. _`Getting started guide`: manual_101_root.html
.. _`Type hinting with docstrings`: manual_adv_type_hints.html
.. _`Install Instructions`: manual_101_install.html

Release 2.8.0
==========================

* This release was done just to back-up the change related to Gtk event loop which had some issues, so, the UI event loop will only work with PyQt4 now. 

Release 2.8.0
==========================

* **Type Inference now works with docstrings** (Sphinx or Epydoc). See: `Type hinting with docstrings`_

* **Fixed debugger to work on Google App Engine**

* **Patch by Edward Catmur**

 * **Interactive console supports running with the Qt and Gtk event loops**
 
* **Patches by Andrew Ferrazzutti**

 * Multiple main modules/packages may be selected in the unittest run configuration
 
 * Properly handling unittest errors caused by setUpClass/setUpModule exceptions
 
 * It's possible to select the Working Set configuration in the New PyDev Project wizard
 
* **Patches by Christoph Zwerschke**

 * It's possible to specify PyLint settings per project by passing --rcfile=.pylintrc (it's now run relative to the project directory)
 
 * PyLint now accepts an executable so that it does not have to rely on the configured interpreter.
 
* Fixed OutOfMemoryError when large file was found in the workspace.
* Editor startup is now faster due to improvements in Jython scripts.
* Improved the way that the interpreter location is shown on the pydev package explorer.
* PyDev Package Explorer icon no longer missing when top level elements is set to Working Sets
* Other minor bugfixes

Note: PyDev is now signed with a new (self-signed) certificate (see `Install Instructions`_ for the new certificate) .

 