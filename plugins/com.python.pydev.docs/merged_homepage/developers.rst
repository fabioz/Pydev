Developers Guide
====================

.. contents::

This page shows how to effectively get up and running with the PyDev code. 


Getting the code
-----------------

The first thing you probably want to do in order to code in PyDev is **getting its code**. 

**Pre-requisites:** Eclipse SDK 3.6.0, Git and Java 5.0 (note that other versions of those should work too but details may differ a bit)

Before getting the code, there's an important step you need to make: Change your java 'compiler compliance-level' to 5.0.
To do this, go to **window > preferences > Java > compiler** and change that setting from **1.4 to 5.0**.

Repository
~~~~~~~~~~~~

.. _https://github.com/aptana/Pydev: https://github.com/aptana/Pydev

Get the code with Git from https://github.com/aptana/Pydev (ideally, fork it at github, create your own branch at the 
forked repository -- usually based in the development branch -- and later send a pull request on github so that 
the code can be merged back). Later, if you want to provide some other feature/bugfix, a new branch should be created again.

Then, in Eclipse, go to: **File > Import > Existing projects into workspace** and point it to the root of the repository you just downloaded.

Note that currently PyDev has a project (org.python.pydev.red_core) which has a dependency on Aptana Studio 3, so, if 
you plan on doing a local build, you'll need to get Aptana Studio 3 installed as a plugin in the SDK used for the build 
(i.e.: not necessarily in the SDK you use for developing), now if you don't need to do a local build 
(i.e.: just do your local changes and run Eclipse with your changes from within the SDK and contribute that 
as a patch later on), you can just close this project so that it doesn't get compiled.


Configuring the environment after getting the code
---------------------------------------------------

Important: Before doing any changes to the code it's important to note that you should create a new branch for doing code changes.
See: http://book.git-scm.com/3_basic_branching_and_merging.html and also http://nvie.com/git-model for details on creating and using branches.

After getting the code, you'll probably note that the tests did **not compile successfully**. This is because there are some settings
that will depend on your installation, and this settings are stored in a class that holds that info (which you'll have to create).
 
There is a 'template' for that file at: 

**org.python.pydev.core/tests/org.python.pydev.core/TestDependent.OS.template**. You should create a copy of that file in that same dir named 
**TestDependent.java** and set the needed things according to your installation. If there is still something that does
not compile, it may mean that: 

 * There is something missing in that TestDependent.java file because it is not synched with all the dependencies (if so, just add the needed variable to it)
 * The head does not compile in git -- if you believe that is the case, send an e-mail to the pydev-code list at sourceforge to know what's happening.
 

.. _`jython scripting in PyDev`: manual_articles_scripting.html
.. _`PyDev Grammar`: developers_grammar.html
.. _`Eclipse FAQ`: http://wiki.eclipse.org/index.php/Eclipse_FAQs
.. _`pydev-code list`: http://lists.sourceforge.net/lists/listinfo/pydev-code

Where to start?
-----------------

Ok, this may be the most difficult thing... especially because answers may change a lot depending on what you want to do, so, below are 
outlined 2 different approaches: 


 * Extending PyDev **with Jython**: recommended if you want to add some editor-related action or something that does not need implementing some Eclipse extension-point.
 * Extending PyDev **in Java**: if you want something that won't map to an action, this might be the better way to go.

To start in any of those approaches it might be worth taking a look at some Eclipse documentation, to try to grasp some of its concepts. One of
the finest documentations for that is the `Eclipse FAQ`_.


If you want to take the Jython approach, check out this article on how to do
`jython scripting in PyDev`_

For supporting a new Python based language, the first step would be creating a grammar that can parse it while providing a Python like AST.
See: `PyDev Grammar`_ for instructions on that. 

And that's it. If you have further doubts about how to code in PyDev, direct your questions to 
the `pydev-code list`_ at sourceforge.


Creating a distribution locally
--------------------------------

Provided that the steps were followed, PyDev should have the following structure:

 ::

	/builders
	        /org.python.pydev.build
	        
	/features
	        /org.python.pydev.feature
	        
	/plugins
	        /org.python.pydev
	        ... (other plugins)


Now, on to the build: start a shell and follow the instructions at /plugins/org.python.pydev.build/build_cmd.txt (read the end of the file for details on customizing it properly)



Contributing back
---------------------

If you do some change at PyDev that you want to contribute back to the main trunk, you should create a patch and attach it to a bug
in the sourceforge tracker with the title: **[PATCH] description of the patch**

**NOTE 1**: Diffs do not work well for binary files, so, if you made some change to some image, please attach the changed
image itself, pointing the complete location to the image when attaching it to the bug.

**NOTE 2**: If you did some whole new script that uses the 'jython scripting in PyDev' infrastructure, you do not need 
to create a patch. Just attach the script itself to the bug.





