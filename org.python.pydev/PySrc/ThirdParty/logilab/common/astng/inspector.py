# Copyright (c) 2000-2004 LOGILAB S.A. (Paris, FRANCE).
# http://www.logilab.fr/ -- mailto:contact@logilab.fr
#
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
"""visitor doing some postprocessing on the astng tree.
Try to resolve definitions (namespace) dictionnary, relationship...

This module has been imported from pyreverse
"""

__revision__ = "$Id: inspector.py,v 1.2 2005-02-16 16:45:44 fabioz Exp $"

from os.path import dirname

from logilab.common import astng
from logilab.common.astng.utils import LocalsVisitor, get_interfaces
from logilab.common.modutils import get_module_part, is_relative, \
     is_standard_module


class IdGeneratorMixIn:
    """
    Mixin adding the ability to generate integer uid
    """
    def __init__(self, start_value=0):
        self.id_count = start_value
    
    def init_counter(self, start_value=0):
        """init the id counter
        """
        self.id_count = start_value
        
    def generate_id(self):
        """generate a new identifer
        """
        self.id_count += 1
        return self.id_count


class Linker(IdGeneratorMixIn, LocalsVisitor):
    """
    walk on the project tree and resolve relationships.
    
    According to options the following attributes may be added to visited nodes:
    
    * uid,
      a unique identifier for the node (on astng.Project, astng.Module,
      astng.Class and astng.locals_type). Only if the linker has been instantiad
      with tag=True parameter (False by default).
            
    * Function
      a mapping from locals'names to their bounded value, which may be a
      constant like a string or an integer, or an astng node (on astng.Module,
      astng.Class and astng.Function).

    * instance_attrs_type
      as locals_type but for klass member attributes (only on astng.Class)
      
    * baseobjects,
      list of parent classes _objects_ (only on astng.Class nodes)
      
    * implements,
      list of implemented interfaces _objects_ (only on astng.Class nodes)
    """
    
    def __init__(self, project, inherited_interfaces=0, tag=False):
        IdGeneratorMixIn.__init__(self)
        LocalsVisitor.__init__(self)
        # take inherited interface in consideration or not
        self.inherited_interfaces = inherited_interfaces
        # tag nodes or not
        self.tag = tag
        # visited project
        self.project = project

        
    def visit_project(self, node):
        """visit an astng.Project node
        
         * optionaly tag the node wth a unique id
        """
        if self.tag:
            node.uid = self.generate_id()
        for module in node.modules:
            self.visit(module)
            
    def visit_package(self, node):
        """visit an astng.Package node
        
         * optionaly tag the node wth a unique id
        """
        if self.tag:
            node.uid = self.generate_id()
        for subelmt in node.values():
            self.visit(subelmt)
            
    def visit_module(self, node):
        """visit an astng.Module node
        
         * set the locals_type mapping
         * set the depends mapping
         * optionaly tag the node wth a unique id
        """
        if hasattr(node, 'locals_type'):
            return
        node.locals_type = {}
        node.depends = []
        if self.tag:
            node.uid = self.generate_id()
    
    def visit_class(self, node):
        """visit an astng.Class node
        
         * set the locals_type and instance_attrs_type mappings
         * set the baseobjects list and build it
         * set the implements list and build it
         * optionaly tag the node wth a unique id
        """
        if hasattr(node, 'locals_type'):
            return
        node.locals_type = {}
        node.baseobjects = []
        if self.tag:
            node.uid = self.generate_id()
        # resolve ancestors
        for base in node.basenames:
            baseobj = self._resolve_dotted_name(node.parent, base)
            if baseobj is not None:
                node.baseobjects.append(baseobj)
                specializations = getattr(baseobj, 'specializations', [])
                specializations.append(node)
                baseobj.specializations = specializations
        # resolve instance attributes
        node.instance_attrs_type = {}
        for assattr in node.instance_attrs.values():
            self.visit_assattr(assattr, node)
        # resolve implemented interface
        try:
            node.implements = list(get_interfaces(node,
                                                  self.inherited_interfaces))
        except TypeError:
            node.implements = ()
            
    def visit_function(self, node):
        """visit an astng.Function node
        
         * set the locals_type mapping
         * optionaly tag the node wth a unique id
        """
        if hasattr(node, 'locals_type'):
            return
        node.locals_type = {}
        if self.tag:
            node.uid = self.generate_id()
            
    link_project = visit_project
    link_module = visit_module
    link_class = visit_class
    link_function = visit_function
        
    def visit_assname(self, node):
        """visit an astng.AssName node

        handle locals_type
        """
        frame = node.get_frame()
        value = self._get_value(node.get_assigned_value())
        frame.locals_type[node.name] = value
        
    def visit_assattr(self, node, parent):
        """visit an astng.AssAttr node

        handle instance_attrs_type
        """
        value = self._get_value(node.get_assigned_value())
        parent.instance_attrs_type[node.attrname] = value
            
    def visit_import(self, node):
        """visit an astng.Import node
        
        resolve module dependencies
        """
        context_file = node.root().file
        for name in node.names:
            relative = is_relative(name[0], context_file)
            self._imported_module(node, name[0], relative)
        

    def visit_from(self, node):
        """visit an astng.From node
        
        resolve module dependencies
        """
        basename = node.modname
        context_file = node.root().file
        relative = is_relative(basename, context_file)
        for name in node.names:
            if name[0] == '*':
                continue
            # analyze dependancies
            fullname = '%s.%s' % (basename, name[0])
            if fullname.find('.') > -1:
                try:
                    fullname = get_module_part(fullname)
                except Exception:
                    continue
            if fullname != basename:
                self._imported_module(node, fullname, relative)

        
    def compute_module(self, context_name, mod_path):
        """return true if the module should be added to dependencies"""
        package_dir = dirname(self.project.path)
        if context_name == mod_path:
            return 0
        elif is_standard_module(mod_path, (package_dir,)):
            return 1
        return 0
    
    # protected methods ########################################################

    def _imported_module(self, node, mod_path, relative):
        """notify an imported module, used to analyze dependancies
        """
        module = node.root()
        context_name = module.name
        if relative:
            mod_path = '%s.%s' % ('.'.join(context_name.split('.')[:-1]),
                                  mod_path)
        if self.compute_module(context_name, mod_path):
            # handle dependancies
            if not hasattr(module, 'depends'):
                module.depends = []
            mod_paths = module.depends
            if not mod_path in mod_paths:
                mod_paths.append(mod_path)

        
    def _get_value(self, node):
        """try to get an object reference or a constante from the assigned value
        """
        if isinstance(node, astng.Name):
            try:
                node = node.resolve(node.name)
            except (astng.ResolveError, astng.NotFoundError):
                pass
        if isinstance(node, astng.Const):
            node = node.value
        elif isinstance(node, astng.CallFunc):
#            print node.as_string()
#            print node
#            print node.node
            _node = node.node
            while _node is not None and (isinstance(_node, astng.Getattr) or 
                                         isinstance(_node, astng.Name)):
                try:
                    _node = _node.getChildNodes()[0]
                except IndexError:
                    break
            else:
                # we have found something else than a Getattr or a Name
                return node
            assnode = self._resolve_dotted_name(node, node.node.as_string())
            # is it an association to a class ? (ie constructor call)
            if isinstance(assnode, astng.Class):
                node = assnode
        return node

    def _resolve_dotted_name(self, node, dotted_name):
        """try to resolve name in the form 'a.b.c', return None on failure"""
        try:
            return node.resolve_dotted(dotted_name)
        except astng.ASTNGError:
            return
