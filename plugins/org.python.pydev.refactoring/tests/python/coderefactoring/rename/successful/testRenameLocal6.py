x = 5

class Z(object):
    def __init__(self):
        x = 10
        
        def foo():
            x = 10
            print x
            
        print x

    def barfoo():
        print x
        
print x##|

##r

p = 5

class Z(object):
    def __init__(self):
        x = 10
        
        def foo():
            x = 10
            print x
            
        print x

    def barfoo():
        print p
        
print p