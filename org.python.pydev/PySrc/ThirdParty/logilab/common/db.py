# Copyright (c) 2002-2004 LOGILAB S.A. (Paris, FRANCE).
# http://www.logilab.fr/ -- mailto:contact@logilab.fr
#
# This program is free software; you can redistribute it and/or modify it under
# the terms of the GNU General Public License as published by the Free Software
# Foundation; either version 2 of the License, or (at your option) any later
# version.
#
# This program is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along with
# this program; if not, write to the Free Software Foundation, Inc.,
# 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
"""A generic function to get a database connection.
"""

__revision__ = "$Id: db.py,v 1.5 2005-04-19 14:39:09 fabioz Exp $"

import sys

__all__ = ['UnknownDriver', 'set_prefered_driver', 'get_connexion',
           'binary', 'NoAdapterFound', 'ADAPTER_DIRECTORY',
           'get_dbapi_compliant_module', 'PREFERED_DRIVERS',
           'PyConnection', 'PyCursor',]

class UnknownDriver(Exception):
    """raised when a unknown driver is given to get connexion"""


PREFERED_DRIVERS = {
    "postgres" : [ 'psycopg', 'pgdb', 'pyPgSQL.PgSQL', ],
    "mysql" : ['MySQLdb'], # , 'pyMySQL.MySQL],
    }

def _import_driver_module(driver, drivers, imported_elements = None,
                          quiet = True):
    """Imports the first module found in 'drivers' for 'driver'

    :rtype: tuple
    :returns: the tuple module_object, module_name where module_object
              is the dbapi module, and modname the module's name
    """
    imported_elements = imported_elements or []
    for modname in drivers[driver]:
        try:
            if not quiet:
                print >> sys.stderr, 'Trying %s' % modname
            module = __import__(modname, globals(), locals(), imported_elements)
            break
        except ImportError:
            if not quiet:
                print >> sys.stderr, '%s is not available' % modname
            continue
    else:
        raise ImportError('Unable to import a %s module' % driver)
    return module, modname


def set_prefered_driver(database, module, _drivers=PREFERED_DRIVERS):
    """sets the prefered driver module for database
    database is the name of the db engine (postgresql, mysql...)
    module is the name of the module providing the connect function
    syntax is (params_func, post_process_func_or_None)
    _drivers is a optionnal dictionnary of drivers
    """
    try:
        modules = _drivers[database]
    except KeyError:
        raise UnknownDriver('Unknown database %s' % database)

    # Remove module from modules list, and re-insert it in first position
    try:
        modules.remove(module)
    except ValueError:
        raise UnknownDriver('Unknown module %s for %s' % (module, database))
    modules.insert(0, module)



def binary(driver='postgres', drivers = PREFERED_DRIVERS):
    """return the binary wrapper for the given driver, (None if not found)
    """
    module, modname = _import_driver_module(driver, drivers, ['Binary'])
    try:
        return module.Binary
    except AttributeError:
        return None


def system_database(driver='postgres'):
    """return the system database for the given driver"""
    if driver == 'postgres':
        return 'template1'
    raise UnknownDriver('Unknown driver %s' % driver)



class NoAdapterFound(Exception):
    """Raised when no Adpater to DBAPI was found"""
    def __init__(self, obj, objname = None, protocol = 'DBAPI'):
        if objname is None:
            objname = obj.__name__
        Exception.__init__(self, "Could not adapt %s to protocol %s" %
                           (objname, protocol))
        self.adapted_obj = obj
        self.objname = objname
        self._protocol = protocol



######################################################################
class PyConnection:
    """ A simple connection wrapper in python (useful for profiling) """
    def __init__(self, cnx):
        """Wraps the original connection object"""
        self._cnx = cnx

    # XXX : Would it work if only __getattr__ was defined 
    def cursor(self):
        """Wraps cursor()"""
        return PyCursor(self._cnx.cursor())

    def commit(self):
        """Wraps commit()"""
        return self._cnx.commit()

    def rollback(self):
        """Wraps rollback()"""
        return self._cnx.rollback()

    def close(self):
        """Wraps close()"""
        return self._cnx.close()

    def __getattr__(self, attrname):
        return getattr(self._cnx, attrname)

    

