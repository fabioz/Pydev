"""
Some common declarations for the xmlproc system gathered in one file.
"""

# $Id$

import string,re,urlparse,os,sys

import xmlapp,charconv,errors

# Standard exceptions

class OutOfDataException(Exception):
    """An exception that signals that more data is expected, but the current
    buffer has been exhausted."""
    pass

# ==============================
# The general entity parser
# ==============================

class EntityParser:
    """A generalized parser for XML entities, whether DTD, documents or even
    catalog files."""

    def __init__(self):
        # --- Creating support objects
        self.err=xmlapp.ErrorHandler(self)
        self.ent=xmlapp.EntityHandler(self.err)
        self.isf=xmlapp.InputSourceFactory()
        self.pubres=xmlapp.PubIdResolver()
        self.data_charset="iso-8859-1"
        self.charset_converter=charconv.id_conv # the identity transform
        self.err_lang="en"
        self.errors=errors.get_error_list(self.err_lang)
        
        self.reset()

    def set_error_language(self,language):
        """Sets the language in which errors are reported. (ISO 3166 codes.)
        Throws a KeyError if the language is not supported."""
        self.errors=errors.get_error_list(string.lower(language))
        self.err_lang=string.lower(language) # only set if supported

    def set_error_handler(self,err):
	"Sets the object to send error events to."
        self.err=err

    def set_pubid_resolver(self,pubres):
        self.pubres=pubres
        
    def set_entity_handler(self,ent):
	"Sets the object that resolves entity references."
        self.ent=ent

    def set_inputsource_factory(self,isf):
        "Sets the object factory used to create input sources from sysids."
        self.isf=isf

    def set_data_charset(self,charset):
        """Tells the parser which character encoding to use when reporting data
        to applications. [Currently not in use!]"""
        self.data_charset=charset
        
    def parse_resource(self,sysID,bufsize=16384):
        """Begin parsing an XML entity with the specified system
        identifier.  Only used for the document entity, not to handle
        subentities, which open_entity takes care of."""

        self.current_sysID=sysID
        try:
            infile=self.isf.create_input_source(sysID)
        except IOError,e:
            self.report_error(3000,sysID)
            return
	
        self.read_from(infile,bufsize)
        infile.close()
        self.flush()
        self.parseEnd()

    def open_entity(self,sysID,name="None"):
        """Starts parsing a new entity, pushing the old onto the stack. This
        method must not be used to start parsing, use parse_resource for
        that."""

        sysID=join_sysids(self.get_current_sysid(),sysID)
        
        try:
            inf=self.isf.create_input_source(sysID)
        except IOError,e:
            self.report_error(3000,sysID)
            return

        self._push_ent_stack(name)
        self.current_sysID=sysID
        self.pos=0
        self.line=1
        self.last_break=0
        self.data=""
    
        self.read_from(inf)

        self.flush()
        self.pop_entity()

    def push_entity(self,sysID,contents,name="None"):
        """Parse some text and consider it a new entity, making it possible
        to return to the original entity later."""
        self._push_ent_stack(name)
        self.data=contents
        self.current_sysID=sysID
        self.pos=0
        self.line=1
        self.last_break=0
        self.datasize=len(contents)
        self.last_upd_pos=0
        self.final=1

    def pop_entity(self):
        "Skips out of the current entity and back to the previous one."
        if self.ent_stack==[]: self.report_error(4000)

        self._pop_ent_stack()
        self.final=0
    
    def read_from(self,fileobj,bufsize=16384):
        """Reads data from a file-like object until EOF. Does not close it.
        **WARNING**: This method does not call the parseStart/parseEnd methods,
        since it does not know if it may be called several times. Use
        parse_resource if you just want to read a file."""
        while 1:
            buf=fileobj.read(bufsize)
            if buf=="": break

            try:
                self.feed(buf)
            except OutOfDataException,e:
                break

    def reset(self):
        """Resets the parser, losing all unprocessed data."""
        self.ent_stack=[]
        self.open_ents=[]  # Used to test for entity recursion
        self.current_sysID="Unknown"
        self.first_feed=1

        # Block information
        self.data=""
        self.final=0
        self.datasize=0
        self.start_point=-1
    
        # Location tracking
        self.line=1
        self.last_break=0
        self.block_offset=0 # Offset from start of stream to start of cur block
        self.pos=0
        self.last_upd_pos=0
            
    def feed(self,new_data):
        """Accepts more data from the data source. This method must
        set self.datasize and correctly update self.pos and self.data.
        It also does character encoding translation."""
        if self.first_feed:
            self.first_feed=0                    
            self.parseStart()

        self.update_pos() # Update line/col count

        new_data=self.charset_converter(new_data) # Character enc conversion
        
        if self.start_point==-1:
            self.block_offset=self.block_offset+self.datasize
            self.data=self.data[self.pos:]
            self.last_break=self.last_break-self.pos  # Keep track of column
            self.pos=0
            self.last_upd_pos=0

        # Adding new data and doing line end normalization
        self.data=string.replace(self.data+new_data,
                                 "\015\012","\012")
        self.datasize=len(self.data)

        self.do_parse()
        
    def close(self):
        "Closes the parser, processing all remaining data. Calls parseEnd."
        self.flush()
        self.parseEnd()        
        
    def parseStart(self):
        "Called before the parse starts to notify subclasses."
        pass

    def parseEnd(self):
        "Called when there are no more data to notify subclasses."
        pass

    def flush(self):
        "Parses any remnants of data in the last block."
        if not self.pos+1==self.datasize:
            self.final=1
            pos=self.pos
            try:
                self.do_parse()
            except OutOfDataException,e:
                if pos!=self.pos:
                    self.report_error(3001)
                
    # --- GENERAL UTILITY
    
    # --- LOW-LEVEL SCANNING METHODS

    def set_start_point(self):
        """Stores the current position and tells the parser not to forget any
        of the data beyond this point until get_region is called."""
        self.start_point=self.pos

    def store_state(self):
        """Makes the parser remember where we are now, so we can go back
        later with restore_state."""
        self.set_start_point()
        self.old_state=(self.last_upd_pos,self.line,self.last_break)

    def restore_state(self):
        """Goes back to a state previously remembered with store_state."""
        self.pos=self.start_point
        self.start_point=-1
        (self.last_upd_pos,self.line,self.last_break)=self.old_state
        
    def get_region(self):
        """Returns the area from start_point to current position and remove
        start_point."""
        data=self.data[self.start_point:self.pos]
        self.start_point=-1
        return data

    def find_reg(self,regexp,required=1):
        """Moves self.pos to the first character that matches the regexp and
        returns everything from pos and up to (but not including) that
        character."""
        oldpos=self.pos
        mo=regexp.search(self.data,self.pos)
        if mo==None:
            if self.final and not required:                
                self.pos=len(self.data)   # Just moved to the end
                return self.data[oldpos:]            
                
            raise OutOfDataException()
                
        self.pos=mo.start(0)
        return self.data[oldpos:self.pos]
    
    def scan_to(self,target):
        "Moves self.pos to beyond target and returns skipped text."
        new_pos=string.find(self.data,target,self.pos)
        if new_pos==-1:
            raise OutOfDataException()
        res=self.data[self.pos:new_pos]
        self.pos=new_pos+len(target)
        return res
    
    def get_index(self,target):
        "Finds the position where target starts and returns it."
        new_pos=string.find(self.data,target,self.pos)
        if new_pos==-1:
            raise OutOfDataException()
        return new_pos
    
    def test_str(self,test_str):
        "See if text at current position matches test_str, without moving."
        if self.datasize-self.pos<len(test_str) and not self.final:
            raise OutOfDataException()
        return self.data[self.pos:self.pos+len(test_str)]==test_str
    
    def now_at(self,test_str):
        "Checks if we are at this string now, and if so skips over it."
        pos=self.pos
        if self.datasize-pos<len(test_str) and not self.final:
            raise OutOfDataException()
    
        if self.data[pos:pos+len(test_str)]==test_str:
            self.pos=self.pos+len(test_str)
            return 1
        else:
            return 0
    
    def skip_ws(self,necessary=0):
        "Skips over any whitespace at this point."
        start=self.pos
        
        try:
            while self.data[self.pos] in whitespace:
                self.pos=self.pos+1
        except IndexError:
            if necessary and start==self.pos:
                if self.final:
                    self.report_error(3002)
                else:
                    raise OutOfDataException()

    def test_reg(self,regexp):
        "Checks if we match the regexp."
        if self.pos>self.datasize-5 and not self.final:
            raise OutOfDataException()
    
        return regexp.match(self.data,self.pos)!=None
        
    def get_match(self,regexp):
        "Returns the result of matching the regexp and advances self.pos."
        if self.pos>self.datasize-5 and not self.final:
            raise OutOfDataException()

        ent=regexp.match(self.data,self.pos)
        if ent==None:
            self.report_error(reg2code[regexp.pattern])
            return ""

        end=ent.end(0) # Speeds us up slightly
        if end==self.datasize:
            raise OutOfDataException()

        self.pos=end
        return ent.group(0)
    
    def update_pos(self):
        "Updates (line,col)-pos by checking processed blocks."
        breaks=string.count(self.data,"\n",self.last_upd_pos,self.pos)
        self.last_upd_pos=self.pos

        if breaks>0:
            self.line=self.line+breaks
            self.last_break=string.rfind(self.data,"\n",0,self.pos)

    def get_wrapped_match(self,wraps):
        "Returns a contained match. Useful for regexps inside quotes."
        found=0
        for (wrap,regexp) in wraps:
            if self.test_str(wrap):
                found=1
                self.pos=self.pos+len(wrap)
                break

        if not found:
            msg=""
            for (wrap,regexp) in wraps[:-1]:
                msg="%s'%s', " % (msg,wrap)
            self.report_error(3004,(msg[:-2],wraps[-1][0]))

        data=self.get_match(regexp)
        if not self.now_at(wrap):
            self.report_error(3005,wrap)

        return data

    #--- ERROR HANDLING

    def report_error(self,number,args=None):
        try:
            msg=self.errors[number]
            if args!=None:
                msg=msg % args
        except KeyError:
            msg=self.errors[4002] # Unknown err msg :-)
        
        if number<2000:
            self.err.warning(msg)
        elif number<3000:
            self.err.error(msg)
        else:
            self.err.fatal(msg)
    
    #--- USEFUL METHODS

    def get_current_sysid(self):
        "Returns the sysid of the file we are reading now."
        return self.current_sysID

    def set_sysid(self,sysID):
        "Sets the current system identifier. Does not store the old one."
        self.current_sysID=sysID

    def get_offset(self):
        "Returns the current offset from the start of the stream."
        return self.block_offset+self.pos
    
    def get_line(self):
        "Returns the current line number."
        self.update_pos()
        return self.line

    def get_column(self):
        "Returns the current column position."
        self.update_pos()
        return self.pos-self.last_break  

    def is_root_entity(self):
        "Returns true if the current entity is the root entity."
        return self.ent_stack==[]

    def is_external(self):
        """Returns true if the current entity is an external entity. The root
        (or document) entity is not considered external."""
        return self.ent_stack!=[] and \
               self.ent_stack[0][0]!=self.get_current_sysid()

    # --- Internal methods

    def _push_ent_stack(self,name="None"):
        self.ent_stack.append((self.get_current_sysid(),self.data,self.pos,\
                               self.line,self.last_break,self.datasize,\
                               self.last_upd_pos,self.block_offset,self.final,
                               name))

    def _pop_ent_stack(self):
        (self.current_sysID,self.data,self.pos,self.line,self.last_break,\
         self.datasize,self.last_upd_pos,self.block_offset,self.final,dummy)=\
             self.ent_stack[-1]
        del self.ent_stack[-1]

