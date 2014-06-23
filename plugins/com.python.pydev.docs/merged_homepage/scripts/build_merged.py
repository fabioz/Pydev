import os
import sys
import datetime

manualAdv = (
    ('templateManual.html', 'manual_adv_features'                      , 'Features'),
    ('templateManual.html', 'manual_adv_pyunit'                        , 'Unittest integration'),
    ('templateManual.html', 'manual_adv_interactive_console'           , 'Interactive Console'),
    ('templateManual.html', 'manual_adv_editor_prefs'                  , 'Editor preferences'),
    ('templateManual.html', 'manual_adv_launch'                        , 'Launching'),
    ('templateManual.html', 'manual_adv_markoccurrences'               , 'Mark Occurrences'),
    ('templateManual.html', 'manual_adv_renameoccurrences'             , 'Rename Occurrences'),
    ('templateManual.html', 'manual_adv_refactoring'                   , 'Refactoring'),
    ('templateManual.html', 'manual_adv_assistants'                    , 'Content Assistants'),
    ('templateManual.html', 'manual_adv_coverage'                      , 'Code Coverage'),
    ('templateManual.html', 'manual_adv_tasks'                         , 'Tasks'),
    ('templateManual.html', 'manual_adv_code_analysis'                 , 'Code Analysis'),
    ('templateManual.html', 'manual_adv_pylint'                        , 'PyLint'),
    ('templateManual.html', 'manual_adv_quick_outline'                 , 'Quick Outline'),
    ('templateManual.html', 'manual_adv_open_decl_quick'               , 'Open Declaration Quick Outline'),
    ('templateManual.html', 'manual_adv_gotodef'                       , 'Go to Definition'),
    ('templateManual.html', 'manual_adv_hierarchy_view'                , 'Hierachy View'),
    ('templateManual.html', 'manual_adv_compltemp'                     , 'Templates completion'),
    ('templateManual.html', 'manual_adv_complctx'                      , 'Context-sensitive completions'),
    ('templateManual.html', 'manual_adv_complnoctx'                    , 'Context-insensitive completions'),
    ('templateManual.html', 'manual_adv_complauto'                     , 'Auto-suggest keywords'),
    ('templateManual.html', 'manual_adv_type_hints'                    , 'Type-hinting with comments'),
    ('templateManual.html', 'manual_adv_debugger'                      , 'Debugger'),
    ('templateManual.html', 'manual_adv_debugger_auto_reload'          , 'Auto Reload in Debugger'),
    ('templateManual.html', 'manual_adv_remote_debugger'               , 'Remote Debugger'),
    ('templateManual.html', 'manual_adv_debugger_find_referrers'       , 'Find Referrers in Debugger'),
    ('templateManual.html', 'manual_adv_debug_console'                 , 'Debug Console'),
    ('templateManual.html', 'manual_adv_django'                        , 'Django'),
    ('templateManual.html', 'manual_articles_scripting'                , 'Jython Scripting'),
)

manual101 = (
    ('templateManual.html', 'manual_101_root'           , 'Getting Started'),
    ('templateManual.html', 'manual_101_install'        , 'Install'),
    ('templateManual.html', 'manual_101_interpreter'    , 'Interpreter Configuration'),
    ('templateManual.html', 'manual_101_project_conf'   , 'Project Creation'),
    ('templateManual.html', 'manual_101_project_conf2'  , 'Project Configuration'),
    ('templateManual.html', 'manual_101_first_module'   , 'Module Creation'),
    ('templateManual.html', 'manual_101_run'            , 'Running a program'),
    ('templateManual.html', 'manual_101_eclipse'        , 'Configuring Eclipse'),
    ('templateManual.html', 'manual_101_tips'           , 'Useful tips'),

)

manualScreencasts = (
    ('templateManual.html', 'manual_screencasts'               , 'Screencasts'),
    ('templateManual.html', 'manual_screencasts_presentation1' , 'Screencast: Starring: Interactive Console'),
)

