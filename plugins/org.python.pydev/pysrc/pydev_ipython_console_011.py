# TODO that would make IPython integration better
# - show output other times then when enter was pressed
# - support proper exit to allow IPython to cleanup (e.g. temp files created with %edit)
# - support Ctrl-D (Ctrl-Z on Windows)
# - use IPython (numbered) prompts in PyDev
# - better integration of IPython and PyDev completions
# - some of the semantics on handling the code completion are not correct:
#   eg: Start a line with % and then type c should give %cd as a completion by it doesn't
#       however type %c and request completions and %cd is given as an option
#   eg: Completing a magic when user typed it without the leading % causes the % to be inserted
#       to the left of what should be the first colon.
"""Interface to TerminalInteractiveShell for PyDev Interactive Console frontend
   for IPython 0.11 to 1.0+.
"""

from __future__ import print_function

import os

from IPython.core.error import UsageError
from IPython.core.inputsplitter import IPythonInputSplitter
from IPython.core.completer import IPCompleter
from IPython.core.interactiveshell import InteractiveShell, InteractiveShellABC
from IPython.core.usage import default_banner_parts
from IPython.utils.strdispatch import StrDispatch
import IPython.core.release as IPythonRelease
try:
    from IPython.terminal.interactiveshell import TerminalInteractiveShell
except ImportError:
    # Versions of IPython [0.11,1.0) had an extra hierarchy level
    from IPython.frontend.terminal.interactiveshell import TerminalInteractiveShell
from IPython.utils.traitlets import CBool, Unicode

from pydev_imports import xmlrpclib

pydev_banner_parts = [
    '\n',
    'PyDev -- Python IDE for Eclipse\n',  # TODO can we get a version number in here?
    'For help on using PyDev\'s Console see http://pydev.org/manual_adv_interactive_console.html\n',
]

default_pydev_banner_parts = default_banner_parts + pydev_banner_parts

default_pydev_banner = ''.join(default_pydev_banner_parts)

def show_in_pager(self, strng):
    """ Run a string through pager """
    # On PyDev we just output the string, there are scroll bars in the console
    # to handle "paging". This is the same behaviour as when TERM==dump (see
    # page.py)
    print(strng)

def create_editor_hook(pydev_host, pydev_client_port):
    def call_editor(self, filename, line=0, wait=True):
        """ Open an editor in PyDev """
        if line is None:
            line = 0

        # Make sure to send an absolution path because unlike most editor hooks
        # we don't launch a process. This is more like what happens in the zmqshell
        filename = os.path.abspath(filename)

        # Tell PyDev to open the editor
        server = xmlrpclib.Server('http://%s:%s' % (pydev_host, pydev_client_port))
        server.OpenEditor(filename, line)

        if wait:
            try:
                raw_input("Press Enter when done editing:")
            except NameError:
                input("Press Enter when done editing:")
    return call_editor



class PyDevIPCompleter(IPCompleter):

    def __init__(self, *args, **kwargs):
        """ Create a Completer that reuses the advanced completion support of PyDev
            in addition to the completion support provided by IPython """
        IPCompleter.__init__(self, *args, **kwargs)
        # Use PyDev for python matches, see getCompletions below
        self.matchers.remove(self.python_matches)