# ==============================
# Common code for some parsers
# ==============================

class XMLCommonParser(EntityParser):

    def parse_external_id(self,required=0,sysidreq=1):
        """Parses an external ID declaration and returns a tuple consisting
        of (pubid,sysid). If the required attribute is false neither SYSTEM
        nor PUBLIC identifiers are required. If sysidreq is false a SYSTEM
        identifier is not required after a PUBLIC one."""

        pub_id=None
        sys_id=None
        
        if self.now_at("SYSTEM"):
            self.skip_ws(1)
            sys_id=self.get_wrapped_match([("\"",reg_sysid_quote),\
                           ("'",reg_sysid_apo)])
        elif self.now_at("PUBLIC"):
            self.skip_ws(1)
            pub_id=self.get_wrapped_match([("\"",reg_pubid_quote),\
                           ("'",reg_pubid_apo)])
            pub_id=string.join(string.split(pub_id))

            if sysidreq:
                self.skip_ws(1)
                sys_id=self.get_wrapped_match([("\"",reg_sysid_quote),\
                                                   ("'",reg_sysid_apo)])
            else:
                if self.test_str("'") or self.test_str('"'):
                    self.report_error(3002)
                self.skip_ws()
                if self.test_str("'") or self.test_str('"'):
                    sys_id=self.get_wrapped_match([("\"",reg_sysid_quote),\
                                                   ("'",reg_sysid_apo)])
        else:
            if required: self.report_error(3006)

        return (pub_id,sys_id)

    def __get_quoted_string(self):
        "Returns the contents of a quoted string at current position."
        try:
            quo=self.data[self.pos]
        except IndexError:
            raise OutOfDataException()
            
        if not (self.now_at('"') or self.now_at("'")):
            self.report_error(3004,("'\"'","'"))
            self.scan_to(">")
            return ""

        return self.scan_to(quo)
    
    def parse_xml_decl(self,handler=None):
        "Parses the contents of the XML declaration from after the '<?xml'."

        textdecl=self.is_external() # If this is an external entity, then this
                                    # is a text declaration, not an XML decl

        self.update_pos()
        if self.get_column()!=5 or self.get_line()!=1 or \
           (self.ent_stack!=[] and not textdecl):
            if textdecl:
                self.report_error(3007)
            else:    
                self.report_error(3008)                
            
        if self.seen_xmldecl: # Set in parse_pi, to avoid block problems
            if textdecl:
                self.report_error(3009)
            else:
                self.report_error(3010)

        enc=None
        sddecl=None
        ver=None
        self.skip_ws()
        
        if self.now_at("version"):
            self.skip_ws()
            if not self.now_at("="): self.report_error(3005,"=")
            self.skip_ws()
            ver=self.__get_quoted_string()

            m=reg_ver.match(ver)
            if m==None or m.end()!=len(ver):
                self.report_error(3901,ver)
            elif ver!="1.0":
                self.report_error(3046)                

            if self.test_str("encoding") or self.test_str("standalone"):
                self.report_error(3002)
            self.skip_ws()
        elif not textdecl:
            self.report_error(3011)

        if self.now_at("encoding"):
            self.skip_ws()
            if not self.now_at("="): self.report_error(3005,"=")
            self.skip_ws()
            enc=self.__get_quoted_string()
            if reg_enc_name.match(enc)==None:
                self.report_error(3902)

            # Setting up correct conversion
            if charconv.convdb.can_convert(enc,self.data_charset):
                self.charset_converter=\
                    charconv.convdb.get_converter(enc,self.data_charset)
            else:
                self.report_error(1002,enc)

            self.skip_ws()      

        if self.now_at("standalone"):
            if textdecl:
                self.report_error(3012)
                sddecl="yes"
            else:
                self.skip_ws()
                if not self.now_at("="): self.report_error(3005,"=")
                self.skip_ws()
                sddecl=self.__get_quoted_string()
                if reg_std_alone.match(sddecl)==None:
                    self.report_error(3911)

                self.standalone= sddecl=="yes"

                self.skip_ws()

        self.skip_ws()

        if handler!=None:
            handler.set_entity_info(ver,enc,sddecl)

    def parse_pi(self,handler,report_xml_decl=0):
        """Parses a processing instruction from after the '<?' to beyond
        the '?>'."""
        trgt=self._get_name()

        if trgt=="xml":
            if report_xml_decl:
                self.parse_xml_decl(handler)
            else:
                self.parse_xml_decl()
                
            if not self.now_at("?>"):
                self.report_error(3005,"?>")
            self.seen_xmldecl=1
        else:
            if self.now_at("?>"):
                rem=""
            else:
                self.skip_ws(1)
                rem=self.scan_to("?>") # OutOfDataException if not found

            if reg_res_pi.match(trgt)!=None:
                if trgt=="xml:namespace":
                    self.report_error(1003)
                elif trgt!="xml-stylesheet":
                    self.report_error(3045)
                
            handler.handle_pi(trgt,rem)   

    def parse_comment(self,handler):
        "Parses the comment from after '<!--' to beyond '-->'."
        new_pos = self.get_index("--")
        handler.handle_comment(self.data[self.pos : new_pos])
        self.pos = new_pos
        if not self.now_at("-->"):
            self.report_error(3005,"-->")

    def _read_char_ref(self):
        "Parses a character reference and returns the character."
        if self.now_at("x"):
            digs=unhex(self.get_match(reg_hex_digits))
        else:
            digs=int(self.get_match(reg_digits))

        if not (digs==9 or digs==10 or digs==13 or \
                (digs>=32 and digs<=255)):
            if digs>255:
                self.report_error(1005,digs)
            else:
                self.report_error(3018,digs)
            return ""
        else:
            return chr(digs)

    def _get_name(self):
        """Parses the name at the current position and returns it. An error
        is reported if no name is present."""
        if self.pos>self.datasize-5 and not self.final:
            raise OutOfDataException()

        data=self.data
        pos=self.pos
        if data[pos] in namestart:
            start=pos
            pos=pos+1

            try:
                while data[pos] in namechars:
                    pos=pos+1

                self.pos=pos
                return intern(data[start:pos])
            except IndexError:
                self.pos=pos
                if self.final:
                    return intern(data[start:])
                else:
                    raise OutOfDataException()
        else:
            self.report_error(3900)
            return ""            
    
