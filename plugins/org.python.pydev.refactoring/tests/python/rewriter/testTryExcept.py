import sys
## NOTE: try except finally is supported by Python 2.5!
try:
    print(5 / 0)
except ZeroDivisionError:
    print("foomsg")
try:
    f = open('testTryExcept.py')
    s = f.readline()
    i = int(s.strip())
except IOError as xxx_todo_changeme:
    (errno, strerror) = xxx_todo_changeme.args
    print("I/O error(%s): %s" % (errno, strerror))
except ValueError:
    print("Could not convert data to an integer.")
except:
    print("Unexpected error:", sys.exc_info()[0])
    raise
else:
    print("it works")

finally:
    close(f)
    print("foo")

