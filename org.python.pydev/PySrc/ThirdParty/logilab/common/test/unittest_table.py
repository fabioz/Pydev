"""
Unittests for table management
"""

__revision__ = '$Id: unittest_table.py,v 1.2 2005-02-16 16:45:45 fabioz Exp $'

import unittest
import sys 
from logilab.common.table import Table, TableStyleSheet, DocbookTableWriter, \
     DocbookRenderer, TableStyle, TableWriter, TableCellRenderer
from cStringIO import StringIO

class TableTC(unittest.TestCase):
    """Table TestCase class
    """

    def setUp(self):
        """Creates a default table
        """
        self.table = Table()
        self.table.create_rows(['row1', 'row2', 'row3'])
        self.table.create_columns(['col1', 'col2'])


    def test_indexation(self):
        """we should be able to use [] to access rows"""
        row0 = self.table.data[0]
        row1 = self.table.data[1]
        self.assert_(self.table[0] is row0)
        self.assert_(self.table[1] is row1)

    def test_get_rows(self):
        """tests Table.get_rows()"""
        self.assertEquals(self.table.get_rows(), [[0, 0], [0, 0], [0, 0]])
        self.table.insert_column(1, range(3), 'supp')
        self.assertEquals(self.table.get_rows(), [[0, 0, 0], [0, 1, 0], [0, 2, 0]])
        
    def test_dimensions(self):
        """tests table dimensions"""
        self.assertEquals(self.table.get_dimensions(), (3, 2))
        self.table.insert_column(1, range(3), 'supp')
        self.assertEquals(self.table.get_dimensions(), (3, 3))
        
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
        self.assertRaises(KeyError, self.table.set_column_by_id, 'col123', range(3))

    def test_cells_ids(self):
        """tests that we can access cells by giving row/col ids"""
        self.assertRaises(KeyError, self.table.set_cell_by_ids, 'row12', 'col1', 12)
        self.assertRaises(KeyError, self.table.set_cell_by_ids, 'row1', 'col12', 12)
        self.assertEquals(self.table.get_element(0,0), 0)
        self.table.set_cell_by_ids('row1', 'col1', 'DATA')
        self.assertEquals(self.table.get_element(0,0), 'DATA')
        self.assertRaises(KeyError, self.table.set_row_by_id, 'row12', [])
        self.table.set_row_by_id('row1', ['1.0', '1.1'])
        self.assertEquals(self.table.get_element(0,0), '1.0')

    def test_insert_row(self):
        """tests a row insertion"""
        tmp_data = ['tmp1', 'tmp2']
        self.table.insert_row(1, tmp_data, 'tmprow')
        self.assertEquals(self.table.get_row(1), tmp_data)
        self.assertEquals(self.table.get_row_by_id('tmprow'), tmp_data)
        self.table.delete_row_by_id('tmprow')
        self.assertRaises(KeyError, self.table.delete_row_by_id, 'tmprow')
        self.assertEquals(self.table.get_row(1), [0, 0])
        self.assertRaises(ValueError, self.table.get_row_by_id, 'tmprow')
        
    def test_get_column(self):
        """Tests that table.get_column() works fine.
        """
        self.table.set_cell(0, 1, 12)
        self.table.set_cell(2, 1, 13)
        self.assertEquals(self.table.get_column(1), [12,0,13])
        self.assertEquals(self.table.get_column_by_id('col2'), [12,0,13])

    def test_get_columns(self):
        """Tests if table.get_columns() works fine.
        """
        self.table.set_cell(0, 1, 12)
        self.table.set_cell(2, 1, 13)
        self.assertEquals(self.table.get_columns(), [[0,0,0], [12,0,13]])
    
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
        self.assertEquals(self.table.col_names, ['col1'])
        self.assertEquals(self.table.get_column(0), [0,0,0])
        self.assertRaises(KeyError, self.table.delete_column_by_id, 'col2')
        self.table.delete_column_by_id('col1')
        self.assertEquals(self.table.col_names, [])

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
        
    def test_sort_by_id(self):
        """tests sort_by_column_id()"""
        self.table.set_column_by_id('col1', [3, 1, 2])
        self.table.set_column_by_id('col2', [1, 2, 3])
        self.table.sort_by_column_id('col1')
        self.assertRaises(KeyError, self.table.sort_by_column_id, 'col123')
        self.assertEquals(self.table.row_names, ['row2', 'row3', 'row1'])
        self.assertEquals(self.table.data, [[1, 2], [2, 3], [3, 1]])
        self.table.sort_by_column_id('col2', 'desc')
        self.assertEquals(self.table.row_names, ['row3', 'row2', 'row1'])
        self.assertEquals(self.table.data, [[2, 3], [1, 2], [3, 1]])

    def test_pprint(self):
        """only tests pprint doesn't raise an exception"""
        self.table.pprint()
        str(self.table)
        

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
        # We don't want anything to be printed
        self.stdout_backup = sys.stdout
        sys.stdout = StringIO()

    def tearDown(self):
        sys.stdout = self.stdout_backup
        
    def test_add_rule(self):
        """Tests that the regex pattern works as expected.
        """
        rule = '0_2 = sqrt(0_0**2 + 0_1**2)'
        self.stylesheet.add_rule(rule)
        self.table.set_row(0, [3,4,0])
        self.table.apply_stylesheet(self.stylesheet)
        self.assertEquals(self.table.get_row(0),
                          [3,4,5])
        self.assertEquals(len(self.stylesheet.rules), 1)
        self.stylesheet.add_rule('some bad rule with bad syntax')
        self.assertEquals(len(self.stylesheet.rules), 1, "Ill-formed rule mustn't be added")
        self.assertEquals(len(self.stylesheet.instructions), 1, "Ill-formed rule mustn't be added")

    def test_stylesheet_init(self):
        """tests Stylesheet.__init__"""
        rule = '0_2 = 1'
        sheet = TableStyleSheet([rule, 'bad rule'])
        self.assertEquals(len(sheet.rules), 1, "Ill-formed rule mustn't be added")
        self.assertEquals(len(sheet.instructions), 1, "Ill-formed rule mustn't be added")
    
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