# --- A collection of useful functions

# Utility functions

def unhex(hex_value):
    "Converts a string hex-value to an integer."

    sum=0
    for char in hex_value:
        sum=sum*16
        char=ord(char)
    
        if char<58 and char>=48:
            sum=sum+(char-48)
        elif char>=97 and char<=102:
            sum=sum+(char-87)
        elif char>=65 and char<=70:
            sum=sum+(char-55)
    # else ERROR, but it can't occur here

    return sum

def matches(regexp,str):
    mo=regexp.match(str)
    return mo!=None and len(mo.group(0))==len(str)

def join_sysids_general(base,url):
    if urlparse.urlparse(base)[0]=="":
        if urlparse.urlparse(url)[0]=="":
            return os.path.join(os.path.split(base)[0],url)
        else:
            return url
    else:
        return urlparse.urljoin(base,url)

def join_sysids_win32(base,url):
    if len(urlparse.urlparse(base)[0])<2: # Handles drive identifiers correctly
        if len(urlparse.urlparse(url)[0])<2:
            return os.path.join(os.path.split(base)[0],url)
        else:
            return url
    else:
        return urlparse.urljoin(base,url)    

# here join_sysids(base,url): is set to the correct function

if sys.platform=="win32":
    join_sysids=join_sysids_win32
