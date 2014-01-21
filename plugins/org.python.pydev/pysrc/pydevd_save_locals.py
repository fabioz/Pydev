"""
Utility for saving locals.
"""

def save_locals(frame):
    """
    Copy values from locals_dict into the fast stack slots in the given frame.
    """

    import ctypes
    ctypes.pythonapi.PyFrame_LocalsToFast(ctypes.py_object(frame), ctypes.c_int(1))
