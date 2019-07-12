#Note that this test is run from org.python.pydev.jythontests.JythonTest

from convert_api_to_pypredef import Convert, Contains
try:
    import unittest
except:
    import sys
    sys.stderr.write('--- PYTHONPATH FOUND:\n')
    sys.stderr.write('\n'.join(sys.path))
    sys.stderr.write('\n--- END PYTHONPATH\n')
    raise
from StringIO import StringIO

#===================================================================================================
# Test
#===================================================================================================
class Test(unittest.TestCase):
    
    def setUp(self):
        unittest.TestCase.setUp(self)
    
    def tearDown(self):
        unittest.TestCase.tearDown(self)
        
        
    def Check(self, sub, full):
        if not Contains(sub, full):
            raise AssertionError('%s not in %s.' % (sub, full))
        
    def testConvert(self):
        import convert_api_to_pypredef
        lines = [
            "PyQt4.QtCore.QObject.disconnect?4(QObject, SIGNAL(), QObject, SLOT()) -> object",
            "PyQt4.QtCore.QObject.connect?4(QObject, SIGNAL(), QObject, SLOT(), Qt.ConnectionType=Qt.AutoConnection) -> object",
            "PyQt4.QtCore.QAbstractEventDispatcher.__init__?1(self, QObject parent=None)",
            "PyQt4.QtCore.QByteArray.leftJustified?4(int width, char fill=' ', bool truncate=False) -> QByteArray"
        ]
        cancel_monitor = convert_api_to_pypredef.CancelMonitor()
        output_stream = StringIO()
        Convert('test_passed_lines', 2, cancel_monitor, lines, output_stream=output_stream)
#        print output_stream.getvalue()
        self.Check("def disconnect(QObject, SIGNAL, QObject, SLOT):", output_stream.getvalue())
        self.Check("def connect(QObject, SIGNAL, QObject, SLOT, Qt_ConnectionType=Qt.AutoConnection):", output_stream.getvalue())
        self.Check("def leftJustified(width, fill=' ', truncate=False):", output_stream.getvalue())
        self.Check("def __init__(self, parent=None):", output_stream.getvalue())
        
        
        
#        api_file = r'C:\Documents and Settings\Fabio\Desktop\pydev_temp\PyQt4.api'
#        parts_for_module = 2
#        Convert(api_file, parts_for_module, cancel_monitor)
        

#===================================================================================================
# main
#===================================================================================================
if __name__ == '__main__':
    suite = unittest.makeSuite(Test)
    unittest.TextTestRunner(verbosity=3).run(suite)

