LAST_VERSION_TAG='1.0'

import datetime
def _topicDefault():
    s = \
'''
<td colspan="4" align="center" height="100" valign="middle">
<table  border="0" cellpadding="1" cellspacing="1" bgcolor="#666600" width="100%(perc)s">
<tbody>
<tr>

<td bgcolor="#CCD0C6" align="center" valign="middle" >
<IMG src="images/pydev_logo6.gif" border="0" >

</td>
</tr>
</tbody>
</table>
</td>
'''

    return s

def _getGoogleLiner():
    s = \
'''<script type="text/javascript"><!--
google_ad_client = "pub-0781609843524821";
google_ad_width = 728;
google_ad_height = 15;
google_ad_format = "728x15_0ads_al";
google_ad_channel ="%(channel)s";
//--></script>
<script type="text/javascript"
  src="http://pagead2.googlesyndication.com/pagead/show_ads.js">
</script>'''
    return s


    

def Template( p_template, p_name, p_title, p_topic , p_channel, p_otherFeatures):

    if p_otherFeatures is None:
        p_otherFeatures = ''
        
    if p_topic is None:
        p_topic = _topicDefault()

    if p_title is None:
        p_title = p_name
        
    if p_channel is None:
        raise RuntimeError('Channel not set')
        
    contents_file = '_%s.contents.htm' % p_name
    target_file   = 'final/%s.html' % p_name

    liner = _getGoogleLiner()
    d = {
        'channel': p_channel,
    }
    liner = liner % d
    
    d = {
        'google_liner': liner
    }
    
    
    newFile = file( contents_file, 'r' ).read()
    newFile = newFile % d
    d = {
        'title' :    p_title,
        'contents' : newFile,
        'topic': p_topic,
        'channel': p_channel,
        'date': datetime.datetime.now().strftime('%d %B %Y'),
        'other_features': p_otherFeatures,
        'google_liner_template': liner,
    }

    contents = file( p_template, 'r' ).read()
    contents = contents % d
    contents = contents.replace('LAST_VERSION_TAG', LAST_VERSION_TAG)
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
%s%s%s%s%s%s%s%s%s%s
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
       _getFeaturesTd('console.html','Interactive Console'),
      )


def Main():
#    channelPydev               = '6798751939'
#    channelPydevFeatures       = '1935586933'
    channelPydevFeaturesMain   = '9124732182'
    channelPydevIndex          = '0232202381'
    channelPydevFaq            = '9395062605'
    channelPydevSponsors       = '9586823051'
    channelPydevVersion24      = '8139721027'
    channelPydevScreenshots    = '7446949127'
    channelPydevDownload       = '4850861835'
    channelPydevRoadmap        = '7477309072'
    channelPydevRun            = '8142059816'
    channelPydevDebugPrefs     = '2396011149'
    channelPydevCodeComplScre  = '1729524630'
    channelPydevCodeCompl      = '4190535423'
    channelPydevTemplates      = '9692148260'
    channelPydevContentAss     = '3352888466'
    channelPydevRefactoring    = '6336735894'
    channelPydevCodeCover      = '8064582308'
    channelPydevDebug          = '5155326690'
    channelPydevEditor         = '1542865200'
    channelPydevPyLint         = '5824164055'
    channelPydevTasks          = '0662167821'
    channelPydevDevelopers     = '3105630764'
    channelPydevConsole        = '3298256184'
    
    
    Template( '_template.htm', 'index'                   , 'Pydev'                    ,None             , channelPydevIndex        , None)
    Template( '_template.htm', 'sponsors'                , 'Sponsors'                 ,None             , channelPydevSponsors     , None)
    Template( '_template.htm', 'screenshots'             , 'Screenshots'              ,None             , channelPydevScreenshots  , None)
    Template( '_template.htm', 'download'                , 'Download'                 ,None             , channelPydevDownload     , None)
    Template( '_template.htm', 'roadmap'                 , 'Roadmap'                  ,None             , channelPydevRoadmap      , None)
    Template( '_template.htm', 'faq'                     , 'FAQ'                      ,None             , channelPydevFaq          , None)
    Template( '_template.htm', 'features'                , 'Features'                 ,None             , channelPydevFeaturesMain , _otherFeatures())
    Template( '_template.htm', 'codecompletion'          , 'Code Completion'          ,None             , channelPydevCodeCompl    , _otherFeatures())
    Template( '_template.htm', 'templates'               , 'Templates'                ,None             , channelPydevTemplates    , _otherFeatures())
    Template( '_template.htm', 'contentassist'           , 'Content Assist (Ctrl+1)'  ,None             , channelPydevContentAss   , _otherFeatures())
    Template( '_template.htm', 'refactoring'             , 'Refactoring'              ,None             , channelPydevRefactoring  , _otherFeatures())
    Template( '_template.htm', 'codecoverage'            , 'Code Coverage'            ,None             , channelPydevCodeCover    , _otherFeatures())
    Template( '_template.htm', 'debug'                   , 'Debugger'                 ,None             , channelPydevDebug        , _otherFeatures())
    Template( '_template.htm', 'editor'                  , 'Editor'                   ,None             , channelPydevEditor       , _otherFeatures())
    Template( '_template.htm', 'pylint'                  , 'PyLint'                   ,None             , channelPydevPyLint       , _otherFeatures())
    Template( '_template.htm', 'console'                 , 'Interactive Console'      ,None             , channelPydevConsole      , _otherFeatures())
    Template( '_template.htm', 'tasks'                   , 'Tasks'                    ,None             , channelPydevTasks        , _otherFeatures())
    Template( '_template.htm', 'developers'              , 'Developers Guide'         ,None             , channelPydevDevelopers   , None)
    
def DoIt():
    import faqbuild
    faqbuild.Generate('scripts/_new_faq.template', '_faq.contents.htm')
    
    Main()    
    print "built org"

if __name__ == '__main__':
    DoIt()
    
    
    