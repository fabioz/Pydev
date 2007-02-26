from somepackage import bla as bar
import gtk
import md5 as hash

class foo:
    
    fooAttr = 3
    class nested:
        self.nestAttr = 3
    print hash.blocksize()
    button = gtk.Button()
    # must ignore self.fooMeth but detect methAssign
    methAssign = self.fooMeth()
    gtk.Image()
    bar.moduleCall
        
    def fooMeth():
        self.fooMethAttr = 3
    print "bar"
    
class bar:
    barAttr = "bar"
    print "foo"

##r
# 6
# foo fooAttr
# nested nestAttr
# foo button
# foo methAssign
# foo fooMethAttr
# bar barAttr
# 4
# foo fooAttr
# foo button
# foo methAssign
# foo fooMethAttr