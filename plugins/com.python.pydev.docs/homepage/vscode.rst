..
    <image_area></image_area>


    <quote_area></quote_area>


PyDev on Visual Studio Code
=============================

Although PyDev is a **Python IDE** commonly used along with **Eclipse**, it's now possible to
leverage the features from **PyDev** on **Visual Studio Code**.

While there are some important features to be added (such as the **debugger**), the current version can
already leverage many features that make **PyDev** unique inside of Visual Studio Code! See below which features are
available and details on getting it running.

PyDev on Visual Studio Code (0.0.1)
--------------------------------------------

* Bundled with **PyDev 6.2.0**

* **Code-completion**
    * Fast
    * Context sensitive
    * Common tokens
    * Context insensitive with auto import

* **Code analysis**
    * Real time

* **Go to definition**

* **Code formatter**
    * Fast
    * Works with line ranges

* **Symbols for Workspace**

* **Symbols for open editor**

Planned features (soon)
-------------------------

* Launching

* PyDev Debugger integration

* Find references

* Hover

Getting it running
---------------------

The main requisite to run **PyDev** in **Visual Studio Code** is a **Java 8** installation. If you have
it installed, just installing the extension from the **Visual Studio Code Marketplace** may be all that's
needed, but if it doesn't work, you may customize the setting:

**python.pydev.java.home**

locally to point to the proper **Java Home** in your **Visual Studio Code** installation (note that the **Java Home**
is the directory which contains the **/bin/java** executable).

**Note**: you have to restart **Visual Studio Code** after changing this setting.

Customizations
----------------

Right now, it is possible to change the Python executable to be a different executable
(by default, the **python** in the **PATH** will be used). So, if you
want to use a Python installation which is not the default in the PATH, you can customize the setting:

**python.pydev.pythonExecutable**

to point to a different Python executable.

PYTHONPATH customization
-------------------------

By default, **PyDev** on **Visual Studio Code** will provide code-completion, code-analysis, etc. all based on indexing
info from the folders which are currently in the **PYTHONPATH**, but if none of the folders in the
**PYTHONPATH** are available as root folders inside Visual Studio Code, it will also consider each root folder
from **Visual Studio Code** to be a folder in the **PYTHONPATH** too.

To see information on the current interpreter configured, the command:

**PyDev: Show Python Interpreter Configuration**

may be executed from inside **Visual Studio Code**.