class PyCursor:
    """ A simple cursor wrapper in python (useful for profiling) """
    def __init__(self, cursor):
        self._cursor = cursor

    def close(self):
        """Wraps close()"""
        return self._cursor.close()
        
    def execute(self, *args, **kwargs):
        """Wraps execute()"""
        return self._cursor.execute(*args, **kwargs)

    def executemany(self, *args, **kwargs):
        """Wraps executemany()"""
        return self._cursor.executemany(*args, **kwargs)

    def fetchone(self, *args, **kwargs):
        """Wraps fetchone()"""
        return self._cursor.fetchone(*args, **kwargs)

    def fetchmany(self, *args, **kwargs):
        """Wraps execute()"""
        return self._cursor.fetchmany(*args, **kwargs)

    def fetchall(self, *args, **kwargs):
        """Wraps fetchall()"""
        return self._cursor.fetchall(*args, **kwargs)

    def __getattr__(self, attrname):
        return getattr(self._cursor, attrname)
    

## Adapters list #####################################################
class DBAPIAdapter:
    """Base class for all DBAPI adpaters"""

    def __init__(self, native_module, pywrap = False):
        """
        :type native_module: module
        :param native_module: the database's driver adapted module
        """
        self._native_module = native_module
        self._pywrap = pywrap


    def connect(self, host = '', database = '', user = '', password = ''):
        """Wraps the native module connect method"""
        kwargs = {'host' : host, 'database' : database,
                  'user' : user, 'password' : password}
        cnx = self._native_module.connect(**kwargs)
        return self._wrap_if_needed(cnx)


    def _wrap_if_needed(self, cnx):
        """Wraps the connection object if self._pywrap is True, and returns it
        If false, returns the original cnx object
        """
        if self._pywrap:
            return PyConnection(cnx)
        else:
            return cnx

    
    def __getattr__(self, attrname):
        return getattr(self._native_module, attrname)

##     def adapt(cls, module):
##         return cls(module)
##     adapt = classmethod(adapt)


class _PgdbAdapter(DBAPIAdapter):
    """Simple PGDB Adapter to DBAPI (pgdb modules lacks Binary() and NUMBER)
    """
    def __init__(self, native_module, pywrap = False):
        DBAPIAdapter.__init__(self, native_module, pywrap)
        self.NUMBER = native_module.pgdbType('int2', 'int4', 'serial',
                                             'int8', 'float4', 'float8',
                                             'numeric', 'bool', 'money')
        

class _PsycopgAdapter(DBAPIAdapter):
    """Simple Psycopg Adapter to DBAPI (cnx_string differs from classical ones)
    """
    def connect(self, host = '', database = '', user = '', password = ''):
        """Handles psycopg connexion format"""
        if host:
            cnx_string = 'host=%s  dbname=%s  user=%s' % (host, database, user)
        else:
            cnx_string = 'dbname=%s  user=%s' % (database, user)
        if password:
            cnx_string = '%s password=%s' % (cnx_string, password)
        cnx = self._native_module.connect(cnx_string)
        cnx.set_isolation_level(1)
        return self._wrap_if_needed(cnx)
    


class _PgsqlAdapter(DBAPIAdapter):
    """Simple pyPgSQL Adapter to DBAPI
    """
    def connect(self, host = '', database = '', user = '', password = ''):
        """Handles psycopg connexion format"""
        kwargs = {'host' : host, 'database' : database,
                  'user' : user, 'password' : password or None}
        cnx = self._native_module.connect(**kwargs)
        return self._wrap_if_needed(cnx)


    def Binary(self, string):
        """Emulates the Binary (cf. DB-API) function"""
        return str
    
    def __getattr__(self, attrname):
        # __import__('pyPgSQL.PgSQL', ...) imports the toplevel package
        return getattr(self._native_module.PgSQL, attrname)



class _MySqlDBAdapter(DBAPIAdapter):
    """Simple mysql Adapter to DBAPI
    """
    def connect(self, host = '', database = '', user = '', password = ''):
        """Handles psycopg connexion format"""
        kwargs = {'host' : host, 'db' : database,
                  'user' : user, 'passwd' : password or None}
        return self._native_module.connect(**kwargs)
        
    

_ADAPTERS = {
    'postgres' : { 'pgdb' : _PgdbAdapter,
                   'psycopg' : _PsycopgAdapter,
                   'pyPgSQL.PgSQL' : _PgsqlAdapter,
                   },
    'mysql' : { 'MySQLdb' : _MySqlDBAdapter, },
    }

