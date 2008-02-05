class B(object):
    '''
        comment
    '''
        

##c
'''
<config>
  <classSelection>0</classSelection>
  <methodSelection>
    <string>__hash__</string>
    <string>__delattr__</string>
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
    def __hash__(self, *args, **kwrgs):
        return object.__hash__(self, *args, **kwrgs)


    def __delattr__(self, *args, **kwrgs):
        return object.__delattr__(self, *args, **kwrgs)