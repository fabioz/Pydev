..
    <right_area>
    <p><strong>General questions:</strong><br/>
    &nbsp;&nbsp;&nbsp;&nbsp;<a href="http://stackoverflow.com/questions/tagged/pydev">StackOverflow (with the PyDev tag)</a>.</p><br/>

    <p><strong>Report issues/features: </strong><br/>
    &nbsp;&nbsp;&nbsp;&nbsp;<a href="https://sw-brainwy.rhcloud.com">Brainwy Tracker</a><br/><br/>
    </p>


    <p><strong>Code questions:</strong><br/>
    &nbsp;&nbsp;&nbsp;&nbsp;<a href="http://lists.sourceforge.net/lists/listinfo/pydev-code">pydev-code list</a><br/><br/></p>

    <p><strong>Source Code:</strong><br/>
    &nbsp;&nbsp;&nbsp;&nbsp;<a href="https://github.com/fabioz/Pydev">github.com/fabioz/Pydev</a></p><br/>

    <p><strong>Blog:</strong><br/>
    &nbsp;&nbsp;&nbsp;&nbsp;<a href="http://pydev.blogspot.com">pydev.blogspot.com</a></p><br/>

    <p><strong>E-mail:</strong><br/>
    Please use this only if you <strong>really</strong> can't make your comments public: fabioz.pydev at gmail (.com)</p>

    <br/>

    </right_area>
    <image_area>about.png</image_area>
    <quote_area>So, what about it?</quote_area>


**PyDev** is a trademark of **Appcelerator**. Although it's no longer
supported by Appcelerator, it's kept being developed (as open source)
by **Fabio Zadrozny** with financial support from the PyDev user community
and corporate sponsors.

**License: EPL (Eclipse Public License)**

See: `http://www.eclipse.org/legal/epl-v10.html <http://www.eclipse.org/legal/epl-v10.html>`_



Corporate sponsorship
-----------------------

PyDev is Open Source software and depends on contributions from its users
to remain financially viable. For companies that use PyDev, it's possible to financially 
support it through corporate sponsorship.


How to become a corporate sponsor
----------------------------------

Sponsorship is available for all companies worldwide. The basic idea is that
a company finances the work on a particular feature of PyDev -- for which
a proper invoice is provided -- the code is done and is integrated in PyDev
under the EPL license.

That way the company gets its favorite missing feature, it's later kept supported 
in the development mainline and everyone benefits from the work done.

All negotiations are kept strictly confidential. For inquiries, please contact:

fabioz.pydev at gmail.com. 


Open Sponsorships
------------------

Below is a (non-exhaustive) list of some features in need of sponsorship:


Virtualenv and pip support
----------------------------

It's already possible to use virtualenv and pip from the command line and later configure PyDev for the created
virtualenv interpreter, but this should be easier: it should be possible to have a UI which uses
virtualenv and pip behind the scenes to do the heavy lifting, making it possible to create a new
virtualenv from inside PyDev, while using pip to add/remove packages in the created virtualenv.


Improve rename refactoring support
-------------------------------------
 
The current refactoring support is in need of a refactoring itself in order to work better with renames.
There are some cases where it may not work properly, and it currently does not deal properly with modules 
(currently modules are not renamed as a part of applying the rename refactoring).


Provide context-insensitive code-completion for builtins
----------------------------------------------------------

PyDev can already analyze modules dynamically for code-completion, but those completions do not appear
as part of a Ctrl+1 quick fix for import or in the list of context-insensitive code-completions.

I.e.: when the QPixmap token (which is part of PyQt4.QtGui) is not defined, Ctrl+1 will not show a proper
quick-fix for it as it would if it was actually a source module and not a token in a dll.


Support for namespace packages
-------------------------------

Currently, the PyDev static source analyzer does not deal properly with namespace packages. A current workaround
is possible by manually adding modules to the forced builtins to force it to be analyzed by a shell, but a proper
fix (which is non-trivial) should be done in the static analyzer.


Improved code-formatting
-------------------------

The current code-formatter structure is very simple, working on the text-level and is not able to act with 
knowledge on the code (i.e.: to wrap/unwrap lines, fix spaces before/after methods, update indentation to 
match current settings, etc). So, this sponsorship is for providing a new formatter which is able to work 
with more information and provide additional code-formatting options.

