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
""" Copyright (c) 2002-2003 LOGILAB S.A. (Paris, FRANCE).
 http://www.logilab.fr/ -- mailto:contact@logilab.fr


Help to generate SQL string usable by the Python DB-API
"""

__revision__ = "$Id: sqlgen.py,v 1.3 2005-01-21 17:42:04 fabioz Exp $"


# SQLGenerator ################################################################

class SQLGenerator :
    """
    Helper class to generate SQL strings to use with python's DB-API
    """

    def where(self, keys, addon=None) :
        """
        keys : list of keys
        
        >>> s = SQLGenerator()
        >>> s.where(['nom'])
        'nom = %(nom)s'
        >>> s.where(['nom','prenom'])
        'nom = %(nom)s AND prenom = %(prenom)s'
        >>> s.where(['nom','prenom'], 'x.id = y.id')
        'x.id = y.id AND nom = %(nom)s AND prenom = %(prenom)s'
        """
        restriction = ["%s = %%(%s)s" % (x, x) for x in keys]
        if addon:
            restriction.insert(0, addon)
        return " AND ".join(restriction)

    def set(self, keys) :
        """
        keys : list of keys
        
        >>> s = SQLGenerator()
        >>> s.set(['nom'])
        'nom = %(nom)s'
        >>> s.set(['nom','prenom'])
        'nom = %(nom)s, prenom = %(prenom)s'
        """
        return ", ".join(["%s = %%(%s)s" % (x, x) for x in keys])

    def insert(self, table, params) :
        """
        table : name of the table
        params :  dictionnary that will be used as in cursor.execute(sql,params)
        
        >>> s = SQLGenerator()
        >>> s.insert('test',{'nom':'dupont'})
        'INSERT INTO test ( nom ) VALUES ( %(nom)s )'
        >>> s.insert('test',{'nom':'dupont','prenom':'jean'})
        'INSERT INTO test ( nom, prenom ) VALUES ( %(nom)s, %(prenom)s )'
        """
        keys = ', '.join(params.keys())
        values = ', '.join(["%%(%s)s" % x for x in params])
        sql = 'INSERT INTO %s ( %s ) VALUES ( %s )' % (table, keys, values)
        return sql

    def select(self, table, params) :
        """
        table : name of the table
        params :  dictionnary that will be used as in cursor.execute(sql,params)

        >>> s = SQLGenerator()
        >>> s.select('test',{})
        'SELECT * FROM test'
        >>> s.select('test',{'nom':'dupont'})
        'SELECT * FROM test WHERE nom = %(nom)s'
        >>> s.select('test',{'nom':'dupont','prenom':'jean'})
        'SELECT * FROM test WHERE nom = %(nom)s AND prenom = %(prenom)s'
        """
        sql = 'SELECT * FROM %s' % table
        where = self.where(params.keys())
        if where :
            sql = sql + ' WHERE %s' % where
        return sql

    def adv_select(self, model, tables, params, joins=None) :
        """
        model  : list of columns to select
        tables : list of tables used in from
        params   :  dictionnary that will be used as in cursor.execute(sql, params)
        joins  : optional list of restriction statements to insert in the where
                 clause. Usually used to perform joins.

        >>> s = SQLGenerator()
        >>> s.adv_select(['column'],[('test', 't')], {})
        'SELECT column FROM test AS t'
        >>> s.adv_select(['column'],[('test', 't')], {'nom':'dupont'})
        'SELECT column FROM test AS t WHERE nom = %(nom)s'
        """
        table_names = ["%s AS %s" % (k, v) for k, v in tables]
        sql = 'SELECT %s FROM %s' % (', '.join(model), ', '.join(table_names))
        if joins and type(joins) != type(''):
            joins = ' AND '.join(joins)
        where = self.where(params.keys(), joins)
        if where :
            sql = sql + ' WHERE %s' % where
        return sql

    def delete(self, table, params) :
        """
        table : name of the table
        params :  dictionnary that will be used as in cursor.execute(sql,params)

        >>> s = SQLGenerator()
        >>> s.delete('test',{'nom':'dupont'})
        'DELETE FROM test WHERE nom = %(nom)s'
        >>> s.delete('test',{'nom':'dupont','prenom':'jean'})
        'DELETE FROM test WHERE nom = %(nom)s AND prenom = %(prenom)s'
        """
        where = self.where(params.keys())
        sql = 'DELETE FROM %s WHERE %s' % (table, where)
        return sql

    def update(self, table, params, unique) :
        """
        table : name of the table
        params :  dictionnary that will be used as in cursor.execute(sql,params)

        >>> s = SQLGenerator()
        >>> s.update('test',{'id':'001','nom':'dupont'},['id'])
        'UPDATE test SET nom = %(nom)s WHERE id = %(id)s'
        >>> s.update('test',{'id':'001','nom':'dupont','prenom':'jean'},['id'])
        'UPDATE test SET nom = %(nom)s, prenom = %(prenom)s WHERE id = %(id)s'
        """
        where = self.where(unique)
        set = self.set([key for key in params if key not in unique])
        sql = 'UPDATE %s SET %s WHERE %s' % (table, set, where)
        return sql

# Helper functions #############################################################

def name_fields(cursor, records) :
    """
    Take a cursor and a list of records fetched with that cursor, then return a
    list of dictionnaries (one for each record) whose keys are column names and
    values are records' values.

    cursor : cursor used to execute the query
    records : list returned by fetch*()
    """
    result = []
    for record in records :
        record_dict = {}
        for i in range(len(record)) :
            record_dict[cursor.description[i][0]] = record[i]
        result.append(record_dict)
    return result
    
        
if __name__ == "__main__":
    import doctest
    from logilab.common import sqlgen
    print doctest.testmod(sqlgen)