homepageBase = (
    ('template1.html', 'index'                     , 'PyDev'),
    ('template1.html', 'download'                  , 'Download'),
    ('template1.html', 'developers'                , 'Developers'),
    ('template1.html', 'developers_grammar'        , 'Grammar'),
    ('template1.html', 'manual'                    , 'Manual'),
    ('template1.html', 'about'                     , 'About'),
    ('template1.html', 'history_pydev'             , 'PyDev Releases'),
    ('template1.html', 'history_pydev_extensions'  , 'PyDev Extensions Releases'),
    ('template1.html', 'manual_adv_keybindings'    , 'Keybindings'),
    ('template1.html', 'faq'                       , 'FAQ'),
    ('template1.html', 'screenshots'               , 'Screenshots'),
)

def template(template, contents, title, **kwargs):
    if_not_specified_in_file = kwargs.pop('if_not_specified_in_file', {})

    target_file = 'final/%s.html' % contents

    try:
        contents_file = file('%s.contents.html' % contents, 'r').read()
    except IOError:
        contents_file = file('%s.contents.rst_html' % contents, 'r').read()

    try:
        contents = file(template, 'r').read()
    except IOError, e:
        raise RuntimeError(str(e) + '\nUnable to get contents. Current dir: ' + os.path.realpath(os.path.abspath(os.curdir)))


    toReplace = ['contents_area', 'right_area' , 'image_area', 'quote_area',
                 'prev', 'title_prev', 'next', 'title_next', 'root']

    for r in toReplace:
        if r not in kwargs:
            c = getContents(contents_file, r)
            if not c:
                c = if_not_specified_in_file.get(r, '')
        else:
            c = kwargs[r]
        contents = contents.replace('%(' + r + ')s', c)

    contents = contents.replace('%(title)s', title)
    contents = contents.replace('%(date)s', datetime.datetime.now().strftime('%d %B %Y'))
    contents = contents.replace('LAST_VERSION_TAG', LAST_VERSION_TAG) #@UndefinedVariable

    #If a page didn't specify the image properly, just remove the image declaration.
    contents = contents.replace('<p><IMG src="images/" border="0" alt=""/></p>', '')

    file(target_file, 'wb').write(contents.replace('\r\n', '\n').replace('\r', '\n'))

def getContents(contents_file, tag):
    try:
        istart = contents_file.index('<%s>' % tag) + 2 + len(tag)
        iend = contents_file.index('</%s>' % tag)
        contents_area = contents_file[istart: iend]
    except ValueError:
        return ''
    return contents_area

def templateForAll(lst, first, last, if_not_specified_in_file={}):
    for i, curr in enumerate(lst):
        #we have the previous and the next by default
        prev = first #first one
        if i > 0:
            prev = lst[i - 1]

        next = last #last one
        if i < len(lst) - 1:
            next = lst[i + 1]

        templ, page, title = curr
        template(templ, page, title, prev=prev[1], next=next[1], title_prev='(%s)' % prev[2], title_next='(%s)' % next[2], if_not_specified_in_file=if_not_specified_in_file)


def main():
    for b in homepageBase:
        template(*b)

    templateForAll(manual101, ('', 'manual', 'Root'), ('', 'manual_adv_features'   , 'Features'), if_not_specified_in_file=dict(root='manual_101_root'))
    templateForAll(manualAdv, ('', 'manual', 'Root'), ('', 'manual_adv_features', 'Features'), if_not_specified_in_file=dict(root='manual_adv_features'))
    templateForAll(manualScreencasts, ('', 'manual', 'Root'), ('', 'manual_screencasts', 'Screencasts'))

def getDict(**kwargs):
    return kwargs

def DoIt():
    sys.stdout.write('Built faq\n')

    main()
    sys.stdout.write('Built homepage\n')

if __name__ == '__main__':


    DoIt()
