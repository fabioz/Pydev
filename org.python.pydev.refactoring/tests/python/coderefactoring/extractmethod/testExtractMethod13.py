def method1():
    test((0, 0), style=1)
    
##c
'''
<config>
  <offset>16</offset>
  <selectionLength>21</selectionLength>
  <offsetStrategy>0</offsetStrategy>
</config>
'''

##r
def pepticMethod():
    return test((0, 0), style=1)

def method1():
    pepticMethod()

