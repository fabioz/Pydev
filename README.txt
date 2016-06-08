Licensed under the terms of the Eclipse Public License (EPL).

Please see the license.txt included with this distribution for details.

PyDev is a Python plugin for Eclipse. See http://pydev.org for more details.

To get started in PyDev, read: http://pydev.org/manual_101_root.html

For developing PyDev, read: http://pydev.org/developers.html

ISIS - Notes for getting the console built and visible

* Clone the repo
* Import the top folder into Eclipse (as you would for IBEX)
* Confirm running Java 1.7 in Project->Properties->Java Compiler
* In eclipse open the plugin.xml in com.python.pydev
* Click Launch as Eclipse Application
* Eclipse will now be opened go to Window->Perspective->Open Perspective->Other... and select PyDev from the pop-up list
* Go to Window->Preferences->PyDev->Intepreters->Python Interpreter and click Quick Auto-Config then OK
* After the python has been set up close the next window
* Go to Window->Show View->Console
* In the console window click the icon in the top right with the drop down arrow
* Select pydev console from the drop down
* Confirm the python interpreter has been selected, click OK
* You have a pyDev console (at last!!) Next time you run the plugin you will only have to perform the last few steps

