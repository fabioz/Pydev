from shape import Shape

def test_valid_shape(shape):
    pass

class BetterShape(shape.Shape):
    pass

##|shape = BetterShape(30, 30)
print shape##|
test_valid_shape(shape)

##r extract method from module body

from shape import Shape


def extracted_method():
    shape = BetterShape(30, 30)
    print shape
    return shape

def test_valid_shape(shape):
    pass

class BetterShape(shape.Shape):
    pass

shape = extracted_method()
test_valid_shape(shape)