class A:
    bar = 123
    def foo(self):
        barfoo = 234
        def barMeth(self):
            print barfoo          
        print "foo"
        print self.bar
        barMeth(self)
        
a = A()
a.foo()

##c
'''
<config>
  <offset>92</offset>
  <selectionLength>22</selectionLength>
  <offsetStrategy>0</offsetStrategy>
</config>
'''

##r
class A:

    def pepticMethod():
        print barfoo

    bar = 123
    def foo(self):
        barfoo = 234
        def barMeth(self):
            self.pepticMethod()          
        print "foo"
        print self.bar
        barMeth(self)
        
a = A()
a.foo()