..
    <right_area>
    </right_area>
    <image_area></image_area>
    <quote_area></quote_area>

Getting the code
================

The first thing you probably want to do in order to code in PyDev is
**getting its code**.

**Pre-requisites:** Eclipse SDK 4.4.0, Git and Java 7.0 (note that other
versions of those should work too but details may differ a bit)

Before getting the code, there's an important step you need to make:
Change your java 'compiler compliance-level' to 7.0. To do this, go to
**window > preferences > Java > compiler** and change that setting from
**1.4 to 1.7**.

Repository
----------

Get the code with Git from
`https://github.com/fabioz/Pydev <https://github.com/fabioz/Pydev>`_
(ideally, fork it at github, create your own branch at the forked
repository -- usually based in the **development** branch -- and later send
a pull request on github so that the code can be merged back). Later, if
you want to provide some other feature/bugfix, a new branch should be
created again.

Then, in Eclipse, go to: **File > Import > Existing projects into
workspace** and point it to the root of the repository you just
downloaded (after importing, you may want to close the 2 mylyn-related
projects if you don't have Mylyn locally).


Configuring the environment after getting the code
==================================================

Important: Before doing any changes to the code it's important to note
that you should create a new branch (usually based on the development
branch) for doing code changes. See:
`http://book.git-scm.com/3\_basic\_branching\_and\_merging.html <http://book.git-scm.com/3_basic_branching_and_merging.html>`_
and also `http://nvie.com/git-model <http://nvie.com/git-model>`_ for
details on creating and using branches.

**Note**: for running the tests the file:
**org.python.pydev.core/tests/org.python.pydev.core/TestDependent.OS.properties**
must have the values set regarding to the computer that'll execute the
tests.

Note that to make sure that PyDev keeps working on the long run,
usually tests are required for pull requests (unless it's a really trivial change).
Those reside in the project/tests source folder.

Note that if the head does not compile in git, send an e-mail to the pydev-code
list at sourceforge to know what's happening.

Running it with your changes
==============================

After you download the contents and do changes to the code, you can do a Run As > Eclipse Application and
a new Eclipse instance will be run with the changes you did.

Where to start?
===============

Ok, this may be the most difficult thing... especially because answers
may change a lot depending on what you want to do, so, below are
outlined 2 different approaches:

-  Extending PyDev **with Jython**: recommended if you want to add some
   editor-related action or something that does not need implementing
   some Eclipse extension-point.

-  Extending PyDev **in Java**: if you want something that won't map to
   an action, this might be the better way to go.

To start in any of those approaches it might be worth taking a look at
some Eclipse documentation, to try to grasp some of its concepts. One of
the finest documentations for that is the `Eclipse
FAQ <http://wiki.eclipse.org/index.php/Eclipse_FAQs>`_.

If you want to take the Jython approach, check out this article on how
to do `jython scripting in PyDev <manual_articles_scripting.html>`_

For supporting a new Python based language, the first step would be
creating a grammar that can parse it while providing a Python like AST.
See: `PyDev Grammar <developers_grammar.html>`_ for instructions on
that.

And that's it. If you have further doubts about how to code in PyDev,
direct your questions to the `pydev-code
list <http://lists.sourceforge.net/lists/listinfo/pydev-code>`_ at
sourceforge.

Creating a distribution locally
===============================

Provided that the steps were followed, PyDev should have the following
structure:

    /builders
     /org.python.pydev.build

    /features
     /org.python.pydev.feature

    /plugins
     /org.python.pydev
     ... (other plugins)

Now, on to the build: PyDev uses maven to do the build, so, it should be a matter of 
using "mvn install". 

There's a bat file at: builders/org.python.pydev.build/build_cmd.bat
which can be used as a base to know which environment variables are needed to do a build
and /pom.xml (in the root) has more details on getting pre-requisites.

Contributing back
=================

Create a pull request in github:
`http://help.github.com/send-pull-requests/ <http://help.github.com/send-pull-requests/>`_
