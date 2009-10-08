class CoolApproach(object):
    # this tests also a tuple "special case"
    def foodeco(constant, arg1, arg2="foo", arg3="usual", *arg4, **arg5):
        print arg1, arg2, arg3, arg4
    
    @foodeco(('arg_3', ), 2)
    def __init__(self, arg_1, (arg_2, arg_3), arg_4, arg_5):
        # .. at this point all parameters except for 'arg_3' have been
        # copied to object attributes
        # .. now do whatever initialisation is required ..
        print ">> In initialiser, self.arg_1 = '%s'" % self.arg_1


class FooApproach(CoolApproach):
    # this tests also a tuple "special case"
    @foodeco(5, ('arg_3', ), arg_2="test", arg_1="bla", *arg_4, **arg_5)
    def __init__(self, arg_1, (arg_2, arg_3), *arg_4, **arg_5):
        # .. at this point all parameters except for 'arg_3' have been
        # copied to object attributes
        # .. now do whatever initialisation is required ..
        print ">> In initialiser, self.arg_1 = '%s'" % self.arg_1