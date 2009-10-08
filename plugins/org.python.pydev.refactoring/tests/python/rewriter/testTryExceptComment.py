import sys
# after import
try: # on-line
    print 5 / 0
# after first body
except ZeroDivisionError: # on-line
    # after zerodivision
    print "foomsg" # on-line print "foomsg"
# this comment is dropped by the parser?
try:
    f = open('testTryExcept.py')
    s = f.readline()
    i = int(s.strip())
except IOError, (errno, strerror):
    print "I/O error(%s): %s" % (errno, strerror)
except ValueError:
    print "Could not convert data to an integer."
except:
    print "Unexpected error:", sys.exc_info()[0]
    raise
# after simple raise
else: # on-line else
# after else
    print "it works"

# after everthing