# _AdapterDirectory could be more generic by adding a 'protocol' parameter
# This one would become an adpater for 'DBAPI' protocol
class _AdapterDirectory(dict):
    """A simple dict that registers all adapters"""
    def register_adapter(self, adapter, driver, modname):
        """Registers 'adapter' in directory as adapting 'mod'"""
        try:
            driver_dict = self[driver]
        except KeyError:
            self[driver] = {}
            
        # XXX Should we have a list of adapters ?
        driver_dict[modname] = adapter

    
    def adapt(self, database, prefered_drivers = None, pywrap = False):
        """Returns an dbapi-compliant object based for database"""
        prefered_drivers = prefered_drivers or PREFERED_DRIVERS
        module, modname = _import_driver_module(database, prefered_drivers)
        try:
            return self[database][modname](module, pywrap = pywrap)
        except KeyError:
            raise NoAdapterFound(obj = module)
        

    def get_adapter(self, database, modname):
        try:
            return self[database][modname]
        except KeyError:
            raise NoAdapterFound(None, modname)



ADAPTER_DIRECTORY = _AdapterDirectory(_ADAPTERS)
del _AdapterDirectory


def get_dbapi_compliant_module(driver, prefered_drivers = None, quiet = False,
                               pywrap = False):
    """Returns a fully dbapi compliant module"""
    try:
        return ADAPTER_DIRECTORY.adapt(driver, prefered_drivers, pywrap = pywrap)
    except NoAdapterFound, err:
        if not quiet:
            msg = 'No Adapter found for %s, returning native module' % err.objname
            print >> sys.stderr, msg
        return err.adapted_obj


def get_connexion(driver='postgres', host='', database='', user='',
                  password='', quiet = False, drivers = PREFERED_DRIVERS,
                  pywrap = False):
    """return a db connexion according to given arguments"""
    module, modname = _import_driver_module(driver, drivers, ['connect'])
    try:
        adapter = ADAPTER_DIRECTORY.get_adapter(driver, modname)
    except NoAdapterFound, err:
        if not quiet:
            msg = 'No Adapter found for %s, using default one' % err.objname
            print >> sys.stderr, msg
        adapted_module = DBAPIAdapter(module, pywrap)
    else:
        adapted_module = adapter(module, pywrap)
    return adapted_module.connect(host, database, user, password)


def sql_repr( type, val ):
    if type=='s':
        return "'%s'" % (val,)
    else:
        return val

class BaseTable:
    # table_name = "default"
    # supported types are s/i/d
    # table_fields = ( ('first_field','s'), )
    # primary_key = 'first_field'

    def __init__(self, table_name, table_fields, primary_key=None ):
        if primary_key is None:
            self._primary_key = table_fields[0][0]
        else:
            self._primary_key = primary_key

        self._table_fields = table_fields
        self._table_name = table_name
        info = {
            'key' : self._primary_key,
            'table' : self._table_name,
            'columns' : ",".join( [ f for f,t in self._table_fields ] ),
            'values' : ",".join( [ sql_repr( t, "%%(%s)s" % f ) for f,t in self._table_fields ] ),
            'updates' : ",".join( [ "%s=%s" % ( f, sql_repr( t, "%%(%s)s" % f) ) for f,t in self._table_fields] ),
            }
        self._insert_stmt = "INSERT into %(table)s (%(columns)s) VALUES (%(values)s) WHERE %(key)s=%%(key)s" % info
        self._update_stmt = "UPDATE %(table)s SET (%(updates)s) VALUES WHERE %(key)s=%%(key)s" % info
        self._select_stmt = "SELECT %(columns)s FROM %(table)s WHERE %(key)s=%%(key)s" % info
        self._delete_stmt = "DELETE FROM %(table)s WHERE %(key)s=%%(key)s" % info

        for k,t in table_fields:
            if hasattr(self,k):
                raise ValueError("Cannot use %s as a table field" % k)
            setattr(self,k,None)


    def as_dict( self ):
        d = {}
        for k,t in self._table_fields:
            d[k] = getattr(self,k)
        return d

    def select( self, curs ):
        d = { 'key' : getattr(self,self._primary_key) }
        curs.execute( self._select_stmt % d )
        rows = curs.fetchall()
        if len(rows)!=1:
            raise ValueError("Select: ambiguous query returned %d rows" % len(rows) )
        for (f,t),v in zip(self._table_fields,rows[0]):
            setattr(self,f,v)

    def update( self, curs ):
        d = self.as_dict()
        curs.execute( self._update_stmt % d )

    def delete( self, curs ):
        d = { 'key' : getattr(self,self._primary_key) }
        
