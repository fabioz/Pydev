Here you will learn to **further configure your project** and add Python information to an already existing project.


The first thing to know about Pydev is that to use it to its 'full extent',
you must have your python modules beneath a **source folder** (the source
folders are the paths that are added to your pythonpath). 
You can add a new source folder in the menu: File > new > other > pydev > source folder.

**NOTE:** You may use pydev without configuring that, for quick scripts, but some features such as code-completion and code analysis may not
work at all (but you will still have syntax highlighting and the default editor actions).

.. image:: images/new_source_folder.png
   :class: snap
   :align: center

When you add a source folder to an existing project, it will 'automatically' add the Pydev information to it (in Eclipse terms, it will add 
its nature to it).


You may see which Python information your project has by going to the navigator (it must be the navigator and not the package explorer), rigth-clicking
the project you want info on and selecting 'properties':


.. image:: images/navigator_rigth_click.png
   :class: snap
   :align: center

The project properties allow you to see the **source folders** and the **external source folders** that will be added to 
your **pythonpath**. The external source folders are useful if you have some external library or compiled extension that is used
solely for one project, so that you don't have to add it to the system pythonpath, however, the information on such folders works as
the system information works, it is gathered once and then 'set in stone', so, if you will change it, it is reccommended that you 
create a project for it and make a project reference to that project.


.. image:: images/project_properties.png
   :class: snap
   :align: center

The 'force restore internal info' may be useful if you had an external library that changed and just want to update its information, or
you believe that for some reason pydev did not succed in synchronizing with the latest code-changes you did.


Also, you may change your project type as you wish. If you just added a new source folder to an existing project, you may change it from the
default configuration (python) to a jython project in this screen.

.. image:: images/project_type.png
   :class: snap
   :align: center

To reference another project, just go to the 'project references' page. Note that project references are not treated 
recursively in Pydev, so, if project A references B, which references project C, then project C should be specified
as a reference for project A and B.

.. image:: images/project_refs2.png
   :class: snap
   :align: center

