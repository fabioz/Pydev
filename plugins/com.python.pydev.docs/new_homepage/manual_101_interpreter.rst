.. contents::

Configure Interpreter
======================

After installing it, the first thing you must do is configure the **Python and/or Jython and/or Iron Python** interpreter. 
To configure the interpreter:


1. Go to: **window > preferences > pydev > Interpreter - (Python/Jython/Iron Python)**.
2. Choose the interpreter you have installed in your computer (such as python.exe, jython.jar or ipy.exe).

   Note that the **Auto Config** will try to find it in your PATH, but it can fail if it's not there.
   
3. Select the paths that will be in your **SYSTEM PYTHONPATH**. 

**IMPORTANT**: Select only folders that will **NOT be used as source folders for any project** of yours 
(those should be later configured as source folders in the project).


After those steps, you should have a screen as presented below:

.. image:: images/interpreter.png
   :class: snap
   :align: center   


How to check if the information was correctly gotten
----------------------------------------------------- 

	The **System libs** must contain at least the Lib and the Lib/site-packages directory.
	
	The **Forced builtin libs** must contain the modules built into the interpreter (and others whose
	analysis should be done dynamically. See: `Forced Builtins`_).


What if it is not correct?
~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
	
	The most common error is having a problem in the environment variables used from the shell that spawned Eclipse,
	in a way that for some reason when getting the variables of one interpreter, it gathers the info from another
	interpreter (thus mixing the interpreter and the actual libraries).
	
	If you're unable to find out what's going on, please ask in the users forum.
	
	
What if I add something new in my System PYTHONPATH after configuring it?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
	
	If you add something to your python installation, you need to either
	add it manually as a 'new folder' in the System PYTHONPATH (if it's still not under a folder in the PYTHONPATH)
	or **(recommended) remove your interpreter and add it again**, then, press apply.
	
	Note that if you added a library that's already under a folder in the PYTHONPATH, you have to at least go to
	the interpreter preferences and press apply so that it clears its internal caches (after the configuration
	is done, things are set in stone for Pydev) 


Libraries
----------

	The **System libs** are the libraries that will be added to the PYTHONPATH of any project that is using this interpreter.
	
	For **Python and Iron Python**, it's composed of **folders, zip files and egg files**. Note that if dlls should be added to
	the PYTHONPATH, the folders actually containing those dlls should be added, and they must have the same name to be
	imported in the code (the case is important). I.e.: if you want to import iTextDll, it **must** be called iTextDll.dll
	(note that .pyd and .so extensions are also accepted).
	
	For **Jython**, it's composed of **folders and jars**.

	
_`Forced Builtins`
-------------------



	The Forced builtin libs are the libraries that are built-in the interpreter, such as **__builtin__, sha, etc**.
	
	For **Python**, you should have around **50** entries 
	
	For **Jython** around **30** entries.
	
	For **Iron Python** more than **100** entries. All the packages built into .NET should be included here -- e.g.:
	Microsoft, Microsoft.Windows.Themes, System, System.IO, etc. 
	
	Additionally, you may add other libraries that you want to treat as 
	builtins, such as **os, wxPython, OpenGL, cStringIO, etc**. This is very important, because Pydev works 
	on the java side only with static information, but some modules don't have much information when analyzed 
	statically, so, Pydev creates a shell to get information on those. Another important
	thing is that they **must** be on your system pythonpath (otherwise, the shell will be unable to get that information). 
	
	
.. image:: images/interpreter_forced_builtins.png
   :class: snap
   :align: center   
	
Environment
------------

	The variables defined at the environment will be set as environment variables when running a script that uses the 
	given interpreter (note	that it can still be overridden in the run configuration)
	
	
Cygwin users
--------------- 
	
	Pydev currently has no support for cygwin. Currently you'll be able to configure the interpreter 
	with cygwin, but there are still other related problems (mostly on converting between windows and cygwin paths as needed).



