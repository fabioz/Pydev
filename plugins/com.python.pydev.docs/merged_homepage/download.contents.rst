..
    <right_area>
    <h3>'Quick Install':</h3>

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
        <A href="http://pydev.org/updates">http://pydev.org/updates</A>&nbsp;&nbsp;&nbsp;(for latest version)&nbsp;&nbsp;&nbsp;or
        <br>
        <br>
        <A href="http://pydev.org/nightly">http://pydev.org/nightly</A>&nbsp;&nbsp;&nbsp;(for nightly build)&nbsp;&nbsp;&nbsp
        <br>
        <br>
        and follow the Eclipse steps.<br/>
        <br/>
        <strong>Note: </strong>View <A href="http://pydev.org/update_sites">http://pydev.org/update_sites</A> to browse the update sites for other versions.
        </p>


    <br/>

    <p><strong>Zip File</strong></p>

    <p>An alternative is just getting the zip file and extracting it yourself in eclipse.</p>

    <p>For <strong>Eclipse 3.4 onwards</strong>, you can extract it in the '<strong>dropins</strong>' folder (and restart Eclipse).</p>

    <p>For <strong>Eclipse 3.2 and 3.3</strong>, you have to make sure the plugins folder
    is extracted on top of the Eclipse plugins folder and <strong>restart with '-clean'</strong>.</p>


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


Profiling
============

To profile your programs, `PyVmMonitor <http://www.pyvmmonitor.com/>`_ is required and integrated through the
profile view inside PyDev (window > show view > other > PyDev > profile).


Requirements
============

-  `Java <http://www.javasoft.com/>`_ 8: **Important**: If you don't have java 8, the update process may appear to succeed, but PyDev will simply not show in the target installation. See `PyDev does not appear after install!`_ below for details on how to fix that.

At least one of:

-  `Python <http://www.python.org/>`_ **(2.5 or newer)**
-  `Jython <http://www.jython.org/>`_ **(2.5 or newer)**
-  `IronPython <http://www.codeplex.com/Wiki/View.aspx?ProjectName=IronPython>`_
   **(2.6 or newer)**

and

-  `Eclipse (4.5 onwards) <http://www.eclipse.org/>`_

**Note** if using Eclipse standalone: `Python <http://www.python.org/>`_
and
`IronPython <http://www.codeplex.com/Wiki/View.aspx?ProjectName=IronPython>`_
require only the `Platform Runtime
Binary <http://download.eclipse.org/eclipse/downloads/>`_ (download
around 45-50 MB), and `Jython <http://www.jython.org/>`_ also requires
`JDT <http://www.eclipse.org/jdt/>`_.


PyDev does not appear after install!
======================================

Well, the main issue at this time is that PyDev requires Java 8 in order to run. So, if you don't want to support PyDev by
going the LiClipse route (which is mostly a PyDev standalone plus some goodies), you may have to go through some loops to
make sure that you're actually using Java 8 to run Eclipse/PyDev (as explained below).

Also, keep in mind that PyDev 5.x requires Eclipse 4.5 onwards (for Eclipse 3.8 use PyDev 4.x).

All OSes
---------
Make sure you download/install the latest Java 8 JRE or JDK, try restarting to see if it got it automatically.

I.e.: in **help > about > installation details > configuration** check if it's actually using the java 8 version you pointed at.

If it didn't get it automatically, follow the instructions from:

http://wiki.eclipse.org/Eclipse.ini to add the -vm argument to eclipse.ini on "Specifying the JVM" to specify the java 8 vm (this should also be used in case you just extracted java and didn't override the default system installation).

**Note on Mac OS**: You can use the command "/usr/libexec/java_home -v 1.8" to get the base path for the JVM (though you also need to append "/bin/java" to the output of said command to the -vm arg in eclipse.ini).


URLs for PyDev as Eclipse plugin
================================

Urls to use when updating with the Eclipse update manager:

Latest version:

-  `http://pydev.org/updates <http://pydev.org/updates>`_

Nightly builds:

-  `http://pydev.org/nightly <http://pydev.org/nightly>`_

Browse other versions (open in browser):

-  `http://pydev.org/update_sites <http://pydev.org/update_sites>`_

Get zip releases
================

-  `SourceForge
   download <http://sourceforge.net/projects/pydev/files/>`_

