===================
Interactive Console
===================

* `Highlights`_
* `Starting the Interactive Console`_
* `Choosing the Console Type`_
* `Command Line Editing and History Navigation`_
* `Full Debug Support in Interactive Console`_
* `User Module Deleter`_
* `GUI Event Loop Integration`_
* `Always Getting Better`_

Highlights
==========

The interactive console provides a perfect complement to the PyDev environment. It allows interactive editing, running and
debugging of your Python project. Some of the highlights are:

* `Code Completion`_
* `Full Debug Support in Interactive Console`_
* `Hyperlinks are linked in tracebacks`_

.. _`Hyperlinks are linked in tracebacks`: `And More!`_

Starting the Interactive Console
--------------------------------

To use it, do **Ctrl+Alt+Enter** (while in the PyDev editor) to:

* Open a console if there's no open console
* Make an runfile of the current editor in the console (if no text is selected), so that its symbols are available for further experimentation.

and **F2** to:

* Send the current line(s) to the console (fixing indentation and moving to next line).

Alternatively, it can be initialized from the console view from the dropdown for a new console
(rightmost corner icon in the console view)

.. figure:: images/console/start_console.png
    :class: snap

Choosing the Console Type
-------------------------

Choose the console type - this will determine the PYTHONPATH and the interpreter that will be used in the console.

.. figure:: images/console/choose_console.png
    :class: snap

* Console for currently active editor
   only the PYTHONPATH for the current editor (retrieved from the related project).

   If a PyDev editor is not active this option will not be available. In it's place will be
   an error message about why the option is not available.

* Python console
   PYTHONPATH containing all the paths from Python (for all the projects in the workspace).

   If no Python interpreters are configured this option will not be available. In it's place
   will be an error message about why the option is not available.

* Jython console
   PYTHONPATH containing all the paths from Jython (for all the projects in the workspace).

   If no Jython interpreters are configured this option will not be available. In it's place
   will be an error message about why the option is not available.

* IronPython console
   PYTHONPATH containing all the paths from IronPython (for all the projects in the workspace).

   If no IronPython interpreters are configured this option will not be available. In it's place
   will be an error message about why the option is not available.

