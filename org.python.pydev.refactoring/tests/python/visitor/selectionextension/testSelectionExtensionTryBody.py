class A:
    def test(self):
        a = 1
        try:
            print a
            print "foo"
        except:
            print b
            print "bar"
        
        var = a * a
        print var
            
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.test()

##c selection starts at the first char, but we have to normalize indentation!
'''
<config>
  <offset>68</offset>
  <selectionLength>31</selectionLength>
</config>
'''

##r
print a
print "foo"
