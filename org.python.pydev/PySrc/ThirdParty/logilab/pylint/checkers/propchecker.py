from logilab.common import astng

from logilab.pylint.interfaces import IASTNGChecker
from logilab.pylint.checkers import BaseChecker
from logilab.pylint.checkers.base import BasicChecker
from logilab.pylint.checkers.utils import are_exclusive

def _title(prop_name):
    return prop_name.title().replace('_', '')

def make_get_name(prop_name):
    return 'Get' + _title(prop_name)
    
def make_set_name(prop_name):
    return 'Set' + _title(prop_name)

class MyChecker(BaseChecker):
    """add member attributes defined using my own "properties.create"
    function to the class locals dictionary
    """
    
    __implements__ = IASTNGChecker

    name = 'custom'
    msgs = {}
    options = ()
    # this is important so that your checker is executed before others
    priority = -1 

    def visit_callfunc(self, node):
        if not (isinstance(node.node, astng.Getattr)
                and isinstance(node.node.expr, astng.Name)
                and node.node.expr.name == 'properties'
                and node.node.attrname == 'create'):
            return
        in_class = node.get_frame()
        for param in node.args:
            in_class.locals[param.name] = node
            in_class.locals['_'+param.name] = node
            in_class.locals[make_set_name(param.name)] = node
            in_class.locals[make_get_name(param.name)] = node
            node._customPropertyDefinition = True

        in_class.locals["__properties__"] = node


def _check_redefinition(self, redef_type, node):
    """check for redefinition of a function / method / class name"""
    defined_self = node.parent.get_frame().locals[node.name]
    if not hasattr(defined_self , '_customPropertyDefinition'):#we don't want to check redefinition of property... this is commom!
        if defined_self is not node and not are_exclusive(node, defined_self):
            self.add_message('E0102', node=node,
                             args=(redef_type, defined_self.lineno))


def register(linter):
    """required method to auto register this checker"""
    
    #when overriding a property, we don't want any error
    BasicChecker._check_redefinition = _check_redefinition
    linter.register_checker(MyChecker(linter))
