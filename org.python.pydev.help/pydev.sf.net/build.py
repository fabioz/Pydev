


def Template( p_template, p_name ):

    contents_file = '_%s.contents.html' % p_name
    target_file   = '%s.html' % p_name
    
    d = {
        'title' :    p_name,
        'contents' : file( contents_file, 'r' ).read(),
    }

    contents = file( p_template, 'r' ).read()
    contents = contents % d
    file( target_file, 'w' ).write( contents ) 


def Main():
    Template( '_template.html', 'index' )
    Template( '_template.html', 'Features' )
    Template( '_template.html', 'Download' )
    Template( '_template.html', 'Roadmap' )
    Template( '_template.html', 'codecompletion' )
    Template( '_template.html', 'contentassist' )
    Template( '_template.html', 'refactoring' )
    Template( '_template.html', 'Faq' )
    Template( '_template.html', 'Credits' )


Main()
