# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation; either version 2 of the License, or (at your option) any later
# version.
#
# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along with
# this program; if not, write to the Free Software Foundation, Inc.,
# 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
""" Copyright (c) 2002-2003 LOGILAB S.A. (Paris, FRANCE).
 http://www.logilab.fr/ -- mailto:contact@logilab.fr

 
 Command line interface helper classes.
 
 It provides some default commands, a help system, a default readline
 configuration with completion and persistent history
"""

__revision__ = "$Id: cli.py,v 1.3 2005-01-21 17:42:05 fabioz Exp $"


if not __builtins__.has_key('_'):
    _ = str

def init_readline(complete_method, histfile=None):
    """init the readline library if available"""
    try:
        import readline
        readline.parse_and_bind("tab: complete")
        readline.set_completer(complete_method)
        string = readline.get_completer_delims().replace(':', '')
        readline.set_completer_delims(string)
        if histfile is not None:
            try:
                readline.read_history_file(histfile)
            except IOError:
                pass
            import atexit
            atexit.register(readline.write_history_file, histfile)
    except:
        print 'readline si not available :-('


class Completer :
    """readline completer"""
    
    def __init__(self, commands):
        self.list = commands
        
    def complete(self, text, state):
        """hook called by readline when <tab> is pressed"""
        n = len(text)
        matches = []
        for cmd in self.list :
            if cmd[:n] == text :
                matches.append(cmd)
        try:
            return matches[state]
        except IndexError:
            return None


class CLIHelper:
    """ an abstract command line interface client which recognize commands
    and provide an help system
    """
    
    CMD_MAP = {'help' : _("Others"),
               'quit' : _("Others"),
               }
    CMD_PREFIX = ''
    
    def __init__(self, histfile=None) :
        self._topics = {}
        self.commands = None
        self._completer = Completer(self._register_commands())
        init_readline(self._completer.complete, histfile)

    def run(self):
        """loop on user input, exit on EOF"""
        while 1:
            try:
                line = raw_input('>>> ')
            except EOFError:
                print 
                break
            s_line = line.strip()
            if not s_line:
                continue
            args = s_line.split()
            if self.commands.has_key(args[0]):
                try:
                    cmd = 'do_%s' % self.commands[args[0]]
                    getattr(self, cmd)(*args[1:])
                except EOFError:
                    break
                except:
                    import traceback
                    traceback.print_exc()
            else:
                try:
                    self.handle_line(s_line)
                except:
                    import traceback
                    traceback.print_exc()

    def handle_line(self, stripped_line):
        """method to overload in the concrete class
        
        should handle lines wich are not command
        """
        raise NotImplementedError()


    # private methods #########################################################
    
    def _register_commands(self):
        """ register available commands method and return the list of
        commands name
        """
        self.commands = {}
        self._command_help = {}
        commands = [attr[3:] for attr in dir(self) if attr[:3] == 'do_']
        for command in commands:
            topic = self.CMD_MAP[command]
            help_method = getattr(self, 'help_do_%s' % command)
            self._topics.setdefault(topic, []).append(help_method)
            self.commands[self.CMD_PREFIX + command] = command
            self._command_help[command] = help_method
        return self.commands.keys()

    def _print_help(self, cmd, syntax, explanation):
        print _('Command %s') % cmd
        print _('Syntax: %s') % syntax
        print '\t', explanation
        print


    # predefined commands #####################################################
    
    def do_help(self, command=None) :
        """base input of the help system"""
        if self._command_help.has_key(command):
            self._print_help(*self._command_help[command])
        elif command is None or not self._topics.has_key(command):
            print _("Use help <topic> or help <command>.")
            print _("Available topics are:")
            topics = self._topics.keys()
            topics.sort()
            for topic in topics:
                print '\t', topic
            print
            print _("Available commands are:")
            commands = self.commands.keys()
            commands.sort()
            for command in commands:
                print '\t', command[len(self.CMD_PREFIX):]
                
        else:
            print _('Available commands about %s:') % command
            print
            for command_help_method in self._topics[command]:
                try:
                    if callable(command_help_method):
                        self._print_help(*command_help_method())
                    else:
                        self._print_help(*command_help_method)
                except:
                    import traceback
                    traceback.print_exc()
                    print 'ERROR in help method %s'% (
                        command_help_method.func_name)
                
    help_do_help = ("help", "help [topic|command]",
                    _("print help message for the given topic/command or \
available topics when no argument"))

    def do_quit(self):
        """quit the CLI"""
        raise EOFError()
    
    def help_do_quit(self):
        return ("quit", "quit", _("quit the application"))
