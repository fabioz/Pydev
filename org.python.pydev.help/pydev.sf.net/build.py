import datetime
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
#    return ''
    s = \
'''
<td colspan="2" align="middle" height="100" valign="middle">
</td>
<td colspan="1" align="middle" height="100" valign="middle">
<table  border="1" cellpadding="0" cellspacing="0" width="100%%">
<tbody>

<tr>
%s%s%s%s%s
</tr>
%s%s%s%s%s
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
       _getTopicTd('templates.html','Templates'),
       _getTopicTd('codecoverage.html','Code Coverage'),
       _getTopicTd('contentassist.html','Content Assistants'),
       _getTopicTd('refactoring.html','Refactoring'),
       _getTopicTd('tasks.html','Tasks'),
       _getTopicTd('pylint.html','PyLint'),
      )
    

def Template( p_template, p_name, p_title, p_topic , p_channel, p_otherFeatures):

    if p_otherFeatures is None:
        p_otherFeatures = ''
        
    if p_topic is None:
        p_topic = _topicDefault()

    if p_title is None:
        p_title = p_name
        
    if p_channel is None:
        raise RuntimeError('Channel not set')
        
    contents_file = '_%s.contents.html' % p_name
    target_file   = '%s.html' % p_name
    
    
    d = {
        'title' :    p_title,
        'contents' : file( contents_file, 'r' ).read(),
        'topic': p_topic,
        'channel': p_channel,
        'date': datetime.datetime.now().strftime('%d %B %Y'),
        'other_features': p_otherFeatures
    }

    contents = file( p_template, 'r' ).read()
    contents = contents % d
    file( target_file, 'w' ).write( contents ) 


def _getFeaturesTd(link, name):
    s = \
'''
<tr>
<td colspan="1"><img src="images/button.png"/> </td>
<td colspan="1"><a class="menu_links" href="%s">%s</a> </td>
</tr>
''' % (link, name)

    return s
    
def _otherFeatures():
    s = \
'''
%s%s%s%s%s%s%s%s%s
''' 
    return s % (
       _getFeaturesTd('editor.html','Editor'),
       _getFeaturesTd('debug.html','Debugger'),
       _getFeaturesTd('codecompletion.html','Code Completion'),
       _getFeaturesTd('templates.html','Templates'),
       _getFeaturesTd('codecoverage.html','Code Coverage'),
       _getFeaturesTd('contentassist.html','Content Assistants'),
       _getFeaturesTd('refactoring.html','Refactoring'),
       _getFeaturesTd('tasks.html','Tasks'),
       _getFeaturesTd('pylint.html','PyLint'),
      )


def Main():
    channelPydev             = '6798751939'
    channelPydevFeatures     = '1935586933'
    channelPydevFeaturesMain = '9124732182'
    channelPydevIndex        = '0232202381'
    channelPydevFaq          = '9395062605'
    
    
    Template( '_template.html', 'index'                   , 'Pydev'                    ,None             , channelPydevIndex        , None)
    Template( '_template.html', 'version_2_4'             , 'Python 2.4 Support'       ,None             , channelPydev             , None)
    Template( '_template.html', 'screenshots'             , 'Screenshots'              ,None             , channelPydev             , None)
    Template( '_template.html', 'download'                , 'Download'                 ,None             , channelPydev             , None)
    Template( '_template.html', 'roadmap'                 , 'Roadmap'                  ,None             , channelPydev             , None)
    Template( '_template.html', 'codecompletionsnapshots' , 'Code Completion Snapshots',None             , channelPydevFeatures     , None)
    Template( '_template.html', 'faq'                     , 'FAQ'                      ,None             , channelPydevFaq          , None)
    Template( '_template.html', 'credits'                 , 'Credits'                  ,None             , channelPydev             , None)
    Template( '_template.html', 'run'                     , 'Run'                      ,None             , channelPydev             , None)
    Template( '_template.html', 'debug_prefs'             , 'Debug Preferences'        ,None             , channelPydev             , None)
    Template( '_template.html', 'pychecker'               , 'PyChecker'                ,None             , channelPydevFeatures     , None)
    Template( '_template.html', 'features'                , 'Features'                 ,_topicFeatures() , channelPydevFeaturesMain , _otherFeatures())
    Template( '_template.html', 'codecompletion'          , 'Code Completion'          ,_topicFeatures() , channelPydevFeatures     , _otherFeatures())
    Template( '_template.html', 'templates'               , 'Templates'                ,_topicFeatures() , channelPydevFeatures     , _otherFeatures())
    Template( '_template.html', 'contentassist'           , 'Content Assist (Ctrl+1)'  ,_topicFeatures() , channelPydevFeatures     , _otherFeatures())
    Template( '_template.html', 'refactoring'             , 'Refactoring'              ,_topicFeatures() , channelPydevFeatures     , _otherFeatures())
    Template( '_template.html', 'codecoverage'            , 'Code Coverage'            ,_topicFeatures() , channelPydevFeatures     , _otherFeatures())
    Template( '_template.html', 'debug'                   , 'Debugger'                 ,_topicFeatures() , channelPydev             , _otherFeatures())
    Template( '_template.html', 'editor_prefs'            , 'Editor Preferences'       ,_topicFeatures() , channelPydev             , _otherFeatures())
    Template( '_template.html', 'editor'                  , 'Editor'                   ,_topicFeatures() , channelPydevFeatures     , _otherFeatures())
    Template( '_template.html', 'pylint'                  , 'PyLint'                   ,_topicFeatures() , channelPydevFeatures     , _otherFeatures())
    Template( '_template.html', 'tasks'                   , 'Tasks'                    ,_topicFeatures() , channelPydevFeatures     , _otherFeatures())
    print "done"
    
if __name__ == '__main__':
    Main()
