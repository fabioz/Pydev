# Copyright (c) 2002-2003 LOGILAB S.A. (Paris, FRANCE).
# http://www.logilab.fr/ -- mailto:contact@logilab.fr
#
# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation; either version 2 of the License, or (at your option) any later
# version.
#
# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details
#
# You should have received a copy of the GNU General Public License along with
# this program; if not, write to the Free Software Foundation, Inc.,
# 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
""" %prog [options] module_or_package

  Check that a module satisfy a coding standard (and more !).

    %prog --help
    
  Display this help message and exit.

    %prog --help-msg <msg-id>[,<msg-id>]

  Display help messages about given message identifiers and exit.
"""

__revision__ = "$Id: lint.py,v 1.1 2004-10-26 12:52:29 fabioz Exp $"

from __future__ import nested_scopes



import sys
import os
import re
import tokenize
from os.path import dirname

#HACK - TODO:Fix it!!
sys.path.insert(1, os.path.join(os.path.dirname(sys.argv[0]), "../../../ThirdParty"))
#sys.path.insert(1, os.path.join(os.path.dirname("D:/dev_programs/eclipse_3/eclipse/workspace/org.python.pydev/PySrc/") , "ThirdParty"))


from logilab.common.configuration import OptionsManagerMixIn
from logilab.common.astng import ASTNGManager, ASTNGBuildingException, Module
from logilab.common.modutils import modpath_from_file, get_modules
from logilab.common.interface import implements
from logilab.common.textutils import normalize_text, get_csv
from logilab.common.ureports import Section, Table, Text

from logilab.pylint.interfaces import ILinter, IRawChecker, IASTNGChecker
from logilab.pylint.checkers import CheckerHandler, BaseRawChecker, EmptyReport
from logilab.pylint import config
from logilab.pylint.reporters import diff_string
from logilab.pylint.reporters.text import TextReporter

from logilab.pylint.__pkginfo__ import version

# utilities ###################################################################



def sort_checkers(checkers, enabled_only=1):
    """return a list of enabled checker sorted by priority
    """
    if enabled_only:
        checkers = [(-checker.priority, checker) for checker in checkers
                    if checker.is_enabled()]
    else:
        checkers = [(-checker.priority, checker) for checker in checkers]
    checkers.sort()
    return [item[1] for item in checkers]


def get_module_and_frameid(node):
    """return the module name and the frame id in the module
    """
    frame = node.get_frame()
    module, obj = '', []
    while frame:
        if isinstance(frame, Module):
            module = frame.name
        else:
            obj.append(frame.name)
        try:
            frame = frame.parent.get_frame()
        except AttributeError:
            frame = None
    obj.reverse()
    return module, '.'.join(obj)

class UnknownMessage(Exception):
    """raised when a unregistered message id is encountered
    """
MSG_TYPES = {
    'I' : 'info',
    'W' : 'warning',
    'E' : 'error',
    'F' : 'fatal'
    }

MSGS = {
    'F0001': ('%s',
              'Used when an error occured while building the ASTNG \
              representation.'),
    'F0002': ('%s: %s',
              'Used when an unexpected error occured while building the ASTNG \
              representation. This is usually accomopagned by a traceback. \
              Please report such errors !'),
## 'F0003': ('Unable to load file %s (%s)',
##           'Used when an OSError is raised when trying to open a module. \
##           It may occurs if the .py for a .pyc file doen\'t exists. In \
##           this case raw checkers won\'t be executed on that file.'),
    
    'I0001': ('Unable to run raw checkers on built-in module %s',
              'Used to inform that a built-in module has not been checked \
              using the raw checkers.'),
    
    'I0011': ('Locally disabling %r',
              'Used when an inline option disable a message.'),
    'E0011': ('Unrecognized file option %r',
              'Used when an unknown inline option is encountered.'),
    'E0012': ('Bad option value %r',
              'Used when an bad value for an inline option is encountered.'),
    }

OPTION_RGX = re.compile('#*\s*pylint:(.*)')

# Python Linter class #########################################################

