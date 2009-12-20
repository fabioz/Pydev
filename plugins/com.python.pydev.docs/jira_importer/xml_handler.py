import xml.dom.minidom


#=======================================================================================================================
# Handler
#=======================================================================================================================
class Handler(object):
    
    
    def __init__(self):
        self._node_trackers = None
        self._node_tracker = None
        self._node_tracker_items = None
        self._node_tracker_item = None
    
    
    def _GetText(self, node):
        if len(node.childNodes) == 0:
            return ''
        
        assert len(node.childNodes) == 1, 'Expected 1 node. Found: %s' % (len(node.childNodes),)
        return node.childNodes[0].nodeValue
        
        
    def PrintText(self, node):
        print self._GetText(node)
        
        
    _NEEDED_FOR_ITEM = set((
        'url', 'id', 'status_id', 'category_id', 'group_id', 'resolution_id', 
        
        'submitter', 'assignee', 'closer', 
        
        'submit_date', 'close_date', 
        
        'priority', 
        
        'summary', 'details', 
        
        ))
        
        
        
    def HandleTrackerItem(self):
        children_as_dict = {}
        for e in self._node_tracker_item.childNodes:
            
            if e.nodeType == e.ELEMENT_NODE:
                local = str(e.localName)
                if local in self._NEEDED_FOR_ITEM:
                    children_as_dict[local] = self._GetText(e)
                    
                elif local == 'followups':
                    pass
        
                elif local == 'attachments':
                    pass
        
                elif local == 'history_entries':
                    pass
        
        #TODO: Create issue (waiting for definition on how to deal with users)
        print children_as_dict
    
    
    def HandleTrackerItems(self):
        total_found = 0
        for t in self._node_tracker_items.childNodes:
            if t.nodeType == t.TEXT_NODE:
                assert self._GetText(t).strip() == ''
            else:
                assert t.localName == 'tracker_item'
                self._node_tracker_item = t
                total_found += 1
                self.HandleTrackerItem()
        print 'Found %s items' % (total_found,)
                
    
    def HandleTracker(self):
        
        print '\n\nStarted handling tracker...'
        
        for e in self._node_tracker.childNodes:
            if e.localName == 'name':
                self.PrintText(e)
                
            elif e.localName == 'url':
                self.PrintText(e)
                
            elif e.localName == 'tracker_items':
                self._node_tracker_items = e
                self.HandleTrackerItems()
                
    
    
    def HandleTrackers(self, trackers):
        self._node_trackers = trackers
        for e in trackers.childNodes: 
            if e.nodeType == e.ELEMENT_NODE and e.localName == "tracker":
                self._node_tracker = e
                self.HandleTracker()


#=======================================================================================================================
# entry point
#=======================================================================================================================
if __name__ == '__main__':
    doc = xml.dom.minidom.parse(r'W:\pydev_pro\plugins\com.python.pydev.docs\jira_importer\pydev_export2.xml')
    
    trackers_found = doc.getElementsByTagName('trackers')
    
    for trackers in trackers_found:
        Handler().HandleTrackers(trackers)
    