else:
    join_sysids=join_sysids_general
    
# --- Some useful regexps

namestart="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_:"+\
          "ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿ"
namechars=namestart+"0123456789.·-"
whitespace="\n\t \r"

reg_ws=re.compile("[\n\t \r]+")
reg_ver=re.compile("[-a-zA-Z0-9_.:]+")
reg_enc_name=re.compile("[A-Za-z][-A-Za-z0-9._]*")
reg_std_alone=re.compile("yes|no")
reg_name=re.compile("["+namestart+"]["+namechars+"]*")
reg_names=re.compile("["+namestart+"]["+namechars+"]*"
             "([\n\t \r]+["+namestart+"]["+namechars+"]*)*")
reg_nmtoken=re.compile("["+namechars+"]+")
reg_nmtokens=re.compile("["+namechars+"]+([\n\t \r]+["+namechars+"]+)*")
reg_sysid_quote=re.compile("[^\"]*")
reg_sysid_apo=re.compile("[^']*")
reg_pubid_quote=re.compile("[- \n\t\ra-zA-Z0-9'()+,./:=?;!*#@$_%]*")
reg_pubid_apo=re.compile("[- \n\t\ra-zA-Z0-9()+,./:=?;!*#@$_%]*")
reg_start_tag=re.compile("<[A-Za-z_:]")
reg_quoted_attr=re.compile("[^<\"]*")
reg_apo_attr=re.compile("[^<']*")
reg_c_data=re.compile("[<&]")
reg_pe_ref=re.compile("%["+namestart+"]["+namechars+"]*;")

