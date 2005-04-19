'''unit tests for ureports.text_writer
'''

__revision__ = "$Id: unittest_ureports_text.py,v 1.3 2005-04-19 14:39:13 fabioz Exp $"

import unittest
from utils import WriterTC
from logilab.common.testlib import TestCase

from logilab.common.ureports.text_writer import TextWriter

class TextWriterTC(TestCase, WriterTC):
    def setUp(self):
        self.writer = TextWriter()

    # Section tests ###########################################################
    section_base = '''
Section title
=============
Section\'s description.
Blabla bla

'''
    section_nested = '''
Section title
=============
Section\'s description.
Blabla bla

Subsection
----------
Sub section description


'''
    
    # List tests ##############################################################
    list_base = '''
* item1
* item2
* item3
* item4'''
    
    nested_list = '''
* blabla
  - 1
  - 2
  - 3

* an other point'''
    
    # Table tests #############################################################
    table_base = '''
head1 head2 
cell1 cell2 
'''
    field_table = '''
f1  : v1
f22 : v22
f333: v333
'''
    advanced_table = '''
field                value 
:::::::::::::::::::::::::::
f1                   v1    
f22                  v22   
f333                 v333  
http://www.perdu.com 
'''


    # VerbatimText tests ######################################################
    verbatim_base = '''::

    blablabla

'''
    
if __name__ == '__main__':
    unittest.main()
