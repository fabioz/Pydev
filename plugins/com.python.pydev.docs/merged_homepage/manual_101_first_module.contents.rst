**Creating our first module** (now that the interpreter and the project are already configured).



To make things easier, make sure that you are in the 'PyDev perspective' -- it is 'automatically' opened when you create a PyDev project, but
just in case, you can open it by going to the menu: window > open perspective > other > PyDev, as pointed in the picture below. 
	
**IMPORTANT:** if you had an earlier version of PyDev installed, you must close the perspective and open it again, as older
versions of it may not have everything needed for this example (to do that, make the perspective active, then go to the menu: window > close perspective)


.. image:: images/open_perspective.png
   :class: snap
   :align: center


A perspective 'defines' what appears in your window and which actions are enabled... If you want to add something (even some menu), you can
go to the menu: window > customize perspective. To create our first module, we will use the default PyDev perspective, as it already has the wizard shortcuts
pre-defined in the **File > new** menu 



First, we will start creating a new package in a project named 'test' (it was created with the default 'src' folder, and all the code should be put
underneath it).



So, let the 'src' folder selected and go to the menu: **File > new > PyDev package** and fill the package name as below (the source folder should
be automatically filled)..

.. image:: images/new_package.png
   :class: snap
   :align: center


If everything goes ok, the structure below will be created (and the file /root/nested/__init__.py will be opened).


**Note:** Check to see if the 'P' icon is appearing for your items (as in the picture below) and in the top of your editor
after opening it. If it's not appearing, it may be that there's a problem with the file association, so, go to window > preferences >
general > editors > file associations and make sure that the .py files are associated with the Python Editor (note that
because of an eclipse bug, if it seems correct, you may have to remove the association and add it again)

.. image:: images/structure1.png
   :class: snap
   :align: center


Now, let's create the 'example' module. Let the folder /root/nested selected and go to the menu: **File > new > PyDev module** and fill the
module name as below (again, the other fields should be automatically filled). 

In this screen you may also select which should be the template used to create the new module (and the **Config...** link
in the dialog can take you to the place where you can add/remove/edit those -- which are the templates under the **New Module**
context.

.. image:: images/new_module.png
   :class: snap
   :align: center

The file '/root/nested/example.py' should have been created, so, to finish this example, in the empty file, press Ctrl+Space (that's the 
shortcut for the PyDev code-completion). Once you do that, the 'context-sensitive' completions should appear (as below). 

.. image:: images/codecompl1.png
   :class: snap
   :align: center

**NOTE:** If the code-completion does not work, you should check:

 * If you do not have a firewall that is blocking communication from the shell.
 * If the timeout to connect to the shell is enough for your system (in the menu: window > preferences > PyDev > code completion).


**NOTE FOR JYTHON USERS:** If you are in **jython**, the first activation might take a while. This usually happens when jython has to do the processing
of new jars. The next time you start it, if it takes the same time, it could be that your jython 'cache-dir' is not writable, so, you can
create a script with the code below to see where is the cache dir and check its permissions (or if there is no permission problem, you can try deleting the dir and
seeing if it gets correctly recreated in the next jython activation).

**Code:**

import sys

print sys.cachedir


