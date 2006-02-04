import datetime
LAST_VERSION_TAG='0.9.8.8'

#http://www.fabioz.com/pydev/successful_payment.html
#http://www.fabioz.com/pydev/cancel_payment.html
manual_101 = \
(('manual_101_root'           , 'Getting Started'                ),
('manual_101_install'        , 'Installing'                     ),
('manual_101_interpreter'    , 'Configuring the interpreter'    ),
('manual_101_project_conf'   , 'Creating a project'             ),
('manual_101_project_conf2'  , 'Configuring a project'          ),
('manual_101_first_module'   , 'Creating a module'              ),
('manual_101_run'            , 'Running your first program'     ),
('manual_101_tips'           , 'Some useful tips'               ))

manual_adv = \
(('manual_adv_features'       , 'Features'                        ),
('manual_adv_editor_prefs'   , 'Editor preferences'              ),
('manual_adv_refactoring'    , 'Refactoring'                     ),
('manual_adv_assistants'     , 'Content Assistants'              ),
('manual_adv_coverage'       , 'Code Coverage'                   ),
('manual_adv_tasks'          , 'Tasks'                           ),
('manual_adv_code_analysis'  , 'Code Analysis'                   ),
('manual_adv_quick_outline'  , 'Quick Outline'                   ),
('manual_adv_gotodef'        , 'Go to Definition'                ),
('manual_adv_compltemp'      , 'Templates completion'            ),
('manual_adv_complctx'       , 'Context-sensitive completions'   ),
('manual_adv_complnoctx'     , 'Context-insensitive completions' ),
('manual_adv_complauto'      , 'Auto-suggest keywords'           ),
('manual_adv_debugger'       , 'Debugger'                        ),
('manual_adv_remote_debugger', 'Remote Debugger'                 ))

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
    
    template('../templateManual.html', 'manual_adv_features'       , 'Features'                        )
    template('../templateManual.html', 'manual_adv_editor_prefs'   , 'Editor preferences'              )
    template('../templateManual.html', 'manual_adv_refactoring'    , 'Refactoring'                     )
    template('../templateManual.html', 'manual_adv_assistants'     , 'Content Assistants'              )
    template('../templateManual.html', 'manual_adv_coverage'       , 'Code Coverage'                   )
    template('../templateManual.html', 'manual_adv_tasks'          , 'Tasks'                           )
    template('../templateManual.html', 'manual_adv_code_analysis'  , 'Code Analysis'                   )
    template('../templateManual.html', 'manual_adv_quick_outline'  , 'Quick Outline'                   )
    template('../templateManual.html', 'manual_adv_gotodef'        , 'Go to Definition'                )
    template('../templateManual.html', 'manual_adv_compltemp'      , 'Templates completion'            )
    template('../templateManual.html', 'manual_adv_complctx'       , 'Context-sensitive completions'   )
    template('../templateManual.html', 'manual_adv_complnoctx'     , 'Context-insensitive completions' )
    template('../templateManual.html', 'manual_adv_complauto'      , 'Auto-suggest keywords'           )
    template('../templateManual.html', 'manual_adv_debugger'       , 'Debugger'                        )
    template('../templateManual.html', 'manual_adv_remote_debugger', 'Remote Debugger'                 )
    
    template('../templateManual.html', 'manual_adv_keybindings'    , 'Keybindings'                     )

def DoIt():
    main()
    print 'built com'

if __name__ == '__main__':
    DoIt()