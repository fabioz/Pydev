class Foo:
    
    class Nested:
        print(self.nest_attr)
    
    def meth(self):
        print(self.bar * self.bar)
    print("bar")
    
##r

# 2
# Nested nest_attr
# Foo bar
# 1
# Foo bar