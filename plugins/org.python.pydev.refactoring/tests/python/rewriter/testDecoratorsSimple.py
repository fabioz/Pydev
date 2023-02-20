def showdoc(f):
    print('%s: %s' % (f.__name__, f.__doc__))
    return f

@showdoc
def f1(): 
    "a docstring"
    print("decorators are fun")
    
##r
def showdoc(f):
    print('%s: %s' % (f.__name__, f.__doc__))
    return f

@showdoc
def f1():
    "a docstring"
    print("decorators are fun")    
    