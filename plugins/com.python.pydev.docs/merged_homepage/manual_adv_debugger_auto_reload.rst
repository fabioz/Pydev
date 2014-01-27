Debugger auto reload
======================

During a debug session, when a file is changed and saved within PyDev, the debugger will automatically 
do a reload of the related code.

This can be enabled/disabled in the preferences: **PyDev > Debug > When file is changed, automatically reload module?**

How does it work?
==================

When such a setting is turned on (default), and the editor is saved, PyDev will reload the code in-place. 

It follows a conservative approach (to avoid breaking singletons or other application related state) and works mostly on patching
methods and functions in place as well as new attributes (but it won't act upon existing nor deleted attributes).

To do that it creates a new namespace, imports the new code (with the recently done changes) and patches the code
of functions and updates modules and classes accordingly.

When it patches anything, the console will show details on what's being changed.

Also, the debugger provides hooks for clients that may want to act during or after the reload takes place.

Note that it may not work properly on a number of situations as the problem itself is undecidable on a number of situations.

The file: org.python.pydev/pysrc/pydevd_reload.py in your local Eclipse install 
(which may be found online at: https://github.com/fabioz/Pydev/blob/development/plugins/org.python.pydev/pysrc/pydevd_reload.py)
contains more details on the hooks and limitations of the current reload approach.

Is it useful?
===============

Definitely, despite its limitations, when it works, it's pretty useful, but beware of the current limitations where it's possible that it won't
get the change because it can't apply it (see the console output to check if it did reload what you expected as it should show all the changes it does).

Also note that it's not able to change the frame that's executing currently (it can change the code of a function, but it's not
able to change the code of a frame that's executing as this is currently a Python limitation), so, you may have to get
out of the method and back in to see the change (note that the **set next statement** action, which can set which is the
next line to execute can be very handy there). 