class PyDevTerminalInteractiveShell(TerminalInteractiveShell):
    banner1 = Unicode(default_pydev_banner, config=True,
        help="""The part of the banner to be printed before the profile"""
    )

    # TODO term_title: (can PyDev's title be changed???, see terminal.py for where to inject code, in particular set_term_title as used by %cd)
    # for now, just disable term_title
    term_title = CBool(False)

    # Note in version 0.11 there is no guard in the IPython code about displaying a
    # warning, so with 0.11 you get:
    #  WARNING: Readline services not available or not loaded.
    #  WARNING: The auto-indent feature requires the readline library
    # Disable readline, readline type code is all handled by PyDev (on Java side)
    readline_use = CBool(False)
    # autoindent has no meaning in PyDev (PyDev always handles that on the Java side),
    # and attempting to enable it will print a warning in the absence of readline.
    autoindent = CBool(False)
    # Force console to not give warning about color scheme choice and default to NoColor.
    # TODO It would be nice to enable colors in PyDev but:
    # - The PyDev Console (Eclipse Console) does not support the full range of colors, so the
    #   effect isn't as nice anyway at the command line
    # - If done, the color scheme should default to LightBG, but actually be dependent on
    #   any settings the user has (such as if a dark theme is in use, then Linux is probably
    #   a better theme).
    colors_force = CBool(True)
    colors = Unicode("NoColor")

    # In the PyDev Console, GUI control is done via hookable XML-RPC server
    @staticmethod
    def enable_gui(gui=None, app=None):
        """Switch amongst GUI input hooks by name.
        """
        # Deferred import
        from pydev_ipython.inputhook import enable_gui as real_enable_gui
        try:
            return real_enable_gui(gui, app)
        except ValueError as e:
            raise UsageError("%s" % e)

    #-------------------------------------------------------------------------
    # Things related to hooks
    #-------------------------------------------------------------------------

    def init_hooks(self):
        super(PyDevTerminalInteractiveShell, self).init_hooks()
        self.set_hook('show_in_pager', show_in_pager)

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
    # Things related to text completion
    #-------------------------------------------------------------------------

    # The way to construct an IPCompleter changed in most versions,
    # so we have a custom, per version implementation of the construction

    def _new_completer_011(self):
        return PyDevIPCompleter(self,
                             self.user_ns,
                             self.user_global_ns,
                             self.readline_omit__names,
                             self.alias_manager.alias_table,
                             self.has_readline)


    def _new_completer_012(self):
        completer = PyDevIPCompleter(shell=self,
                             namespace=self.user_ns,
                             global_namespace=self.user_global_ns,
                             alias_table=self.alias_manager.alias_table,
                             use_readline=self.has_readline,
                             config=self.config,
                             )
        self.configurables.append(completer)
        return completer


    def _new_completer_100(self):
        completer = PyDevIPCompleter(shell=self,
                             namespace=self.user_ns,
                             global_namespace=self.user_global_ns,
                             alias_table=self.alias_manager.alias_table,
                             use_readline=self.has_readline,
                             parent=self,
                             )
        self.configurables.append(completer)
        return completer

    def _new_completer_200(self):
        # As of writing this, IPython 2.0.0 is in dev mode so subject to change
        completer = PyDevIPCompleter(shell=self,
                             namespace=self.user_ns,
                             global_namespace=self.user_global_ns,
                             use_readline=self.has_readline,
                             parent=self,
                             )
        self.configurables.append(completer)
        return completer



    def init_completer(self):
        """Initialize the completion machinery.

        This creates a completer that provides the completions that are
        IPython specific. We use this to supplement PyDev's core code
        completions.
        """
        # PyDev uses its own completer and custom hooks so that it uses
        # most completions from PyDev's core completer which provides
        # extra information.
        # See getCompletions for where the two sets of results are merged

        from IPython.core.completerlib import magic_run_completer, cd_completer
        try:
            from IPython.core.completerlib import reset_completer
        except ImportError:
            # reset_completer was added for rel-0.13
            reset_completer = None

        if IPythonRelease._version_major >= 2:
            self.Completer = self._new_completer_200()
        elif IPythonRelease._version_major >= 1:
            self.Completer = self._new_completer_100()
        elif IPythonRelease._version_minor >= 12:
            self.Completer = self._new_completer_012()
        else:
            self.Completer = self._new_completer_011()

        # Add custom completers to the basic ones built into IPCompleter
        sdisp = self.strdispatchers.get('complete_command', StrDispatch())
        self.strdispatchers['complete_command'] = sdisp
        self.Completer.custom_completers = sdisp

        self.set_hook('complete_command', magic_run_completer, str_key='%run')
        self.set_hook('complete_command', cd_completer, str_key='%cd')
        if reset_completer:
            self.set_hook('complete_command', reset_completer, str_key='%reset')

        # Only configure readline if we truly are using readline.  IPython can
        # do tab-completion over the network, in GUIs, etc, where readline
        # itself may be absent
        if self.has_readline:
            self.set_readline_completer()


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
        # TODO PyDev's console does not have support from the Python side to exit
        # the console. If user forces the exit (with sys.exit()) then the console
        # simply reports errors. e.g.:
        # >>> import sys
        # >>> sys.exit()
        # Failed to create input stream: Connection refused
        # >>>
        # Console already exited with value: 0 while waiting for an answer.
        # Error stream:
        # Output stream:
        # >>>
        #
        # Alternatively if you use the non-IPython shell this is what happens
        # >>> exit()
        # <type 'exceptions.SystemExit'>:None
        # >>>
        # <type 'exceptions.SystemExit'>:None
        # >>>
        #
        super(PyDevTerminalInteractiveShell, self).ask_exit()
        print('To exit the PyDev Console, terminate the console within Eclipse.')

    #-------------------------------------------------------------------------
    # Things related to magics
    #-------------------------------------------------------------------------

    def init_magics(self):
        super(PyDevTerminalInteractiveShell, self).init_magics()
        # TODO Any additional magics for PyDev?

