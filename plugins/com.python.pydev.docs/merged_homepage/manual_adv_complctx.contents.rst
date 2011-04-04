Code Completion
================

.. contents::


Requesting Code Completion
--------------------------

Code completion provides context-sensitive completions and is enabled with **Ctrl+Space**. (Note that eclipse has simple 
emacs-style text-completion with **Alt+/**).

It's important to note that your interpreter must be properly configured for the code-completion to work, and for getting
the **builtins**, PyDev spawns a shell, so, having a **firewall can prevent the code-completion from working** if it's
blocking the communication from eclipse to that shell.


Preferences
------------

If you want to configure something, you have to go to
**window > preferences > PyDev > Editor > Code Completion**.

.. image:: images/codecompletion/codecompletionpreferences.png
   :class: snap
   

Snapshots
-----------


Completing on a variable on the class (also works for locals) that are defined in the same scope we are.
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. image:: images/codecompletion/codecompletionattr1.png
   :class: snap


Getting the builtins.
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. image:: images/codecompletion/codecompletionbuiltins.png
   :class: snap


Completing on a class (note that we get the hierarchy even from builtins).
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. image:: images/codecompletion/codecompletionhierarchy1.png
   :class: snap


Completing for making an import (goes for PYTHONPATH)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. image:: images/codecompletion/compl2.png
   :class: snap


Completing on an import
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. image:: images/codecompletion/compl3.png
   :class: snap


Completing for global tokens (handles wild-imports, local imports, local variables, etc.)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. image:: images/codecompletion/compl4.png
   :class: snap



