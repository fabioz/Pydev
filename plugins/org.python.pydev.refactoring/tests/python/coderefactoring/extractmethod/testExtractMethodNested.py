class A:
    bar = 123
    def foo(self):
        barfoo = 234
        def bar_method(self):
            pass
  ##|          print(barfoo)##|          
        print("foo")
        print(self.bar)
        bar_method(self)
        
a = A()
a.foo()

##r

class A:

    def extracted_method(self):
        return print(barfoo)

    bar = 123
    def foo(self):
        barfoo = 234
        def bar_method(self):
            pass
            self.extracted_method()          
        print("foo")
        print(self.bar)
        bar_method(self)
        
a = A()
a.foo()