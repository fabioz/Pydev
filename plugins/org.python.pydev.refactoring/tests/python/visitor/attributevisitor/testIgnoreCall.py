from somepackage import bla as bar
import gtk
import md5 as hash

class Foo:
    
    foo_attr = 3
    class Nested:
        self.nest_attr = 3
    print(hash.blocksize())
    button = gtk.Button()
    # must ignore self.foo_meth but detect meth_assign
    meth_assign = self.foo_meth()
    gtk.Image()
    bar.module_call
        
    def foo_meth():
        self.foo_meth_attr = 3
    print("bar")
    
class bar:
    bar_attr = "bar"
    print("foo")

##r

# 6
# Foo foo_attr
# Nested nest_attr
# Foo button
# Foo meth_assign
# Foo foo_meth_attr
# bar bar_attr
# 4
# Foo foo_attr
# Foo button
# Foo meth_assign
# Foo foo_meth_attr