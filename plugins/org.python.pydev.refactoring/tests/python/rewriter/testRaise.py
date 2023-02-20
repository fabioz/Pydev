class fooexception(Exception):
    def __init__(self, msg):
        Exception.__init__(self)
        print(msg)
    
    def __init__(self):
        Exception.__init__(self)
        print("i am a fooexception")


data = 2
# before
raise fooexception # on-line
# after
# before
raise fooexception("bla") # on-line
# after
raise fooexception([1, 2, 3])
raise fooexception(list(range(3)))
raise fooexception(1, 2, 3)
raise fooexception(1, 2, 3).with_traceback(1)
# after
raise fooexception(1, 2, 3).with_traceback("foo") # on-line
# after
raise fooexception(1, 2, 3).with_traceback((1, 2, 3))
raise fooexception(1, 2, 3).with_traceback([1, 2, 3])
# after
raise fooexception(1, 2, 3).with_traceback(list(range(1))) # on-line
# after
raise fooexception(1, 2, 3).with_traceback((1 + 1))
raise
raise fooexception(1, 2, 3).with_traceback(1 + 1)
raise