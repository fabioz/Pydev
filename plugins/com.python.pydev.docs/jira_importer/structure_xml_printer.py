'''
Structure for tracker items in sourceforge xml:

 trackers
 document projectresources trackers 

     tracker
     document projectresources trackers tracker 

         tracker_id
         document projectresources trackers tracker tracker_id 

         tracker_name
         document projectresources trackers tracker tracker_name 

         description
         document projectresources trackers tracker description 

         is_public
         document projectresources trackers tracker is_public 

         allow_anonymous
         document projectresources trackers tracker allow_anonymous 

 trackers
 document trackers 

     tracker
     document trackers tracker 

         url
         document trackers tracker url 

         tracker_id
         document trackers tracker tracker_id 

         name
         document trackers tracker name 

         description
         document trackers tracker description 

         is_public
         document trackers tracker is_public 

         allow_anon
         document trackers tracker allow_anon 

         email_updates
         document trackers tracker email_updates 

         due_period
         document trackers tracker due_period 

         submit_instructions
         document trackers tracker submit_instructions 

         browse_instructions
         document trackers tracker browse_instructions 

         status_timeout
         document trackers tracker status_timeout 

         due_period_initial
         document trackers tracker due_period_initial 

         due_period_update
         document trackers tracker due_period_update 

         reopen_on_comment
         document trackers tracker reopen_on_comment 

         canned_responses
         document trackers tracker canned_responses 

             canned_response
             document trackers tracker canned_responses canned_response 

                 id
                 document trackers tracker canned_responses canned_response id 

                 title
                 document trackers tracker canned_responses canned_response title 

                 body
                 document trackers tracker canned_responses canned_response body 

                 state
                 document trackers tracker canned_responses canned_response state 

                 added_user
                 document trackers tracker canned_responses canned_response added_user 

                 mod_user
                 document trackers tracker canned_responses canned_response mod_user 

                 last_updated
                 document trackers tracker canned_responses canned_response last_updated 

         groups
         document trackers tracker groups 

             group
             document trackers tracker groups group 

                 id
                 document trackers tracker groups group id 

                 group_name
                 document trackers tracker groups group group_name 

         categories
         document trackers tracker categories 

             category
             document trackers tracker categories category 

                 id
                 document trackers tracker categories category id 

                 category_name
                 document trackers tracker categories category category_name 

                 auto_assignee
                 document trackers tracker categories category auto_assignee 

         resolutions
         document trackers tracker resolutions 

             resolution
             document trackers tracker resolutions resolution 

                 id
                 document trackers tracker resolutions resolution id 

                 name
                 document trackers tracker resolutions resolution name 

         statuses
         document trackers tracker statuses 

             status
             document trackers tracker statuses status 

                 id
                 document trackers tracker statuses status id 

                 name
                 document trackers tracker statuses status name 

         tracker_items
         document trackers tracker tracker_items 

             tracker_item
             document trackers tracker tracker_items tracker_item 

                 url
                 document trackers tracker tracker_items tracker_item url 

                 id
                 document trackers tracker tracker_items tracker_item id 

                 status_id
                 document trackers tracker tracker_items tracker_item status_id 

                 category_id
                 document trackers tracker tracker_items tracker_item category_id 

                 group_id
                 document trackers tracker tracker_items tracker_item group_id 

                 resolution_id
                 document trackers tracker tracker_items tracker_item resolution_id 

                 submitter
                 document trackers tracker tracker_items tracker_item submitter 

                 assignee
                 document trackers tracker tracker_items tracker_item assignee 

                 closer
                 document trackers tracker tracker_items tracker_item closer 

                 submit_date
                 document trackers tracker tracker_items tracker_item submit_date 

                 close_date
                 document trackers tracker tracker_items tracker_item close_date 

                 priority
                 document trackers tracker tracker_items tracker_item priority 

                 summary
                 document trackers tracker tracker_items tracker_item summary 

                 details
                 document trackers tracker tracker_items tracker_item details 

                 is_private
                 document trackers tracker tracker_items tracker_item is_private 

                 followups
                 document trackers tracker tracker_items tracker_item followups 

                     followup
                     document trackers tracker tracker_items tracker_item followups followup 

                         id
                         document trackers tracker tracker_items tracker_item followups followup id 

                         submitter
                         document trackers tracker tracker_items tracker_item followups followup submitter 

                         date
                         document trackers tracker tracker_items tracker_item followups followup date 

                         details
                         document trackers tracker tracker_items tracker_item followups followup details 

                 attachments
                 document trackers tracker tracker_items tracker_item attachments 

                     attachment
                     document trackers tracker tracker_items tracker_item attachments attachment 

                         url
                         document trackers tracker tracker_items tracker_item attachments attachment url 

                         id
                         document trackers tracker tracker_items tracker_item attachments attachment id 

                         filename
                         document trackers tracker tracker_items tracker_item attachments attachment filename 

                         description
                         document trackers tracker tracker_items tracker_item attachments attachment description 

                         filesize
                         document trackers tracker tracker_items tracker_item attachments attachment filesize 

                         filetype
                         document trackers tracker tracker_items tracker_item attachments attachment filetype 

                         date
                         document trackers tracker tracker_items tracker_item attachments attachment date 

                         submitter
                         document trackers tracker tracker_items tracker_item attachments attachment submitter 

                 history_entries
                 document trackers tracker tracker_items tracker_item history_entries 

                     history_entry
                     document trackers tracker tracker_items tracker_item history_entries history_entry 

                         id
                         document trackers tracker tracker_items tracker_item history_entries history_entry id 

                         field_name
                         document trackers tracker tracker_items tracker_item history_entries history_entry field_name 

                         old_value
                         document trackers tracker tracker_items tracker_item history_entries history_entry old_value 

                         date
                         document trackers tracker tracker_items tracker_item history_entries history_entry date 

                         updator
                         document trackers tracker tracker_items tracker_item history_entries history_entry updator 




'''

import xml.sax.handler
 

#=======================================================================================================================
# XMLStructurePrinter
#=======================================================================================================================
class XMLStructurePrinter(xml.sax.handler.ContentHandler):
    '''
    Prints the overview of a xml document (unique fields and its location in the document.
    '''
    
    def __init__(self):
        self._printed = set()
        self._start_recording = 0
        self._level = -1
        self._stack = []
 
    def startElement(self, name, attributes):
        self._stack.append(name)
        if name == 'trackers':
            self._start_recording += 1
        
        if self._start_recording:
            self._level += 1
            to_print = ('    '*self._level), ' '.join(self._stack), tuple(attributes.items())
            if to_print not in self._printed:
                self._printed.add(to_print)
                print ('    '*self._level), name
                if to_print[2]:
                    attributes = 'attributes:' + ' '.join(to_print[2])
                else:
                    attributes = ''
                    
                print to_print[0], to_print[1], attributes
                print ''
 
    def characters(self, data):
        pass
 
    def endElement(self, name):
        del self._stack[-1]
        if self._start_recording:
            self._level -= 1
            
        if name == 'trackers':
            self._start_recording -= 1
        


#=======================================================================================================================
# main
#=======================================================================================================================
if __name__ == '__main__':
    parser = xml.sax.make_parser(  )
    handler = XMLStructurePrinter()
    parser.setContentHandler(handler)
    parser.parse("pydev_export2.xml")
