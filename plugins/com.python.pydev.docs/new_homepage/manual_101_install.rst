**Note: Instructions are targeted at Eclipse 3.5**, but contain notes about other versions.


.. contents::


Installing with the update site 
================================

To install Pydev and Pydev Extensions using the Eclipse Update Manager, you need to use the **Help > Install New Software...**
menu (note that in older versions, this would be the 'Find and Install' menu).

.. image:: images/install_menu.png
   :class: snap
   :align: center   

Then select 'Search for new features for install' (You will follow the same path even if
you already have an older version installed).

.. image:: images/install_select_new.png
   :class: snap
   :align: center   
   
In the next screen (below), click 'new remote site'

.. image:: images/update_sites.png
   :class: snap
   :align: center   

Set the 'Pydev Extensions' update site: http://www.fabioz.com/pydev/updates
	
	**NOTE:** if you currently have the sourceforge update site, you're advised to remove it (some
	reports pointed to some problems when having both).
	
	**NOTE:** The only difference between the sourceforge update site and this one is that the one at sourceforge only contains
	the Pydev 'Open Source' version, and this one contains both.
	
    **NOTE: To get versions before 1.4.3 use:** <A href="http://www.fabioz.com/pydev/updates_old/">http://www.fabioz.com/pydev/updates_old/</A>
    
    **NOTE: To get nightly builds use:** <A href="http://nightly.aptana.com/pydev/site.xml">http://nightly.aptana.com/pydev/site.xml</A> and <A href="http://nightly.aptana.com/pydev-pro/site.xml">http://nightly.aptana.com/pydev-pro/site.xml</A> 


.. image:: images/update_address.png
   :class: snap
   :align: center   

Click 'Finish'. You should be presented with the screen below:

.. image:: images/found_features.png
   :class: snap
   :align: center   

Select both features and click 'next'... 
	
	**NOTE:** if the features do not appear to you, you should restart Eclipse and try again (that's because
	Eclipse caches the results, and sometimes it may have the wrong version in its cache -- which is only cleared when you 
	restart Eclipse).


.. image:: images/update_license.png
   :class: snap
   :align: center   

Now, you'll have to accept the license, click 'next' and in the next screen, review it and click 'finish'. Eclipse should automatically download
the plugin contents and present you to a dialog asking if you want to restart (to which you should say **yes**).




Installing with the zip file
==============================

After downloading the zip file (from <a href="http://www.fabioz.com/pydev/zips">http://www.fabioz.com/pydev/zips</a>), you have to extract it 
yourself on top of Eclipse. If you choose to do it, just make sure the plugins folder 
is extracted on top of the Eclipse plugins folder. After that, restart it with the '-clean' flag, to
make sure Eclipse finds out about it.



Checking the installation
===========================

You can verify if it is correctly installed going to the menu **'help > about > plug in details'** and 
checking if there are at least 5 plugins with the 'plug-in Id' starting with **'com.python.pydev'** and at least other 5 starting with 
**'org.python.pydev'** (and check if they have the version you got).


Uninstalling
==============

If at any time you wish to stop using the Pydev extensions plugin (or any other Eclipse plugin), you can disable it by going to the menu 
**'help > software updates > manage configuration'**, selecting the plugin and clicking 'disable', then, you have to restart Eclipse,
go to the same place again and then click on 'remove' (note that you have a button in the menu that enables you to see the 'disabled' features.


	
Most commom problems
======================


**Corrupted install:** Eclipse sometimes is not able to correctly get the plugin, but will do no checking
	on whether it is really correct (no md5 checking), and when this happens, you'll usually get a ClassNotFoundException
	(similar to the example below).



	When that happens, **you should uninstall it and reinstall again** with the update site... 
	if that still fails, you could try to get the zip files, as it will at least give you a warning when it is corrupt.
	
	
	
	-- the chance of the files being corrupt in the server is pretty low, as that's something that's always checked 
	in a new release -- but if you're suspicious about it, please contact me, so that I can double-check it.



	Also, there have been reports with that error where the only solution that
	has been consistent has been **removing all** previous versions of pydev and then installing 
	the latest version.
	

**EXAMPLE**
Unable to create this part due to an internal error. Reason for the failure:
The editor class could not be instantiated. This usually indicates that the
editor's class name was mistyped in plugin.xml.



java.lang.ClassNotFoundException: org.python.pydev.editor.PyEdit 
at org.eclipse.osgi.framework.internal.core.BundleLoader.findClass(BundleLoader.java:405)       
at org.eclipse.osgi.framework.internal.core.BundleLoader.findClass(BundleLoader.java:350)
at org.eclipse.osgi.framework.adaptor.core.AbstractClassLoader.loadClass(AbstractClassLoader.java:78)
at java.lang.ClassLoader.loadClass(ClassLoader.java:235)       
at org.eclipse.osgi.framework.internal.core.BundleLoader.loadClass(BundleLoader.java:275)
...
	
