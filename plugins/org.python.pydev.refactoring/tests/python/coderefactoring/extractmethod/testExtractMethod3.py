class A:
  
    def test(self):
        attribute = ##|"hello" # peptic rocks##|
    
    def my_method(self):
        print self.attribute
        
a = A()
a.test()

##r

class A:

    def extracted_method(self):
        return "hello" # peptic rocks

  
    def test(self):
        attribute = self.extracted_method()
    
    def my_method(self):
        print self.attribute
        
a = A()
a.test()