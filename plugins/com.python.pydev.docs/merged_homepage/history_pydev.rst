History For PyDev
~~~~~~~~~~~~~~~~~

Release 2.2.3
===============

* Performance improvements

* Major: Fixed critical issue when dealing with zip files.

* Added option to create method whenever a field would be created in quick fixes (and vice-versa), to properly deal with functional programming styles.

* Fixed issue where PyDev was changing the image from another plugin in the Project Explorer (i.e.: removing error decorations from JSP).

* Fixed issue: if the django models was opened in PyDev, the 'objects' object was not found in the code analysis.

* Test runner no longer leaves exception visible.

* Fixed issue on Py3: Relative imports are only relative if they have a leading dot (otherwise it always goes to the absolute).

* Default is now set to create project with the projects itself as the source folder.

* Handling deletion of .class files.

* Fixed issue where loading class InterpreterInfo in AdditionalSystemInterpreterInfo.getPersistingFolder ended up raising a BundleStatusException in the initialization. 

* Fixed some code formatting issues


Release 2.2.2
===============

**IPython / Interactive console**

    .. image:: images/index/ipython_console.png
        :class: no_border

    * IPython (0.10 or 0.11) is now used as the interactive console backend if PyDev can detect it in the PYTHONPATH.
    * While waiting for the output of a command, intermediary results are printed in the console.
    * ANSI color codes are supported in the interactive console.

**Code Analysis**

    .. image:: images/index/assignment_to_builtin.png
        :class: no_border

    * Reporting variables that shadow builtins as warnings.
    * Fixed issue where __dict__ was not found.
    
**Code completion**

    * Aliases have a better treatment (i.e.: unittest.assertEqual will show the proper type/parameters).
    * Improved support for analyzing function builtins where the return type is known (i.e.: open, str.split, etc).
    
**Debugger**

    * When doing a remote debug session, if the files cannot be found in the local filesystem, PyDev will ask for files in the remote debugger.

