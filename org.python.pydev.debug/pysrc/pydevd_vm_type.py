#=======================================================================================================================
# PydevdVmType
#=======================================================================================================================
class PydevdVmType:
    
    PYTHON = 'python'
    JYTHON = 'jython'
    
def SetVmType(vm_type):
    PydevdVmType.vm_type = vm_type
    
def GetVmType():
    return PydevdVmType.vm_type

def SetupType(str=None):
    if str is not None:
        PydevdVmType.vm_type = str
        return
    
    if sys.platform.startswith("java"):
        PydevdVmType.vm_type = PydevdVmType.JYTHON
    else:
        PydevdVmType.vm_type = PydevdVmType.PYTHON
        
