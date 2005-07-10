'''
Usage:
    
    runfiles.py <dir> [<dir>...]

Run all unit tests found in the current path. An unit test is a file named
test_*.py and with TestCase-derived classes. 
'''

import sys
import unittest
import optparse
import fnmatch
import os
import os.path
import re


def MatchMasks( p_FileName, p_Filters ):
    for filter in p_Filters:
        if fnmatch.fnmatch( p_FileName, filter ):
            return 1
    return 0


def NotDir( p_FileName ):
    return not os.path.isdir( p_FileName )


def _FindFiles( p_Path, p_InFilters, p_OutFilters, p_Recursive = True ):
    import os
    import fnmatch
    
    if not p_Path: p_Path = '.'
    
    def AddFile( o_Result, p_DirName, p_FileNames ):
        p_FileNames = filter( lambda x: MatchMasks( x, p_InFilters ), p_FileNames ) 
        p_FileNames = filter( lambda x: not MatchMasks( x, p_OutFilters ), p_FileNames ) 
        p_FileNames = filter( NotDir, p_FileNames ) 
        p_FileNames = [os.path.join( p_DirName, x ) for x in p_FileNames]
        o_Result.extend( p_FileNames )

    result = []
    if (p_Recursive):
        os.path.walk( p_Path, AddFile, result )
    else:
        result = os.listdir( p_Path )
        result = filter( lambda x: MatchMasks( x, p_InFilters ), result ) 
        result = filter( lambda x: not MatchMasks( x, p_OutFilters ), result ) 
        result = filter( NotDir, result )
        result = [os.path.join( p_Path, x ) for x in result]
    return result;


def make_list( p_element ):
    '''
        Returns p_element as a list.
    '''
    if isinstance( p_element, list ):
        return p_element
    else:
        return [p_element,]


def FindFiles( p_Pathes, p_InFilters=None, p_OutFilters=None, p_Recursive = True ):
    '''
        Find files recursivelly, in one or more directories, matching the
        given IN and OUT filters.

        @param p_Patches: One or a list of patches to search.

        @param p_InFilters: A list of filters (DIR format) to match. Defaults
            to ['*.*'].

        @param p_OutFilters
        A list of filters (DIR format) to ignore. Defaults to [].

        @param p_Recursive
        Recursive search? 
    '''
    if p_InFilters is None:
        p_InFilters = ['*.*']
    if p_OutFilters is None:
        p_OutFilters = []
    p_Pathes = make_list( p_Pathes )
    result = []
    for i_path in p_Pathes:
        files = _FindFiles( i_path, p_InFilters, p_OutFilters, p_Recursive )
        result.extend( files )
    return result



def GrepFiles( p_Pathes, p_Text, p_InFilters, p_OutFilters, p_Recursive = True ):
    def HasText( p_FileName, p_Text ):
        if (not os.path.isfile( p_FileName )):
            return False
        iss = open( p_FileName )
        line = iss.readline()
        while (line):
            if (line.find( p_Text ) >= 0):
                return True
            line = iss.readline()
        return False
    result = FindFiles( p_Pathes, p_InFilters, p_OutFilters, p_Recursive )
    return filter( lambda x: HasText( x, p_Text ), result )



def CheckForUpdate( p_source, p_target ):
    '''
        Returns wether the given source needs to be (re)processed to generate the given target.
        Check the target existence and date. 
    '''
    return \
        not os.path.isfile( p_target ) or \
        os.path.getmtime( p_source ) > os.path.getmtime( p_target )

def CheckType( p_object, p_type ):
    result = isinstance( p_object, p_type )
    if not result:
        types_ = [str(x.__name__) for x in make_tuple( p_type )]
        types_ = '" or "'.join( types_ )
        raise RuntimeError, 'CheckType: Expecting "%s", got "%s": %s' % \
            (types_, p_object.__class__.__name__, repr( p_object ) ) 
    return result


def FileNames( p_filename, p_masks ):
    '''
        Returns the given masks substituing variables:
            - Environmetn variables
            - Extra varibles:
                - abs_path
                - platform
                - PLATFORM
                - filename
                - basename
    '''
    import coilib
    CheckType( p_filename, str )
    abs_path = os.path.abspath( p_filename )
    filename = os.path.basename( p_filename )
    basename = filename.split('-')
    basename = basename[0]
    d = {
        'abs_path'  : abs_path,
        'platform'  : sys.platform,
        'PLATFORM'  : coilib.Platform(),
        'filename'  : filename,
        'basename'  : basename,
    }
    d.update( os.environ )
    return [os.path.normpath( i % d ) for i in p_masks]


def FindFileName( p_filenames ):
    result    = None
    for i_filename in p_filenames:
        if os.path.isfile( i_filename ):
            result = i_filename
            break
    if (result is None):
        import coilib.Exceptions
        raise coilib.Exceptions.EFileNotFound( '\n - '.join( [''] + p_filenames ) )
    return result



#===============================================================================
# GetShortPathName
#===============================================================================
def GetShortPathName(path):
    '''Returns on windows the short version of the given path.
    On other platforms, return the path unchanged.
    '''
    if sys.platform == 'win32':
        import win32api
        return win32api.GetShortPathName(path)
    else:
        return path









def parse_cmdline():
    usage='usage: %prog directory [other_directory ...]'  
    parser = optparse.OptionParser(usage=usage)

    options, args = parser.parse_args()
    if not args:
        parser.print_help()
        sys.exit(1)
    return args



def runtests(dirs, verbosity=2):
    loader = unittest.defaultTestLoader
    print 'Finding files...',dirs
    names = []
    for dir in dirs:
        names.extend(FindFiles(dir, ['*.py'], '', True))
    print 'done.'
    print 'Importing test modules...',
    alltests = []
    for name in names:
        dir, fullname = os.path.split(name)
        modulename = os.path.splitext(fullname)[0]
        sys.path.append(dir)
        module = __import__(modulename)
        tests = loader.loadTestsFromModule(module)
        alltests.append(tests)
    print 'done.'
    runner = unittest.TextTestRunner(sys.stdout, 1, verbosity)
    runner.run(unittest.TestSuite(alltests))

    
if __name__ == '__main__':
    dirs = parse_cmdline()
    runtests(dirs)
