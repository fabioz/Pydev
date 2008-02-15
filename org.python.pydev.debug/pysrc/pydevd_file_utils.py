from pydevd_constants import * #@UnusedWildImport
import os.path

NORM_FILENAME_CONTAINER = {}
NORM_FILENAME_AND_BASE_CONTAINER = {}


def NormFile(filename):
    try:
        return NORM_FILENAME_CONTAINER[filename]
    except KeyError:
        try:
            rPath = os.path.realpath #@UndefinedVariable
        except:
            # jython does not support os.path.realpath
            # realpath is a no-op on systems without islink support
            rPath = os.path.abspath   
        r = os.path.normcase(rPath(filename))
        #cache it for fast access later
        NORM_FILENAME_CONTAINER[filename] = r
        return r
    

def GetFilenameAndBase(frame):
    f = frame.f_code.co_filename
    try:
        return NORM_FILENAME_AND_BASE_CONTAINER[f]
    except KeyError:
        filename = NormFile(f)
        base = os.path.basename(filename)
        NORM_FILENAME_AND_BASE_CONTAINER[f] = filename, base
        return filename, base
