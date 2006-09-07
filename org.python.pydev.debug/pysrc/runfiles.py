'''
Usage:
    
    runfiles.py <dir> [<dir>...]

Run all unit tests found in the current path. An unit test is a file with 
TestCase-derived classes. 
'''

import sys
import unittest
import optparse #@UnresolvedImport
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






def parse_cmdline():
    usage='usage: %prog directory [other_directory ...]'  
    parser = optparse.OptionParser(usage=usage)
    parser.add_option("-v", "--verbosity", dest="verbosity", default=2)

    options, args = parser.parse_args()
    if not args:
        parser.print_help()
        sys.exit(1)
    return args, int(options.verbosity)


#=============================================================================================================
#IMPORTING 
#=============================================================================================================
def FormatAsModuleName( filename):
    result = filename
    result = result.replace( '\\', '/' )
    result = result.split( '/' )
    result = '.'.join( result )
    return result

def SystemPath( directories ):
    return [FormatAsModuleName( p ) for p in sys.path]

def ImportModule( p_import_path ):
    '''
        Import the module in the given import path.
        
        * Returns the "final" module, so importing "coilib40.subject.visu" return the "visu"
        module, not the "coilib40" as returned by __import__
    '''
    result = __import__( p_import_path )
    for i in p_import_path.split('.')[1:]:
        result = getattr( result, i )
    return result

def ModuleName( filename, system_path ):
    '''
        Given a module filename returns the module name for it considering the given system path.
    '''
    import os.path, sys

    def matchPath( path_a, path_b ):
        if sys.platform == 'win32':
           return path_a.lower().startswith( path_b.lower() )
        else:
           return path_a.startswith( path_b )
    result = FormatAsModuleName( os.path.splitext( filename )[0] )
    
    for i_python_path in system_path:
        if matchPath( result, i_python_path ):
            result = result[len( i_python_path )+1:]
            #print 'entered', filename, 'exit', result
            return result
        
    raise RuntimeError( "Python path not found for filename: %r" % filename )

#=============================================================================================================
#RUN TESTS
#=============================================================================================================
def runtests(dirs, verbosity=2):
    
    loader = unittest.defaultTestLoader
    print 'Finding files...',dirs
    names = []
    for dir in dirs:
        if os.path.isdir(dir):
            #a test can be in any .py file (excluding __init__ files)
            names.extend(FindFiles(dir, ['*.py', '*.pyw'], ['__init__.*'], True))
            
        elif os.path.isfile(dir):
            names.append(dir)
        
        else:
            print dir, 'is not a dir nor a file... so, what is it?'
            
    print 'done.'
    print 'Importing test modules...',
    alltests = []
    system_path = SystemPath(sys.path)
    for name in names:
        module = ImportModule(ModuleName(name, system_path))
        tests = loader.loadTestsFromModule(module)
        alltests.append(tests)
    print 'done.'

    runner = unittest.TextTestRunner(stream=sys.stdout, descriptions=1, verbosity=verbosity)
    runner.run(unittest.TestSuite(alltests))

    
if __name__ == '__main__':
    dirs, verbosity = parse_cmdline()
    runtests(dirs, verbosity)
