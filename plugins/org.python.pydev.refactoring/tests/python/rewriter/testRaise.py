class fooexception(Exception):
    def __init__(self, msg):
        Exception.__init__(self)
        print msg
    
    def __init__(self):
        Exception.__init__(self)
        print "i am a fooexception"


data = 2
raise "foo"
raise "foo", data
# before
raise fooexception # on-line
# after
# before
raise fooexception, "bla" # on-line
# after
raise fooexception, [1, 2, 3]
raise fooexception, range(3)
raise fooexception, (1, 2, 3)
raise fooexception, (1, 2, 3), 1
# after
raise fooexception, (1, 2, 3), "foo" # on-line
# after
raise fooexception, (1, 2, 3), (1, 2, 3)
raise fooexception, (1, 2, 3), [1, 2, 3]
# after
raise fooexception, (1, 2, 3), range(1) # on-line
# after
raise fooexception, (1, 2, 3), (1 + 1)
raise
raise fooexception, (1, 2, 3), 1 + 1
raise