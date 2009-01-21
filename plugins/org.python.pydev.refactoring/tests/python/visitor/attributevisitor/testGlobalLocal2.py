class foo:
    
    fooAttr = 3
    class nested:
        self.nestAttr = 3
        
    def fooMeth():
        self.fooMethAttr = 3
    print "bar"
    
class bar:
    barAttr = "bar"
    print "foo"

##r
# 4
# foo fooAttr
# nested nestAttr
# foo fooMethAttr
# bar barAttr
# 2
# foo fooAttr
# foo fooMethAttr