Code Completion
===============

Requesting Code Completion
==========================

Code completion provides context-sensitive completions and is enabled
with **Ctrl+Space**. (Note that eclipse has simple emacs-style
text-completion with **Alt+/**).

It's important to note that your interpreter must be properly configured
for the code-completion to work, and for getting the **builtins**, PyDev
spawns a shell, so, having a **firewall can prevent the code-completion
from working** if it's blocking the communication from eclipse to that
shell.

Preferences
===========

If you want to configure something, you have to go to **window >
preferences > PyDev > Editor > Code Completion**.

.. figure:: images/codecompletion/codecompletionpreferences.png
   :align: center

Snapshots
=========

Completing on a variable on the class (also works for locals) that are defined in the same scope we are.
========================================================================================================

.. figure:: images/codecompletion/codecompletionattr1.png
   :align: center

Getting the builtins.
=====================

.. figure:: images/codecompletion/codecompletionbuiltins.png
   :align: center

Completing on a class (note that we get the hierarchy even from builtins).
==========================================================================

.. figure:: images/codecompletion/codecompletionhierarchy1.png
   :align: center

Completing for making an import (goes for PYTHONPATH)
=====================================================

.. figure:: images/codecompletion/compl2.png
   :align: center

Completing on an import
=======================

.. figure:: images/codecompletion/compl3.png
   :align: center

Completing for global tokens (handles wild-imports, local imports, local variables, etc.)
===========================================================================================

.. figure:: images/codecompletion/compl4.png
   :align: center



