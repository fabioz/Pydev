

def Template( p_template, p_name, p_title=None ):

    if p_title is None:
        p_title = p_name
        
    contents_file = '_%s.contents.html' % p_name
    target_file   = '%s.html' % p_name
    
    d = {
        'title' :    p_title,
        'contents' : file( contents_file, 'r' ).read(),
        'perc':'%',
    }

    contents = file( p_template, 'r' ).read()
    contents = contents % d
    file( target_file, 'w' ).write( contents ) 


def Main():
    Template( '_template.html', 'index'                   , 'Pydev')
    Template( '_template.html', 'features'                , 'Features')
    Template( '_template.html', 'download'                , 'Download')
    Template( '_template.html', 'roadmap'                 , 'Roadmap')
    Template( '_template.html', 'codecompletion'          , 'Code Completion')
    Template( '_template.html', 'codecompletionsnapshots' , 'Code Completion Snapshots')
    Template( '_template.html', 'contentassist'           , 'Content Assist (Ctrl+1)')
    Template( '_template.html', 'refactoring'             , 'Refactoring')
    Template( '_template.html', 'faq'                     , 'FAQ')
    Template( '_template.html', 'credits'                 , 'Credits')
    Template( '_template.html', 'codecoverage'            , 'Code Coverage')
    Template( '_template.html', 'run'                     , 'Run')
    Template( '_template.html', 'debug'                   , 'Debug')
    Template( '_template.html', 'debug_prefs'             , 'Debug Preferences')
    Template( '_template.html', 'editor_prefs'            , 'Editor Preferences')
    Template( '_template.html', 'pylint'                  , 'PyLint')
    Template( '_template.html', 'pychecker'               , 'PyChecker')
    Template( '_template.html', 'tasks'                   , 'Tasks')
    
    
Main()
