'''unit tests for ureports.html_writer
'''

__revision__ = "$Id: unittest_ureports_html.py,v 1.1 2005-01-21 17:46:21 fabioz Exp $"

import unittest

from utils import WriterTC
from logilab.common.testlib import TestCase

from logilab.common.ureports.html_writer import *

class HTMLWriterTC(TestCase, WriterTC):

    def setUp(self):
        self.writer = HTMLWriter(1)
        
    # Section tests ###########################################################
    section_base = '''<div>
<h1>Section title</h1>
<p>Section\'s description.
Blabla bla</p></div>
'''
    section_nested = '''<div>\n<h1>Section title</h1>\n<p>Section\'s description.\nBlabla bla</p><div>\n<h2>Subsection</h2>\n<p>Sub section description</p></div>\n</div>\n'''
    
    # List tests ##############################################################
    list_base = '''<ul>\n<li>item1</li>\n<li>item2</li>\n<li>item3</li>\n<li>item4</li>\n</ul>\n'''
    
    nested_list = '''<ul>
<li><p>blabla<ul>
<li>1</li>
<li>2</li>
<li>3</li>
</ul>
</p></li>
<li>an other point</li>
</ul>
'''
    
    # Table tests #############################################################
    table_base = '''<table>\n<tr class="odd">\n<td>head1</td>\n<td>head2</td>\n</tr>\n<tr class="even">\n<td>cell1</td>\n<td>cell2</td>\n</tr>\n</table>\n'''
    field_table = '''<table class="field" id="mytable">\n<tr class="odd">\n<td>f1</td>\n<td>v1</td>\n</tr>\n<tr class="even">\n<td>f22</td>\n<td>v22</td>\n</tr>\n<tr class="odd">\n<td>f333</td>\n<td>v333</td>\n</tr>\n</table>\n'''
    advanced_table = '''<table class="whatever" id="mytable">\n<tr class="header">\n<th>field</th>\n<th>value</th>\n</tr>\n<tr class="even">\n<th>f1</th>\n<td>v1</td>\n</tr>\n<tr class="odd">\n<th>f22</th>\n<td>v22</td>\n</tr>\n<tr class="even">\n<th>f333</th>\n<td>v333</td>\n</tr>\n<tr class="odd">\n<th> <a href="http://www.perdu.com">toi perdu ?</a></th>\n</tr>\n</table>\n'''


    # VerbatimText tests ######################################################
    verbatim_base = '''<pre>blablabla</pre>'''
        
if __name__ == '__main__':
    unittest.main()