* Jython using VM running Eclipse console
   Creates a Jython console using the running Eclipse environment (can potentially halt Eclipse depending on what's done).

* PyDev Debug Console
   Creates a Python debug console associated with the frame selected in the debug view.

   If no PyDev frames are selected in the Debug view this option will not be available. In it's place
   will be an error message about why the option is not available.

   It is also possible to open the PyDev Debug Console by right-clicking on a frame in the Debug View
   and choosing "Debug Console" from the "PyDev" menu.


Command Line Editing and History Navigation
-------------------------------------------

PyDev features a rich set of command completion, editing and history management.

* `Code Completion`_
* `Code Editing`_
* `Documentation Hovers`_
* `History`_
* `And More!`_


Code Completion
...............

Code completion can be activated with Ctrl+Space (or the default keybinding in the target installation). All the features available for code completion in the editor are also available in the console (and they're controlled from the same place).

.. figure:: images/console/code_completion.png
    :class: snap

Ctrl+1 can be used to make an assign to a variable

.. figure:: images/console/ctrl_1.png
    :class: snap

Code Editing
............

* **Esc**: clears current line
* Paste added directly to the command line
* Cut will only cut from the command line
* Copy does not get the prompt chars
* Home goes to: first text char / prompt end / line start (and cycles again)
* Cursor automatically moved to command line on key events

Documentation Hovers
....................

Hovering over some element in the console shows docstrings (or other suitable description if not available)

.. figure:: images/console/hover.png
    :class: snap

History
.......

**Up / Down Arrows** cycles through the history (and uses the current text to match for the start of the history command)

Page up shows the history (multiple lines may be selected to be re-executed and the field can be used to filter suitable lines - with wildcards)

.. figure:: images/console/page_up.png
    :class: snap


And More!
.........

Hyperlinks are linked in tracebacks

.. figure:: images/console/hyperlink.png
    :class: snap

The color, initial commands and vm args for jython can be configured in window > preferences > PyDev > interactive console

.. figure:: images/console/prefs.png
    :class: snap


Multiple views of the same console can be created


Full Debug Support in Interactive Console
=========================================

Starting with release 3.0 of PyDev, the interactive console in PyDev can be connected to the full debug infrastructure provided by PyDev. In addition to the new features available since PyDev 2.5.0 of connecting Variables and Expressions view, now breakpoints, single-stepping, etc is all available within the Console.

To enable that feature, go to window > preferences > PyDev > Interactive Console and check 'Connect console to Debug Session?'.

.. figure:: images/interactiveconsole/interactive_console_variables_view_preference.png
    :class: snap

With that setting in place, when a new interactive console is created and the debug perspective is shown, it's possible to see the variables available in the console through the variables view and even add expressions to be resolved in the expressions view. In addition, breakpoints set in code are hit and code can be stepped through.

Running a Python File with a Breakpoint
---------------------------------------

When a Python file is run (any method, demonstrated here with runfile) any breakpoints will suspend execution.

.. figure:: images/interactiveconsole/breakpoint_on_runfile.png
    :class: snap

Running Code with a Breakpoint
------------------------------

If any code is run that has a breakpoint, as for example shown here by having defined a function called "my_sample_function" earlier, code will suspend execution as expected.

.. figure:: images/interactiveconsole/breakpoint_on_running_function.png
    :class: snap


Examining Variables with an Active Prompt
-----------------------------------------

When code is being run that is typed by the user in the prompt, the "Interactive Console" thread shows as executing, when the prompt is available, the contents of the namespace of the interactive console is available in the Variables and Expressions view.

.. figure:: images/interactiveconsole/view_when_in_console.png
    :class: snap


User Module Deleter
===================

The user module deleter, a feature activated by using "runfile" to run scripts instead of "execfile",
forces all user modules imported modules to be forcefully removed so that a subsequent import of the
deleted module is loaded from disk again. **Ctrl+Alt+Enter** uses runfile and therefore the UMD if it
is enabled in the preferences.

To access the UMD preference, go to window > preferences > PyDev > Interactive Console > User Module Deleter (UMD).

.. figure:: images/console/umd_prefs.png
    :class: snap

The first time runfile is called it collects a list of all the modules already loaded and does not attempt
to reload those modules. In addition, built-in modules, C modules and modules listed in the Excluded Modules
preference list will not be reloaded.

The UMD achieves the reload by deleting the module from sys.modules so that the subsequent import reloads it
fully. It is important to remember that any references to items inside an imported module will not be updated.

Example of User Module Deleter in Action
----------------------------------------

In step 1 we have a module called myothermodule with a function that returns 1. The program runme.py is being
executed showing that myfunc does indeed return 1.

.. figure:: images/console/umd_step1.png
    :class: snap

In step 2 we have updated myfunc to return 2, but we have used execfile to execute runme.py. This shows
that myfunc has indeed returned 1.

.. figure:: images/console/umd_step2.png
    :class: snap

In step 3 we have use runfile to execute runme.py, the UMD shows that myothermodule has been deleted and
we can see that myfunc now returns 2.

.. figure:: images/console/umd_step3.png
    :class: snap

In this example, at some point prior to running step 1, we did a runfile to preload the list of modules
that should not be deleted.

GUI Event Loop Integration
==========================

Optionally, when running the interactive console, the interactive console can run the GUI event loop
while idle. This allows the launching of interactive GUIs while using the console and is of particular
benefit when using matplotlib, mayavi or similar.

The Event loop that is enabled can be selected in window > preferences > PyDev > Interactive Console and
selecting the desired item in "Enable GUI event loop integration?".

In addition, the GUI can be set if using IPython with the %gui IPython magic function. e.g. "%gui wx"
enables wxPython integration.

.. figure:: images/console/gui_prefs.png
    :class: snap


For example, with the wxPython event loop on we can do the following with Mayavi. In this example, the
console stays fully interactive and while it is idle, the wxPython events are handled, making the Mayavi
scene interactive too. (Example function from Mayavi documentation)

.. figure:: images/console/gui_mayavi.png
    :class: snap


IPython Integration
===================

When available, IPython is used as the backend for the Interactive Console (as opposed to Python's InteractiveConsole_).
When in use, all of the wonderful features of IPython are available within PyDev. For example, **%edit** will open up
the file in the fully featured PyDev editor, and %gui provides command line access to `GUI Event Loop Integration`_.

.. _InteractiveConsole: http://docs.python.org/2/library/code.html#code.InteractiveConsole

**Note**: From PyDev 2.2.2 onwards, if IPython is found in the PYTHONPATH, PyDev will use it as the backend for the console.
Supported versions are releases 0.10 to 1.1.0, however it is expected that PyDev should work with any newer releases too, please
file a bug in the tracker if any issues are encountered.

Always Getting Better
=====================

The Interactive Console is one of the areas benefiting from new contributions so in upcoming releases expect many
more features and improvements.

For example:

- Allow stdout and stderr to be displayed from the console asynchronously. This will resolve the current limitiation
  that at the moment stdout and stderr is only "collected" from the Python process synchronously with displaying the prompt.
