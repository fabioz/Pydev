class Foo:
    
    foo_attr = 3
    class Nested:
        self.nest_attr = 3
        
    def foo_meth():
        self.foo_meth_attr = 3
    print "bar"
    
class Bar:
    bar_attr = "bar"
    print "foo"

##r

# 4
# Foo foo_attr
# Nested nest_attr
# Foo foo_meth_attr
# Bar bar_attr
# 2
# Foo foo_attr
# Foo foo_meth_attr