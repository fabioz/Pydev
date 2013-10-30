===================
Interactive Console
===================

* `New on PyDev 3.0.0: Full Debug Support in Interactive Console`_
* `Using the Interactive Console`_


New on PyDev 3.0.0: Full Debug Support in Interactive Console
=============================================================

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


Using the Interactive Console
===================================================

**Note**: From PyDev 2.2.2 onwards, if IPython (0.10 or 0.11) is found in the PYTHONPATH,
PyDev will use it as the backend for the console.


To use it, do **Ctrl+Alt+Enter** (while in the PyDev editor) to:


* Open a console if there's no open console
* Send the selected text to the console
* Make an execfile of the current editor in the console (if no text is selected), so that its symbols are available for further experimentation.



Alternatively, it can be initialized from the console view from the dropdown for a new console
(rightmost corner icon in the console view)

.. figure:: images/console/start_console.png
    :class: snap

Choose the console type - this will determine the PYTHONPATH and the interpreter that will be used in the console:


* only the PYTHONPATH for the current editor (gotten from the related project)
* PYTHONPATH containing all the paths from Python (for all the projects in the workspace)
* PYTHONPATH containing all the paths from Jython (for all the projects in the workspace)




.. figure:: images/console/choose_console.png
    :class: snap

Code completion can be activated with Ctrl+Space (or the default keybinding in the target installation). All the features available for code completion in the editor are also available in the console (and they're controlled from the same place).

.. figure:: images/console/code_completion.png
    :class: snap

Ctrl+1 can be used to make an assign to a variable

.. figure:: images/console/ctrl_1.png
    :class: snap

Hovering over some element in the console shows docstrings (or other suitable description if not available)

.. figure:: images/console/hover.png
    :class: snap

Page up shows the history (multiple lines may be selected to be re-executed and the field can be used to filter suitable lines - with wildcards)

.. figure:: images/console/page_up.png
    :class: snap

Hyperlinks are linked in tracebacks

.. figure:: images/console/hyperlink.png
    :class: snap

The color, initial commands and vm args for jython can be configured in window > preferences > PyDev > interactive console

.. figure:: images/console/prefs.png
    :class: snap

Other actions/features available:


* **Up / Down Arrows** cycles through the history (and uses the current text to match for the start of the history command)
* **Esc**: clears current line
* Paste added directly to the command line
* Cut will only cut from the command line
* Copy does not get the prompt chars
* Home goes to: first text char / prompt end / line start (and cycles again)
* Cursor automatically moved to command line on key events
* Multiple views of the same console can be created





* Limitation: Output is not asynchonous (stdout and stderr are only shown after a new command is sent to the console)
