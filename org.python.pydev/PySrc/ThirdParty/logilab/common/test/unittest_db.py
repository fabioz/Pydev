"""
unit tests for module logilab.common.db

"""
__revision__ = "$Id: unittest_db.py,v 1.2 2005-02-16 16:45:45 fabioz Exp $"

import unittest
from logilab.common.db import *

class PreferedDriverTC(unittest.TestCase):
    def setUp(self):
        self.drivers = {"pg":[('foo', None), ('bar', None)]}
        self.drivers = {'pg' : ["foo", "bar"]}
        
    def testNormal(self):
        set_prefered_driver('pg','bar', self.drivers)
        self.assertEquals('bar', self.drivers['pg'][0])
    
    def testFailuresDb(self):
        try:
            set_prefered_driver('oracle','bar', self.drivers)
            self.fail()
        except UnknownDriver, exc:
            self.assertEquals(exc.args[0], 'Unknown database oracle')

    def testFailuresDriver(self):
        try:
            set_prefered_driver('pg','baz', self.drivers)
            self.fail()
        except UnknownDriver, exc:
            self.assertEquals(exc.args[0], 'Unknown module baz for pg')

    def testGlobalVar(self):
        old_drivers = PREFERED_DRIVERS['postgres'][:]
        expected = old_drivers[:]
        expected.insert(0, expected.pop(1))
        set_prefered_driver('postgres','pgdb')
        self.assertEquals(PREFERED_DRIVERS['postgres'], expected)
        set_prefered_driver('postgres','psycopg')
        self.assertEquals(PREFERED_DRIVERS['postgres'], old_drivers)


class getCnxTC(unittest.TestCase):
    def setUp(self):
        self.host = 'crater.logilab.fr'
        self.db = 'gincotest2'
        self.user = 'adim'
        self.passwd = 'adim'
        
    def testPsyco(self):
        set_prefered_driver('postgres', 'psycopg')
        try:
            cnx = get_connexion('postgres',
                                self.host, self.db, self.user, self.passwd,
                                quiet=1)
        except ImportError:
            self.fail('python-psycopg is not installed')

    def testPgdb(self):
        set_prefered_driver('postgres', 'pgdb')
        try:
            cnx = get_connexion('postgres',
                                self.host, self.db, self.user, self.passwd,
                                quiet=1)
        except ImportError:
            self.fail('python-pgsql is not installed')

    def testPgsql(self):
        set_prefered_driver('postgres', 'pyPgSQL.PgSQL')
        try:
            cnx = get_connexion('postgres',
                                self.host, self.db, self.user, self.passwd,
                                quiet=1)
        except ImportError:
            self.fail('python-pygresql is not installed')

    def testMysql(self):
        set_prefered_driver('mysql', 'MySQLdb')
        try:
            cnx = get_connexion('mysql',
                                self.host, self.db, self.user, self.passwd,
                                quiet=1)
        except ImportError:
            self.fail('python-mysqldb is not installed')
        except Exception, ex:
            # no mysql running ?
            import MySQLdb
            if not (isinstance(ex, MySQLdb.OperationalError) and ex.args[0] == 2003):
                raise


class DBAPIAdaptersTC(unittest.TestCase):
    """Tests DbApi adapters management"""

    def setUp(self):
        """Memorize original PREFERED_DRIVERS"""
        self.old_drivers = PREFERED_DRIVERS['postgres'][:]
        self.host = 'crater.logilab.fr'
        self.db = 'gincotest2'
        self.user = 'adim'
        self.passwd = 'adim'

    def tearDown(self):
        """Reset PREFERED_DRIVERS as it was"""
        PREFERED_DRIVERS['postgres'] = self.old_drivers

    def test_pgdb_types(self):
        """Tests that NUMBER really wraps all number types"""
        set_prefered_driver('postgres', 'pgdb')        
        module = get_dbapi_compliant_module('postgres')
        number_types = 'int2', 'int4', 'serial', \
                       'int8', 'float4', 'float8', \
                       'numeric', 'bool', 'money'
        for num_type in number_types:
            self.assertEquals(num_type, module.NUMBER)
        self.assertNotEquals('char', module.NUMBER)


    def test_pypgsql_getattr(self):
        """Tests the getattr() delegation for pyPgSQL"""
        set_prefered_driver('postgres', 'pyPgSQL.PgSQL')
        module = get_dbapi_compliant_module('postgres')
        try:
            binary = module.BINARY
        except AttributeError, err:
            self.fail(str(err))
        

    def test_connection_wrap(self):
        """Tests the connection wrapping"""
        cnx = get_connexion('postgres',
                            self.host, self.db, self.user, self.passwd,
                            quiet=1)
        self.failIf(isinstance(cnx, PyConnection),
                    'cnx should *not* be a PyConnection instance')
        cnx = get_connexion('postgres',
                            self.host, self.db, self.user, self.passwd,
                            quiet=1, pywrap = True)
        self.failUnless(isinstance(cnx, PyConnection),
                        'cnx should be a PyConnection instance')
        

    def test_cursor_wrap(self):
        """Tests cursor wrapping"""
        cnx = get_connexion('postgres',
                            self.host, self.db, self.user, self.passwd,
                            quiet=1, pywrap = True)
        cursor = cnx.cursor()
        self.failUnless(isinstance(cursor, PyCursor),
                        'cnx should be a PyCursor instance')
        

if __name__ == '__main__':
    unittest.main()
