Launching/Debugging Python scripts
===================================

.. contents::

Regular Launch
----------------

The easiest way of launching a python file from PyDev is opening an editor and using the **F9 keybinding**. 
Through that command, PyDev will create/reuse a **launch** config to run the current editor based on the **current settings 
of the project** (i.e.: if the project is configured as IronPython, it'll use an IronPython interpreter).

.. image:: images/debugger/f9.png
   :class: snap
   
Another option would be running using the **context menu**, where you can choose how you want to make 
the run (in that way, you could run a python project with a jython interpreter)

.. image:: images/debugger/run_as_regular.png
   :class: snap









Unit Test Launch
-----------------

If you use **unit-tests**, and want to run only a single unit-test or a few unit-tests of a module, 
you can use the **Ctrl+F9 keybinding**, which will open a tree where you can choose which test(s) you want to run:



.. image:: images/debugger/ctrl_f9.png
   :class: snap


In that dialog, there are some options:


* Extending the filter and pressing 'Enter' will run the filtered tests
* Explicitly selecting the class/tests to run will run those tests





Debug Launch
--------------

To run in debug mode, you can use the **context menu**, where you can choose how you want to make 
the debug (note that in that way, you could debug a python project with a jython interpreter)

.. image:: images/debugger/run_as_debug.png
   :class: snap


Another option would be running the last launch in debug mode. See: `Rerun Last Launch (regular or debug mode)`_




Rerun Last Launch (regular or debug mode)
---------------------------------------------


If you use the F9 and Ctrl+F9 keybindings or launch through the context menu, 
usually you'll also want to check the default eclipse preferences so that 
**Ctrl+F11** and **F11**
will run your last configuration (otherwise, they'll bring a dialog to run/debug your current editor, which is mostly what's
already given by PyDev through F9 and Ctrl+F9).



To do that, open the eclipse preferences (**window > preferences**) and check 
**"Always launch previously launched application"**, that way, when you use **F11**, your last launch
will be repeated in **debug mode** and with **Ctrl+F11**, it'll be relaunched in the 
**regular mode**.


.. image:: images/debugger/launching_dialog.png
   :class: snap

Another option would be running the last launch from the menu. You can go to the menu with keybindings 
(**Alt+R, then 'T' regular run or 'H' for debug run**) and choose some existing launch to be run.

.. image:: images/debugger/run_history.png
   :class: snap





Debugging
------------

Currently the debugger supports:

* Step in: **F5**
* Step over: **F6**
* Step return: **F7**
* Continue: **F8**
* Breakpoints (with optional condition)
* Stack view showing multiple threads
* Locals and Globals variable display
* Expressions display
* Temporary display for selection: **Ctrl+Shift+D**
* Hover showing the selected expression evaluation



To add breakpoints, you can double click the left bar or use Ctrl+F10 > Add breakpoint. In a line with
an existing breakpoint Ctrl+F10 will be able to remove the breakpoint, disable it and edit its properties (which
can be used to provide conditions for the breakpoint to be hit). Double-clicking an existing breakpoint will remove it.



When you hit a breakpoint, you'll get a view that allows you to inspect the stack, see locals and globals, hover over
variables (or select a text to be evaluated) and add expressions.

.. image:: images/debugger/debug_perspective.png
   :class: snap


Note that the program output is displayed in the console, and the errors in the console are hyperlinked back to the file:

.. image:: images/debugger/hyperlink.png
   :class: snap