**Editor**
    
    * Files without extension that have a python shebang (e.g.: #!/usr/bin/python in the first line) are automatically opened with the PyDev editor (in the PyDev Package Explorer).

**Django**

    * When the shell command is used in the django custom commands, PyDev no longer uses 100% cpu while it doesn't complete.

**Others** 
    
    * Fixed issue where the * operator was not properly formatted.
    * When the quick outline dialog is deactivated, it's closed.
    * Fixed heuristic for finding position for local import. 
    * Fixed compare editor issue with Eclipse 3.2.
    * Fixed integration issue with latest PyLint.
    * Fixed deadlock issue on app engine manage window.
    * More options added to configure the automatic deletion of .pyc files (delete always, never delete, delete only on .py delete).



Release 2.2.1
=============

**Quick-outline**

    .. figure:: images/index/quick_outline_parent.png
       :align: center
       :alt: images/index/quick\_outline\_parent.png

       images/index/quick\_outline\_parent.png

    -  Parent methods may be shown with a 2nd Ctrl+O.
    -  The initial node is selected with the current location in the
       file.

**Extract local refactoring**

    .. figure:: images/index/refactor_duplicate.png
       :align: center
       :alt: images/index/refactor\_duplicate.png

       images/index/refactor\_duplicate.png

    -  Option to replace duplicates.
    -  Fixed issue where wrong grammar could be used.

**Others**

    -  Improved handling of Ctrl+Shift+T so that no keybinding conflict
       takes place (now it'll be only active on the PyDev views/editor).
    -  PyLint markers always removed on a project clean.
    -  If the standard library source files are not found, more options
       are presented.
    -  If the completion popup is focused and shift is pressed on a
       context insensitive completion, a local import is done.
    -  Fixed issue where a local import wasn't being added to the
       correct location.
    -  Fixed error message in debugger when there was no caught/uncaught
       exception set in an empty workspace.
    -  Performance improvements on hierarchy view.
    -  Django commands may be deleted on dialog with backspace.

Release 2.2
===========

**Eclipse 3.7**

    -  Eclipse 3.7 (Indigo) is now supported.

**Break on Exceptions**

    .. figure:: images/index/manage_exceptions.png
       :align: center
       :alt: images/index/manage\_exceptions.png

       images/index/manage\_exceptions.png

    -  It's now possible to **break on caught exceptions** in the
       debugger.
    -  There's an UI to break on caught or uncaught exceptions (menu:
       Run > Manage Python Exception Breakpoints).

**Hierarchy view**

    .. figure:: images/index/hierarchy_view.png
       :align: center
       :alt: images/index/hierarchy\_view.png

       images/index/hierarchy\_view.png

    -  UI improved (now only uses SWT -- access through F4 with the
       cursor over a class).

**PyPy**:

    -  PyDev now supports PyPy (can be configured as a regular Python
       interpreter).

**Django**

    -  Django configuration in project properties page (improved UI for
       configuration of the django manage.py and django settings
       module).
    -  Improved support for debugging Django with autoreload. Details
       at: `Django remote debugging with
       auto-reload <manual_adv_remote_debugger.html#django-remote-debugging-with-auto-reload>`_.

**Code analysis**

    -  Fixed issue where a resolution of a token did not properly
       consider a try..except ImportError (always went for the first
       match).
    -  Fixed issue with relative import with wildcards.
    -  Fixed issue with relative import with alias.
    -  Fixed issue where binary files would be wrongly parsed (ended up
       generating errors in the error log).

**Code completion**

    -  Improved sorting of proposals (\_\_\*\_\_ come at last)

**Others**

    -  Improved ctrl+1 quick fix with local import.
    -  Fixed issue running with py.test.
    -  PyDev test runner working properly with unittest2.
    -  Fixed compatibility issue with eclipse 3.2.
    -  No longer sorting libraries when adding interpreter/added option
       to select all not in workspace.
    -  Fixed deadlock in the debugger when dealing with multiple
       threads.
    -  Fixed debugger issue (dictionary changing size during thread
       creation/removal on python 3.x).

**Note**: Java 1.4 is no longer supported (at least Java 5 is required
now).

Release 2.1
===========

Noteworthy
----------

**Code Analysis**

    .. figure:: images/index/code_analysis.png
       :align: center

    -  By default, only the currently opened editor will be analyzed
       (much shorter build times).
    -  Added action to force the analysis on a given folder or file.
    -  Showing error markers for PyDev elements in the tree.
    -  New option to remove error markers when the editor is closed
       (default).

**Editor**

    .. figure:: images/index/override_methods.png
       :align: center

    -  Override method completions (Ctrl+Space after a 'def ') .
    -  Completions starting with '\_' now have lower priority.
    -  Fixed major issue when replacing markers which could make errors
       appear when they shouldn't appear anymore
    -  Auto-linking on close parens is now optional (and disabled by
       default).

**Code coverage**

    -  No longer looses the selection on a refresh.
    -  Fixed issue where coverage was not working properly when running
       with multiple processes.
    -  Added orientation options

**PyUnit**

    .. figure:: images/index/rerun_on_change.png
       :align: center

    -  Added feature to relaunch the last launch when file changes (with
       option to relaunch only errors).
    -  setUpClass was not called when running with the pydev test runner
    -  F12 makes the editor active even if there's a tooltip active in
       the PyUnit view.
    -  The PyUnit tooltip is now properly restoring the focus of the
       previous active control.
    -  Added orientation options

**Others**

    -  Upon starting up PyDev, the interpreter information is validated
       for changes.
    -  Improved the django templates code-completion to better deal with
       the html/css counterparts.
    -  When the interpreter is not configured, detect it and take the
       proper actions to ask the user to configure it.
    -  No longer using StyleRange.data as it's not available for older
       versions of Eclipse.
    -  Fixed issue where references to modules could become obsolete in
       memory.
    -  When a source folder is added/removed, the package explorer will
       properly update to remove/add errors.
    -  Fixed issue where code-formatting could be really slow on
       unbalanced parenthesis on a big file.
    -  Fixed error accessing \_\_builtins\_\_.\_\_import\_\_ when
       running in the debugger.
    -  Fixed issue with wrong code-formatting with numbers.
    -  The assist to create a docstring will remove the pass right after
       it (if there's one).
    -  The path of the file that holds the preferences no longer has the
       same number of chars as the path for the interpreter.
    -  Fixed some TDD actions
    -  Fixed issue where project references were not being gotten
       recursively as they should.
    -  Fixed dedent issues on else and elif.
    -  Fixed issue with \_\_init\_\_.py not showing the parent package
       name (when set in the preferences to do so).
    -  sys.\_getframe shouldn't be needed when running unit-tests in
       IronPython.
    -  Showing interpreter information when a given project is also a
       source folder.

Release 2.0
===========

Major (see: `video <video_pydev_20.html>`_)
-------------------------------------------

**TDD actions on Ctrl+1**

**Improved code coverage support**

Noteworthy
----------

**PyUnit**

    -  It's possible to pin a test run and restore it later.
    -  Errors that occur while importing modules are properly shown.
    -  It's possible to override the test runner configurations for a
       given launch.
    -  The Nose test runner works properly when there's an error in a
       fixture.

**Editor**

    -  When there's some text selected and ' or " is entered, the
       content is converted to a string.
    -  Handling literals with ui linking.
    -  Creating ui link in the editor after entering (,[,{ when it is
       auto-closed.
    -  On hover, when there's a name defined in another module, the
       statement containing the name is shown.
    -  It's possible to launch an editor with a file not in the
       workspace (a project must be selected in this case)
    -  If a line starts with \_\_version\_\_ no import is added above
       it.
    -  When doing assign to attributes, if there's a pass in the line
       the assign will be added, it's removed.
    -  When Ctrl+1 is used to add an import on an unresolved variable,
       if Ctrl is pressed on apply a local import is done.

**Interactive console (options)**

    -  Focus on creation
    -  When created the selection may be directly sent to the console

The DJANGO\_SETTINGS\_MODULE environment var is passed when making a
launch.

The outline page now has a filter.

The input() method properly works in Python 3.2 (last "\\r" no longer
shown).

**LOTS of other adjustments and bug fixes**

Release 1.6.5
=============

    -  Syntax highlighting now has options to have {}, [] and () as well
       as operators in different colors

    -  Code generation for classes and methods:

           Note that this is an initial implementation of the idea,
           aimed as those that use a TDD (Test Driven Development)
           approach, so, one can create the test first and generate the
           classes/methods later on from using shortcuts or quick-fixes
           (which is something that those using JDT -- Java Development
           Tools -- in Eclipse should be already familiar with). This
           feature should be already usable on a number of situations
           but it's still far from being 100% complete.

           -  Alt+Shift+S C can be used to create a class for the
              currently selected token
           -  Alt+Shift+S M can be used to create a method for the
              currently selected token
           -  Ctrl+1 has as a quick fix for creating a class or method

    -  

       Debugger

           -  When discovering encoding on Python 3.x, the file is
              opened as binary
           -  Remote debugger (pydevd.settrace()) properly synchronized
           -  Fixed debugger issue on interpreter shutdown on Python 2.7

    -  

       Bug fixes:

           -  Fixed issue when doing code-completion on a line that
              started with some token that started with 'import'. e.g.:
              import\_foo = a
           -  Fixed import when running unittest with coverage
           -  Fixed extract local (could extract to wrong location)
           -  Fixed NPE when requesting print of arguments in the
              context-information tooltips
           -  Fixed AttributeError with pydevconsole on Python 3.x

Release 1.6.4
=============

    -  Improved `Unittest integration <manual_adv_pyunit.html>`_:

           -  Created a PyUnit view (with a red/green bar) which can be
              used to see the results of tests and relaunching them
           -  The default test runner now allows parallel execution
              (distributing tests by module or individually)
           -  The nose and py.test test runners are also supported now

    -  Major Bug Fixed: existing interpreters could be corrupted when
       adding a new one

    -  Fixed AttributeError on console startup in Python 3.0

    -  Added theming and automatic sash orientation to the PyDev code
       coverage view

    -  Patch by frigo7: When creating a new remote debugger target, the
       terminated ones are removed

    -  Patch by frigo7: compare editor properly showing the revision
       information and fixed broken shortcuts (e.g.: ctrl+z)

    -  Read-only files no longer editable in PyDev actions

    -  Fixed issue of remaining \\r on python 3.0 on input()

    -  The PyDev parser is now properly dealing with bom (utf-8)

    -  Assign to local: if method starts with '\_', the leading '\_' is
       not added to the local

Release 1.6.3
=============

-  Improved editor preferences page when using Aptana themes

-  Icons updated to work better with dark backgrounds

-  Handling code-completion for keywords (e.g.: a method definition with
   a parameter 'call' will have a 'call=' completion on the caller)

-  Showing a better tooltip for parameters

-  No longer marking the Django templates editor as the default editor
   for css nor html (it can be restored at window > preferences >
   general > editors > file associations)

-  

   **Globals Browser**

       -  

          Improved message in globals browser to better explan its
          features:

              -  Exact match with a whitespace in the end
              -  CamelCase matching (so, entering only TC would be
                 enough to find a class named TestCase)
              -  Dotted names may be used to filter through the packages
                 (so, dj.ut.TC would find a TestCase class defined in
                 the django.utils package)

       -  Fix: When a space is added in the end, an exact match is done

       -  Fix: No longer restoring items that don't exist anymore

-  

   Bug Fixes

       -  Fixed issue on dict and set comprehension code analysis
       -  Syntax errors on hover in a debug session not shown
       -  Block preferences page validation before save
       -  Improved django wizard configuration a bit to cover cases
          where the user does not have django installed or tries to add
          'django' as the project name
       -  The example code in the PyDev editor preferences is no longer
          editable
       -  2to3 only added in the context menu of projects with the PyDev
          nature
       -  If a debug session is terminated, no message saying that the
          variable can't be resolved in the hover is shown if the debug
          target is still selected
       -  Fixed path issues in sqlite3 path in django project creation
       -  Fixed issue where quotes could end up in the execfile when
          they should not be there
       -  Fixed issue where shift right did not work properly because
          the indent prefixes were not properly set when the tab
          preference changed

Release 1.6.2
=============

-  PyDev is now also distributed with Aptana Studio 3, so it can be
   gotten in a version that doesn't require installing it as a separate
   plugin. Get it at:
   `http://aptana.com/products/studio3/download <http://aptana.com/products/studio3/download>`_

-  **Django templates editor** (requires Aptana Studio 3)

       -  Supports HTML files with HTML, CSS and Javascript
       -  Supports CSS files
       -  Outline page
       -  Code-completion for Django templates based on templates
          (window > preferences > PyDev > django templates editor >
          templates)
       -  Code-completion for HTML, CSS and Javascript
       -  Syntax highlighting based on the templates with the 'Django
          tags' context
       -  Colors based on the Aptana themes

-  **Python 2.7 grammar** supported

-  Fixed indexing issue on contents getting getting stale in the cache

-  Fixed issue where the partitioning became wrong when entering a
   multiline string

-  Colors in the compare editor are now correct when using the Aptana
   themes

-  Extract method refactoring now works with "import" and "from ...
   import" inside a method

-  Source folders now appear before other folders

-  Fixed False positive on code analysis when using the property
   decorator

Release 1.6.1
=============

-  **Debugger**

       -  **Critical Fix: issue that prevented the debugger from working
          with Python 3 solved**
       -  Improving socket connection handling

-  **Launching**

       -  

          Restart last launch and terminate all launches actions created

              -  Restart last: **Ctrl+Shift+F9** (in PyDev editor)
              -  Terminate all: **Ctrl+Alt+F9** (in PyDev editor)
              -  Buttons were also added to PyDev consoles

-  **Utilities**

       -  **2to3**: Right-clicking a folder or file will show an option
          in the PyDev menu to convert from python 2 to python 3 (note
          that lib2to3 must available in the python installation).
       -  Defining execfile in a Python 3 interactive console so that
          Ctrl+Alt+Enter works.
       -  Fixed issue in the code style preferences page (switched value
          shown).
       -  com.ziclix.python.sql added to the forced builtins in a Jython
          install by default.
       -  Improved some icons when on a dark theme (patch from Kenneth
          Belitzky)

Release 1.6.0
=============

-  **Debugger**

       -  Code-completion added to the debug console
       -  Entries in the debug console are evaluated on a line-by-line
          basis (previously an empty line was needed)
       -  Threads started with thread.start\_new\_thread are now
          properly traced in the debugger
       -  Added method -- pydevd.set\_pm\_excepthook() -- which clients
          may use to debug uncaught exceptions
       -  Printing exception when unable to connect in the debugger

-  **General**

       -  Interactive console may be created using the eclipse vm (which
          may be used for experimenting with Eclipse)
       -  Apply patch working (Fixed NPE when opening compare editor in
          a dialog)
       -  Added compatibility to Aptana Studio 3 (Beta) -- release from
          July 12th

Release 1.5.9
=============

-  **Added compatibility to Aptana Studio 3 (Beta) -- release from June
   24th**

       -  Fixed issues related to backward incompatible changes

Release 1.5.8
=============

-  **Features only available on Aptana Studio 3 (Beta) -- release from
   June 4th:**

       -  Theming support provided by Aptana Studio used
       -  Find bar provided by Aptana used (instead of the default
          find/replace dialog)
       -  Aptana App Explorer provides PyDev nodes

-  **Eclipse:**

       -  Eclipse 3.6 is now supported
       -  PyDev Jars are now signed

-  **Django:**

       -  DoesNotExist and MultipleObjectsReturned recognized in Django
       -  Added option to make the name of Django models,views,tests
          editors work as regular editors while still changing the icon

-  **Run/Debug:**

       -  Ctrl+Shift+B properly working to toggle breakpoint
       -  If file is not found in debugger, only warn once (and properly
          cache the return)
       -  Run configuration menus: Only showing the ones that have an
          available interpreter configured

-  **Outline/PyDev Package Explorer:**

       -  Fixed sorting issue in PyDev package explorer when comparing
          elements from the python model with elements from the eclipse
          resource model
       -  Fixed issue when the 'go into' was used in the PyDev package
          explorer (refresh was not automatic)
       -  Added decoration to class attributes
       -  Added node identifying if \_\_name\_\_ == '\_\_main\_\_'

-  **General:**

       -  Properly working with editor names when the path would be the
          same for different editors
       -  Fixed issue where aptanavfs appeared in the title for aptana
          remote files
       -  Fixed halting condition
       -  Not always applying completion of dot in interactive console
          on context-insensitive completions
       -  Home key properly handled in compare editor
       -  Interactive console working with pickle
       -  String substitution configuration in interpreter properly
          works
       -  On import completions, full module names are not shown
          anymore, only the next submodule alternative

Release 1.5.7
=============

-  **Uniquely identifying editors:**

       -  Names are never duplicated
       -  Special treatment for \_\_init\_\_
       -  Special treatment for django on views, models and tests
       -  See:
          `http://pydev.blogspot.com/2010/04/identifying-your-editors.html <http://pydev.blogspot.com/2010/04/identifying-your-editors.html>`_
          for details

-  **Debugger:**

       -  **CRITICAL**: Fixed issue which could make the debugger skip
          breakpoints
       -  Properly dealing with varibles that have '<' or '>'
       -  Debugging file in python 3 with an encoding works
       -  Double-clicking breakpoint opens file from the workspace
          instead of always forcing an external file
       -  Added '\* any file' option for file selection during a debug
          where the file is not found

-  **Performance improvements for dealing with really large files:**

       -  Code folding marks won't be shown on *really large files* for
          performance reasons
       -  Performance improvements in the code-analysis (much faster for
          *really large files*)
       -  Outline tree is also faster

-  **Interpreter configuration:**

       -  Only restoring the needed interpreter info (so, it's much
          faster to add a new interpreter)
       -  Using an asynchronous progress monitor (which makes it even
          faster)
       -  Interpreter location may not be duplicated (for cases where
          the same interpreter is used with a different config,
          virtualenv should be used)
       -  Properly refreshing internal caches (which made a ctrl+2+kill
          or a restart of eclipse needed sometimes after configuring the
          interpreter)
       -  socket added to forced builtins

-  **Python 3 grammar:**

       -  Code completion and code-analysis work when dealing with
          keyword only parameters
       -  Properly reporting syntax error instead of throwing a
          NumberFormatException on "1.0L"

-  **Editor and forcing tabs:**

       -  Option to toggle forcing tabs added to the editor context menu
       -  Fixed tabs issue which could change the global setting on
          force tabs

-  **Indentation:**

       -  Added rule so that indentation stops at the level of the next
          line def or @ (to indent to add a decorator)
       -  Auto indent strategy may indent based on next line if the
          previous is empty

-  **General:**

       -  Django configuration supporting version 1.2 (contribution by
          Kenneth Belitzky)
       -  Fixed encoding problem when pasting encoded text with
          indentation
       -  asthelper.completions no longer created on current directory
          when project is removed
       -  \_\_all\_\_ semantics correct when a tuple is defined (and not
          only when a list is defined)
       -  Fixed issue in extract method (was not creating tuple on
          caller function with multiple returns)
       -  Improved heuristic for assist assign (ctrl+1)
       -  On search open files (ctrl+2+s), dialog is opened if nothing
          is entered and there's no editor selection
       -  Fixed issue where ctrl+2 would not work on linux

Release 1.5.6
=============

-  **Django integration:**

       -  New Django project can be created through wizards
       -  Can set an existing project as a Django project (right-click
          project > PyDev > set as django project)
       -  Can remove Django project config (right-click project > django
          > remove django project config)
       -  Custom actions can be passed to the configured manage.py
          through **ctrl+2+dj django\_action** -- if no action is
          passed, will open dialog to choose from a list of previously
          used commands.
       -  Predefined/custom actions can be used through right-clicking
          the project > django > select custom action
       -  manage.py location and settings module configured
       -  Django shell (with code-completion, history, etc) available
       -  Run/Debug as Django available
       -  See: `Django Integration <manual_adv_django.html>`_ for more
          details

-  **Find/Replace:**

       -  The search in open files is no longer added in the
          find/replace dialog and now works through **Ctrl+2+s
          word\_to\_find** (in the PyDev editor) and if no word is
          passed, the editor selection is used

-  **Go to definiton:**

       -  Properly works with unsaved files (so, it will work when
          searching for a definition on an unsaved file)
       -  Properly working with eclipse 3.6 (having FileStoreEditorInput
          as the editor input)

-  **Editor:**

       -  Automatically closing literals.
       -  Removing closing pair on backspace on literal
       -  Improved heuristics for automatically closing (, [ and {
       -  Removing closing pairs on backspace on (,[ and {
       -  **ctrl+2+sl** (sl comes from 'split lines' -- can be used to
          add a new line after each comma in the selection
       -  **ctrl+2+is** (is comes from 'import string' -- can be used to
          transform the selected import into a string with dots

-  **General:**

       -  Code-completion properly working on relative import with an
          alias.
       -  Fixed racing issue that could deadlock PyDev (under really
          hard to reproduce circumstances)
       -  Removing reloading code while debugging until (if) it becomes
          more mature in the python side
       -  Fixed issue where a new project created didn't have the source
          folder correctly set
       -  Text selection in double click no longer has weird behavior
       -  Local refactoring working on files not in the PYTHONPATH
       -  Edit properly working on string substitution variables
       -  Using with statement on python 2.5 no longer makes lines wrong
          in the AST

Release 1.5.5
=============

-  **Predefined completions available for code completion:**

       -  Predefined completions may be created for use when sources are
          not available
       -  Can also be used for providing better completions for compiled
          modules (e.g.: PyQt, wx, etc.)
       -  Defined in .pypredef files (which are plain Python code)
       -  Provides a way for generating those from a QScintilla .api
          file (experimental)
       -  `See Predefined Completions in manual for more
          info <manual_101_interpreter.html>`_

-  **PyDev Package Explorer:**

       -  Showing the contents of the PYTHONPATH from the interpreter
          for each project
       -  Shows the folder containing the python interpreter executable
          (to browse for docs, scripts, etc)
       -  Allows opening files in the interpreter PYTHONPATH (even
          inside zip files)

-  **Editor options:**

       -  Find/replace dialog has option to search in currently opened
          editors
       -  Move line up/down can move considering Python indentation (not
          default)
       -  Simple token completions can have a space or a space and colon
          added when applied. E.g.: print, if, etc (not default)

-  **Refactoring:**

       -  Fixed InvalidThreadAccess on refactoring
       -  Fixed problem doing refactoring on external files (no file was
          found)

-  **Globals Browser (Ctrl+Shift+T):**

       -  No longer throwing NullPointerException when the interpreter
          is no longer available for a previously found token

-  **General:**

       -  When creating a new PyDev project, the user will be asked
          before changing to the PyDev perspective
       -  Only files under source folders are analyzed (files in the
          external source folders would be analyzed if they happened to
          be in the Eclipse workspace)
       -  Interactive console now works properly on non-english systems
       -  Hover working over tokens from compiled modules (e.g.: file,
          file.readlines)
       -  JYTHONPATH environment variable is set on Jython (previously
          only the PYTHONPATH was set)
       -  Fixed path translation issues when using remote debugger
       -  Fixed issue finding definition for a method of a locally
          created token

Release 1.5.4
=============

-  **Actions**:

   -  Go to matching bracket (Ctrl + Shift + P)
   -  Copy the qualified name of the current context to the clipboard.
   -  Ctrl + Shift + T keybinding is resolved to show globals in any
      context (**note**: a conflict may occur if JDT is present -- it
      can be fixed at the keys preferences if wanted).
   -  Ctrl + 2 shows a dialog with the list of available options.
   -  Wrap paragraph is available in the source menu.
   -  Globals browser will start with the current word if no selection
      is available (if possible).

-  **Templates**:

   -  Scripting engine can be used to add template variables to PyDev.
   -  New template variables for next, previous class or method, current
      module, etc.
   -  New templates for super and super\_raw.
   -  print is now aware of Python 3.x or 2.x

-  **Code analysis and code completion**:

   -  Fixed problem when getting builtins with multiple Python
      interpreters configured.
   -  If there's a hasattr(obj, 'attr), 'attr' will be considered in the
      code completion and code analysis.
   -  Fixed issue where analysis was only done once when set to only
      analyze open editor.
   -  Proper namespace leakage semantic in list comprehension.
   -  Better calltips in IronPython.
   -  Support for code-completion in Mac OS (interpreter was crashing if
      \_CF was not imported in the main thread).

-  **Grammar**:

   -  Fixed issues with 'with' being used as name or keyword in 2.5.
   -  Fixed error when using nested list comprehension.
   -  Proper 'as' and 'with' handling in 2.4 and 2.5.
   -  'with' statement accepts multiple items in python 3.0.

-  **Improved hover**:

   -  Showing the actual contents of method or class when hovering.
   -  Link to the definition of the token being hovered (if class or
      method).

-  **Others**:

   -  Completions for [{( are no longer duplicated when on block mode.
   -  String substitution can now be configured in the interpreter.
   -  Fixed synchronization issue that could make PyDev halt.
   -  Fixed problem when editing with collapsed code.
   -  Import wasn't found for auto-import location if it import started
      with 'import' (worked with 'from')
   -  Fixed interactive console problem with help() function in Python
      3.1
   -  NullPointerException fix in compare editor.

Release 1.5.3
=============

Fixed bug where an error was being print to the PyDev console on a run.

Release 1.5.2
=============

Profile to have **much** lower memory requirements (especially on
code-analysis rebuilds)

Profile for parsing to be faster

Compare Editor

-  Syntax highlighting integrated
-  Editions use the PyDev editor behaviour
-  Code completion works

Fixed issue where PyDev could deadlock

No longer reporting import redefinitions and unused variables for the
initial parts of imports such as import os.path

Fixed issue where PyDev was removing \_\_classpath\_\_ from the
pythonpath in jython

Using M1, M2 and M3 for keys instead of hardcoding Ctrl, Shift and Alt
(which should make keybindings right on Mac OS)

Fixed some menus and popups

Properly categorizing PyDev views

Handling binary numbers in the python 2.6 and 3.0 grammar

from \_\_future\_\_ import print\_function works on python 2.6

Added drag support from the PyDev package explorer

Properly translating slashes on client/server debug

Other minor fixes

Release 1.5.1
=============

-  Improvements in the AST rewriter
-  Improvements on the refactoring engine:

   -  No longer using BRM
   -  Merged with the latest PEPTIC
   -  Inline local available
   -  Extract method bug-fixes
   -  Extract local on multi-line
   -  Generating properties using coding style defined in preferences
   -  Add after current method option added to extract method
   -  A bunch of other corner-case situations were fixed

-  Bug-fixes:

   -  Minor editor improvements
   -  Adding default forced builtins on all platforms (e.g.: time, math,
      etc) which wouldn't be on sys.builtin\_module\_names on some
      python installations
   -  Adding 'numpy' and 'Image' to the forced builtins always
   -  Ctrl+1: Generate docstring minor fixes
   -  Ctrl+1: Assign to local now follows coding style preferences
      properly
   -  Exponential with uppercase E working on code-formatting
   -  When a set/get method is found in code-completion for a java class
      an NPE is no longer thrown
   -  Backspace properly treated in block mode
   -  Setting IRONPYTHONPATH when dealing with IronPython (projects
      could not be referenced)
   -  No longer giving spurious 'statement has no effect' inside of
      lambda and decorators
   -  Fixed new exec in python 3k
   -  Fixed NPE when breakpoint is related to a resource in a removed
      project
   -  Fixed import problem on regexp that could lead to a recursion.
   -  No longer giving NPE when debugging with the register view open
   -  List access be treated as \_\_getitem\_\_() in the list -- patch
      from Tassilo Barth
   -  Fix for invalid auto-self added when typing

Release 1.5.0
=============

**PyDev Extensions is now Open Source!**

Release: 1.4.8
~~~~~~~~~~~~~~

This was the last version where PyDev and PyDev extensions were not merged.
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

-  Debugger can jump to line
-  Reloading module when code changes in the editor if inside debug
   session
-  Usability improvement on the preferences pages (editor,
   code-formatter, comment block and code-style showing examples)
-  Pythonpath reported in the main tab was not correct for ironpython
   launch configs
-  Main module tab in launch configuration was not appearing for jython
-  Multiline block comments considering the current indentation (and
   working with tabs)
-  Hover works correctly when the document is changed
-  The interactive console no longer uses the UI thread (so, it doesn't
   make eclipse halt anymore on slow requests to the shell)
-  The interactive console save history now saves the contents in the
   same way they're written
-  When creating a python run, the classpath was being set (and
   overridden), which should only happen in jython runs
-  Fixed issue where a line with only tabs and a close parenthesis would
   have additional tabs entered on code-formatting
-  A PyDev (Jython) project can coexist with a JDT project (and properly
   use its info -- only project references worked previously)
-  Many small usability improvements (editors improved)
-  Verbosity option added to run as unit-test
-  No longer using respectJavaAccessibility=False for jython
-  When there are too many items to show in the debugger, handle it
   gracefully

Release: 1.4.7
~~~~~~~~~~~~~~

**IronPython (2.6 and newer) support**

Fixed issue when configuring interpreter on Eclipse 3.3 and 3.2 (was
using API only available in 3.4)

**Google App Engine**

-  Popup menus for google app engine are now working with eclipse 3.2
-  Fixed issues when google app engine project has spaces in path

**Launching**

-  **Ctrl+F9** can be used to run as unit-test and select which tests
   will be run
-  **F9** will now run the current editor based on the project type
-  Changed run icons
-  Run configurations can be created for the project
-  Run as unit-test can have --filter and --tests as a parameter set in
   the run configuration

Shift left can now shift even when there are less chars than the
required indent string

Top-level modules on .egg files are now properly recognized

Auto-config fixed

Fixed problem when .pydevproject was not a parseable xml file (the PyDev
package explorer wouldn't work because of that)

When a new interpreter is created, it's properly selected in the tree

Code-completion better heuristic when analyzing function return that's
called on self.

Code-completion in the interactive console now handles import sections
correctly

Code formatter: Spaces after square and curly braces are no longer
changed when an unary operator is found afterwards

Fixed problem when recognizing encodings (regexp was not correct)

Release: 1.4.6
~~~~~~~~~~~~~~

**Google App Engine**: customized setup and management of Google App
Engine projects

String substitution variables can be used for pythonpath and launch
config.

The interpreter can be referred from a user-editable name

Submodules shown on import completion (e.g.: from x\|<-- request
completion here will show xml, xml.dom, xml.etree, etc)

os.path added to default forced builtins

Showing better errors when code-completion fails

Fixed problem finding definition for java class when the constructor was
referenced.

Fixed recursion error on Python 3.0 grammar

Jython debugger - local variables are properly updated

Multiple forced builtins can be added/removed at once

Python 2.6 grammar: kwarg after unpacking arg list

Python 3.0 grammar: star expr on for

Fixed problem on code-completion when file is not in the workspace
(SystemASTManager cannot be cast to ASTManager)

Not throwing IllegalCharsetNameEx on illegal encoding declaration
anymore (patch by Radim Kubacki)

\_\_future\_\_ imports are always added/reorganized as the 1st import in
the module

Code-completion in Jython recognizes that a method get/setName should be
available as a 'name' property.

Finding 'objects' for django

PyDev Package Explorer

-  Added filter for the python nodes
-  Showing configuration errors
-  Showing the interpreter info

Release: 1.4.5
~~~~~~~~~~~~~~

Better **error handling** in the grammar

**Code Formatter**

-  Can be applied from context menu (recursively applied for folders)
-  Can trim whitespaces from the end of the lines
-  Can add new a line to the end of the file
-  Can automatically apply code-formatting on save
-  Fixed issues with unary operators and exponential
-  Fixed issues where parenthesis was lost if no closing parenthesis was
   available

**Python 3.0**

-  Parser supporting unicode identifiers
-  Star expr recognized

Python 3.1 version acknowledged (and proper grammar used)

**PyDev package explorer**

-  Can show working sets as top-level elements
-  Folders without \_\_init\_\_.py are no longer shown as packages

**Interactive console**

-  When waiting for user input, the prompt is not shown
-  Console initial commands compatible with Python 3.0
-  Timeout for starting console communication while the shell is not
   fully initilized
-  More info is available if connection fails

Alt+R working (mnemonics set for PyDev contributed menus)

With Ctrl+2, matches will no longer take into acount the case

Code completion: Can get args from docstring when '\*' is present.

Better heuristics for automatic insertion of "self" and "import"

Fixed problem configuring external jars and zip files

Launch getting interpreter from project on default config

After a parenthesis, 'n' indentation levels may be applied (patch by
Radim Kubacki)

.pyc files are now marked as derived (note that this will only happen
when they're changed)

Fixed debugger issue with Jython 2.5b3

Jython: completions working for static members access

Hover works on Eclipse 3.2

Release: 1.4.4
~~~~~~~~~~~~~~

Release: 1.4.3
~~~~~~~~~~~~~~

**Interactive console** The interpreter to be used can be chosen

**New modules** can be created from **templates**

**Interpreter configuration** improved!

-  Environment variables can be specified for a given interpreter
-  Canceling operation now works correctly

**Debugger**

-  Variables correctly gotten on Jython 2.1 / 2.2
-  Using globals as an union of original globals+locals so that
   generator expressions can be evaluated
-  Breakpoints only opened on double-click (no longer on select)

The project preferences are now applied even if the page to configure
the project is not visible.

Jython 2.5b1 working (problem with sitecustomize)

Wrap paragraph fixed

Set comprehension working on Python 3.0 parsing

Find definition working when a module outside of the known pythonpath is
found

Source folders were not properly found sometimes -- when workspace was
not properly refreshed

Invalid modules could get in the memory

Getting the grammar version for a project could be wrong (and could
loose its indexing at that time)

Multiple external zip files can be added at once to the pythonpath

nonlocal added to keywords

Fixed annoying problem where cursor was jumping when it shouldn't
(outline)

Fixed problem where the breakpoint could be lost (now, playing safe and
matching anything in the file if the context cannot be gotten)

Ctrl + 2 + --reindex can be used to reindex all the opened projects if
the indexing becomes corrupt

Changing nothing on project config and pressing OK no longer reanalyzes
the modules

Release: 1.4.1
~~~~~~~~~~~~~~

**Interpreter** can be configured on a **per-project** basis

Jython 2.5b0 properly supported

Find definition working for Jython builtins

**Run**: can be python/jython even if it doesn't match the interpreter
configured for the project

Fixed problem on find definition if one of the interpreters was not
configured

Fixed halting condition that could occur on code-completion

\_\_file\_\_ available in code-completion

Reorganized preferences (removed editor preferences from the root)

Preferences for showing hover info

Fixed problem when formatting binary operator that was in a new line

When converting spaces to tabs (and vice-versa), the number of spaces
for each tab is asked

**Debugger**

-  When finishing the user code debugging, it doesn't step into the
   debugger code anymore
-  Fixes for working with Jython
-  Fix for Python 3.0 integration (could not resolve variables)

New on: 1.4
~~~~~~~~~~~

-  **Python 3.0** supported
-  **Python 2.6** supported
-  **Find Definition**: The context-sensitive code to find a definition
   from PyDev Extensions is now available (and used) in the open source
   version
-  **Hover**: Showing docstring on hover (currently only available for
   files that are not analyzed as builtins)
-  **Hover**: Showing variables on hover while debugging
-  **Parser**: One thread could corrupt the parse of another one
   (because of some static variables)
-  **Parser**: Major refactoring which also made the parser faster
-  **Task tags**: The task tags that are created by the user are no
   longer removed
-  **Code formatter** unary operators don't have a space added

New on: 1.3.24
~~~~~~~~~~~~~~

-  **Code-completion**: when a relative import was used from
   \_\_init\_\_ and the imported module used a token from the
   \_\_init\_\_ in a 'full' way, PyDev did not recognize it
-  **Debugger**: Fixed debugger halting problem
-  **Debugger and Jython**: Debugger working with Jython (itertools and
   pid not available)

New on: 1.3.23
~~~~~~~~~~~~~~

-  Can cancel scanning of files (Radim Kubacki)
-  Detection of symlink cycles inside of the pythonpath structure (could
   enter in a loop) (Radim Kubacki)
-  Removed log message if log is not enabled
-  .pyc remover not giving error anymore
-  Fixed code-completion bug when importing token with the same name of
   module where it's declared (datetime.datetime)
-  Assign with tuple not being correctly handled in the type-inference
   engine
-  Nature no longer initialized by shutdown
-  Code-completion works when inner method is declared without self
-  \_\_all\_\_: when imported no longer filters out the builtins from
   the current module on a wild import
-  Fixed problem in update site and Eclipse 3.4 (after installed could
   prevent other plugins from being installed -- compatibility problem
   on eclipse 3.4 and old versions of PyDev)

New on: 1.3.22
~~~~~~~~~~~~~~

-  **Debugger**: Pythonpath is the same in debug and regular modes
   (sys.path[0] is the same directory as the file run)
-  **Debugger**: Choices for paths not found are persisted
-  **Code-completion**: If \_\_all\_\_ is defined with runtime elements
   (and not only in a single assign statement), it's ignored for
   code-completion purposes
-  **Code-completion**: Works on case where imported module has same
   name of builtin
-  **Editor**: Cursor settings no longer overridden
-  **Interpreter config**: "email" automatically added to the "forced
   builtins"
-  **Parser**: Correctly recognizing absolute import with 3 or more
   levels
-  **Syntax check**: Option analyze only active editor (window >
   preferences > PyDev > builders)
-  **getpass.getpass**: No longer halts when run from PyDev (but will
   show the password being written)
-  **Remove error markers**: Context menu in folders to remove error
   markers created

New on: 1.3.21
~~~~~~~~~~~~~~

-  Internal release

New on: 1.3.20
~~~~~~~~~~~~~~

-  **PyDev Package Explorer**: Editor-link does not remove focus from
   current editor if it's already a match (bug when compare editor was
   opened)
-  **PyDev debugger**: Showing set and frozenset contents
-  **PyDev debugger**: Watch working in eclipse 3.4
-  **PyDev debugger**: Breakpoint properties accept new lines and tabs
-  **PyDev debugger**: Workaround for python bug when filenames don't
   have absolute paths correctly generated
-  **PyDev code-completion**: Wild import will only show tokens defined
   in \_\_all\_\_ (if it's available)
-  **Interactive console**: Fixed problem when more attempts to connect
   were needed
-  **Interactive console**: Fixed console integration problem with other
   plugins because of interfaces not properly implemented
-  **Incremental find**: Backspace works correctly
-  **Launch icons**: Transparent background (thanks to Radim Kubacki)
-  **Code Formatter**: Exponentials handled correctly
-  **Launching**: Unit-test and code-coverage may launch multiple
   folders/files at once
-  **Code coverage**: Number format exception no longer given when
   trying to show lines not executed in the editor and all lines are
   executed
-  **Auto-indent**: Fixed issue when using tabs which could result in
   spaces being added

New on: 1.3.19
~~~~~~~~~~~~~~

-  **Eclipse 3.2**: Interactive console working
-  **Eclipse 3.4**: Hyperlinks working
-  **Eclipse 3.4**: Move / rename working
-  **raw\_input() and input()**: functions are now changed when a
   program is launched from eclipse to consider a trailing '\\r'
-  **Ctr+/**: Changed to toggle comment (instead of only comment) --
   patch from Christoph Pickl
-  **PyDev package explorer**: Link working with compare editor
-  **Auto-indent**: Fixed problem when smart indent was turned off
-  **Debugger**: Better inspection of internal variables for dict, list,
   tuple, set and frozenset
-  **Console**: When a parenthesis is entered, the text to the end of
   the line is no longer deleted
-  **Code Formatter**: can deal with operators (+, -, \*, etc)
-  **Code Formatter**: can handle '=' differently inside function calls
   / keyword args
-  Problem while navigating PyDev package explorer fixed
-  Race condition fixed in PythonNatureStore/PythonNature (thanks to
   Radim Kubacki)
-  Halt fixed while having multiple editors with the same file (with the
   spell service on)
-  Pythonpath is no longer lost on closed/imported projects
-  Applying a template uses the correct line delimiter
-  NPE fixed when creating editor with no interpreter configured
-  Hyperlink works in the same way that F3 (saves file before search)

New on: 1.3.18
~~~~~~~~~~~~~~

-  **Executing external programs**: Using Runtime.exec(String[] cmdargs)
   instead of a string with the generated command (fixes problems
   regarding having spaces in the installation).
-  **Organize Imports (ctrl+shift+O)**: Imports can be grouped.
-  **Cygwin**: sys.executable in cygwin was not returning '.exe' in the
   end of the executable as it should.
-  **Additional paths for PYTHONPATH** (Patch from Eric Wittmann):
   extension point allows plugins to contribute paths to the PYTHONPATH.
-  **Code-completion**: typing '.' won't apply the selected completion,
   but will still request a new one with the current contents.
-  **PyDev Package Explorer**: Problem while trying to show active
   editor on the PyDev package explorer.

New on: 1.3.17
~~~~~~~~~~~~~~

**PyDev Package Explorer**: projects that had the project folder in the
pythonpath did not show children items correctly.

**Debugger**: Disable all works. Patch from: Oldrich Jedlicka

**Debugger**: Problem when making a step return / step over

**Code-completion**: Working for attributes found in a superclass
imported with a relative import

Patches from Felix Schwarz:

-  Allow to configure an interpreter even if the workspace path name
   contains spaces
-  Completion server does not work when the eclipse directory contains
   spaces
-  Fix deletion of resources in PyDev package explorer for Eclipse 3.4

New on: 1.3.16
~~~~~~~~~~~~~~

-  **Interactive console**: help() works
-  **Interactive console**: context information showing in completions
-  **Interactive console**: backspace will also delete the selected text
-  **Interactive console**: ESC does not close the console when in
   floating mode anymore
-  **Code completion**: calltips context info correctly made 'bold'
-  **Code completion**: variables starting with '\_' do not come in
   import \*
-  **Code completion**: can be requested for external files (containing
   system info)
-  **Code completion**: fixed recursion condition
-  **Code completion**: egg file distributed with dll that has a source
   module with the same name only with a \_\_bootstrap\_\_ method now
   loads the dll instead of the source module (e.g.: numpy egg)
-  **Debugger**: Step over/Step return can now execute with untraced
   frames (much faster)
-  **Debugger**: Problem when handling thread that had no context traced
   and was directly removed.
-  **Launching**: F9 will reuse an existing launch instead of creating a
   new one every time
-  **Launching**: The default launch with Ctrl+F11 will not ask again
   for the launch associated with a file (for new launches -- old
   launches should be deleted)
-  **Project Explorer**: fixed integration problems with CDT (and
   others)
-  **Launch**: console encoding passed as environment variable (no
   longer written to the install location)
-  More templates for "surround with" (Ctrl+1)
-  Previous/next method could match 'class' and 'def' on invalid
   location
-  Outline: Assign with multiple targets is recognized
-  Bug fix for PyDev package explorer when refreshed element parent was
   null

New on: 1.3.15
~~~~~~~~~~~~~~

**Files without extension**: If a file that does not have an extension
is found in the root of the pythonpath, code-completion and breakpoints
work with it.

**Extract method**: comma not removed when found after a tuple and
before a keyword argument.

**Console Encoding**: print u"\\xF6" works (console encoding correctly
customized in python -- see
`http://sourceforge.net/tracker/index.php?func=detail&aid=1580766&group\_id=85796&atid=577329 <http://sourceforge.net/tracker/index.php?func=detail&aid=1580766&group_id=85796&atid=577329>`_
for details).

**Debugger**: Context of breakpoint correctly defined when comments are
present in the end of the module.

from \_\_future\_\_ import (xxx, with\_statement): works.

`Interactive Console View <manual_adv_interactive_console.html>`_, featuring:

**Code Completion**

-  Context sensitive with shell completions
-  Qualifier matches as case insensitive
-  Templates
-  Repeating the activation changes from templates to default
   completions

**Console Configurations**

-  Initial commands for starting the console
-  Colors for the console
-  Vmargs can be specified for jython

**Auto-indent**

**Auto-edits**

**Context info on hover**

**Up / Down Arrows** cycles through the history (and uses the current
text to match for the start of the history command)

**Page Up**: shows dialog with console history (where lines to be
re-executed can be selected)

**Esc**: clears current line

**ctrl+1** works for assign quick-assist

**Hyperlinks** addedd to tracebacks in the console

Paste added directly to the command line

Cut will only cut from the command line

Copy does not get the prompt chars

Home goes to: first text char / prompt end / line start (and cycles
again)

Cursor automatically moved to command line on key events

Multiple views of the same console can be created

Limitation: Output is not asynchonous (stdout and stderr are only shown
after a new command is sent to the console)

New on: 1.3.14
~~~~~~~~~~~~~~

-  **Outline view**: patch by Laurent Dore: better icons for different
   types of fields methods.
-  **Outline view**: patch by Laurent Dore: more filters.
-  **PyLint**: working dir is the directory of the analyzed file.
-  **Project explorer**: fixed bug on integration with Dynamic Web
   Project.
-  **Extract method**: fixed bug when trying to refactor structure: a =
   b = xxx.
-  **Generate constructor using fields**: working for classes that
   derive from builtin classes.
-  **Override methods**: working for classes that derive from builtin
   classes.
-  **Debugger can use psyco for speedups**: see
   `http://pydev.blogspot.com/2008/02/pydev-debugger-and-psyco-speedups.html <http://pydev.blogspot.com/2008/02/pydev-debugger-and-psyco-speedups.html>`_.
-  **Debugger**: shows parent frame when stepping in a return event.
-  **Go to previous/next method**: (Ctrl+Shift+Up/Down): does not rely
   on having a correct parse anymore.
-  **Auto-formatting**: No space after comma if next char is new line.
-  **Code Completion**: Handling completions from attribute access in
   classes (accessed from outside of the class).
-  **Auto-indent**: Better handling when indenting to next tab position
   within the code.
-  **Caches**: Some places were recreating the cache used during a
   completion request instead of using the available one (which could
   have a memory impact on some situations).

New on: 1.3.13
~~~~~~~~~~~~~~

-  **Outline view**: working correctly again.
-  **Keybinding conflict**: Alt+shift+T+XXX refactoring keybindings are
   now only defined in the PyDev scope.
-  **Hyperlink**: Using new hyperlink mechanism (added at Eclipse 3.3).

New on: 1.3.12
~~~~~~~~~~~~~~

-  **Code Coverage**: coverage.py updated to version 2.78
   (`http://nedbatchelder.com/code/modules/coverage.html <http://nedbatchelder.com/code/modules/coverage.html>`_).
-  **Optimization**: Caches (with no memory overhead) added for a number
   of situations, which can speed completion requests a lot (up to 40x
   on tests).

New on: 1.3.11
~~~~~~~~~~~~~~

-  **Jython Integration**: Java modules may be referenced from PyDev
   projects (working with code-completion, go to definition, etc).
-  **Jython Debugger**: Does not attempt to run untraced threads if
   version <= 2.2.1 (this was a Jython bug that's patched for the
   current trunk -- **note:** it prevented the debugger from working
   correctly with Jython).
-  **Project build**: Only referenced projects are rebuilt (and not all
   projects in the workspace -- e.g.: unreferenced c++ projects).
-  **Spell checking (depends on JDT)**: Integrated for comments and
   strings within PyDev (eclipse 3.4 should add the support for working
   without JDT. Reference:
   `http://www.eclipse.org/eclipse/platform-text/3.4/plan.php <http://www.eclipse.org/eclipse/platform-text/3.4/plan.php>`_).
-  **Files without extension**: A file without extension can have
   code-completion / go to definition (as long as the others around it
   do have extensions)
-  **Debug**: Variable substitution is no longer asked twice in debug
   mode.
-  **Custom Filters**: User-defined filters can be specified in the
   **PyDev package explorer**.
-  **Debugger**: performance improvements to get the existing frames for
   Python 2.4 and Jython 2.1.
-  **Outline view**: Better refresh (doesn't collapse the tree for
   simple structure changes).
-  **Undo limit**: The undo limit set in window > preferences > general
   > editors > text editors works for PyDev.
-  **Editor: Tabs as spaces**: The newly added 'insert spaces for tabs'
   in the general preferences was conflicting with PyDev (those settings
   are now ignored)
-  **Patch by Laurent Dore**: Added filter for \*.py~ and comments
-  **Delete \*.pyc action**: also deletes \*.pyo files
-  **Ctrl+Click**: behaves exactly as F3.
-  **Dedent**: No auto-dedent after yield

New on: 1.3.10
~~~~~~~~~~~~~~

-  **Symlinks** supported in the system pythonpath configuration.
-  **Egg/zip** files are now supported.
-  The creation of a project in a non-default location is now allowed
   within the workspace
-  **JDT** used to get completions from jars (but referencing other java
   projects is still not supported).
-  Configuration of pythonpath allows multiple selection for removal.
-  Configuration of pythonpath allows multiple jars/zips to be added at
   once.
-  When configuring the pythonpath, the paths are sorted for selection.
-  The file extensions that PyDev recognizes for python can now be
   customized.
-  Patch by **Carl Robinson**: **Code-folding** for elements such as
   for, try, while, etc.
-  Removed the go to next/previous problem annotation (Eclipse 3.3
   already provides a default implementation for it).

New on: 1.3.9
~~~~~~~~~~~~~

-  Fixed problem when configuring jython
-  Patch from **paulj**: debbugger working with jython 2.2rc2
-  Patch from **Oskar Heck**: debbugger can change globals
-  Added action to **delete all .pyc / $py.class** files
-  Added actions to **add/remove the PyDev configuration from a
   project** (previously, the only way to add a nature was to open a
   python file within a project).
-  **Ctrl+Shift+O**: When used with a selection will consider lines
   ending with \\ (without selection organizes imports)
-  Auto-add "import" string will not be added when adding a space in the
   case: from xxximport (just after from xxx)
-  Templates created with tabs (or spaces indent) are now converted to
   the indent being used in the editor
-  Hide non-PyDev projects filter working
-  Don't show assignments/imports after **if \_\_name\_\_ ==
   '\_\_main\_\_'**: in outline
-  **Code-completion**: after a completion is requested, pressing '.'
   will apply that completion (and if it has parameters, they'll not be
   added).
-  **Code-completion**: when a code-completion is applied with Ctrl
   pressed (toggle mode), parameters are not added.
-  **Assign to local** variable/attribute handles **constants**
   correctly.
-  **psyco** changed for Null object for **debug** (so, no changes are
   required to the code if psyco is used while debugging).
-  **Code-folding** annotations won't change places.
-  **PyDev package explorer** will correctly show outline for files if
   the project root is set as a source folder.
-  **PyDev package explorer**: folders under the pythonpath have a
   package icon.
-  Unittest runner: handles **multiple selection**.

New on: 1.3.8
~~~~~~~~~~~~~

-  Fixed problems related to the PyDev package explorer that appeared
   when using **java 1.6** (ConcurrentModificationException)
-  Other minor bug-fixes

New on: 1.3.7
~~~~~~~~~~~~~

**Support for Eclipse 3.3**

**Bug Fix**: Interpreter modules not correctly set/persisted after
specifying interpreter (so, the builtins and other system libraries
would not be available in completions).

`Mylyn <http://www.eclipse.org/mylyn/>`_ integration.

**Open With PyDev**: does not appear for containers anymore.

**Code-completion:**

The folowing cases are now considered in code-completion to discover the
type of a variable:

-  assert isinstance(obj, Interface) -- default from python
-  assert Interface.implementedBy(obj) -- zope
-  assert IsImplementation(obj, Interface) -- custom request
-  assert IsInterfaceDeclared(obj, Interface) -- custom request

-  a = adapt(obj, Interface) -- pyprotocols
-  a = obj.GetAdapter(Interface) -- custom request
-  a = obj.get\_adapter(Interface) -- custom request
-  a = GetSingleton(Interface) -- custom request
-  a = GetImplementation(Interface) -- custom request

New on: 1.3.6
~~~~~~~~~~~~~

-  **Bug Fix:** Builtins were not correctly used after specifying
   interpreter (so, the builtins would not be available in
   completions/code-analysis).
-  **Patch (from Carl Robinson):** PyLint severities can now be
   specified.

New on: 1.3.5
~~~~~~~~~~~~~

**Eclipse 3.3 Integration:** Does not keep eclipse from a correct
shutdown anymore.

**Docstrings and code completion pop-up:**

-  The docstrings are now wrapped to the size of the pop-up window.
-  The initial columns with whitespaces that are common for all the
   docstring is now removed.
-  The previous size of the pop-up window in completions is now
   restored.

**Extract method refactoring:** was not adding 'if' statement correctly
on a specific case.

**Organize imports:** (Ctrl+Shift+O): comments are not erased in import
lines when using it anymore.

**Interpreter Config:** solved a concurrency issue (which could issue an
exception when configuring the interpreter).

**Jython integration:** can now work with a j9 vm.

**Jython integration:** those that don't use jython can now use eclipse
without JDT (but it's still required for jython development).

**Outline:**

-  The comments are now set in the correct level (below module, class or
   method).
-  Comments are sorted by their position even when alphabetic sorting is
   in place.
-  Comments are added to the outline if they start or **end** with
   '---'.

New on: 1.3.4
~~~~~~~~~~~~~

-  **Debugger:** Breakpoints working correctly on external files opened
   with 'File > Open File...'.
-  **Debugger:** Python 2.5 accepts breakpoints in the module level.
-  **Debugger:** Unicode variables can be shown in the variables view.
-  **Editor:** Coding try..except / try..finally auto-dedents.
-  **Code Completion:** \_\_builtins\_\_ considered a valid completion
-  **PyDev Package Explorer:** Opens files with correct editor (the
   PyDev editor was forced).

New on: 1.3.3
~~~~~~~~~~~~~

-  **Performance:** Optimizations in the code-completion structure.
-  **Debugger:** Performance improvements (it will only actually trace
   contexts that have breakpoints -- it was doing that in a module
   context before).
-  **Debugger:** Step over correctly stops at the previous context.
-  **Debugger:** Breakpoint labels correct when class/function name
   changes.
-  **Quick-Fix:** Move import to global scope would not be correct if
   the last line was a multi-line import.
-  **Outline:** Syntax errors will show in the outline.
-  **Outline:** Selection on import nodes is now correct.
-  **Outline:** Link with editor created.
-  **Outline:** Show in outline added to the PyDev perspective.
-  **Find Previous Problem:** action created (**Ctrl+Shift+.**).
-  **Extract method refactoring:** end line delimiters are gotten
   according to the document (it was previously fixed to \\n).
-  **Extension-points:** Documentation added for some of the extension
   points available.
-  **Perspective:** The PyDev package explorer has been set as the
   preferred browser in the PyDev perspective.

New on: 1.3.2
~~~~~~~~~~~~~

-  **PyDev Editor:** If multiple editors are open for the same file, a
   parser is shared among them (which greatly improves the performance
   in this case)
-  **PyDev Editor:** Backspace is now indentation-aware (so, it'll try
   to dedent to legal levels)
-  **PyDev Editor:** sometimes the 'import' string was added when it
   shouldn't
-  **Fix: Code-completion:** case where a package shadows a .pyd is now
   controlled (this happened with mxDateTime.pyd)
-  **Fix: Code-completion:** recursion condition was wrongly detected
-  **Fix: Code-completion:** halting condition was found and removed
-  **Fix: Project Config:** if a closed project was referenced, no
   project was gathered for any operation (e.g.: code-completion)
-  **Fix:** The filter for showing only PyDev projects is not active by
   default anymore

New on: 1.3.1
~~~~~~~~~~~~~

-  `Mylyn <http://www.eclipse.org/mylyn/>`_ integration: the PyDev
   package explorer now supports
   `mylyn <http://www.eclipse.org/mylyn/>`_ (packaged as a separate
   feature: **org.python.pydev.mylyn.feature)**
-  **Code-completion**: comment completion is now the same as string
   completion
-  **Debug**: Breakpoints can be set in external files
-  **Debug**: Breakpoint annotations now show in external files
-  **Package Explorer**: filter for import nodes created
-  **Fix: Package Explorer Actions**: Open action does not expand
   children when opening python file
-  **Fix: Project Explorer (WTP) integration**: does not conflict with
   elements from other plugins anymore (such as java projects)
-  **Fix: halt in new project wizard**: when creating a new project from
   the PyDev wizard it was halting in some platforms
-  **Fix: ${string\_prompt} in run config**: now only evaluated on the
   actual run
-  **Fix: Code-Completion**: jython shell was not handling
   java.lang.NoClassDefFoundError correctly

New on: 1.3.0
~~~~~~~~~~~~~

-  **Code-completion**: Deep analysis for discovering arguments in
   constructs 'from imports' now can be configured given the number of
   chars of the qualifier
-  **Refactoring for override methods**: changed so that it uses the
   PyDev code-completion engine for getting the hierarchy and methods
-  **Fix: Python Nature Restore**: begin rule does not match outer scope
   rule fixed
-  **Fix: Package Explorer**: if show-in is in a deep structure, it will
   show it is the 1st try and not only in the 2nd
-  **Fix: Package Explorer**: some intercepts removed elements
   incorrectly, and ended up messing the navigator and search (which has
   'null' elements because of that)

New on: 1.2.9
~~~~~~~~~~~~~

-  Repackaging fix

New on: 1.2.8
~~~~~~~~~~~~~

-  **Refactoring**: integration of the `PEPTIC refactoring
   engine <http://sifsstud2.hsr.ch/peptic>`_
-  **Package Explorer**: many fixes (special thanks for Don Taylor for
   the bug reports)
-  **Debugger**: a number of small optimizations
-  **Code-completion**: works in emacs mode
-  **Code-completion**: added the possibility of auto-completing for all
   letter chars and '\_' (so, it starts completing once you start
   writing)
-  **Code-completion**: code-completion for epydoc inside strings
-  **Code-completion**: assigns after global statement considered added
   to the global namespace
-  **Code-completion**: now works when a class is declared in a nested
   scope
-  **Code-completion**: if multiple assigns are found to some variable,
   the completion will be a merge of them
-  **Code-completion**: functions are analyzed for their return values
   for code-completion purposes
-  **Code-completion**: working on multi-line imports
-  **Code-completion**: can discover instance variables not declared in
   the class (in the scope where the class was instanced)
-  **Auto-edit**: adds 'self', 'cls' or no parameter based on the
   @clasmethod, @staticmethod declaration on previous line
-  **Auto-edit**: doesn't add 'self' if a method is declared in a method
   inner scope
-  **Fix: BRM Refactoring**: wrong column was being passed to the BRM
   refactoring engine
-  **Code-folding**: added for comments and strings
-  **Fix**: sometimes the 'create docstring' assistant was not
   recognizing method definitons

New on: 1.2.7
~~~~~~~~~~~~~

-  **Debugger**: change value implemented
-  **PyDev Package Explorer**: Default actions (copy, paste, rename,
   etc) reimplemented (see
   `blog <http://pydev.blogspot.com/2006/12/package-explorer-status-for-pydev.html>`_
   to see what this fixed)
-  **Block Comments**: The remove block comments (Ctrl+5) will now
   remove contiguous comments -- if several lines are commented, putting
   the cursor in any of those lines and pressing Ctrl+5 will remove all
   those comments
-  **Launch**: When creating a new launch, the 'default' option is
   chosen, so, if the default interpreter changes later, this won't
   affect existing launch configurations
-  **Code Completion**: 'cls' does not appear as 1st parameter on code
   completion anymore
-  **Code Completion**: completions for cls on classmethods now works
   correctly
-  **Keybindings**: Ctrl+Tab and Ctrl+Shift+Tab keybindings removed from
   convert tabs to space-tabs (and vice-versa) -- they are still
   available in the source menu
-  **Fix**: the PYTHONPATH passed to **PyLint** was not containing the
   project PYTHONPATH
-  **Fix**: halting condition on code-completion execution
-  **Fix**: 'create docstrings' assist creates **@param** even if there
   is a comment in a function definition
-  **Fix**: block comment for class will recognize better when it is on
   a class definition line

New on: 1.2.6
~~~~~~~~~~~~~

**PyDev package explorer** (Still **BETA**, so, use it with care -- the
navigator is still the preferred way of browsing your project)

-  Filters for .pyc and .pyo files created
-  Working set integration added (just available for Eclipse **3.2.1**)
-  Some minor bugs fixed

Patch by **Olof Wolgast**: The **'create docstrings'** assistant has
more options (windows > preferences > PyDev > code formatter >
docstrings)

-  Option to choose between **single and double quotes**
-  Option to **create '@type'** too and not only **'@param'**
-  Option to **skip creation of the @type** tag for parameters starting
   with some prefix (if 'Custom' is chosen)

**Block comments** now have more options (windows > preferences > PyDev
> code formatter > block comments)

-  Option to **align single-line block comment to the left or right**
-  Option to put class name above class if applied on a class definition

**Fix**: Multi-line Templates indent correctly

**Fix**: When a file from a project that was deleted was still opened
inside PyDev, Eclipse would not start-up correctly

**Fix**: When a different interpreter is chosen in the run dialog, the
pythonpath is correctly gotten

**Fix**: when PyDev was started, it would re-save the .pydevproject file
even if no change was done

**Fix**: When tab width was set to 0 it could halt the editor (now if 0
is set, 4 is assumed)

Grammar Patch by **Ueli Kistler, Dennis Hunziker**

New on: 1.2.5
~~~~~~~~~~~~~

-  **PyDev package explorer** was created. Features already implemented:

   -  Common resource actions (delete, copy, rename, team...)
   -  Shows the Source folder with a different icon
   -  Linking mode enabled
   -  Shows the outline for a python file
   -  Opening an item in the outline opens the correct place in the
      correspondent file

-  Debugger bug-fix: Crash when debugging wxPython programs should not
   happen anymore
-  When opening a file, the encoding is considered (and not only when
   saving it)
-  Patches from **Gergely Kis**:

   -  Option for having a 'default interpreter' in the combo for
      selecting which interpreter to use for a run
   -  Saving the things related to the PyDev project in a .pydevproject
      file to be commited

-  Patch from **Gregory Golberg**:

   -  **Ctrl+Shift+D** when a variable is selected in debug mode shows
      the variable value

New on: 1.2.4
~~~~~~~~~~~~~

-  Completions for parameters based on the tokens that were defined for
   it in a given context
-  Removed the default PyLint options, because its command-line
   interface changed (that was crashing PyLint in newer versions)
-  Changed the grammar so that 'yield' is correctly parsed as an
   expression
-  Giving better error message when external file is opened without any
   interpreter selected
-  Icons for the builtins gotten on large libraries (such as wx -- it
   was optimized not to do that before)
-  Adding jars relative to the project now works correctly
-  The debugger is now able to get the variables in a context when
   handling negative payloads (**patch by Javier Sanz**)

New on: 1.2.3
~~~~~~~~~~~~~

-  When the user specifies an invalid interpreter, a better error report
   is given (previously it was only shown in the error log)
-  When threads die, the debugger is notified about it (so that they are
   removed from the stack)
-  Writing the preferences to the disk is now buffered
-  Fixed problem when debugging in jython with the statement "from xxx
   import \*"
-  Fixed one issue with the indentation engine
-  Commenting a line does not remove a blank line in the end anymore
-  Added debug mode for unit-test
-  Added the possibility of setting the **-Dpython.cachedir** for
   running the jython shell (errors can arise in unix-based machines if
   jython is not able to write its cache)

   **Contributions**

   -  **Darrell Maples**:

      -  Unit-test refactoring
      -  Run as jython unit-test
      -  Filter test methods to run

   -  **Joel Hedlund**

      -  Added a support to ease adding options to Ctrl+1 in the
         scripting engine
      -  Added a ctrl+1 for the case:
          def m1(self, arg=None):
          arg \|<-- Ctrl+1 shows option to do:
          def m1(self, arg=None):
          if arg is None:
          arg = []

   **Support for python 2.5**
   
   -  Added the new relative import
   -  Added the new if expression
   -  Added the unified try..except..finally statement
   -  Added the with x:... statement
   -  Indenting after the new with statement
   -  Recognizing 'with' as a keyword in syntax highlighting

New on: 1.2.2
~~~~~~~~~~~~~

**Code Completion**

-  Calltips added to PyDev
-  The parameters are now linked when a completion is selected (so, tab
   iterates through them and enter goes to the end of the definition)
-  Parameters gotten from docstring analysis for builtins that don't
   work with 'inspect.getargspec'
-  Getting completions for the pattern a,b,c=range(3) inside a class
-  Code completion for nested modules had a bug fixed
-  Added the 'toggle completion type' when ctrl is pressed in the
   code-completion for context-sensitive data
-  Code-completion works correctly after instantiating a class:
   MyClass(). <-- will bring correct completions
-  Code-completion can now get the arguments passed when instatiating a
   class (getting the args from \_\_init\_\_)
-  self is added as a parameter in the completion analyzing whether
   we're in a bounded or unbounded call
-  Pressing Ctrl+Space a second time changes default / template
   completions

**Outline View**

-  Added option for hiding comments and imports
-  Persisting configuration
-  Added option for expanding all

**Others**

-  Possibility of setting pyunit verbosity level (by **Darrell Maples**)
-  Errors getting the tests to run are no longer suppressed
-  Ctrl+2+kill also clears the internal cache for compiled modules
   (especially useful for those that create compiled dependencies).
-  Last opened path remembered when configuring the pythonpath (dialog)

New on: 1.2.1
~~~~~~~~~~~~~

-  The user is asked for which paths should be added to the system
   pythonpath
-  Go to previous method now works with decorators
-  Stack-trace link now opens in correct line for external files
-  Variables now show in the variables view while debugging
-  If an invalid interpreter is selected to run a file (old interpreter
   or wrong project type), a warning is given to the user
-  Ctrl+w is removed as the default for select word (the action is still
   there, but its keybinding is removed, so, users have to configure
   themselves which keybinding they want for it)
-  Assign to local or field variable now enters in linked mode
-  Added dependency to Eclipse 3.2 features, as version 1.2.0 of PyDev
   and newer are only Eclipse 3.2 compatible.

New on: 1.2.0
~~~~~~~~~~~~~

-  Eclipse 3.2 supported (and 3.1 support is now discontinued)
-  Lot's of optimizations to make PyDev faster
-  Ctrl+Click now works with the find definition engine
-  Comments that start with #--- are shown in the outline
-  Attributes are shown in the outline
-  Parse errors are now shown (again) in the editor
-  Many other bugs fixed

New on: 1.1.0
~~~~~~~~~~~~~

-  Startup is now faster for the plugin: actions, scripts, etc. are now
   all initialized in a separate thread
-  Indentation engine does not degrade as document grows
-  Multiple-string does not mess up highlighting anymore
-  code completion issue with {} solved
-  Ctrl+W: now expands the selection to cover the whole word where the
   cursor is
-  Assign to attributes (provided by Joel Hedlund) was expanded so that
   Ctrl+1 on method line provides it as a valid proposal
-  A Typing preferences page was created so that the main page now fits
   in lower resolutions

 **NOTE**: this is the last version with support for Eclipse 3.1

New on: 1.0.8
~~~~~~~~~~~~~

-  The Parser character stream was redone to be more efficient
   (especially when dealing with big files)
-  The thread that does analysis had its priority lowered
-  When running a file, the pythonpath set now let's the project
   pythonpath before the system pythonpath
-  The way modules are resolved for loading when running unit-tests has
   changed
-  Indentation further improved
-  Debugger changes for working with jython
-  **Ctrl+2+w**: wraps the current paragraph to the number of lines
   specified in the preferences. This was provided by **Don Tailor**
   (revisions are also available in
   `http://pilger.googlepages.com/pydevstuff <http://pilger.googlepages.com/pydevstuff>`_)
-  Lot's of bug-fixes and optimizations

New on: 1.0.7
~~~~~~~~~~~~~

-  This is a single-bugfix release. It fixes an error that could occur
   when adding a newline in a document that had a docstring with an
   empty newline in the global level.

New on: 1.0.6
~~~~~~~~~~~~~

-  Assign variables to attributes (Ctrl+2+a): Contributed by Joel
   Hedlund (this is the first contribution using the new jython
   scripting engine).
-  3 minor 'quirks' were fixed in the indentation engine
-  The debugger had some changes (so, if you had halts with it, please
   try it again).
-  Allow changing the keybinding for activating the Find next problem
   (Ctrl+.)
-  The debugger step-return had its behaviour changed.
-  Additional scripts location added to pythonpath in the jython
   scripting engine
-  Transversal of nested references improved
-  Fixed problems with compiled modules when they had 'nested' module
   structures (e.g.: wx.glcanvas)

New on: 1.0.5
~~~~~~~~~~~~~

-  Another batch of things to improve **indentation**:

   -  Indent does not try to make auto-indentation when pasting
   -  When smart-indent is not selected, it will still add an
      indentation level after ':'
   -  It will keep the indent of the previous line on new-lines if the
      current line is empty
   -  Other little things

-  Added a place to specify **vm arguments** (for jython or python) --
   thanks to Rudi de Andrade for this patch
-  Added a way to kill the underlying python/jython shells
   (**Ctrl+2+kill**)

New on: 1.0.4
~~~~~~~~~~~~~

**Added jython scripting**

-  `Link to article on how to create your own scripts with the new
   scripting engine. <manual_articles_scripting.html>`_

Added an 'easy' way to bind actions after Ctrl+2 (to make scripting
easier)

Added a way to list things binded with Ctrl+2 (To see: Ctrl+2+help)

Added a 'go to next problem' with jython scripting capabilities, as a
first example on how to script PyDev with Jython (Ctrl+.)

A brand new indentation engine is available:

-  Does not try to make different indentations inside multilines
-  Does not try to add spaces to make smart-indent when in only tabs
   mode
-  Indents correctly after opening some bracket or after closing some
   bracket
-  Indents to 'expected level' when hitting tab

Fixed bug: syntax error described instead of throwing an error

Profiled PyDev (not that much, but still, I hope you'll be able to
'feel' it)

Fixed bug: the pythonpath was not added when additional environment
variables where specified

And as always, other bugs have been fixed

New on: 1.0.3
~~~~~~~~~~~~~

-  Fixed error while organizing imports with the construct from xxx
   import (a,b\\n c)
-  Auto-dedent for 'else:' and 'elif' constructs
-  Added color customization for the function name and class name
-  Fixed debugger error: it could halt when getting some variable
   representation if the variable translated in a string that was huge
-  Fixed error while debugging with conditional breakpoint (only
   evaluated the first time) -- Thanks to Achim Nierbeck for this fix
-  Show in view: Resource Navigator (Ctrl+Alt+W) now is always active on
   the PyDev view
-  Fixed leak on template images

New on: 1.0.2
~~~~~~~~~~~~~

-  Jython debugging now working.
-  Code coverage does not report docstrings as not being executed.
-  Freeze when making a 'step-in' fixed in the debugger.
-  Grammar generated with javacc version 4.0

New on: 1.0.1
~~~~~~~~~~~~~

-  **Fix for an out-of-memory error when restoring the interpreter**
   (single thing in this release)

New on: 1.0
~~~~~~~~~~~

-  **High-speed Debugger** (on par with the best debuggers available)
-  Debugger now gets the variables 'on-demand'
-  The variables returned for jython are much more complete
-  Wizard to create new project has option for creating a default 'src'
   folder (and add it to the pythonpath).
-  The create new python module and new python package have been
   reviewed (you can still use the regular ones, but the new ones are
   really reccommended -- also it will help in making sure you have your
   pythonpath correctly configured!).
-  Create new source folder option added.
-  PyLint can now give the output to the console (configurable).
-  PyLint 0.9.0 tested
-  PyLint errors now show in the hover
-  The PyDev perspective was changed (so, please, close the current and
   ro-open it)
-  Templates were added for the keywords
-  Keybindings were added to run the current editor as python (F9) or as
   jython (Ctrl+F9). Those are customizable in the 'keys' preferences
-  And many other bug-fixes as usual

New on: 0.9.8.7
~~~~~~~~~~~~~~~

-  **The debugger tracing was turned off** (this was a bug in 0.9.8.6
   and could make debugging much slower)
-  **Fixed jython shell** (and extended it to get better information on
   code-completion).
-  **Changed the interpreter configuration so that it is backwards-compatible from now on...**
   (but the current interpreters will be lost and will need to be configured)
-  **Breakpoints can have conditionals** (this was contributed by Achim
   Nierbeck, and was actually provided in release 0.9.8.6, but I forgot
   to put it in the release notes)
-  Some other bugfixes are also in this build.

New on: 0.9.8.6
~~~~~~~~~~~~~~~

-  **Added a new 'PyDev project' wizard (Mikko Ohtamaa contribution)**--
   it is named as PyDev Project instead of Python project because it
   creates Python and Jython projects.
-  **Added a new 'PyDev module' wizard (Mikko Ohtamaa contribution)** --
   NOTE: it still needs some work.
-  **Changes in the shell spawning were done, and no hangs should appear
   when trying to do code-completion anymore** (if it still hapens,
   please report it as a bug -- NOTE: a little delay on the first time
   code-completion is executed is expected, as this is the time the
   shell is started).

New on: 0.9.8.5
~~~~~~~~~~~~~~~

-  Removed the dependency on packages 'sun.xxxx.Base64', so that other
   VMs can be targetted
-  Some code-completion problems in the 'resolution order' regarding
   tokens in \_\_init\_\_ were solved
-  Added option so that the user can choose whether to automatically add
   'self' or not in method definitions

New on: 0.9.8.4
~~~~~~~~~~~~~~~

-  The license was changed to EPL. It can be found at:
   `http://www.opensource.org/licenses/eclipse-1.0.php <http://www.opensource.org/licenses/eclipse-1.0.php>`_
-  Code-completion information is now saved in deltas instead of "saving
   only at shutdown" (being so, it does not loose information if it does
   not have a regular shut-down).
-  Added option for not using the smart-indent after opening brackets

New on: 0.9.8.3
~~~~~~~~~~~~~~~

-  Debugger was improved to be faster (more info about it `at the PyDev
   blog <http://pydev.blogspot.com/2005/10/high-speed-debugger.html>`_).
-  Add watch added to the editor popup menu
-  Added syntax highlighting to the 'self' token
-  Code folding added for 'glued' imports
-  Fixed some outline problems
-  Debugger does not try to get breakpoints on closed projects anymore
-  Some refreshing issues regarding the outline and colors when reusing
   the editor were fixed
-  Code completion for relative imports has changed a lot (there were
   some pretty hard-to-find bugs in this area...)
-  Some move imports problems fixed
-  The auto-add '(self):' now works with tabs too

New on: 0.9.8.2
~~~~~~~~~~~~~~~

-  `Content assistants <contentassist.html>`_ reviewed (and better
   documented on the homepage -- I really reccomend checking
   `it <contentassist.html>`_)
-  **Timeout parsing options added (this is available in the builder
   preferences page)**
-  **Auto-dedent added**
-  .pyc is removed when the corresponding .py file is removed.
-  Debugger has been changed so that it becomes faster (still not as
   fast as I would like, but still... faster)
-  Some escaped quotes problems fixed when formatting code
-  Navigation with Ctrl+Shift+ (up or down) has been slightly improved,
   so that it goes to the start or the end of the file when no other
   class or method definition is found
-  Other bug-fixes (as ususal)

New on 0.9.8.1
~~~~~~~~~~~~~~

-  **Java 1.4 support reintroduced**.
-  **Styles added for syntax highlighting (bold and italic), contributed
   by Gerhard Kalab**.
-  zombie process after exiting eclipse should not happen anymore
-  paths with '.' are accepted for the pythonpath (unless they start
   with a '.', because it may not accept relative paths).
-  relative imports are added to code-completion
-  local imports are taken into consideration when doing code completion
-  debugger has 'change support', so, changed variables in a scope
   appear red

New on 0.9.8
~~~~~~~~~~~~

-  jython integration supports spaces for jython.jar and java install
-  jython code-completion support for new style objects (jython 2.2a1)
   has been enhanced.
-  many templates were added
-  the grammar evolved a lot, so, now you actually have decorators in
   the grammar, list comprehension on method calls and tuples and the
   new from xxx import (a,b,c) syntax.
-  pylint supports spaces
-  pylint is no longer distributed with PyDev (it must be installed in
   the site-packages and its location must be specified in the
   preferences)
-  some problems regarding 'zombie processes' after eclipse exit with
   the shells used for code-completion should be fixed

New on 0.9.7.99
~~~~~~~~~~~~~~~

-  **PyDev has its first shot at Jython**. you should be able to use
   many things already, meaning: all the common editor features and code
   completion.
-  **The debugger is working**.
-  Code completion has been improved for supporting wild imports and
   relative imports better (sometimes it had some problems).
-  There are hovers for the text and annotations (when you pass the
   mouse through an error it will show its description).
-  Block comment (Ctrl+4) now uses the size defined for the print
   margin.
-  New block-comment style added (Ctrl+Shift+4).
-  New icons were created.
-  Many other bug-fixes as usual.

New on 0.9.7
~~~~~~~~~~~~

-  This release contains some high-priority bug fixes...

New on 0.9.6
~~~~~~~~~~~~

-  **Eclipse 3.1 is supported**
-  **Only java 5 is supported**
-  PyDev builder ignores team private members
-  Print Margin indicator now displays correctly
-  Help docs are shown again
-  Text editor configurations are inherited from the text editor (and
   just extended in the preferences)
-  Auto-close parentesis and 'eat colon' (courtesy from Karol Pietrzak)
-  Some more bugs...

New on 0.9.5
~~~~~~~~~~~~

-  **Last release with java 1.4 support**
-  File encodings now follow the python convention
-  Overview ruler now works
-  Editor is synchronized when working in multiple windows with the same
   file
-  Code folding improved
-  Syntax highlighting is not confused by escaped quote + triple quote
   anymore
-  Insertion of parentheses now replaces selected text

New on 0.9.4
~~~~~~~~~~~~

-  Nice PYTHONPATH configuration, and it is used for running your files,
   PyLint, code completion...
-  Integrated Scott Schleiser patches for the debugger (you won't see
   any 'implement me' anymore!).
-  Integrated Heikki Toivonen patch for `PyLint <pylint.html>`_ using
   the project pythonpath.
-  Integrated Heikki Toivonen patch for indentation after '(', '[' and
   '{' (if the line ends with a comma).
-  Some StackOverflow errors were removed from code completion.
-  Keybindings added for `Refactoring <refactoring.html>`_ (powered by
   bycicle repair man) - check the `FAQ <faq.html>`_.

New on 0.9.3
~~~~~~~~~~~~

-  Code completion is finished until 1.0 is released, so, if you\`re
   missing something, please report it!
-  New `Content Assistants <contentassist.html>`_ added. Not very well
   documented right now, but I'll do it later...
-  Removed dependecy on java.internals packages, so, this should solve
   the problems some people had when updating to 0.9.2
-  Latest PyLint integrated.

New on 0.9.2
~~~~~~~~~~~~

-  Scott Schleiser inside the editor... now on apply, it applies new
   settings, without restarting the editor.
-  Scott Schleiser inside the debugger... most 'implement me' and
   'volunteers needed' were fixed.
-  New `Content Assistants <contentassist.html>`_ added.
-  Docstrings in national encodings should work.
-  from foo import bar, xxx, yyy... should work.
-  Custom colors for decorators and numbers.
-  Matching brackets highlighted.
-  Bugs: code-formatting and others...

New on 0.9.1:
~~~~~~~~~~~~~

-  Ctrl+Shift+O: Organizes imports or sorts selection alphabetically
-  Ctrl+Shift+F: Autoformat your code (preferences can be set)
-  Namespace and PYTHONPATH now are the same for debug and run
-  `Code Completion <codecompletion.html>`_ has been improved:
   Parameters are gotten as completions, builtins like -- [], {} and ''
   -- return completions. Relative imports should be working (along with
   some other bug-fixes).
-  `PyLint 0.6.3 <pylint.html>`_ integrated.

New on 0.9.0:
~~~~~~~~~~~~~

-  `Code Completion <codecompletion.html>`_ bug for python 2.4 fixed.
-  `Code Completion <codecompletion.html>`_ has other bug-fixes solved,
   mostly due to imports that it was unable to find.
-  `PyLint 0.6 <pylint.html>`_ integrated.
-  New Python 2.4 syntax supported
-  PyDev builders can be disabled (NOTE: some features might not work
   when this is done - see the `FAQ <faq.html>`_).

New on 0.8.5:
~~~~~~~~~~~~~

-  Better `Code Completion <codecompletion.html>`_ *(Ctrl+Space)*
-  Watch in debugger.
-  Background and current line color chooser.

New on 0.8:
~~~~~~~~~~~

-  `PyLint integrated <pylint.html>`_
-  `TODO tasks <tasks.html>`_ supported

New on 0.7.1:
~~~~~~~~~~~~~

-  `Code Coverage <codecoverage.html>`_
-  `Run subset from dir <run.html>`_ (this is not unit-test, but it can
   be really useful if you do tests).
-  `New Content Assistants <contentassist.html>`_ *(Ctrl+1)*
-  Integrated new `refactorings <refactoring.html>`_ from `bicycle
   repair man <http://bicyclerepair.sourceforge.net/>`_ (inline local
   variable and extract local variable)

.. |images/index/code\_analysis.png| image:: images/index/code_analysis.png
.. |images/index/override\_methods.png| image:: images/index/override_methods.png
.. |images/index/rerun\_on\_change.png| image:: images/index/rerun_on_change.png

