import datetime

manualAdv = (
    ('../templateManual.html', 'manual_adv_features'                      , 'Features'                        ),
    ('../templateManual.html', 'manual_adv_editor_prefs'                  , 'Editor preferences'              ),
    ('../templateManual.html', 'manual_adv_refactoring'                   , 'Refactoring'                     ),
    ('../templateManual.html', 'manual_adv_assistants'                    , 'Content Assistants'              ),
    ('../templateManual.html', 'manual_adv_coverage'                      , 'Code Coverage'                   ),
    ('../templateManual.html', 'manual_adv_tasks'                         , 'Tasks'                           ),
    ('../templateManual.html', 'manual_adv_code_analysis'                 , 'Code Analysis'                   ),
    ('../templateManual.html', 'manual_adv_quick_outline'                 , 'Quick Outline'                   ),
    ('../templateManual.html', 'manual_adv_open_decl_quick'               , 'Open Declaration Quick Outline'  ),
    ('../templateManual.html', 'manual_adv_gotodef'                       , 'Go to Definition'                ),
    ('../templateManual.html', 'manual_adv_compltemp'                     , 'Templates completion'            ),
    ('../templateManual.html', 'manual_adv_complctx'                      , 'Context-sensitive completions'   ),
    ('../templateManual.html', 'manual_adv_complnoctx'                    , 'Context-insensitive completions' ),
    ('../templateManual.html', 'manual_adv_complauto'                     , 'Auto-suggest keywords'           ),
    ('../templateManual.html', 'manual_adv_debugger'                      , 'Debugger'                        ),
    ('../templateManual.html', 'manual_adv_remote_debugger'               , 'Remote Debugger'                 ),
    ('../templateManual.html', 'manual_adv_debug_console'                 , 'Debug Console'                   ),
    ('../templateManual.html', 'manual_adv_interactive_console'           , 'Interactive Console'             ),
)

def template( template, contents, title, **kwargs ):

    contents_file = '../%s.contents.html' % contents
    target_file   = '../final/%s.html' % contents

    contents_file = file( contents_file, 'r' ).read()
    
    contents = file( template, 'r' ).read()
    toReplace = ['contents_area', 'right_area' , 'image_area',  'quote_area',
                 'prev', 'title_prev', 'next', 'title_next', 'root']
    
    for r in toReplace:
        if r not in kwargs:
            c = getContents(contents_file, r)
        else:
            c = kwargs[r]
        contents = contents.replace('%('+r+')s', c)
    
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
        return ''
    return contents_area
    

def main():
    template('../template1.html     ', 'index'                     , 'Pydev Extensions'                )
    template('../template1.html     ', 'terms'                     , 'License'                         )
    template('../template1.html     ', 'download'                  , 'Download'                        )
    template('../template1.html     ', 'buy'                       , 'Buy'                             )
    template('../template1.html     ', 'manual'                    , 'Manual'                          )
    template('../template1.html     ', 'about'                     , 'About'                           )
    
    template('../templateManual.html', 'manual_101_root'           , 'Getting Started'                 )
    template('../templateManual.html', 'manual_101_install'        , 'Installing'                      )
    template('../templateManual.html', 'manual_101_interpreter'    , 'Configuring the interpreter'     )
    template('../templateManual.html', 'manual_101_project_conf'   , 'Creating a project'              )
    template('../templateManual.html', 'manual_101_project_conf2'  , 'Configuring a project'           )
    template('../templateManual.html', 'manual_101_first_module'   , 'Creating a module'               )
    template('../templateManual.html', 'manual_101_run'            , 'Running your first program'      )
    template('../templateManual.html', 'manual_101_tips'           , 'Some useful tips'                )
    
    for i, curr in enumerate(manualAdv):
        #we have the previous and the next by default
        prev = ('', 'manual','Root') #first one
        if i > 0:
            prev = manualAdv[i-1]
        
        next = ('', 'manual_adv_features','Features') #last one
        if i < len(manualAdv)-1:
            next = manualAdv[i+1]
        
        templ, page, title = curr
        template(templ, page, title, prev=prev[1], next=next[1], title_prev='(%s)'%prev[2], title_next='(%s)'%next[2])
    
    template('../templateManual.html', 'manual_adv_keybindings'    , 'Keybindings'                     )

def DoIt():
    main()
    print 'built com'

if __name__ == '__main__':
    DoIt()