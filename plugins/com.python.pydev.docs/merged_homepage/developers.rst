Developers Guide
====================

.. contents::

This page shows how to effectively get up and running with the pydev code. 


Getting the code
-----------------

The first thing you probably want to do in order to code in pydev is **getting its code**. 

**Pre-requisites:** Eclipse SDK 3.6.0, Git and Java 5.0 (note that other versions of those should work too but details may differ a bit)

Before getting the code, there's an important step you need to make: Change your java 'compiler compliance-level' to 5.0.
To do this, go to **window > preferences > Java > compiler** and change that setting from **1.4 to 5.0**.

Repository
~~~~~~~~~~~~

.. _https://github.com/aptana/Pydev: https://github.com/aptana/Pydev

Get the code with Git from https://github.com/aptana/Pydev (or you may browse it to get a zip with the contents)

And later go to: **File > Import > Existing projects into workspace** and point it to the root of the repository you just downloaded.


Configuring the environment after getting the code
---------------------------------------------------



After you do that, you'll probably note that the tests did **not compile successfully**. This is because there are some settings
that will depend on your installation, and this settings are stored in a class that holds that info (which you'll have to create).
 
There is a 'template' for that file at: 

**org.python.pydev.core/tests/org.python.pydev.core/TestDependent.OS.template**. You should create a copy of that file in that same dir named 
**TestDependent.java** and set the needed things according to your installation. If there is still something that does
not compile, it may mean that: 

 * There is something missing in that TestDependent.java file because it is not synched with all the dependencies (if so, just add the needed variable to it)
 * The head does not compile in the svn -- if you believe that is the case, send an e-mail to the pydev-code list at sourceforge to know what's happening.



.. _`jython scripting in pydev`: manual_articles_scripting.html
.. _`Pydev Grammar`: developers_grammar.html
.. _`Eclipse FAQ`: http://wiki.eclipse.org/index.php/Eclipse_FAQs
.. _`pydev-code list`: http://lists.sourceforge.net/lists/listinfo/pydev-code

Where to start?
-----------------

Ok, this may be the most difficult thing... especially because answers may change a lot depending on what you want to do, so, below are 
outlined 2 different approaches: 


 * Extending Pydev **with Jython**: recommended if you want to add some editor-related action or something that does not need implementing some Eclipse extension-point.
 * Extending Pydev **in Java**: if you want something that won't map to an action, this might be the better way to go.

To start in any of those approaches it might be worth taking a look at some Eclipse documentation, to try to grasp some of its concepts. One of
the finest documentations for that is the `Eclipse FAQ`_.


If you want to take the Jython approach, check out this article on how to do
`jython scripting in pydev`_

For supporting a new Python based language, the first step would be creating a grammar that can parse it while providing a Python like AST.
See: `Pydev Grammar`_ for instructions on that. 

And that's it. If you have further doubts about how to code in pydev, direct your questions to 
the `pydev-code list`_ at sourceforge.


Creating a distribution locally
--------------------------------

Provided that the steps were followed, Pydev should have the following structure:

 ::

	/builders
	        /org.python.pydev.build
	        
	/features
	        /org.python.pydev.feature
	        
	/plugins
	        /org.python.pydev
	        ... (other plugins)


Now, on to the build: start a shell and make sure ant is in your **PATH** and the **JAVA_HOME** 
is properly set.

In windows (update paths accordingly): 


**set PATH=%PATH%;W:\\eclipse_350_clean\\plugins\\org.apache.ant_1.7.0.v200803061910\\bin**

**set JAVA_HOME=D:\\bin\\jdk_1_5_09**


For the other instructions, we'll supose that pydev was downloaded to c:/pydev and the structure we have is:
 
	c:/pydev/builders
	
	c:/pydev/features
	
	c:/pydev/plugins

Go to the folder:

	c:/pydev/builders/org.python.pydev.build

And type the following command (customizing the variables as explained below)

**ant -DbuildDirectory=c:/pydev -Dbaseos=win32 -Dbasews=win32 -Dbasearch=x86 -Ddeploy.dir=c:/pydev/pydev_deploy -DcleanAfter=false -Dvanilla.eclipse=W:/eclipse_350_clean**


	**-DbuildDirectory=c:/pydev**       
		
		The folder that has /builders, /features and /plugins
	
	**-Dbaseos=win32**        
		
		The platform (e.g.: linux, macosx, solaris, etc) 
	
	**-Dbasews=win32**        
	
		The windows system (e.g.: gtk, motif, carbon)
	
	**-Dbasearch=x86**        
		
		The architechure (e.g.: ppc, sparc)
	
	**-Ddeploy.dir=c:/pydev/pydev_deploy**        
		
		Directory where the update site and zips will be added.
	
	**-DcleanAfter=false**        
	
		Whether it should clean things up after doing the build (reverts the sources to the svn version and deletes eclipse)
	
	**-Dvanilla.eclipse=W:/eclipse_350_clean**      
	
		A location of a clean eclipse folder to be copied to do the build

And that's it, if everything went OK, you should have created an update site at the deploy dir specified (and the zip distribution
should be there too).





Contributing back
---------------------

If you do some change at pydev that you want to contribute back to the main trunk, you should create a patch and attach it to a bug
in the sourceforge tracker with the title: **[PATCH] description of the patch**

**NOTE 1**: Diffs do not work well for binary files, so, if you made some change to some image, please attach the changed
image itself, pointing the complete location to the image when attaching it to the bug.

**NOTE 2**: If you did some whole new script that uses the 'jython scripting in pydev' infrastructure, you do not need 
to create a patch. Just attach the script itself to the bug.





