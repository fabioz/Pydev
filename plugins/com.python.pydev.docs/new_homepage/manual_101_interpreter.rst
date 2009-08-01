

After installing it, the first thing you must do is configure the **python and/or jython interpreter. To configure the interpreter:

<ul>
<li>1. Go to: window > preferences > pydev > Interpreter - (Jython/Python).
</li><li>2. Choose the interpreter you have installed in your computer (such as python.exe or jython.jar)
</li><li>3. Select the paths that will be in your **SYSTEM PYTHONPATH. It's important to select only
those folders that will **not be used as the root for some project of yours.
</li>
</ul>
After those steps, you should have a screen as presented below:

<p align="center"><IMG src="images/interpreter.png" border="1"/>


**How to check if the information was correctly gotten: 


	Make sure that you have lots of **'Forced builtin libs' and some **'System libs'.  
	
	The Forced builtin libs are the libraries that are built-in the interpreter, such as **__builtin__, sha, etc.
	For python, you should have about 50 libs and for jython about 30 libs. 
	
	Additionally, you may add other libraries that you want to treat as 
	builtins, such as **os, wxPython, OpenGL, cStringIO, etc. This is very important, because Pydev works on the java side only with static information, but 
	some modules don't have much information when analyzed statically, so, Pydev creates a shell to get information on those. Another important
	thing is that they **must be on your system pythonpath (otherwise, the shell will be unable to get that information). 
	
	
	**What if it is not correct? 
	
	The most commom error is having spaces in the Eclipse installation. If you do have spaces in your Eclipse installation, you must remove the interpreter, 
	close Eclipse, move it to a new folder without spaces, restart it and follow the steps above again.
	
	The information on your projects will not be lost, as this is not stored with the eclipse installation -- 
	unless you specifically pointed that location for Eclipse, in which case you must move only the 
	Eclipse files, and not the .metadata folder or any of your project folders.

	
	**What if I add something new in my System PYTHONPATH after configuring it? 
	
	Well, if you add something to your python installation (anything that goes under the site-packages), you need to either
	add it manually as a 'new folder' in the System PYTHONPATH or **(recommended) remove your interpreter, 
	press apply so that it clears its cache and re-add the python interpreter.
	
	
	**Cygwin users 
	
	Pydev currently has no full support for cygwin. Keep track of the 
	<a href="https://sourceforge.net/tracker/?atid=577329&group_id=85796&func=browse">bugs in the sourceforge tracker</a>,
	as there are bugs open to finish the cygwin support (which will mostly be converting between windows and cygwin paths
	as needed). Currently you'll already be able to configure the interpreter with cygwin, but there are still many other
	problems related to cygwin support.



