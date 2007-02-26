class A:
    def test(self):
        a = 1
        try:
            print a
            print "foo"
        except:
            print b
            print "bar"
        else:
            print "foo2"
            print "bar2"
        finally:
            print "it works"
            
        var = a * a
        print var
            
    def myMethod(self):
        print self.anAttribute
        
a = A()
a.test()

##c selection starts at the first char, but we have to normalize indentation!
'''
<config>
  <offset>115</offset>
  <selectionLength>46</selectionLength>
</config>
'''

##r
print b
print "bar"