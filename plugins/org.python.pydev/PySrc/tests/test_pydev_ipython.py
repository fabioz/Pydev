import unittest


    
    

#=======================================================================================================================
# TestCase
#=======================================================================================================================
class TestCase(unittest.TestCase):
    
    def setUp(self):
        unittest.TestCase.setUp(self)
    
    def tearDown(self):
        unittest.TestCase.tearDown(self)
        
    def testIPython(self):
        from pydev_ipython_console import PyDevFrontEnd
        front_end = PyDevFrontEnd()
        
        front_end.input_buffer = 'if True:'
        self.assert_(not front_end._on_enter())
        
        front_end.input_buffer = 'if True:\n' + \
            front_end.continuation_prompt()+'    a = 10\n'
        self.assert_(not front_end._on_enter())
        
        
        front_end.input_buffer = 'if True:\n' + \
            front_end.continuation_prompt()+'    a = 10\n\n'
        self.assert_(front_end._on_enter())
        
        
#        front_end.input_buffer = '  print a'
#        self.assert_(not front_end._on_enter())
#        front_end.input_buffer = ''
#        self.assert_(front_end._on_enter())
        
        
#        front_end.input_buffer = 'a.'
#        front_end.complete_current_input()
#        front_end.input_buffer = 'if True:'
#        front_end._on_enter()
        front_end.input_buffer = 'a = 30'
        front_end._on_enter()
        front_end.input_buffer = 'print a'
        front_end._on_enter()
        front_end.input_buffer = 'a?'
        front_end._on_enter()
        print front_end.complete('%')
#        front_end.input_buffer = 'print raw_input("press enter\\n")'
#        front_end._on_enter()
#        
        
#=======================================================================================================================
# main
#=======================================================================================================================
if __name__ == '__main__':
    unittest.main()