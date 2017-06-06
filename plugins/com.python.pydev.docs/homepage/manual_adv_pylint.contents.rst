`PyLint <https://pylint.org/>`_ can be used with PyDev
==========================================================================

Note that `PyLint <https://pylint.org/>`_ is **disabled
by default**.

To activate it, go to the PyLint preferences page, **specify its location** (the executable
from the python/Scripts directory) and then check **Use PyLint**.

  .. figure:: images/pylint/pylint_prefs.png
     :align: center

The integration is done so that when the PyDev code analysis is passed on a saved file, PyLint will also
be run on that file (so, the PyDev code analysis must also be turned on for the PyLint analysis to be done).

What's the differences from the PyDev code analysis to PyLint?
================================================================

The main difference for PyDev and PyLint in the code analysis is that PyDev favors fewer and faster checks,
trying to have few false positives while PyLint favors completeness.

In practice, this means that PyLint catches more errors than PyDev but it's also considerably slower to run
and can give more false positives.

Also, PyLint doesn't currently accept running in files that are not saved in the disk (so, the PyDev code
analysis can work on the editor contents while PyLint can't -- this may be fixed
when/if something as https://github.com/PyCQA/pylint/pull/1189 is integrated in PyLint).

Files analyzed
================================================

Note that the PyDev code analysis will only be run in the currently opened editor
(right after opening it or when the file is saved) and only if the file is
actually in the PYTHONPATH (i.e.: it needs to be under a source folder, have a valid
Python name and have __init__.py files in each package to access it).

Forcing analysis without open editor
================================================

It's possible to force running code analysis on files which are not opened in an editor
by right-clicking a folder and choosing the menu **PyDev > Code Analysis**.

  .. figure:: images/pylint/ask_code_analysis.png
     :align: center

The problems flagged by PyLint can then be seen at the Problems view (it can be accessed
by using **Ctrl+3** and then typing **Problems**).


Forcing file to be reanalyzed in open editor
================================================

Usually, when the editor is opened, the analysis will run automatically, and whenever the
file is saved, it'll be rerun, but it's also possible to force running the analysis by
using:

**Ctrl+2, C**

That'll force the code to be re-analyzed (which may be useful if you're tweaking your
settings on PyLint). Note that PyLint will always work on the file on the disk (so,
if your file is changed in the editor, the results from PyLint may not match what
you're seeing in the editor).


Per-project settings
================================================

It's possible to specify PyLint settings per project by creating a **.pylintrc** file in the project
root and passing **--rcfile=.pylintrc**, in the preferences (PyLint inside PyDev is always run
relative to the project directory, so, **.pylintrc** in this case must be alongside **.project** and **.pydevproject**).


PyLint Configuration vs Severity Configuration in PyDev
========================================================

In PyDev, you can specify the severity for each of the PyLint checkers in the preferences page,
but PyLint will still make those analysis. If you want, you can disable the errors from being generated in PyLint itself.

For instance, it's possible to pass:

  **--disable=C --disable=R**

in the PyLint args to disable all the conventions and refactor messages from being generated in PyLint.

You may also disable a specific messages by using its id. For instance:

  **--disable=protected-access**

can be used to disable messages regarding accessing protected fields.


Ignoring a PyLint message
================================================

PyDev also allows users to ignore a given PyLint message in a given line by pressing **Ctrl+1** in a line with
a PyLint error and choosing the appropriate ignore option.