class PyLinter(OptionsManagerMixIn, BaseRawChecker, CheckerHandler):
    """lint Python modules using external checkers.
    """

    __implements__ = (ILinter, IRawChecker, IASTNGChecker)
    
    name = 'master'
    priority = 0
    msgs = MSGS
    may_be_disabled = 0
    
    options = (("ignore",
                {'action' :"append", 'type' : "string", 'metavar' : "<file>",
                 'dest' : "black_list", "default" : ('CVS',),
                 'help' : """Add <file> (may be a directory) to the black list\
. It should be a base name, not a path. You may set this option multiple times\
."""}),
               ("persistent",
                {'default': 1, 'type' : 'yn', 'metavar' : '<y_or_n>',
                 'help' : 'Pickle collected data for later comparisons.'}),
               
               ("reports",
                {'default': 1, 'type' : 'yn', 'metavar' : '<y_or_n>',
                 'group': 'Reports',
                 'help' : "Tells wether to display a full report or only the\
 messages"}),
               ("files-output",
                {'default': 0, 'type' : 'yn', 'metavar' : '<y_or_n>',
                 'group': 'Reports',
                 'help' : 'Put messages in a separate file for each module / \
package specified on the command line instead of printing them on stdout. \
Reports (if any) will be written in a file name "pylint_global.[txt|html]".'}),               
               
               ("evaluation",
                {'type' : 'string', 'metavar' : '<python_expression>',
                 'group': 'Reports',
                 'default': '10.0 - ((float(5 * errors + warnings) / \
statements) * 10)', 
                 'help' : 'Python expression which should return a note less \
than 10 (10 is the highest note).You have access to the variables errors, \
warnings, statements which respectivly contain the number of errors / warnings\
 messages and the total number of statements analyzed. This is used by the \
 global evaluation report (R0004).'}),
               
               ("comment",
                {'default': 0, 'type' : 'yn', 'metavar' : '<y_or_n>',
                 'group': 'Reports',
                 'help' : 'Add a comment according to your evaluation note. \
This is used by the global evaluation report (R0004).'}),

               ("include-ids",
                {'type' : "yn", 'metavar' : '<y_or_n>', 'default' : 0,
                 'group': 'Reports',
                 'help' : "Include message's id in output"}),
               
               )
    
    option_groups = (
        ('Reports', 'Options related to messages / statistics reporting'),
            )
    
    def __init__(self, options=(), reporter=TextReporter(),
                 option_groups=()):
        self.options = options + PyLinter.options
        self.option_groups = option_groups + PyLinter.option_groups
        full_version = "%%prog %s\nPython %s" % (version,
                                                 sys.version)
        OptionsManagerMixIn.__init__(self, usage=__doc__,
                                     version=full_version,
                                     config_file=config.PYLINTRC)
        BaseRawChecker.__init__(self)
        # helpers
        self.manager = ASTNGManager()
        self.reporter = reporter
        # dictionary of registered messages
        self._messages = {}
        self._messages_help = {}
        self._msgs_state = {}
        self._module_msgs_state = None
        # reports variables
        self._reports = []
        self._reports_state = {}
        # checkers
        self._checkers = {}
        # visit variables
        self.base_name = None
        self.base_file = None
        self.current_name = None
        self.stats = None
        # provided reports
        self.reports = (('R0001', 'Global evaluation',
                         self.report_evaluation),
                        ('R0002', '% errors / warnings by module',
                         self.report_error_warning_by_module_stats),
                        ('R0003', 'Total errors / warnings',
                         self.report_error_warning_stats),
                        ('R0004', 'Messages',
                         self.report_messages_stats),
                        )
        # set this variable since some modules may have to know they are
        # imported by pylint (i.e. logilab.constraint)
        os.environ['PYLINT_IMPORT'] = '1'
        # PyLinter is itself a checker, register it
        self.register_checker(self)


    # checkers manipulation methods ###########################################
    
    def register_checker(self, checker):
        """register a new checker

        checker is an object implementing IRawChecker or / and IASTNGChecker
        """
        assert checker.priority <= 0, 'checker priority can\'t be >= 0'
        self._checkers[checker] = 1
        self.register_options_provider(checker)
        if hasattr(checker, 'msgs'):
            self.register_messages(checker)
        if hasattr(checker, 'reports'):
            for r_id, r_title, r_cb in checker.reports:
                self.register_report(r_id, r_title, r_cb, checker)
                
    def disable_all_checkers(self):
        """disable all possible checkers """
        for checker in self._checkers.keys():
            checker.enable(0)


    # messages handling #######################################################

    def register_messages(self, checker):
        """register a dictionary of messages

        Keys are message ids, values are a 2-uple with the message type and the
        message itself

        message ids should be a string of len 4, where the to first characters
        are the checker id and the two last the message id in this checker
        """
        msgs_dict = checker.msgs
        chk_id = None
        for msg_id, (msg, msg_help) in msgs_dict.items():
            # avoid duplicate / malformed ids
            assert not self._messages.has_key(msg_id), \
                   'Message id %r is already defined' % msg_id
            assert len(msg_id) == 5, 'Invalid message id %s' % msg_id
            assert chk_id is None or chk_id == msg_id[1:3], \
                   'Inconsistent checker part in message id %r' %msg_id
            assert msg_id[0] in ('I', 'W', 'E', 'F'), \
                   'Bad message type %s in %r' % (msg_id[0], msg_id)
            chk_id = msg_id[1:3]
            if checker is not None:
                add = ' This message belongs to the %s checker.' % checker.name
                msg_help += add
            self._messages_help[msg_id] = msg_help
            self._messages[msg_id] = msg

    def get_message_help(self, msg_id):
        """return the help string for the given message id"""
        msg_id = self.check_message_id(msg_id)
        try:
            msg = self._messages_help[msg_id]
            msg = normalize_text(' '.join(msg.split()), indent='  ')
        except KeyError:
            msg = 'No help available for message %s' % msg_id
        return '%s:\n%s' % (msg_id, msg)

    def disable_message(self, msg_id, scope='package'):
        """don't output message of the given id"""
        assert scope in ('package', 'module')
        msg_id = self.check_message_id(msg_id)
        if scope == 'module':
            self._module_msgs_state[msg_id] = 0
        else:
            self._msgs_state[msg_id] = 0
        
    def enable_message(self, msg_id, scope='package'):
        """reenable message of the given id"""
        assert scope in ('package', 'module')
        msg_id = self.check_message_id(msg_id)
        if scope == 'module':
            self._module_msgs_state[msg_id] = 1
        else:
            self._msgs_state[msg_id] = 1
            
    def check_message_id(self, msg_id):
        """raise UnknownMessage if the message id is not defined"""
        msg_id = msg_id.upper()
        if not self._messages.has_key(msg_id):
            print self._messages
            raise UnknownMessage('No such message id %s' % msg_id)
        return msg_id

    def is_message_enabled(self, msg_id):
        """return true if the message associated to the given message id is
        enabled
        """
        try:
            return self._module_msgs_state[msg_id]
        except KeyError:
            return self._msgs_state.get(msg_id, 1)
        
    def add_message(self, msg_id, line=None, node=None, args=None):
        """add the message corresponding to the given id.

        If provided, msg is expanded using args
        
        astng checkers should provide the node argument,
        raw checkers should provide the line argument.
        """
        # should this message be displayed
        if not self.is_message_enabled(msg_id):
            return        
        # update stats
        key = '%ss' % MSG_TYPES[msg_id[0]]
        self.stats[key] += 1
        self.stats['by_module'][self.current_name][key] += 1
        try:
            self.stats['by_msg'][msg_id] += 1
        except KeyError:
            self.stats['by_msg'][msg_id] = 1
        msg = self._messages[msg_id]
        # expand message ?
        if args:
            msg %= args
        if line is None and node is not None:
            line = node.lineno or node.get_statement().lineno
        # get module and object
        if node is None:
            module, obj = self.current_name, ''
        else:
            module, obj = get_module_and_frameid(node)
        # add the message
        self.reporter.add_message(msg_id, (module, obj, line or 0), msg)

        
    def process_tokens(self, tokens):
        """process the module with the given name
        
        the module's content is accessible via the stream object
        
        stream must implements the readline method.

        Search for file specific options
        """
        comment = tokenize.COMMENT
        newline = tokenize.NEWLINE
        line_num = 0
        for (tok_type, token, start, end, line) in tokens:
            if tok_type not in (comment, newline):
                break
            if start[0] == line_num:
                continue
            line_num = start[0]
            match = OPTION_RGX.match(line)
            if match is None:
                continue
            option, value = match.group(1).split('=', 1)
            option = option.strip()
            if option == 'disable-msg':
                for msg_id in get_csv(value):
                    try:
                        self.disable_message(msg_id, 'module')
                        self.add_message('I0011', args=msg_id, line=line_num)
                    except UnknownMessage:
                        self.add_message('E0012', args=msg_id, line=line_num)
                        
            elif option == 'enable-msg':
                for msg_id in get_csv(value):
                    try:
                        self.enable_message(msg_id, 'module')
                    except UnknownMessage:
                        self.add_message('E0012', args=msg_id, line=line_num)
            else:
                self.add_message('E0011', args=option, line=line_num)

    def set_reporter(self, reporter):
        """set the reporter used to display messages"""
        self.reporter = reporter
        
    # reports / stats manipulation method #####################################
    
    def register_report(self, r_id, r_title, r_cb, checker):
        """register a report
        
        r_id is the unique identifier for the report
        r_title the report's title
        r_cb the method to call to make the report
        checker is the checker defining the report
        """
        r_id = r_id.upper()
        self._reports.insert(0, (r_id, r_title, r_cb, checker) )
        
    def disable_report(self, r_id):
        """disable the report of the given id"""
        r_id = r_id.upper()
        self._reports_state[r_id] = 0
        
    def make_reports(self, stats, old_stats):
        """render registered reports"""
        if self.config.files_output:
            filename = 'pylint_global.' + self.reporter.extension
            self.reporter.set_output(open(filename, 'w'))
        sect = Section('Report',
                       '%s statements analysed.'% (self.stats['statements']))
        for r_id, r_title, r_cb, checker in self._reports:
            if not (self._reports_state.get(r_id, 1) and checker.is_enabled()):
                continue
            report_sect = Section(r_title)
            try:
                r_cb(report_sect, stats, old_stats)
            except EmptyReport:
                continue
            report_sect.report_id = r_id
            sect.append(report_sect)
        self.reporter.display_results(sect)
            
    def add_stats(self, **kwargs):
        """add some stats entries to the statistic dictionary
        raise an AssertionError if there is a key conflict
        """
        for key, value in kwargs.items():
            if key[-1] == '_':
                key = key[:-1]
            assert not self.stats.has_key(key)
            self.stats[key] = value
        return self.stats

    # code checking methods ###################################################

    def check(self, mod_names):
        """main checking entry : check a list of modules or packages from their
        name.
        """
        self.reporter.include_ids = self.config.include_ids
        if type(mod_names) not in (type(()), type([])):
            mod_names = [mod_names]
        checkers = sort_checkers(self._checkers.keys())
        rev_checkers = checkers[:]
        rev_checkers.reverse()
        # notify global begin
        for checker in checkers:
            checker.open()
        # check modules or packages        
        for mod_name in mod_names:
            if self.config.files_output:
                filename = 'pylint_%s.%s' % (mod_name, self.reporter.extension)
                self.reporter.set_output(open(filename, 'w'))
            try:
                self.check_module(mod_name, checkers, rev_checkers)
            except:
                pass
        # notify global end
        for checker in rev_checkers:
            checker.close()

    def check_module(self, mod_name, checkers, rev_checkers):
        """check a module or package from its name
        if mod_name is a package, recurse on its subpackages / submodules
        """
        # get the given module representation
        self.base_name = mod_name
        self.base_file = None
        # check this module
        astng = self._check_module(mod_name, checkers, rev_checkers)
        if astng is None:
            return
        # recurse in package except if __init__ was explicitly given
        if not mod_name.endswith('.__init__') and astng.package:
            # recurse on others packages / modules if this is a package
            for mod_name in get_modules(mod_name, dirname(astng.file),
                                        self.config.black_list):
                self._check_module(mod_name, checkers, rev_checkers)
                    
    def _check_module(self, mod_name, checkers, rev_checkers):
        """check a module by building its astng representation"""
        self.current_name = mod_name
