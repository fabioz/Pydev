#Should not be found when we're in Py3K because it'll resolve to the root (which is the opposite in Python 2.x)
from relative import NotFound 
print(NotFound)