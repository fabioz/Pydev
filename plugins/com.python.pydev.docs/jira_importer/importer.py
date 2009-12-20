#!/usr/bin/python

# Sample Python client accessing JIRA via XML-RPC. Methods requiring
# more than basic user-level access are commented out.
#
# Refer to the XML-RPC Javadoc to see what calls are available:
# http://docs.atlassian.com/software/jira/docs/api/rpc-jira-plugin/latest/com/atlassian/jira/rpc/xmlrpc/XmlRpcService.html
# http://confluence.atlassian.com/pages/viewpage.action?pageId=9623





#
#xmlrpclib.Fault: <Fault 0: 'java.lang.Exception: com.atlassian.jira.rpc.exception.RemoteValidationException: {
#
#customfield_10002=Invalid value \'10011\' passed for customfield \'Studio Install Type\'. 
#Allowed values are: com.atlassian.jira.issue.customfields.option.OptionsImpl@17b837f[
#optionsLookup={
#    10011=Eclipse Plugin, 
#    10010=Standalone, 10019=N/A},
#
#relatedField=com.atlassian.jira.issue.fields.config.FieldConfigImpl@c01d8ac9,
#reorderer=com.atlassian.jira.util.CollectionReorderer@1f63f00,
#optionsManager=com.atlassian.jira.issue.customfields.manager.CachedOptionsManager@1a23de8,size=3], 
#
#
#customfield_10000=Invalid value \'<spanstyle="color:gray">Low</span>\' passed for customfield \'Importance\'.
#Allowed values are: com.atlassian.jira.issue.customfields.option.OptionsImpl@16f597b[
#optionsLookup={
#    10009=<span style="color:gray">Low</span>, 
#    10008=Average, 10007=<b>Major</b>, 
#    10006=<span style="color:red; font-weight:bold;">Critical</span>},
#relatedField=com.atlassian.jira.issue.fields.config.FieldConfigImpl@c76680cc,
#reorderer=com.atlassian.jira.util.CollectionReorderer@1f63f00,
#optionsManager=com.atlassian.jira.issue.customfields.manager.CachedOptionsManager@1a23de8,size=4]} : []'>
#


import xmlrpclib

s = xmlrpclib.ServerProxy('http://support.aptana.com/asap/rpc/xmlrpc')
auth = s.jira1.login('fabioz', raw_input('password'))
try:

    print '----PYD PROJECT INFO -------'
    for p in s.jira1.getProjects(auth):
        if p['key'] == 'PYD':
            for key, val in p.iteritems():
                print key, val

    print '---- END PYD PROJECT INFO -------'


#    issue = s.jira1.createIssue(auth, {
#         'project': 'PYD',
#         'type':2,
#         'customFieldValues':[
#              {'customfieldId': 'customfield_10002', 'values': ['Eclipse Plugin']}, #Studio install type
#              {'customfieldId': 'customfield_10000', 'values': ['Average']}, #Importance
#         ],
#         'summary': 'Issue created via XML-RPC',
#         'description': 'Testing XML-RPC for PYD'})

    issue = s.jira1.getIssue(auth, 'PYD-4')
    for key, val in issue.iteritems():
        print key, val
        
    s.jira1.updateIssue(auth, issue['key'],
        {'customfield_10000':['Average']}
    )
    
    
finally:
    print 'logout', s.jira1.logout(auth)



#print "Created %s/browse/%s" %(s.jira1.getServerInfo(auth)['baseUrl'], newissue['key'])
#
#print "Commenting on issue.."
#s.jira1.addComment(auth, newissue['key'], 'Comment added with XML-RPC')
#
#print "Modifying issue..."
#
#s.jira1.updateIssue(auth, newissue['key'], {
#                     "summary": ["[Updated] issue created via XML-RPC"],
#
#                     # Setting a custom field. The id (10010) is discoverable from
#                     # the database or URLs in the admin section
#
#                     "customfield_10010": ["Random text set in updateIssue method"],
#
#                     # Demonstrate setting a cascading selectlist:
#                     "customfield_10061": ["10098"],
#                     "customfield_10061_1": ["10105"],
#                     "components": ["10370"]
#
#                     })
#
#
#print "Done!"