##         # notify visit begin
##         for checker in checkers:
##             checker.open_module()
        self.open_module(mod_name)
        # get the module representation
        astng = self.get_astng(mod_name)
        if astng is not None:
            # set the base file if necessary
            self.base_file = self.base_file or astng.file
            # and check it
            self.check_astng_module(astng, checkers)
##         # notify visit end
##         for checker in rev_checkers:
##             checker.close_module()
        return astng
        
    def open_module(self, name):
        """initialize counters for a given module name"""
        self.stats['by_module'][name] = {}
        self.stats['by_module'][name]['infos'] = 0
        self.stats['by_module'][name]['warnings'] = 0
        self.stats['by_module'][name]['errors'] = 0
        self.stats['by_module'][name]['fatals'] = 0
        self._module_msgs_state = {}

    def get_astng(self, mod_name):
        """return a astng representation for a module"""
        try:
            return self.manager.astng_from_module_name(mod_name)
        except ASTNGBuildingException, ex:
            self.add_message('F0001', args=ex)
        except KeyboardInterrupt:
            raise
        except Exception, ex:
            import traceback
            traceback.print_exc()
            self.add_message('F0002', args=(ex.__class__, ex))
        

    def check_astng_module(self, astng, checkers):
        """check a module from its astng representation, real work"""
        # this is required to make works relative imports in analyzed code
        sys.path.insert(0, dirname(astng.file))
        try:
            # call raw checkers if possible
            if not astng.pure_python:
                self.add_message('I0001', args=astng.name)
            else:
##                 try:
                stream = open(astng.file.replace('.pyc', '.py'))
                for checker in checkers:
                    if implements(checker, IRawChecker):
                        stream.seek(0)
                        checker.process_module(stream)
##                 except IOError, e:
##                     self.add_message('F0003', args=(file, e))
            # generate events to astng checkers
            self.astng_events(astng, [checker for checker in checkers
                                      if implements(checker, IASTNGChecker)])

        finally:
            sys.path.pop(0)
    
    def astng_events(self, astng, checkers):
        """generate event to astng checkers according to the current astng
        node and recurse on its children
        """
        if astng.is_statement():
            self.stats['statements'] += 1
        # generate events for this node on each checkers
        for checker in checkers:
            checker.visit(astng)
        # recurse on children
        for child in astng.getChildNodes():
            self.astng_events(child, checkers)
        checkers.reverse()
        for checker in checkers:
            checker.leave(astng)
        

    # IASTNGChecker interface #################################################
        
    def open(self):
        """initialize counters"""
        self.stats = { 'statements': 0,
                       'infos'     : 0,
                       'warnings'  : 0,
                       'errors'    : 0,
                       'fatals'    : 0,
                       'by_module' : {},
                       'by_msg' : {},
                       }
    
    def close(self):
        """close the whole package /module, it's time to make reports !
        
        if persistent run, pickle results for later comparison
        """
        # load old results if any
        old_stats = config.load_results(self.base_name)
        if self.config.reports:
            self.make_reports(self.stats, old_stats)
        # save results if persistent run
        if self.config.persistent:
            config.save_results(self.stats, self.base_name)
            
    # specific reports ########################################################
        
    def report_error_warning_stats(self, sect, stats, old_stats):
        """make total errors / warnings report
        """
        lines = ('type', 'number', 'previous', 'difference')
        for m_type in ('warnings', 'errors'):
            new = stats[m_type]
            old = old_stats.get(m_type, None)
            if old is not None:
                diff_str = diff_string(old, new)
            else:
                old, diff_str = 'NC', 'NC'
            lines += (m_type, str(new), str(old), diff_str)
        sect.append(Table(children=lines, cols=4, rheaders=1))
        
    def report_messages_stats(self, sect, stats, old_stats):
        """make messages type report
        """
        if not stats['by_msg']:
            # don't print this report when we didn't detected any errors
            raise EmptyReport()
        in_order = [(value, msg_id)
                    for msg_id, value in stats['by_msg'].items()
                    if not msg_id.startswith('I')]
        in_order.sort()
        in_order.reverse()
        lines = ('message id', 'occurences')
        for value, msg_id in in_order:
            lines += (msg_id, str(value))
        sect.append(Table(children=lines, cols=2, rheaders=1))
        
    def report_error_warning_by_module_stats(self, sect, stats, old_stats):
        """make errors / warnings by modules report"""
        if len(stats['by_module']) == 1:
            # don't print this report when we are analysing a single module
            raise EmptyReport()
        by_mod = {} 
        for m_type in ('fatals', 'errors', 'warnings'):
            total = stats[m_type]
            for module in stats['by_module'].keys():
                mod_total = stats['by_module'][module][m_type]
                if total == 0:
                    percent = 0
                else:
                    percent = float((mod_total)*100) / total
                by_mod.setdefault(module, {})[m_type] = percent            
        sorted_result = []
        for module, mod_info in by_mod.items():
            sorted_result.append((mod_info['errors'],
                                  mod_info['warnings'],
                                  module))
        sorted_result.sort()
        sorted_result.reverse()
        lines = ('module', 'error', 'warning')
        for line in sorted_result:
            if line[0] == 0 and line[1] == 0:
                break
            lines += (line[2], '%.2f' % line[0], '%.2f' % line[1])
        if len(lines) == 3:
            raise EmptyReport()
        sect.append(Table(children=lines, cols=3, rheaders=1))

    def report_evaluation(self, sect, stats, old_stats):
        """make the global evaluation report"""
        # check with at least check 1 statements (usually 0 when there is a
        # syntax error preventing pylint from further processing)
        if stats['statements'] == 0:
            raise EmptyReport()
        # get a global note for the code
        evaluation = self.config.evaluation
        try:
            note = eval(evaluation, {}, self.stats)
        except Exception, ex:
            msg = 'An exception occured while rating: %s' % ex
        else:
            stats['global_note'] = note
            msg = 'Your code has been rated at %.2f/10' % note
            if old_stats.has_key('global_note'):
                msg += ' (previous run: %.2f/10)' % old_stats['global_note']
            if self.config.comment:
                msg = '%s\n%s' % (msg, config.get_note_message(note))
        sect.append(Text(msg))

        