reg_ent_val_quote=re.compile("[^\"]+")
reg_ent_val_apo=re.compile("[^\']+")

reg_attr_type=re.compile(r"CDATA|IDREFS|IDREF|ID|ENTITY|ENTITIES|NMTOKENS|"
             "NMTOKEN") # NOTATION support separate
reg_attr_def=re.compile(r"#REQUIRED|#IMPLIED")

reg_digits=re.compile("[0-9]+")
reg_hex_digits=re.compile("[0-9a-fA-F]+")

reg_res_pi=re.compile("xml",re.I)

reg_int_dtd=re.compile("\"|'|<\\?|<!--|\\]|<!\\[")

reg_attval_stop_quote=re.compile("<|&|\"")
reg_attval_stop_sing=re.compile("<|&|'")

reg_decl_with_pe=re.compile("<(![^-\[]|\?)")
reg_subst_pe_search=re.compile(">|%")

reg_cond_sect=re.compile("<!\\[|\\]\\]>")
reg_litval_stop=re.compile("%|&#")

# RFC 1766 language codes

reg_lang_code=re.compile("([a-zA-Z][a-zA-Z]|[iIxX]-([a-zA-Z])+)(-[a-zA-Z])*")

# Mapping regexps to error codes
# NB: 3900 is reported directly from _get_name

reg2code={
    reg_name.pattern : 3900, reg_ver.pattern : 3901,
    reg_enc_name.pattern : 3902, reg_std_alone.pattern : 3903,
    reg_hex_digits.pattern : 3905,
    reg_digits.pattern : 3906, reg_pe_ref.pattern : 3907,
    reg_attr_type.pattern : 3908, reg_attr_def.pattern : 3909,
    reg_nmtoken.pattern : 3910}
    
# Some useful variables

predef_ents={"lt":"&#60;","gt":"&#62;","amp":"&#38;","apos":"&#39;",
             "quot":'&#34;'}

# Translation tables

ws_trans=string.maketrans("\r\t\n","   ")  # Whitespace normalization
id_trans=string.maketrans("","")           # Identity transform 
