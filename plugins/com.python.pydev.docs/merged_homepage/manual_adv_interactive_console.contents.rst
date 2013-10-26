===================
Interactive Console
===================

New on PyDev 2.5.0: Interaction with Variables View
===================================================

From PyDev 2.5.0 onwards, the interactive console may be connected to the variables/expressions view (as if it was a debug process... but without breakpoints).

To enable that feature, go to window > preferences > PyDev > Interactive Console and check 'Connect console to Variables Debug View?'.

.. figure:: images/interactiveconsole/interactive_console_variables_view_preference.png

With that setting in place, when a new interactive console is created and the debug perspective is shown, it's possible to see the variables available in the console through the variables view and even add expressions to be resolved in the expressions view.

.. figure:: images/interactiveconsole/interactive_console_variables_view.png


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

Choose the console type - this will determine the PYTHONPATH and the interpreter that will be used in the console:


* only the PYTHONPATH for the current editor (gotten from the related project)
* PYTHONPATH containing all the paths from Python (for all the projects in the workspace)
* PYTHONPATH containing all the paths from Jython (for all the projects in the workspace)




.. figure:: images/console/choose_console.png

Code completion can be activated with Ctrl+Space (or the default keybinding in the target installation). All the features available for code completion in the editor are also available in the console (and they're controlled from the same place).

.. figure:: images/console/code_completion.png

Ctrl+1 can be used to make an assign to a variable

.. figure:: images/console/ctrl_1.png

Hovering over some element in the console shows docstrings (or other suitable description if not available)

.. figure:: images/console/hover.png

Page up shows the history (multiple lines may be selected to be re-executed and the field can be used to filter suitable lines - with wildcards)

.. figure:: images/console/page_up.png

Hyperlinks are linked in tracebacks

.. figure:: images/console/hyperlink.png

The color, initial commands and vm args for jython can be configured in window > preferences > PyDev > interactive console

.. figure:: images/console/prefs.png

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
