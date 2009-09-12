class A:
    bar = 123
    def foo(self):
        barfoo = 234
        def bar_method(self):
  ##|          print barfoo##|          
        print "foo"
        print self.bar
        bar_method(self)
        
a = A()
a.foo()

##r

class A:

    def extracted_method():
        print barfoo

    bar = 123
    def foo(self):
        barfoo = 234
        def bar_method(self):
            self.extracted_method()          
        print "foo"
        print self.bar
        bar_method(self)
        
a = A()
a.foo()