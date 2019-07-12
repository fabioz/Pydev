..
    <right_area>
    <h3>Quick Install</h3>

    <p><strong>LiClipse</strong> </p>

		<p>
	    Get LiClipse from <a href="http://www.liclipse.com/">http://www.liclipse.com</a> (and help supporting PyDev) and use a
	    native installer with PyDev builtin.
	    </p>
        <br>

    <p><strong>Update Manager</strong> </p>

    <p> Go to the update manager (Help > Install New Software) and add:
        <br>
        <br>
        <A href="http://www.pydev.org/updates">http://www.pydev.org/updates</A>&nbsp;&nbsp;&nbsp;(for latest version)&nbsp;&nbsp;&nbsp;or
        <br>
        <br>
        <A href="http://www.pydev.org/nightly">http://www.pydev.org/nightly</A>&nbsp;&nbsp;&nbsp;(for nightly build)&nbsp;&nbsp;&nbsp
        <br>
        <br>
        and follow the Eclipse steps.<br/>
        <br/>
        <strong>Note: </strong>View <A href="http://www.pydev.org/update_sites">http://www.pydev.org/update_sites</A> to browse the update sites for other versions.
        </p>


    <br/>

    <p><strong>Zip File</strong></p>

    <p>An alternative is just getting the zip file and extracting it yourself in the eclipse <strong>dropins</strong> (and restart Eclipse).</p>

    </right_area>
    <image_area>download.png</image_area>
    <quote_area><strong>Getting it up and running in your computer...</strong></quote_area>

Important
=========

First time users are **strongly** advised to read the `PyDev Getting
Started <manual_101_root.html>`_ which explains how to properly
configure PyDev.

Standalone install
===================

PyDev is available in **LiClipse**, which provides a hassle free (and OS-native) experience to install it.

**Note that by supporting LiClipse you also directly support the development PyDev itself.**

See the  `LiClipse homepage <http://www.liclipse.com/>`_ for details on getting it.

Also, if using Django-templates, Mako or RST, `LiClipse <http://www.liclipse.com/>`_ is the recommended install as
it provides support for those languages (among others such as C++, CoffeScript, HTML, JavaScript, CSS, etc.), along
with theming support -- which is especially nice for dark themes -- if you're into it :)


Install as Plugin
=====================

If you wish to install PyDev as a plugin in an existing Eclipse installation, make
sure you meet the requirements below and follow the **Quick Install** from the right-bar
(or follow the step-by-step from `Getting Started Guide > Install <manual_101_install.html>`_).

Requirements
-----------------

- `Java <http://www.javasoft.com/>`_ 8: **Important**: If you don't have java 8, the update process may appear to succeed,
  but PyDev will simply not show in the target installation. See `Getting Started Guide > Install <manual_101_install.html>`_ for troubleshooting.

At least one of:

-  `Python <http://www.python.org/>`_ **(2.6 or newer)**
-  `Jython <http://www.jython.org/>`_ **(2.6 or newer)**
-  `IronPython <http://www.codeplex.com/Wiki/View.aspx?ProjectName=IronPython>`_ **(2.6 or newer)** - excluding 2.7.6 and 2.7.7 have a `bug <https://github.com/IronLanguages/main/issues/1663>`_ which makes them unusable in PyDev.

and

-  `Eclipse 4.6 (Neon) onwards <http://www.eclipse.org/>`_

**Note** if using Eclipse standalone: `Python <http://www.python.org/>`_
and
`IronPython <http://www.codeplex.com/Wiki/View.aspx?ProjectName=IronPython>`_
require only the `Platform Runtime
Binary <http://download.eclipse.org/eclipse/downloads/>`_ (get from http://download.eclipse.org/eclipse/downloads - download
around 45-50 MB), and `Jython <http://www.jython.org/>`_ also requires
`JDT <http://www.eclipse.org/jdt/>`_.


Requirements for Profiling
---------------------------

To profile your programs, `PyVmMonitor <http://www.pyvmmonitor.com/>`_ is required and integrated through the
profile view inside PyDev (window > show view > other > PyDev > profile).

Troubleshooting install problems
--------------------------------------

To troubleshoot install problems, please visit the `Getting Started Guide > Install <manual_101_install.html>`_ page.

Need to use older Eclipse/Java/Python
--------------------------------------

If you need to use an older version of Eclipse/Java/Python, below is the latest PyDev version to be used based on your requisites.

- Eclipse 4.5, Java 8: PyDev 5.2.0
- Eclipse 3.8, Java 7: PyDev 4.5.5
- Eclipse 3.x, Java 6: PyDev 2.8.2
- Python 2.5 or older: PyDev 5.5.0


URLs for PyDev as Eclipse plugin
----------------------------------

Urls to use when updating with the Eclipse update manager:

Latest version:

-  `http://www.pydev.org/updates <http://www.pydev.org/updates>`_

Nightly builds:

-  `http://www.pydev.org/nightly <http://www.pydev.org/nightly>`_

Browse other versions **(open in browser to select URL for Eclipse)**:

-  `http://www.pydev.org/update_sites <http://www.pydev.org/update_sites>`_

Get zip releases
------------------

-  `SourceForge
   download <http://sourceforge.net/projects/pydev/files/>`_