InteractiveShellABC.register(PyDevTerminalInteractiveShell)  # @UndefinedVariable

#=======================================================================================================================
# PyDevFrontEnd
#=======================================================================================================================
class PyDevFrontEnd:

    def __init__(self, pydev_host, pydev_client_port, *args, **kwarg):

        # Create and initialize our IPython instance.
        self.ipython = PyDevTerminalInteractiveShell.instance()

        # Back channel to PyDev to open editors (in the future other
        # info may go back this way. This is the same channel that is
        # used to get stdin, see StdIn in pydev_console_utils)
        self.ipython.set_hook('editor', create_editor_hook(pydev_host, pydev_client_port))

        # Create an input splitter to handle input separation
        self.input_splitter = IPythonInputSplitter()

        # Display the IPython banner, this has version info and
        # help info
        self.ipython.show_banner()

    def complete(self, string):
        return self.ipython.complete(None, line=string)

    def getCompletions(self, text, act_tok):
        # Get completions from IPython and from PyDev and merge the results
        # IPython only gives context free list of completions, while PyDev
        # gives detailed information about completions.
        try:
            TYPE_IPYTHON = '11'
            TYPE_IPYTHON_MAGIC = '12'
            _line, ipython_completions = self.complete(text)

            from _pydev_completer import Completer
            completer = Completer(self.getNamespace(), None)
            ret = completer.complete(act_tok)
            append = ret.append
            ip = self.ipython
            pydev_completions = set([f[0] for f in ret])
            for ipython_completion in ipython_completions:
                if ipython_completion not in pydev_completions:
                    pydev_completions.add(ipython_completion)
                    inf = ip.object_inspect(ipython_completion)
                    if inf['type_name'] == 'Magic function':
                        pydev_type = TYPE_IPYTHON_MAGIC
                    else:
                        pydev_type = TYPE_IPYTHON
                    pydev_doc = inf['docstring']
                    if pydev_doc is None:
                        pydev_doc = ''
                    append((ipython_completion, pydev_doc, '', pydev_type))
            return ret
        except:
            import traceback;traceback.print_exc()
            return []


    def getNamespace(self):
        return self.ipython.user_ns

    def addExec(self, line):
        self.input_splitter.push(line)
        if not self.input_splitter.push_accepts_more():
            self.ipython.run_cell(self.input_splitter.source_reset(), store_history=True)
            return False
        else:
            return True

# If we have succeeded in importing this module, then monkey patch inputhook
# in IPython to redirect to PyDev's version. This is essential to make
# %gui in 0.11 work (0.12+ fixes it by calling self.enable_gui, which is implemented
# above, instead of inputhook.enable_gui).
# See testGui (test_pydev_ipython_011.TestRunningCode) which fails on 0.11 without
# this patch
import IPython.lib.inputhook
import pydev_ipython.inputhook
IPython.lib.inputhook.enable_gui = pydev_ipython.inputhook.enable_gui
# In addition to enable_gui, make all publics in pydev_ipython.inputhook replace
# the IPython versions. This enables the examples in IPython's examples/lib/gui-*
# to operate properly because those examples don't use %gui magic and instead
# rely on using the inputhooks directly.
for name in pydev_ipython.inputhook.__all__:
    setattr(IPython.lib.inputhook, name, getattr(pydev_ipython.inputhook, name))
