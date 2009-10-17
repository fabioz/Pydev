class A(object):
    
    def myMethod(self):
        self.a = 10

##c
'''
<config>
  <classSelection>0</classSelection>
  <attributeSelection>
    <int>0</int>
  </attributeSelection>
  <offsetStrategy>1</offsetStrategy>
</config>
'''

##r
class A(object):

    def __init__(self, a):
        self.a = a

    
    def myMethod(self):
        self.a = 10