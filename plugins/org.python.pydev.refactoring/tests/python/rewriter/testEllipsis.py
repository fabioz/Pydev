import numpy
print(numpy.__version__)
a = numpy.array(([0, 1], [1, 3]))
print(a[..., 1])
print(a[..., ...])