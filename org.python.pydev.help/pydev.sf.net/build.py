def _topicDefault():
    s = \
'''
<td colspan="4" align="center" height="100" valign="middle">
<table  border="1" cellpadding="0" cellspacing="0" width="100%(perc)s">
<tbody>
<tr>
<td bgcolor="#eeeeee" align="center" valign="middle" >
<h1>PyDev - Python IDE</h1>


(<a href="http://www.python.org">Python</a> development enviroment for <a href="http://www.eclipse.org">Eclipse</a>)<br /><br/>

 </td>
</tr>
</tbody>
</table>
</td>
'''

    return s

def _getTopicTd(link, name):
    s = \
'''
<td bgcolor="#eeeeee" align="center" valign="middle" >
<a href="%s">%s</a>
</td>
''' 
    return s % (link, name)

def _topicFeatures():
    s = \
'''
<td colspan="2" align="middle" height="100" valign="middle">
</td>
<td colspan="1" align="middle" height="100" valign="middle">
<table  border="1" cellpadding="0" cellspacing="0" width="100%%">
<tbody>

<tr>
%s
%s
%s
%s
%s
</tr>
%s
%s
%s
%s
%s
<tr>

</tr>


</tbody>
</table>
</td>
''' 
    return s % (
       _getTopicTd('features.html','Features'),
       _getTopicTd('editor.html','Editor'),
       _getTopicTd('debug.html','Debugger'),
       _getTopicTd('codecompletion.html','Code Completion'),
       _getTopicTd('templatescompletion.html','Templates'),
       _getTopicTd('codecoverage.html','Code Coverage'),
       _getTopicTd('contentassist.html','Content Assistants'),
       _getTopicTd('refactoring.html','Refactoring'),
       _getTopicTd('tasks.html','Tasks'),
       _getTopicTd('pylint.html','PyLint'),
      )
    

def Template( p_template, p_name, p_title=None , p_topic = None):

    if p_topic is None:
        p_topic = _topicDefault()

    if p_title is None:
        p_title = p_name
        
    contents_file = '_%s.contents.html' % p_name
    target_file   = '%s.html' % p_name
    
    d = {
        'title' :    p_title,
        'contents' : file( contents_file, 'r' ).read(),
        'perc':'%',
        'topic': p_topic
    }

    contents = file( p_template, 'r' ).read()
    contents = contents % d
    file( target_file, 'w' ).write( contents ) 


def Main():
    Template( '_template.html', 'index'                   , 'Pydev'                    )
    Template( '_template.html', 'features'                , 'Features'                 ,_topicFeatures())
    Template( '_template.html', 'download'                , 'Download'                 )
    Template( '_template.html', 'roadmap'                 , 'Roadmap'                  )
    Template( '_template.html', 'codecompletion'          , 'Code Completion'          ,_topicFeatures())
    Template( '_template.html', 'codecompletionsnapshots' , 'Code Completion Snapshots')
    Template( '_template.html', 'contentassist'           , 'Content Assist (Ctrl+1)'  ,_topicFeatures())
    Template( '_template.html', 'refactoring'             , 'Refactoring'              ,_topicFeatures())
    Template( '_template.html', 'faq'                     , 'FAQ'                      )
    Template( '_template.html', 'credits'                 , 'Credits'                  )
    Template( '_template.html', 'codecoverage'            , 'Code Coverage'            ,_topicFeatures())
    Template( '_template.html', 'run'                     , 'Run'                      )
    Template( '_template.html', 'debug'                   , 'Debugger'                 ,_topicFeatures())
    Template( '_template.html', 'debug_prefs'             , 'Debug Preferences'        )
    Template( '_template.html', 'editor_prefs'            , 'Editor Preferences'       ,_topicFeatures())
    Template( '_template.html', 'pylint'                  , 'PyLint'                   ,_topicFeatures())
    Template( '_template.html', 'pychecker'               , 'PyChecker'                )
    Template( '_template.html', 'tasks'                   , 'Tasks'                    ,_topicFeatures())
    
    
Main()
