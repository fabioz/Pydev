import datetime
LAST_VERSION_TAG='0.9.8.8'



def template( template, contents, title ):

    contents_file = '../%s.contents.html' % contents
    target_file   = '../final/%s.html' % contents

    contents_file = file( contents_file, 'r' ).read()
    
    contents = file( template, 'r' ).read()
    toReplace = ['contents_area', 'right_area' , 'image_area',  'quote_area',
                 'prev', 'title_prev', 'next', 'title_next', 'root']
    
    for r in toReplace:
        contents = contents.replace('%('+r+')s', getContents(contents_file, r))
    
    contents = contents.replace('%(title)s',         title)
    contents = contents.replace('%(date)s',          datetime.datetime.now().strftime('%d %B %Y'))
    contents = contents.replace('LAST_VERSION_TAG',  LAST_VERSION_TAG)
    
    file( target_file, 'w' ).write( contents ) 

def getContents(contents_file, tag):
    try:
        istart = contents_file.index('<%s>'%tag)+2+len(tag)
        iend = contents_file.index('</%s>'%tag)
        contents_area = contents_file[istart: iend]
    except ValueError:
        return ""
    return contents_area
    

def main():
    template('../template1.html', 'index', 'Pydev Extensions')
    template('../template1.html', 'terms', 'License')
    template('../template1.html', 'download', 'Download')
    template('../template1.html', 'buy', 'Buy')
    template('../template1.html', 'manual', 'Manual')
    template('../template1.html', 'about', 'About')
    template('../templateManual.html', 'manual_101_root', 'Getting Started')
    template('../templateManual.html', 'manual_101_install', 'Installing')
    template('../templateManual.html', 'manual_101_interpreter', 'Configuring the interpreter')
    template('../templateManual.html', 'manual_101_project_conf', 'Creating a project')
    template('../templateManual.html', 'manual_101_project_conf2', 'Configuring a project')
    template('../templateManual.html', 'manual_101_first_module', 'Creating a module')
    template('../templateManual.html', 'manual_101_run', 'Running your first program')
    template('../templateManual.html', 'manual_101_tips', 'Some useful tips')
    template('../templateManual.html', 'manual_adv_root', "What's available")
    template('../templateManual.html', 'manual_adv_features', "Features")
    template('../templateManual.html', 'manual_adv_editor_prefs', "Editor preferences")
    template('../templateManual.html', 'manual_adv_refactoring', "Refactoring")
    template('../templateManual.html', 'manual_adv_assistants', "Content Assistants")


if __name__ == '__main__':
    main()
    print 'built'