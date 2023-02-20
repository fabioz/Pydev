def test_arg_lists_1(*args, **kwargs):
    print('args:', args)
    print('kwargs:', kwargs)

test_arg_lists_1('aaa', 'bbb', arg1='ccc', arg2='ddd')
def test_arg_lists_2(arg0, *args, **kwargs):
    print('arg0: "%s"' % arg0)
    print('args:', args)
    print('kwargs:', kwargs)

def test_arg_lists_3(arg0):
    pass

def test_arg_lists_4(arg0, test=123, bla=123):
    pass

def test_arg_lists_5(arg0, test=123, bla=123):
    pass

print('=' * 40)
test_arg_lists_2('a first argument', 'aaa', 'bbb', arg1='ccc', arg2='ddd')