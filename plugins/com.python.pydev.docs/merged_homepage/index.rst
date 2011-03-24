What is Pydev?
=================

Pydev is a **Python IDE** for **Eclipse**, which may be used in **Python**, **Jython** and **IronPython** development.

.. _Features Matrix: manual_adv_features.html
.. _History for Pydev Extensions: history_pydev_extensions.html
.. _History for Pydev: history_pydev.html
.. _Pydev Blog: http://pydev.blogspot.com/

.. _Django Integration: manual_adv_django.html
.. _Code Completion: manual_adv_complctx.html
.. _Code completion with auto import: manual_adv_complnoctx.html
.. _Code Analysis: manual_adv_code_analysis.html
.. _Go to definition: manual_adv_gotodef.html
.. _Refactoring: manual_adv_refactoring.html
.. _Mark occurrences: manual_adv_markoccurrences.html
.. _Debugger: manual_adv_debugger.html
.. _Remote debugger: manual_adv_remote_debugger.html
.. _Tokens browser: manual_adv_open_decl_quick.html
.. _Interactive console: manual_adv_interactive_console.html
.. _Syntax highlighting: manual_adv_editor_prefs.html
.. _Unittest integration: manual_adv_pyunit.html


It comes with many goodies such as:

 * `Django integration`_
 * `Code completion`_
 * `Code completion with auto import`_
 * `Syntax highlighting`_
 * `Code analysis`_
 * `Go to definition`_
 * `Refactoring`_
 * `Mark occurrences`_
 * `Debugger`_
 * `Remote debugger`_
 * `Tokens browser`_
 * `Interactive console`_
 * `Unittest integration`_
 * **and many others**:

For more details on the provided features, check the `Features Matrix`_.


Release 2.0
==============


TDD actions on Ctrl+1

PyUnit:
    Added actions to pin a test run and restore it later in the PyUnit view.
    Errors that occur while importing modules are properly shown.
    Provided a way to override the pyunit test runner configurations at a given launch configuration.
    Nose test runner works properl when there's an error in a fixture
    
    
Added the possibility of launching a file that's not under the workspace (asks the user for the project).
Adding DJANGO_SETTINGS_MODULE environment var when making a launch (so that the test runners work accordingly).
Added filter to the outline page.
Added option to make local import when Ctrl is pressed to apply quick fix that adds import.
When doing assign to attributes, if there's a pass in the line the assign will be added, remove the pass.

Improved coverage support

Fixed issue with Py3k not having the input() method properly redefined.
New options for the interactive console: 
    - focus on creation 
    - initial send command on creation.

Added buttons to the UI to add/remove elements from the Django custom commands.
Pydev package explorer: Closed projects are not removed when the option to hide non pydev projects is on.

When adding options to ignore errors, only show an option to ignore a given error once.
If a line starts with __version__, don't add an import above it.

On hover, when there's a name defined in another module, the statement containing the name is shown.

Bugs:
------
Fixed indentation issue with the surround with assist.
Fixed integration issue with latest google app engine
Fixed issue with multiple keywords in class declaration in python 3.0.
Fixed set construct (was not properly handling an optional ending comma).
Ellipsis in Python 3.0 properly working


Editor
When there's some text selected and ' is entered, the content is converted to a string.
Handling literals with ui linking.
Creating ui link in the editor after entering (,[,{ when it is auto-closed.





Release 1.6.5
==============

 * Syntax highlighting now has options to have {}, [] and () as well as operators in different colors

 * Code generation for classes and methods:
 
     Note that this is an initial implementation of the idea, aimed as those that use a TDD (Test Driven Development) approach,
     so, one can create the test first and generate the classes/methods later on from using shortcuts or quick-fixes (which is 
     something that those using JDT -- Java Development Tools -- in Eclipse should be already familiar with). This feature 
     should be already usable on a number of situations but it's still far from being 100% complete.
 
     * Alt+Shift+S C can be used to create a class for the currently selected token
     * Alt+Shift+S M can be used to create a method for the currently selected token
     * Ctrl+1 has as a quick fix for creating a class or method

 * Debugger
     * When discovering encoding on Python 3.x, the file is opened as binary
     * Remote debugger (pydevd.settrace()) properly synchronized
     * Fixed debugger issue on interpreter shutdown on Python 2.7

 * Bug fixes:    
     * Fixed issue when doing code-completion on a line that started with some token that started with 'import'. e.g.: import_foo = a
     * Fixed import when running unittest with coverage
     * Fixed extract local (could extract to wrong location)    
     * Fixed NPE when requesting print of arguments in the context-information tooltips
     * Fixed AttributeError with pydevconsole on Python 3.x


Release 1.6.4
==============

 * Improved `Unittest integration`_:
 
     * Created a PyUnit view (with a red/green bar) which can be used to see the results of tests and relaunching them
     * The default test runner now allows parallel execution (distributing tests by module or individually)
     * The nose and py.test test runners are also supported now

 * Major Bug Fixed: existing interpreters could be corrupted when adding a new one

 * Fixed AttributeError on console startup in Python 3.0
 
 * Added theming and automatic sash orientation to the pydev code coverage view
 
 * Patch by frigo7: When creating a new remote debugger target, the terminated ones are removed
 
 * Patch by frigo7: compare editor properly showing the revision information and fixed broken shortcuts (e.g.: ctrl+z)
 
 * Read-only files no longer editable in pydev actions
 
 * Fixed issue of remaining \\r on python 3.0 on input()
 
 * The pydev parser is now properly dealing with bom (utf-8)
 
 * Assign to local: if method starts with '_', the leading '_' is not added to the local



Release 1.6.3
==============


* Improved editor preferences page when using Aptana themes

* Icons updated to work better with dark backgrounds

* Handling code-completion for keywords (e.g.: a method definition with a parameter 'call' will have a 'call=' completion on the caller)

* Showing a better tooltip for parameters

* No longer marking the Django templates editor as the default editor for css nor html (it can be restored at window > preferences > general > editors > file associations)

* **Globals Browser**
    * Improved message in globals browser to better explan its features:
        * Exact match with a whitespace in the end
        * CamelCase matching (so, entering only TC would be enough to find a class named TestCase)  
        * Dotted names may be used to filter through the packages (so, dj.ut.TC would find a TestCase class defined in the django.utils package)
    * Fix: When a space is added in the end, an exact match is done
    * Fix: No longer restoring items that don't exist anymore
    
* Bug Fixes
    * Fixed issue on dict and set comprehension code analysis
    * Syntax errors on hover in a debug session not shown
    * Block preferences page validation before save
    * Improved django wizard configuration a bit to cover cases where the user does not have django installed or tries to add 'django' as the project name
    * The example code in the pydev editor preferences is no longer editable
    * 2to3 only added in the context menu of projects with the pydev nature
    * If a debug session is terminated, no message saying that the variable can't be resolved in the hover is shown if the debug target is still selected
    * Fixed path issues in sqlite3 path in django project creation
    * Fixed issue where quotes could end up in the execfile when they should not be there
    * Fixed issue where shift right did not work properly because the indent prefixes were not properly set when the tab preference changed
    

    
What happened to Pydev Extensions?
====================================


Pydev Extensions is now merged with Pydev, and its once closed source code has become open source (on version 1.5.0). 
Thus, there is no more Pydev Extensions, only the open source Pydev, with all the capabilities of Pydev Extensions
incorporated.

Development Info
====================================

`Pydev Blog`_

Releases History:
==================

`History for Pydev`_

`History for Pydev Extensions`_

 