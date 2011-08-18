Interactive Console
=======================




Console View
--------------

**Ctrl+Alt+Enter** (while in the PyDev editor) can be used to:

* Open a console if there's no open console
* Send the selected text to the console
* Make an execfile of the current editor in the console (if no text is selected), so that its symbols are available for further experimentation.

Alternatively, it can be initialized from the console view from the dropdown for a new console 
(rightmost corner icon in the console view)

.. image:: images/console/start_console.png
   :class: snap


Choose the console type -- this will determine the PYTHONPATH and the interpreter that will be used in the console: 

* only the PYTHONPATH for the current editor (gotten from the related project)
* PYTHONPATH containing all the paths from Python (for all the projects in the workspace)
* PYTHONPATH containing all the paths from Jython (for all the projects in the workspace)


.. image:: images/console/choose_console.png
   :class: snap


Code completion can be activated with Ctrl+Space (or the default keybinding in the target installation). All
the features available for code completion in the editor are also available in the console (and they're controlled from
the same place).

.. image:: images/console/code_completion.png
   :class: snap


Ctrl+1 can be used to make an assign to a variable

.. image:: images/console/ctrl_1.png
   :class: snap


Hovering over some element in the console shows docstrings (or other suitable description if not available)

.. image:: images/console/hover.png
   :class: snap


Page up shows the history (multiple lines may be selected to be re-executed and the field can be used to filter suitable lines -- with wildcards)

.. image:: images/console/page_up.png
   :class: snap



Hyperlinks are linked in tracebacks

.. image:: images/console/hyperlink.png
   :class: snap


The color, initial commands and vm args for jython can be configured in window > preferences > PyDev > interactive console

.. image:: images/console/prefs.png
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


