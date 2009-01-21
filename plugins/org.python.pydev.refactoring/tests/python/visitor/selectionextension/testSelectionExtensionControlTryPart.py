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
            print "hopefully"
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
  <offset>51</offset>
  <selectionLength>33</selectionLength>
</config>
'''

##r
# Invalid selection:
# try:
#     print a