class TableStyleTC(unittest.TestCase):
    """Test suite for TableSuite"""
    def setUp(self):
        self.table = Table()
        self.table.create_rows(['row1', 'row2', 'row3'])
        self.table.create_columns(['col1', 'col2'])
        self.style = TableStyle(self.table)
        self._tested_attrs = (('size', '1*'),
                              ('alignment', 'right'),
                              ('unit', ''))

    def test_getset(self):
        """tests style's get and set methods"""
        for attrname, default_value in self._tested_attrs:
            getter = getattr(self.style, 'get_%s' % attrname)
            setter = getattr(self.style, 'set_%s' % attrname)
            self.assertRaises(KeyError, getter, 'badcol')
            self.assertEquals(getter('col1'), default_value)
            setter('FOO', 'col1')
            self.assertEquals(getter('col1'), 'FOO')

    def test_getset_index(self):
        """tests style's get and set by index methods"""
        for attrname, default_value in self._tested_attrs:
            getter = getattr(self.style, 'get_%s' % attrname)
            setter = getattr(self.style, 'set_%s' % attrname)
            igetter = getattr(self.style, 'get_%s_by_index' % attrname)
            isetter = getattr(self.style, 'set_%s_by_index' % attrname)
            self.assertEquals(getter('__row_column__'), default_value)
            isetter('FOO', 0)
            self.assertEquals(getter('__row_column__'), 'FOO')
            self.assertEquals(igetter(0), 'FOO')
            self.assertEquals(getter('col1'), default_value)
            isetter('FOO', 1)
            self.assertEquals(getter('col1'), 'FOO')
            self.assertEquals(igetter(1), 'FOO')
        

class RendererTC(unittest.TestCase):
    """Test suite for DocbookRenderer"""
    def setUp(self):
        self.renderer = DocbookRenderer(alignment = True)
        self.table = Table()
        self.table.create_rows(['row1', 'row2', 'row3'])
        self.table.create_columns(['col1', 'col2'])
        self.style = TableStyle(self.table)
        self.base_renderer = TableCellRenderer()
        
    def test_cell_content(self):
        """test how alignment is rendered"""
        entry_xml = self.renderer._render_cell_content('data', self.style, 1)
        self.assertEquals(entry_xml, "<entry align='right'>data</entry>\n")
        self.style.set_alignment_by_index('left', 1)
        entry_xml = self.renderer._render_cell_content('data', self.style, 1)
        self.assertEquals(entry_xml, "<entry align='left'>data</entry>\n")

    def test_default_content_rendering(self):
        """tests that default rendering just prints the cell's content"""
        rendered_cell = self.base_renderer._render_cell_content('data', self.style, 1)
        self.assertEquals(rendered_cell, "data")

    def test_replacement_char(self):
        """tests that 0 is replaced when asked for"""
        cell_content = self.base_renderer._make_cell_content(0, self.style, 1)
        self.assertEquals(cell_content, 0)
        self.base_renderer.properties['skip_zero'] = '---'
        cell_content = self.base_renderer._make_cell_content(0, self.style, 1)
        self.assertEquals(cell_content, '---')

    def test_unit(self):
        """tests if units are added"""
        self.base_renderer.properties['units'] = True
        self.style.set_unit_by_index('EUR', 1)
        cell_content = self.base_renderer._make_cell_content(12, self.style, 1)
        self.assertEquals(cell_content, '12 EUR')


from logilab.common import testlib
class DocbookTableWriterTC(testlib.TestCase):
    """TestCase for table's writer"""
    def setUp(self):
        self.stream = StringIO()
        self.table = Table()
        self.table.create_rows(['row1', 'row2', 'row3'])
        self.table.create_columns(['col1', 'col2'])
        self.writer = DocbookTableWriter(self.stream, self.table, None)
        self.writer.set_renderer(DocbookRenderer())

    def test_write_table(self):
        """make sure write_table() doesn't raise any exception"""
        self.writer.write_table()

    def test_abstract_writer(self):
        """tests that Abstract Writers can't be used !"""
        writer = TableWriter(self.stream, self.table, None)
        self.assertRaises(NotImplementedError, writer.write_table)
    
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
