'''
$Header: http://subversion/SVN_MSS/python/mss/core/trunk/core/pyrun/eclipse_config/pydev/jysrc/pyedit_exec_line_in_shell.py 5857 2012-04-27 09:22:20Z anroberts $

PyDev plugin to send lines from the editor to a python console.
'''
# This is the command ID as specified in plugin.xml
COMMAND_ID = "com.mi.ahl.eclipse.python.execLineInConsole"

#
# Required Locals
#
# interface: String indicating which command will be executed
# As this script will be watching the PyEdit (that is the actual editor in Pydev), and this script
# will be listening to it, this string can indicate any of the methods of org.python.pydev.editor.IPyEditListener
assert cmd is not None

#interface: PyEdit object: this is the actual editor that we will act upon assert editor is not None
assert editor is not None

import sys
print 'Command: ' + cmd
print 'File: ' + editor.getEditorFile().getName()
print 'sys.version:' + sys.version

from org.eclipse.jface.action import Action
from org.eclipse.jface.dialogs import MessageDialog
from org.eclipse.jface.text import IDocumentListener
from org.eclipse.core.runtime.jobs import Job
from org.eclipse.ui.progress import UIJob
from org.eclipse.core.runtime import Status
from org.eclipse.ui.contexts import IContextService

from org.python.pydev.core.docutils import PySelection
from org.python.pydev.debug.newconsole import PydevConsoleConstants
from org.python.pydev.debug.newconsole import PydevConsoleFactory
from org.python.pydev.debug.newconsole import PydevConsole
from org.python.pydev.editor.actions import PyAction

from org.eclipse.ui import PlatformUI
from org.eclipse.ui.console import ConsolePlugin
from org.eclipse.ui.console import IConsoleConstants
from java.lang import Runnable
from org.eclipse.swt.widgets import Display

import re
RE_COMMENT = re.compile('^\s*#')
RE_BLOCK_CONTINUATION = re.compile("^\s*(else|elif|except|finally).*:\s*$")


class ConsoleDocumentListener(IDocumentListener):
    def __init__(self, execution_engine):
        self.execution_engine = execution_engine
        self.new_prompt = False

    def documentAboutToBeChanged(self,event):
        pass

    def documentChanged(self,event):
        if (self.new_prompt and len(event.getText())== 0 and self.lines_to_process == 0) or self.lines_to_process < 0 :  
            self.new_prompt = False        
            self.execution_engine.complete_top_command()
        else :
            self.new_prompt = event.getText() == '>>> ' or event.getText() == '... '
            if self.new_prompt :
                self.lines_to_process = self.lines_to_process - 1
                
class LinesCursor(object):
    """Cursor object to iterate over selected lines"""
    def __init__(self, lines):
        self._lines = lines
        self._cursor = 0
    def get_line(self):
        '''Find the current line, if we've already passed the end of the selection just return empty lines'''
        if self.is_complete():
            return ""
        else:
            return self._lines[self._cursor]
    def goto_next_line(self):
        '''
        Find the next line. Return if there was a new line to traverse to.
        '''
        self._cursor += 1
        return not self.is_complete()
    def is_complete(self):
        return self._cursor >= len(self._lines)

class SourceCursor(object):
    """Cursor object to iterate over all lines the editor"""
    def __init__(self, editor):
        self._editor = editor
    def get_line(self):
        '''Find the current line'''
        selection = PySelection(self._editor).getLine()
        # strip tailing whitespace
        return selection.rstrip()
    def goto_next_line(self):
        '''
        Find the next line. Return if there was a new line to traverse to.
        Note: the selection system appears to wrap around to the beginning if 
        the line is incremented past the end. No user wants to go back to imports 
        once they've completed their step-through, so we protect against that.
        '''
        # skip cursor to next line
        oSelection = PySelection(self._editor)
        current_line = oSelection.getCursorLine()
        last_line = oSelection.getDoc().getNumberOfLines()-1
        offset = oSelection.getLineOffset(current_line + 1)
        if current_line == last_line:
            return False
        self._editor.setSelection(offset, 0)
        return True

class DoTopCommandJob(UIJob):  
    def __init__(self, executor) :
        UIJob.__init__(self, 'do top command')
        self.executor = executor
        self.setPriority(Job.SHORT)

    def runInUIThread(self, progress_monitor):
        try:
            self.executor._do_top_command()
        except:
            # This can be benign if, e.g. the Script Console has focus.
            pass
        return Status.OK_STATUS

