class A:
    
    def before(self):
        pass
    
    def my_method(self):
        print(##|self.attribute##|)
    

##c
'''
<config>
  <offsetStrategy>8</offsetStrategy>
</config>
'''
##r

class A:
    
    def before(self):
        pass
    

    def extracted_method(self):
        return self.attribute

    def my_method(self):
        print(self.extracted_method())
