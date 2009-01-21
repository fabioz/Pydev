class Foo:
    
    class nested:
        print self.nestAttr
    
    def meth(self):
        print self.bar * self.bar
    print "bar"
    
##r
# 2
# nested nestAttr
# Foo bar
# 1
# Foo bar