class ExecuteLine(Action):
    '''Code to execute a line'''

    def __init__(self, editor=None):
        Action.__init__(self)
        self._editor = editor
        self._console = None
        self._console_listener = ConsoleDocumentListener(self)
        self._commands = []
        self._cursor = SourceCursor(editor)
        self._in_block = False
        self._base_indent = 0

    def _find_top_console(self):
        def isConsoleView(view):
            if view is None:
                return False
            return view.getId() == IConsoleConstants.ID_CONSOLE_VIEW

        window = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
        if window is None:
             return None
        page = window.getActivePage()
        if page is None:
            return None
        views = page.getViewReferences()

        cviews = map(lambda v: v.getView(True), filter(isConsoleView, views))
        if not len(cviews):
            return None
        return cviews[0].getConsole()

    def _show_console(self):
        def isPyDevConsole(console):
            if console is None:
                return False
            return console.getType() == PydevConsoleConstants.CONSOLE_TYPE
        
        console_manager = ConsolePlugin.getDefault().getConsoleManager()
        consoles = console_manager.getConsoles()

        top_console = self._find_top_console()

        if  isPyDevConsole(top_console):
            self._console = top_console
        elif self._console not in consoles:
            pydev_consoles = filter(isPyDevConsole, consoles)
            if len(pydev_consoles):
                self._console = pydev_consoles[0]
            else:
                self._console = PydevConsoleFactory().createConsole('')

        console_manager.showConsoleView(self._console)

    def _get_newline(self):
        return PyAction.getDelimiter(self._editor.getDocument())

    def _get_selection(self):
        return PySelection(self._editor).getSelectedText()

    def _send_to_console(self, text):
        if len(text.rstrip()):
            self._commands.append( text ) 
            if len(self._commands)==1:
                job = DoTopCommandJob(self)
                job.schedule()
                
    def _do_top_command(self):
        document = self._console.getDocument()
        text = self._commands[0]
        document.addDocumentListener(self._console_listener)
        self._console_listener.lines_to_process = text.count('\n')
        document.replace(document.getLength(), 0, text)

    def complete_top_command(self):
        self._console.getDocument().removeDocumentListener(self._console_listener)
        self._commands = self._commands[1:]
        if len(self._commands) > 0 :
            job = DoTopCommandJob(self)
            job.schedule()

    def _reset_line_state(self):
        self._in_block = False
        self._base_indent = 0

    def _should_skip(self, line):
        return len(line.strip()) == 0 or RE_COMMENT.match(line)

    def _run_selection_mode(self, selection):
        '''User has selected a block of text and hit F1'''
        self._reset_line_state()

        # get the lines and remove any empty lines from the start and end
        lines = selection.splitlines()
        while lines:
            if lines[0].strip():
                break
            lines.pop(0)

        while lines:
            if lines[-1].strip():
                break
            lines.pop()

        # don't do anything if no non-blank lines were selected
        if not lines:
            return
        
        cursor = LinesCursor(lines)
        while not cursor.is_complete():
            self._run_line_mode(cursor)
            

    def _run_line_mode(self, cursor):
        '''User is running through the code line by line'''
        # Save away the current line which we'll send to the console
        # and remove any non-block level indentation (i.e. when copying
        # code that's indented in the editor it needs to be shifted left
        # so the indentation is correct in the console).
        current_line = cursor.get_line()
            
        # If the user has F1ed a comment do nothing except moving them on to the next line
        if self._should_skip(current_line):
            cursor.goto_next_line()
            return
            
        if not self._in_block:
            self._base_indent = len(current_line) - len(current_line.lstrip()) 
        current_line = current_line[self._base_indent:]

        # Skip through to the next non-blank line
        cursor.goto_next_line()
        next_line = cursor.get_line()
        while self._should_skip(next_line) and cursor.goto_next_line():
            next_line = cursor.get_line()

        # Look-ahead to see if we're stepping into or out of a block
        # This is determined by indentation change, but not if the line
        # is closing an list/tuple or dict block, or is continued with a \.
        next_indent = len(next_line) - len(next_line.lstrip())
        if next_indent > self._base_indent:
            self._in_block = True
        if self._in_block and next_indent <= self._base_indent:
            end_of_block = True
            if next_line \
            and (next_line.strip()[-1] in ")]}"
                 or next_line.endswith("\\")
                 or RE_BLOCK_CONTINUATION.match(next_line)):
                    end_of_block = False

            if end_of_block:
                # We"ve finished a block - need to send 2 newlines to IPython to tell it to
                # close the block. Don"t do this though if we"re tracking the same level
                # of indentation.
                self._in_block = False
                current_line += self._get_newline()

        # send command to console
        current_line += self._get_newline()
        self._send_to_console(current_line)

    def run(self):
        self._show_console()

        selection = self._get_selection()

        if not len(selection) == 0:
            # User has selected a block of text
            self._run_selection_mode(selection)
        else:
            # User has no selection, use line-by-line mode
            self._run_line_mode(self._cursor)

    def unhook(self):
        if self._console:
            self._console.getDocument().removeDocumentListener(self._console_listener)
        
def bindInInterface():
    # Cribbed from http://eclipse-pydev.sourcearchive.com/documentation/1.2.5/pyedit__next__problem_8py-source.html
    # bind the action to some internal definition
    action = ExecuteLine(editor)


    # The plugin.xml file defined a command and a binding with the string from COMMAND_ID.
    # by seting the action definition id and the id itself, we will bind this command to the keybinding defined
    # (this is the right way of doing it, as it will enter the abstractions of Eclipse and allow the user to
    # later change that keybinding).
    action.setActionDefinitionId(COMMAND_ID)
    action.setId(COMMAND_ID)

    try:
        #We're starting in a thread, so, it may be closed before
        #we've the change to bind it
        last_execute_line = editor.getAction(COMMAND_ID)
        if last_execute_line :
            last_execute_line.unhook()
        editor.setAction(COMMAND_ID, action)
        last_execute_line = editor.getAction(COMMAND_ID)
    except Exception, e:
        pass

class RunInUi(Runnable):
    '''
    Helper class that implements a Runnable (just so that we
    can pass it to the Java side). It simply calls some callable.
    '''
    def __init__(self, c):
        self.callable = c

    def run(self):
        self.callable()

def runInUi(c):
    '''
    @param c: the callable that will be run in the UI
    '''
    Display.getDefault().asyncExec(RunInUi(c))

if cmd == 'onCreateActions' or cmd == 'onSave' or cmd == 'onSetDocument':
    # This chain of crazy bind calls allows us to bind this command to a key. Go figure ;)
    runInUi(bindInInterface)
