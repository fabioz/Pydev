class MyClass2(MyClass):
    
    
    def m1(self, *a):
        new_var = self.call(
            ##|
            [
            1, 
            2,
            self.call(*a)
            ]
            ##|
        )
        pass

    def call(self, a):
        print a

##r
class MyClass2(MyClass):

    def extracted_method(self, a):
        return [1, 
            2, 
            self.call(*a)]

    
    
    def m1(self, *a):
        new_var = self.call(
            
            self.extracted_method(a)
            
        )
        pass

    def call(self, a):
        print a
