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
""" Copyright (c) 2000-2003 LOGILAB S.A. (Paris, FRANCE).
 http://www.logilab.fr/ -- mailto:contact@logilab.fr

imports checkers for Python code
"""

__revision__ = "$Id: imports.py,v 1.1 2004-10-26 12:52:30 fabioz Exp $"

from os.path import dirname

from logilab.common import get_cycles, astng
from logilab.common.modutils import is_standard_module, is_relative, \
     get_module_part, STD_LIB_DIR
from logilab.common.ureports import VerbatimText

from logilab.pylint.interfaces import IASTNGChecker
from logilab.pylint.checkers import BaseChecker, CheckerHandler, EmptyReport
from logilab.pylint.checkers.utils import are_exclusive


# utilities to represents imports as tree ######################################

def make_tree_defs(mod_files_list):
    """get a list of 2-uple (module, list_of_files_which_import_this_module),
    it will return a dictionnary to represent this as a tree
    """
    tree_defs = {}
    for mod, files in mod_files_list:
        node = (tree_defs, ())
        for prefix in mod.split('.'):
            node = node[0].setdefault(prefix, [{}, []])
        node[1] += files
    return tree_defs

def repr_tree_defs(data, indent_str=None):
    """return a string which represents imports as a tree"""
    lines = []
    nodes = data.items()
    for i in range(len(nodes)):
        mod, (sub, files) = nodes[i]
        if not files:
            files = ''
        else:
            files = '(%s)' % ','.join(files)
        if indent_str is None:
            lines.append('%s %s' % (mod, files))
            sub_indent_str = '  '
        else:
            lines.append('%s\-%s %s' % (indent_str, mod, files))
            if i == len(nodes)-1:
                sub_indent_str = '%s  ' % indent_str
            else:
                sub_indent_str = '%s| ' % indent_str
        if sub:
            lines.append(repr_tree_defs(sub, sub_indent_str))
    return '\n'.join(lines)

MSGS = {
    'F0401': ('Unable to import %r (%s)' ,
              'Used when pylint has been unable to import a module.'),
    'W0401': ('Cyclic import (%s)',
              'Used when a cyclic import between two or more modules is \
              detected.'),
    'W0402': ('Wildcard import',
              'Used when "from module import *" is detected.'),
    'W0403': ('Uses of a deprecated module %r',
              'Used a module marked as deprecated is imported.'),
    'W0404': ('Relative import %r',
              'Used when an import relative to the package directory is \
              detected.'),
    'W0405': ('Reimport %r (imported line %s)',
              'Used when a module is reimported multiple times.'),
    'W0406': ('Module import itself',
              'Used when a module is importing itself.'),
    }

class ImportsChecker(BaseChecker, CheckerHandler):
    """checks for                                                              
    * external modules dependancies                                            
    * relative / wildcard imports                                                         
    * cyclic imports                                                           
    * uses of deprecated modules
    """
    
    __implements__ = IASTNGChecker

    name = 'imports'
    msgs = MSGS
    priority = -1
    
    options = (('deprecated-modules',
                {'default' : ('regsub','string', 'TERMIOS',
                              'Bastion', 'rexec'),
                 'type' : 'csv',
                 'metavar' : '<modules>',
                 'help' : 'Deprecated modules which should not be used, \
separated by a comma'}
                ),
               )

    def __init__(self, linter=None):
        BaseChecker.__init__(self, linter)
        CheckerHandler.__init__(self)
        self.stats = None
        self.import_graph = None
        self.reports = (('R0401', 'External dependencies',
                         self.report_external_dependencies),
                        )
        
    def open(self):
        """called before visiting project (i.e set of modules) """
        self.linter.add_stats(dependencies={})
        self.linter.add_stats(cycles=[])
        self.stats = self.linter.stats
        self.import_graph = {}
        
    def close(self):
        """called before visiting project (i.e set of modules) """
        for cycle in get_cycles(self.import_graph):
            self.add_message('W0401', args=' -> '.join(cycle))
         
    def visit_import(self, node):
        """triggered when an import statement is seen """
        for name, asname in node.names:
            self.check_deprecated(node, name)
            relative = self.check_relative(node, name)
            self.imported_module(node, name, relative)
            # handle reimport
            self.check_reimport(node, asname or name)
        

    def visit_from(self, node):
        """triggered when an import statement is seen """
        basename = node.modname
        self.check_deprecated(node, basename)
        relative = self.check_relative(node, basename)
        for name, asname in node.names:
            if name == '*':
                self.add_message('W0402', node=node)
                continue
            # handle reimport
            self.check_reimport(node, asname or name)
            # analyze dependancies
            fullname = '%s.%s' % (basename, name)
            if fullname.find('.') > -1:
                try:
                    fullname = get_module_part(fullname)
                except Exception, ex:
                    self.add_message('F0401', args=(fullname, ex), node=node)
                    continue            
            self.imported_module(node, fullname, relative)
        
    def imported_module(self, node, mod_path, relative):
        """notify an imported module, used to analyze dependancies
        """
        context_name = node.root().name
        if relative:
            mod_path = '%s.%s' % ('.'.join(context_name.split('.')[:-1]),
                                  mod_path)
            
        package_dir = dirname(self.linter.base_file)
        if context_name == mod_path:
            # module importing itself !
            self.add_message('W0406', node=node)
        elif not is_standard_module(mod_path, (STD_LIB_DIR, package_dir)):
            # handle dependancies
            mod_paths = self.stats['dependencies'].setdefault(mod_path, [])
            if not context_name in mod_paths:
                mod_paths.append(context_name)
        else:
            # update import graph
            mgraph = self.import_graph.setdefault(context_name, [])
            if not mod_path in mgraph:
                mgraph.append(mod_path)
            self.import_graph.setdefault(mod_path, [])

    def check_relative(self, node, mod_path):
        """check relative import module
        """
        # check for relative import
        context_file = node.root().file
        relative = is_relative(mod_path, context_file)
        if relative:
            self.add_message('W0404', args=mod_path, node=node)
        return relative
    
    def check_deprecated(self, node, mod_path):
        """check if the module is deprecated
        """
        for mod_name in self.config.deprecated_modules:
            if mod_path.startswith(mod_name):
                self.add_message('W0403', node=node, args=mod_path)
                
    def check_reimport(self, node, name):
        """check if the import is necessary (i.e. not already done)
        """
        first = node.get_frame().locals.get(name)
        if not (isinstance(first, astng.Import) or
                isinstance(first, astng.Import)):
            return
        if first is not None and first is not node and \
               not are_exclusive(first, node):
            self.add_message('W0405', node=node, args=(name, first.lineno))
        else:
            first = node.root().globals.get(name)
            if not (isinstance(first, astng.Import) or
                    isinstance(first, astng.Import)):
                return
            if first is not None and first is not node and \
                   not are_exclusive(first, node):
                self.add_message('W0405', node=node,
                                 args=(name + '???', first.lineno))

        
    def report_external_dependencies(self, sect, stats, old_stats):
        """return a verbatim layout for displaying dependancies
        """
        dep_info = make_tree_defs(self.stats['dependencies'].items())
        if not dep_info:
            raise EmptyReport()
        tree_str = repr_tree_defs(dep_info)
        sect.append(VerbatimText(tree_str))
    
            
def register(linter):
    """required method to auto register this checker """
    linter.register_checker(ImportsChecker(linter))
