a = 5
b = 10
c = 1
d = 222
class Foo:
    c = 2
    # before
    global d # on-line
    # fater
    global a, b # on-ine
    # after
    def foometh(self):
        print a, b, c, d
    
    print a, b, c, d


foo_obj = Foo()
foo_obj.foometh()
## Will print
## 5 10 2 222
## 5 10 1 222