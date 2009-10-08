# before1
# before2
if ([x(False) for x in (lambda x:False if x else True, lambda x:True if x else False) if x(False)] == [True]): # on-line
    # after1
    # after2
    print "foouebertest"
# after everything1
# after everything2