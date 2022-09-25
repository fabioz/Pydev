class B(object):
    '''
        comment
    '''
        

##c
'''
<config>
  <classSelection>0,object</classSelection>
  <methodSelection>
    <string>__hash__</string>
  </methodSelection>
  <offsetStrategy>4</offsetStrategy>
  <editClass>0</editClass>
</config>
'''

##r
class B(object):
    '''
        comment
    '''
    def __hash__(self):
        return object.__hash__(self)
