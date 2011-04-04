PyDev Editor Preferences
=========================

.. contents:: 
    

Preferences
-------------------------


The options in the preferences should be self-explanatory, so, they're only briefly detailed here, 
but if you don't understand one, please ask in the users forum (so that the spelling of the option is improved).


Editor
-------
In **window > preferences > PyDev > editor**, PyDev can edit the tab settings and appearance colors used in PyDev 
(such as the code color, decorators, numbers, strings, comments, etc).

It's important to have in mind that many preferences, 
such as print margin, show line number, background/foreground colors, etc. are inherited from 
the text-editor preferences at **general > editors > text editors**, and 
some other preferences are at **general > appearance > colors and fonts** -- 
-- there's a link for both at the end of the preferences page.


.. image:: images/editor/editor_preferences.png
   :class: snap



Code Completion
----------------

In code completion preferences, configure how you want the code-completion to work.


.. image:: images/editor/code_completion_preferences.png
   :class: snap


Code Folding
----------------

Code-folding: new editors will have it applied.


.. image:: images/editor/code_folding_preferences.png
   :class: snap


Code Style
----------------
Code-style: Choose how you want the **assign to variable quick assist assign** to work (Ctrl+1): with camel case variables or
variables separated with underline (an example is shown when you change it).


.. image:: images/editor/code_style_preferences.png
   :class: snap



Block Comments
----------------
Block comments are comments formatted in a special way. An example of applying the block comment is shown
(2 types of block comments are available: a multi-line and a single line comment).


.. image:: images/editor/block_comments_preferences.png
   :class: snap


Code Formatter
----------------
In the code-formatter preferences page, you can choose different ways of having your code formatted.

.. image:: images/editor/code_formatter_preferences.png
   :class: snap


Docstrings
----------------
With Ctrl+1 when over a function line, you can create the docstring automatically (and these preferences
are used to determine what's the result of doing so)

.. image:: images/editor/docstring_preferences.png
   :class: snap


File Types
----------------
The file types indicate which file extensions are recognized for the type inference engine (it's not an association
to the file editor)

.. image:: images/editor/file_type_preferences.png
   :class: snap


Imports
----------------
Ctrl+Shift+O can organize the available imports (when no selection is done -- if done over a selection it'll do a 
regular text sort over the selected text), and those preferences indicate how the available imports should be organized.

.. image:: images/editor/import_preferences.png
   :class: snap


Hover
----------------
What to show on the mouse hover?

.. image:: images/editor/hover_preferences.png
   :class: snap


Templates
----------------
Here you can enter new templates. There are 2 contexts, the "Editor" and the "New Module". The templates in the
"Editor" context are available for code-completion and the ones with "New Module" are available for the creation
of new modules. 

.. image:: images/editor/template_preferences.png
   :class: snap


Typing
----------------
The typing preferences indicate what should be automatically entered when you're typing text (e.g.: automatic parenthesis,
smart indent, etc).


.. image:: images/editor/typing_preferences.png
   :class: snap
