`PyLint <http://www.logilab.org/projects/pylint>`_ can be used with PyDev
==========================================================================

NOTE: `PyLint <http://www.logilab.org/projects/pylint>`_ is **disabled
by default**, so, if you want to activate it, you should go to the
pylint preferences page, **specify its location** (it can be either the lint.py or the
executable from your pyton/scripts directory -- if it's the lint.py, it MUST be installed
in the site-packages) and activate it (note: after activating it, you
can clean your project on the project menu so that the files are checked
with pylint, or you can do it on deltas as you go and change your
files).

.. figure:: images/pylint/pylint_prefs.png
   :align: center

The integration is done so that it is integrated with the eclipse builder. That means that whenever you change a file it
automatically passes pylint (if autobuild is on,  check the menu Project > Build Automatically. If it is not, you can request a build when you
want with Ctrl+B, so that the deltas are analyzed).

This, however, has a drawback: PyLint can be slow at sometimes, and if
you work in big projects it can be kind of slow (anyway, you can stop
the builder process at any time if you want) - you should expect that it
takes some secs. for each file it analyzes, so, if you are working with
lots of files it can take quite some time...

For this cases, there's an option that specifies the maximum delta to use
PyLint on. So, if you have all of the sudden 100 changed files because
of a repository update, PyLint will not be run unless the limit specified
allows it.

Per-project settings
================================================

It's possible to specify PyLint settings per project by creating a .pylintrc file in the project
root and passing --rcfile=.pylintrc, in the preferences,  
as it's run relative to the project directory (from PyDev 2.8.0 onwards)



Issues on problems view
========================

If you don't see the problems on your problems view, don't forget to
enable it in the problems view filter.

.. figure:: images/pylint/pylint.png
   :align: center


