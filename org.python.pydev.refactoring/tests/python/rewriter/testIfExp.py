# before
if ([x(False) for x in (lambda x: False if x else True, lambda x: True if x else False) if x(False)] == [True]): # on-line
    # after
    print "foouebertest"

# after everything