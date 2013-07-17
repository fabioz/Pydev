
#===================================================================================================
# SplitTextInCommas
#===================================================================================================
def SplitTextInCommas(txt):
    '''
    Splits the text in the commas, considering it's python code (right now, only takes into account
    tuples, but it should be extended for lists, dicts, strings, etc)
    '''
    splitted = []

    parens_level = 0

    buf = ''
    for c in txt:
        if c == '(':
            parens_level += 1

        if c == ')':
            parens_level -= 1


        if parens_level == 0:
            if c == ',':
                splitted.append(buf)
                buf = ''
            else:
                buf += c
        else:
            buf += c


    if buf:
        splitted.append(buf)

    return splitted

#===================================================================================================
# main
#===================================================================================================
if __name__ == '__main__':
    #Not run when it comes from the editor
    import unittest

    class Test(unittest.TestCase):

        def testIt(self):
            self.assertEqual(SplitTextInCommas('a,b,c'), ['a', 'b', 'c'])
            self.assertEqual(SplitTextInCommas('(a,b),c'), ['(a,b)', 'c'])

    unittest.main()