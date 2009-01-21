from shape import Shape

def testValidShape(shape):
    pass

class BetterShape(shape.Shape):
    pass

shape = BetterShape(30, 30)
print shape
testValidShape(shape)
##c extract method from module body
'''
<config>
  <offset>104</offset>
  <selectionLength>39</selectionLength>
  <offsetStrategy>0</offsetStrategy>
</config>
'''

##r
from shape import Shape


def pepticMethod():
    shape = BetterShape(30, 30)
    print shape
    return shape

def testValidShape(shape):
    pass

class BetterShape(shape.Shape):
    pass

shape = pepticMethod()
testValidShape(shape)