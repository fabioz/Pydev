`PyLint <https://pylint.org/>`_ can be used with PyDev
==========================================================================

Note that `PyLint <https://pylint.org/>`_ is **disabled
by default**.

To activate it, go to the PyLint preferences page, **specify its location** (ideally the executable
from the python/scripts directory, but it can also be the lint.py) and then check **Use PyLint**.

  .. figure:: images/pylint/pylint_prefs.png
     :align: center

The integration is done so that when the PyDev code analysis is passed on a saved file, PyLint will
be run on that file (so, the PyDev code analysis must also be turned on for the PyLint analysis to be done).

Files analyzed
================================================

Note that the PyDev code analysis will only be run in the currently opened editor
(right after opening it or when the file is saved) and only if the file is
actually in the PYTHONPATH (i.e.: it needs to be under a source folder and
have __init__.py files in each package to access it).

Forcing analysis without open editor
================================================

It's possible to force running code analysis on files which are not opened in an editor
by right-clicking a folder and choosing the menu **PyDev > Code Analysis**.

  .. figure:: images/pylint/ask_code_analysis.png
     :align: center

The problems flagged by PyLint can then be seen at the Problems view (it can be accessed
by using **Ctrl+3** and then typing **Problems**).


Per-project settings
================================================

It's possible to specify PyLint settings per project by creating a .pylintrc file in the project
root and passing --rcfile=.pylintrc, in the preferences, as it's always run relative to the project directory.