# utilities ###################################################################

# this may help to import modules using gettext

try:
    __builtins__._ = str
except AttributeError:
    __builtins__['_'] = str


class Run:
    """helper class to use as main for pylint :
    
    run(*sys.argv[1:])
    """
    
    def __init__(self, args, reporter=TextReporter(), quiet=0):
        from logilab.pylint import checkers
        self.linter = linter = PyLinter((
            ("disable-all",
             {'action' : "callback",
              'callback' : self.cb_disable_all_checkers,
              'help' : '''Disable all possible checkers. This option should 
              precede enable-* options.'''}),

            ("help-msg",
             {'action' : "callback", 'type' : 'string', 'metavar': '<msg-id>',
              'callback' : self.cb_help_message,
              'help' : '''Display a help message for the given message id and \
exit. This option may be a comma separated list.'''}),

            ("zope",
            {'action' : "callback", 'callback' : self.cb_init_zope,
             'help' : "Initialize Zope products before starting."}),
            
            ("cache-size",
            {'action' : "callback", 'type' : 'int', 'metavar': '<size>',
             'callback' : self.cb_astng_cache_size,
             'help' : "Set the cache size for astng objects."}),
            
            ("generate-rcfile",
             {'action' : "callback", 'callback' : self.cb_generate_config,
              'help' : '''Generate a sample configuration file according to \
the current configuration. You can put other options before this one to use \
them in the configuration. This option causes the program to exit'''}),
            
            ("enable-msg",
             {'action' : "callback", 'type' : 'string', 'metavar': '<msg ids>',
              'group': 'Reports',
              'callback' : self.cb_enable_message,
              'help' : '''Enable the message with the given id. This option \
may be a comma separated list or be set multiple time.'''}),
            
            ("disable-msg",
             {'action' : "callback", 'type' : 'string', 'metavar': '<msg ids>',
              'group': 'Reports',
              'callback' : self.cb_disable_message,
              'help' : '''Disable the message with the given id. This option \
may be a comma separated list or be set multiple time.'''}),

            ("disable-report",
             {'action' : "callback", 'type' : 'string', 'metavar': '<rpt ids>',
              'group': 'Reports',
              'callback' : self.cb_disable_report,
              'help' : '''Disable the report with the given id. This option \
may be a comma separated list or be set multiple time.'''}),

            ("html",
             {'action' : "callback", 'callback' : self.cb_set_format,
              'group': 'Reports',
              'help' : "Use HTML as output format instead of text"}),
                                        
            ("parseable",
             {'action' : "callback", 'callback' : self.cb_set_format,
              'group': 'Reports',
              'help' : "Use a parseable text output format, so your favorite \
text editor will be able to jump to the line corresponding to a message."}),
            
            ), reporter=reporter)
        linter.quiet = quiet
        # register checkers
        checkers.initialize(linter)
        # add some help section
        linter.add_help_section('Environment variables', config.ENV_HELP)
        linter.add_help_section('Output', '''
Using the default text output, the message format is :                         
        MESSAGE_TYPE: LINE_NUM:[OBJECT:] MESSAGE                               
There are 3 kind of message types :                                            
    * (W) warning and (E) error                                                
    these message types are used to distinguish the gravity of the detected    
problem.                                                                       
    * (F) fatal                                                                
    an error occured which prevented pylint from doing further processing.     
        ''')
        # load configuration
        linter.load_file_configuration()
        args = linter.load_command_line_configuration(args)
        if not args:
            print linter.help()
        # insert current working directory to the python path to have a correct
        # behaviour
        sys.path.insert(0, os.getcwd())
        modnames = []
        for modname in args:
            if modname[-3:] == '.py' or modname.find(os.sep) > -1:
                modname = '.'.join(modpath_from_file(modname))
            modnames.append(modname)
        linter.check(modnames)
        sys.path.pop(0)

    def cb_generate_config(self, *args, **kwargs):
        """optik callback for sample config file generation"""
        self.linter.generate_config()
