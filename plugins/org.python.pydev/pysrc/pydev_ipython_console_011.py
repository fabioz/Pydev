"""Interface to TerminalInteractiveShell for PyDev Interactive Console frontend
   for IPython 0.11 to 1.0+.
"""

from __future__ import print_function
from IPython.core.error import UsageError
from IPython.core.inputsplitter import IPythonInputSplitter
from IPython.core.interactiveshell import InteractiveShell, InteractiveShellABC
try:
    from IPython.terminal.interactiveshell import TerminalInteractiveShell
except ImportError:
    # Versions of IPython [0.11,1.0) had an extra hierarchy level
    from IPython.frontend.terminal.interactiveshell import TerminalInteractiveShell
from IPython.utils.traitlets import CBool, Unicode

class PyDevTerminalInteractiveShell(TerminalInteractiveShell):
    # @todo banner2: put something nice here about PyDev (version number is available?)
    banner2 = Unicode('', config=True,
        help="""The part of the banner to be printed after the profile"""
    )
    # @todo editor
    # @todo pager
    # @todo term_title: (can PyDev's title be changed???, see terminal.py for where to inject code, in particular set_term_title as used by %cd)

    # Disable readline, readline type code is all handled by PyDev (on Java side)
    readline_use = CBool(False)

    # @todo colors_force: what should this be?
    # @todo need a show_in_pager hook installed, see page.py:page()

    # autoindent has no meaning in PyDev (PyDev always handles that on the Java side),
    # and attempting to enable it will print a warning in the absence of readline.
    autoindent = CBool(False)

    # @todo connect gui to PyDev's enable GUI code
    # @todo __init__: review which system should be used (piped or raw), (perhaps test with alias ff find / ; ff)


    #-------------------------------------------------------------------------
    # Things related to exceptions
    #-------------------------------------------------------------------------

    def showtraceback(self, exc_tuple=None, filename=None, tb_offset=None,
                  exception_only=False):
        # IPython does a lot of clever stuff with Exceptions. However mostly
        # it is related to IPython running in a terminal instead of an IDE.
        # (e.g. it prints out snippets of code around the stack trace)
        # PyDev does a lot of clever stuff too, so leave exception handling
        # with default print_exc that PyDev can parse and do its clever stuff
        # with (e.g. it puts links back to the original source code)
        import traceback;traceback.print_exc()

    #-------------------------------------------------------------------------
    # Things related to the terminal
    #-------------------------------------------------------------------------

    @property
    def usable_screen_length(self):
        # @todo we shouldn't reach here when using PyDev, before commit change remove exception
        raise Exception("usable_screen_length isn't relevant to PyDev, ")
        return 0

    #-------------------------------------------------------------------------
    # Things related to aliases
    #-------------------------------------------------------------------------

    def init_alias(self):
        # InteractiveShell defines alias's we want, but TerminalInteractiveShell defines
        # ones we don't. So don't use super and instead go right to InteractiveShell
        InteractiveShell.init_alias(self)

    #-------------------------------------------------------------------------
    # Things related to exiting
    #-------------------------------------------------------------------------
    def ask_exit(self):
        """ Ask the shell to exit. Can be overiden and used as a callback. """
        # @todo I need a way to exit???
        super(PyDevTerminalInteractiveShell, self).ask_exit()

    #-------------------------------------------------------------------------
    # Things related to magics
    #-------------------------------------------------------------------------

    def init_magics(self):
        super(PyDevTerminalInteractiveShell, self).init_magics()
        # @todo Any additional magics for PyDev?

InteractiveShellABC.register(PyDevTerminalInteractiveShell)  # @UndefinedVariable

#=======================================================================================================================
# PyDevFrontEnd
#=======================================================================================================================
class PyDevFrontEnd:

    def __init__(self, *args, **kwargs):
        # Create and initialize our IPython instance.
        self.ipython = PyDevTerminalInteractiveShell.instance()
        # Create an input splitter to handle input separation
        self.input_splitter = IPythonInputSplitter()

    def complete(self, string):
        return self.ipython.complete(None, line=string)

    def getNamespace(self):
        return self.ipython.user_ns

    def addExec(self, line):
        self.input_splitter.push(line)
        if not self.input_splitter.push_accepts_more():
            self.ipython.run_cell(self.input_splitter.source_reset())
            return False
        else:
            return True
