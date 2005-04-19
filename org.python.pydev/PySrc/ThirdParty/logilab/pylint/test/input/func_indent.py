"""docstring"""
__revision__ = '$Id: func_indent.py,v 1.4 2005-04-19 14:39:11 fabioz Exp $'

def totoo():
 """docstring"""
 print 'malindented'

def tutuu():
    """docstring"""
    print 'good indentation'

def titii():
     """also malindented"""

def tataa(kdict):
    """blank line unindented"""
    for key in [1, 2, 3]:
        key = key.lower()
    
        if kdict.has_key(key):
            del kdict[key]

