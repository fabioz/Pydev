"""
Unittests for table management
"""

__revision__ = '$Id: unittest_table.py,v 1.1 2005-01-21 17:46:21 fabioz Exp $'

import unittest
import sys 
from logilab.common.table import Table, TableStyleSheet


class TableTC(unittest.TestCase):
    """Table TestCase class
    """

    def setUp(self):
        """Creates a default table
        """
        self.table = Table()
        self.table.create_rows(['row1', 'row2', 'row3'])
        self.table.create_columns(['col1', 'col2'])
        
        
    def test_set_column(self):
        """Tests that table.set_column() works fine.
        """
        self.table.set_column(0, range(3))
        self.assertEquals(self.table.get_element(0,0), 0)
        self.assertEquals(self.table.get_element(1,0), 1)
        self.assertEquals(self.table.get_element(2,0), 2)


    def test_set_column_by_id(self):
        """Tests that table.set_column_by_id() works fine.
        """
        self.table.set_column_by_id('col1', range(3))
        self.assertEquals(self.table.get_element(0,0), 0)
        self.assertEquals(self.table.get_element(1,0), 1)
        self.assertEquals(self.table.get_element(2,0), 2)


    def test_get_column(self):
        """Tests that table.get_column() works fine.
        """
        self.table.set_cell(0, 1, 12)
        self.table.set_cell(2, 1, 13)
        column = self.table.get_column(1)
        self.assertEquals(column, [12,0,13])


    def test_get_columns(self):
        """Tests if table.get_columns() works fine.
        """
        self.table.set_cell(0, 1, 12)
        self.table.set_cell(2, 1, 13)
        columns = self.table.get_columns()
        self.assertEquals(columns, [[0,0,0], [12,0,13]])

    
    def test_insert_column(self):
        """Tests that table.insert_column() works fine.
        """
        self.table.insert_column(1, range(3), "inserted_column")
        self.assertEquals(self.table.get_column(1), [0,1,2])
        self.assertEquals(self.table.col_names,
                          ['col1', 'inserted_column', 'col2'])

    
    def test_delete_column(self):
        """Tests that table.delete_column() works fine.
        """
        self.table.delete_column(1)
        self.assertEquals(self.table.col_names,['col1'])
        self.assertEquals(self.table.get_column(0), [0,0,0])


    def test_transpose(self):
        """Tests that table.transpose() works fine.
        """
        self.table.append_column(range(5,8), 'col3')
        ttable = self.table.transpose()
        self.assertEquals(ttable.row_names, ['col1', 'col2', 'col3'])
        self.assertEquals(ttable.col_names, ['row1', 'row2', 'row3'])
        self.assertEquals(ttable.data, [[0,0,0], [0,0,0], [5,6,7]])



    def test_sort_table(self):
        """Tests the table sort by column
        """
        self.table.set_column(0, [3, 1, 2])
        self.table.set_column(1, [1, 2, 3])
        self.table.sort_by_column_index(0)
        self.assertEquals(self.table.row_names, ['row2', 'row3', 'row1'])
        self.assertEquals(self.table.data, [[1, 2], [2, 3], [3, 1]])
        self.table.sort_by_column_index(1, 'desc')
        self.assertEquals(self.table.row_names, ['row3', 'row2', 'row1'])
        self.assertEquals(self.table.data, [[2, 3], [1, 2], [3, 1]])
        

class TableStyleSheetTC(unittest.TestCase):
    """The Stylesheet test case
    """

    def setUp(self):
        """Builds a simple table to test the stylesheet
        """
        self.table = Table()
        self.table.create_row('row1')
        self.table.create_columns(['a','b','c'])
        self.stylesheet = TableStyleSheet()
        
    def test_add_rule(self):
        """Tests that the regex pattern works as expected.
        """
        rule = '0_2 = sqrt(0_0**2 + 0_1**2)'
        self.stylesheet.add_rule(rule)
        self.table.set_row(0, [3,4,0])
        self.table.apply_stylesheet(self.stylesheet)
        self.assertEquals(self.table.get_row(0),
                          [3,4,5])


    def test_rowavg_rule(self):
        """Tests that add_rowavg_rule works as expected
        """
        self.table.set_row(0, [10,20,0])
        self.stylesheet.add_rowavg_rule((0,2), 0, 0, 1)
        self.table.apply_stylesheet(self.stylesheet)
        val = self.table.get_element(0,2)
        self.assert_(int(val) == 15)
        

    def test_rowsum_rule(self):
        """Tests that add_rowsum_rule works as expected
        """
        self.table.set_row(0, [10,20,0])
        self.stylesheet.add_rowsum_rule((0,2), 0, 0, 1)
        self.table.apply_stylesheet(self.stylesheet)
        val = self.table.get_element(0,2)
        self.assert_(val == 30)
        

    def test_colavg_rule(self):
        """Tests that add_colavg_rule works as expected
        """
        self.table.set_row(0, [10,20,0])
        self.table.append_row([12,8,3], 'row2')
        self.table.create_row('row3')
        self.stylesheet.add_colavg_rule((2,0), 0, 0, 1)
        self.table.apply_stylesheet(self.stylesheet)
        val = self.table.get_element(2,0)
        self.assert_(int(val) == 11)
        

    def test_colsum_rule(self):
        """Tests that add_colsum_rule works as expected
        """
        self.table.set_row(0, [10,20,0])
        self.table.append_row([12,8,3], 'row2')
        self.table.create_row('row3')
        self.stylesheet.add_colsum_rule((2,0), 0, 0, 1)
        self.table.apply_stylesheet(self.stylesheet)
        val = self.table.get_element(2,0)
        self.assert_(val == 22)


    
def suite():
    loader = unittest.TestLoader()
    testsuite = loader.loadTestsFromModule(sys.modules[__name__])
    return testsuite    
    
def Run():
    testsuite = suite()
    runner = unittest.TextTestRunner()
    return runner.run(testsuite)
    
if __name__ == '__main__':
    Run()
