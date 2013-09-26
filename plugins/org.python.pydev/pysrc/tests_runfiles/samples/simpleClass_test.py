import unittest

class SetUpClassTest(unittest.TestCase):

    def setUpClass(cls):
        raise ValueError("This is an INTENTIONAL value error in setUpClass.")
    classmethod = classmethod(setUpClass) #Not using @ decorator to be compatible with Jython 2.1

    def test_blank(self):
        pass


if __name__ == '__main__':
    unittest.main()
