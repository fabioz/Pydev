..
    <image_area></image_area>


    <right_area2>
    <ul>
    PyDev for VSCode may be evaluated for 30 days but must be bought for continued use.<br/>
    <br/>
    See: <a href="license.html">license info</a> for details.<br/>
    <br/>

    Licenses may be purchased using Paypal and will be delivered to the e-mail specified during the checkout process.<br/>
    <br/>
    <br/>
    Promotional launch licensing price is <strong>USD 40.00</strong>.<br/>
    <br/>
    <br/>

	<strong>Buy Single-User License</strong><br/>
    <br/>

	<ul class="libutton">
	    <li class="libutton"><a class="libutton" href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&amp;hosted_button_id=N929BMNSSUJHE">
	    Click to buy using<strong class="libutton">Paypal</strong>
	    </a>
	    </li>
    </ul>

    <br/>
    <br/>
    <br/>

	<strong>Buy Multi-User License</strong><br/>
    <br/>

	<ul class="libutton">
	    <li class="libutton"><a class="libutton" href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&amp;hosted_button_id=ZHYGQQF7728EC">
	    Click to buy using<strong class="libutton">Paypal</strong>
	    </a>
	    </li>
    </ul>
    <br/>
    <br/>

	Note: your license should be delivered within a day after your purchase is completed
	(remember to check your spam folder too). If you don't find it within that timeframe,
	please contact fabiofz (at) gmail (dot) com.

    </ul>
    </right_area2>


PyDev on Visual Studio Code
=============================

Although PyDev is a **Python IDE** commonly used along with **Eclipse**, it's now possible to
leverage the features from **PyDev** on **Visual Studio Code**.

While there are some important features to be added (such as the **debugger**), the current version can
already leverage many features that make **PyDev** unique inside of Visual Studio Code!

See below which features are available and details on getting it running.

Features
=============================

* Code-completion
    * Fast
    * Context sensitive
    * Common tokens
    * Context insensitive with auto import

* Code formatter
    * Fast
    * Format ranges
    * Format on type

* Code analysis
    * Real time

* Go to definition

* Symbols for Workspace

* Symbols for open editor

* Find references

* Quick fix for undefined variables (adds missing import)

* Navigate to previous or next class or method (Ctrl+Shift+Up, Ctrl+Shift+Down)

.. _History: history.html

See: `History`_ for details on releases.

Planned features (soon)
-------------------------

* Launching

* PyDev Debugger integration

* Hover


.. _Download: download.html

For details on installing and getting it running, see: `Download`_

Customizations
----------------

Right now, it is possible to change the Python executable to be a different executable
(by default, the **python** in the **PATH** will be used). So, if you
want to use a Python installation which is not the default in the PATH, you can customize the setting:

**python.pydev.pythonExecutable**

to point to a different Python executable.

PYTHONPATH customization
-------------------------

By default, **PyDev** on **Visual Studio Code** will provide code-completion, code-analysis, etc. all based on indexing
info from the folders which are currently in the **PYTHONPATH**, but if none of the folders in the
**PYTHONPATH** are available as root folders inside Visual Studio Code, it will also consider each root folder
from **Visual Studio Code** to be a folder in the **PYTHONPATH** too.

To see information on the current interpreter configured, the command:

**PyDev: Show PyDev Configuration Information**

may be executed from inside **Visual Studio Code**.


Settings
--------------------------

.. _Manual: manual.html

For information on the settings which affect PyDev on Visual Studio Code see: `Manual`_.
