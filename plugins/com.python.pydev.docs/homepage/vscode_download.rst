..
    <image_area>download.png</image_area>
    <quote_area><strong>Getting it up and running in your computer...</strong></quote_area>


To install PyDev on VSCode, search for PyDev in the extensions marketplace and install it.

.. image:: ../images/vscode/install.png
   :class: snap

Getting it running
---------------------

The main requisite to run **PyDev** in **Visual Studio Code** is a **Java 8** installation. If you have
it installed, just installing the extension from the **Visual Studio Code Marketplace** may be all that's
needed, but if it doesn't work, you may customize the setting:

**python.pydev.java.home**

locally to point to the proper **Java Home** in your **Visual Studio Code** installation (note that the **Java Home**
is the directory which contains the **/bin/java** executable).

**Note**: you have to restart **Visual Studio Code** after changing this setting.