#        sys.exit(0)
        
    def cb_set_format(self, option, opt_name, value, parser):
        """optik callback for HTML format setting"""
        if opt_name == '--html':
            from logilab.pylint.reporters.html import HTMLReporter
            self.linter.set_reporter(HTMLReporter())
        elif opt_name == '--parseable':
            from logilab.pylint.reporters.text import TextReporter2
            self.linter.set_reporter(TextReporter2())
        
    def cb_astng_cache_size(self, option, opt_name, value, parser):
        """set the astng cache size"""
        self.linter.manager.set_cache_size(int(value))
        
    def cb_init_zope(self, *args, **kwargs):
        """optik callback for Zope products initialization"""
        import Zope
        if hasattr(Zope, 'startup'):
            # Zope >= 2.6
            Zope.startup()

    def cb_disable_all_checkers(self, *args, **kwargs):
        """optik callback for disabling all checkers"""
        self.linter.disable_all_checkers()

    def cb_disable_message(self, option, opt_name, value, parser):
        """optik callback for disabling some particular messages"""
        for msg_id in get_csv(value):
            self.linter.disable_message(msg_id)
            
    def cb_disable_report(self, option, opt_name, value, parser):
        """optik callback for disabling some particular messages"""
        for r_id in get_csv(value):
            self.linter.disable_report(r_id)
            
    def cb_enable_message(self, option, opt_name, value, parser):
        """optik callback for enabling some particular messages"""
        for msg_id in get_csv(value):
            self.linter.enable_message(msg_id)
            
    def cb_help_message(self, option, opt_name, value, parser):
        """optik callback for printing some help about a particular message"""
        for msg_id in get_csv(value):
            try:
                print self.linter.get_message_help(msg_id)
                print
            except UnknownMessage, ex:
                print ex
                print
                continue
#        sys.exit(0)
        


if __name__ == "__main__":
    sys.stderr = sys.stdout #we don't want to write in stderr, only in stdout.
    Run(sys.argv[1:])
