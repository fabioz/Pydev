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

 This module provides a way to optimize globals in certain functions by binding
 their names to values provided in a dictionnary
"""

__revision__ = '$Id: bind.py,v 1.6 2005-04-19 14:39:09 fabioz Exp $'

# TODO: unit tests
# * this module provide a function bind(func,vars) which replaces every
#   global variable 'm' by the value vars['m'] if such value exists in dict

from dis import HAVE_ARGUMENT
from new import code as make_code, function as make_function
import inspect

LOAD_GLOBAL = 116
LOAD_CONST = 100
EXTENDED_ARG = 143
STORE_GLOBAL = 97

def bind_code(co, globals):
    """
    Take a code object and a dictionnary and returns a new code object where
    the opcodes LOAD_GLOBAL are replaced by LOAD_CONST whenever the global's
    name appear in the dictionnary
    """
    consts = list(co.co_consts)
    assigned = {}
    
    code = co.co_code
    new_code = ""
    n = len(code)
    i = 0
    while i < n:
        c = code[i]
        op = ord(c)
        i += 1
        if op >= HAVE_ARGUMENT:
            oparg = ord(code[i]) + ord(code[i+1]) * 256
            i += 2
        else:
            oparg = None
        if op == LOAD_GLOBAL:
            name = co.co_names[oparg]
            if globals.has_key(name):
                k = assigned.get(name,None)
                if k == None:
                    k = len(consts)
                    assigned[name] = len(consts)
                    consts.append(globals[name])
                op = LOAD_CONST
                oparg = k
        new_code += chr(op)
        if oparg is not None:
            new_code += chr(oparg & 255)
            new_code += chr( (oparg>>8) & 255 )
            
    return make_code(co.co_argcount,
                     co.co_nlocals,
                     co.co_stacksize,
                     co.co_flags,
                     new_code,
                     tuple(consts),
                     co.co_names,
                     co.co_varnames,
                     co.co_filename,
                     co.co_name,
                     co.co_firstlineno,
                     co.co_lnotab )


def bind(f, globals):
    """Returns a new function whose code object has been
    bound by bind_code()"""
    newcode = bind_code(f.func_code, globals)
    defaults = f.func_defaults or ()
    return make_function(newcode, f.func_globals, f.func_name, defaults)

if type(__builtins__) == dict:
    builtins = __builtins__
else:
    builtins = __builtins__.__dict__
    
bind_code_opt = bind(bind_code, builtins )
bind_code_opt = bind(bind_code_opt, globals() )


def optimize_module(m, global_consts):
    if not inspect.ismodule(m):
        raise TypeError
    d = {}
    for i in global_consts:
        v = m.__dict__.get(i)
        d[i] = v
    builtins = m.__builtins__
    for name, f in m.__dict__.items():
        if inspect.isfunction(f):
            f = bind(f, builtins)
            if d:
                f = bind(f, d)
            m.__dict__[name] = f
            



def analyze_code(co, globals, consts_dict, consts_list):
    """Take a code object and a dictionnary and returns a
    new code object where the opcodes LOAD_GLOBAL are replaced
    by LOAD_CONST whenever the global's name appear in the
    dictionnary"""
    modified_globals = []
    for c in co.co_consts:
        if c not in consts_list:
            consts_list.append(c)
    modified = []
    code = co.co_code
    new_code = ""
    n = len(code)
    i = 0
    extended_arg = 0
    while i < n:
        c = code[i]
        op = ord(c)
        i += 1
        if op >= HAVE_ARGUMENT:
            oparg = ord(code[i]) + ord(code[i+1])*256 + extended_arg
            extended_arg = 0
            i += 2
        else:
            oparg = None
        if op == EXTENDED_ARG:
            extended_arg = oparg*65536L

        if op == LOAD_GLOBAL:
            name = co.co_names[oparg]
            if globals.has_key(name):
                k = consts_dict.get(name,None)
                if k == None:
                    k = len(consts_list)
                    consts_dict[name] = k
                    consts_list.append(globals[name])
        if op == STORE_GLOBAL:
            name = co.co_names[oparg]
            if globals.has_key(name):
                modified_globals.append(name)
    return modified_globals

def rewrite_code(co, consts_dict, consts_tuple):
    """Take a code object and a dictionnary and returns a
    new code object where the opcodes LOAD_GLOBAL are replaced
    by LOAD_CONST whenever the global's name appear in the
    dictionnary"""
    code = co.co_code
    new_code = ""
    n = len(code)
    i = 0
    consts_list = list(consts_tuple)
    while i < n:
        c = code[i]
        op = ord(c)
        i += 1
        extended_arg = 0
        if op >= HAVE_ARGUMENT:
            oparg = ord(code[i]) + ord(code[i+1])*256+extended_arg
            extended_arg = 0
            i += 2
        else:
            oparg = None
        if op == EXTENDED_ARG:
            extended_arg = oparg*65536L
        elif op == LOAD_GLOBAL:
            name = co.co_names[oparg]
            k = consts_dict.get(name)
            if k is not None:
                op = LOAD_CONST
                oparg = k
        elif op == LOAD_CONST:
            val = co.co_consts[oparg]
            oparg = consts_list.index(val)
        new_code += chr(op)
        if oparg is not None:
            new_code += chr(oparg & 255)
            new_code += chr( (oparg>>8) & 255 )
            
    return make_code(co.co_argcount,
                     co.co_nlocals,
                     co.co_stacksize,
                     co.co_flags,
                     new_code,
                     consts_tuple,
                     co.co_names,
                     co.co_varnames,
                     co.co_filename,
                     co.co_name,
                     co.co_firstlineno,
                     co.co_lnotab )

def optimize_module_2(m, globals_consts, bind_builtins=1):
    if not inspect.ismodule(m):
        raise TypeError
    consts_dict = {}
    consts_list = []
    if type(globals_consts) == list or type(globals_consts) == tuple:
        globals = {}
        for i in globals_consts:
            v = m.__dict__.get(i)
            globals[i] = v
    else:
        globals = globals_consts
    if bind_builtins:
        for builtin_name, builtin_value in m.__builtins__.items():
            # this way it is possible to redefine a builtin in globals_consts
            globals.setdefault(builtin_name, builtin_value)
    functions = {}
    for name, f in m.__dict__.items():
        if inspect.isfunction(f):
            functions[name] = f
            analyze_code(f.func_code, globals, consts_dict, consts_list)
    consts_list = tuple(consts_list)
    for name, f in functions.items():
        newcode = rewrite_code(f.func_code, consts_dict, consts_list)
        defaults = f.func_defaults or ()
        m.__dict__[name] = make_function(newcode, f.func_globals, f.func_name,
                                         defaults)
        

def run_bench(n):
    from time import time
    t = time()
    g = globals()
    for i in range(n):
        test = bind(bind_code,g)
    t1 = time()-t
    bind2 = bind(bind, {'bind_code':bind_code_opt})
    t = time()
    for i in range(n):
        test=bind2(bind_code,g)
    t2 = time()-t
    print "1 regular version", t1
    print "2 optimized version", t2
    print "ratio (1-2)/1 : %f %%" % (100.*(t1-t2)/t1)
    

def test_pystone():
    from test import pystone
    for i in range(5):
        pystone.main()
    optimize_module(pystone,('TRUE','FALSE','Proc0','Proc1','Proc2','Proc3',
                             'Proc4','Proc5','Proc6','Proc7','Proc8','Func1',
                             'Func2','Func3'))
    optimize_module(pystone, builtins.keys())
    for i in range(5):
        pystone.main()


if __name__ == "__main__":
    run_bench(1000)
