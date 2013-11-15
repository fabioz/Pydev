from pydevd_constants import DictContains
import sys
import pydevd_vars
try:
    from urllib import quote, quote_plus, unquote, unquote_plus
except:
    from urllib.parse import quote, quote_plus, unquote, unquote_plus  #@Reimport @UnresolvedImport

#===================================================================================================
# print_var_node
#===================================================================================================
def print_var_node(xml_node, stream):
    name = xml_node.getAttribute('name')
    value = xml_node.getAttribute('value')
    val_type = xml_node.getAttribute('type')

    found_as = xml_node.getAttribute('found_as')
    stream.write('Name: ')
    stream.write(unquote_plus(name))
    stream.write(', Value: ')
    stream.write(unquote_plus(value))
    stream.write(', Type: ')
    stream.write(unquote_plus(val_type))
    if found_as:
        stream.write(', Found as: %s' % (unquote_plus(found_as),))
    stream.write('\n')

#===================================================================================================
# print_referrers
#===================================================================================================
def print_referrers(obj, stream=None):
    if stream is None:
        stream = sys.stdout
    result = get_referrer_info(obj)
    from xml.dom.minidom import parseString
    dom = parseString(result)

    xml = dom.getElementsByTagName('xml')[0]
    for node in xml.childNodes:
        if node.nodeType == node.TEXT_NODE:
            continue

        if node.localName == 'for':
            stream.write('Searching references for: ')
            for child in node.childNodes:
                if child.nodeType == node.TEXT_NODE:
                    continue
                print_var_node(child, stream)

        elif node.localName == 'var':
            stream.write('Referrer found: ')
            print_var_node(node, stream)

        else:
            sys.stderr.write('Unhandled node: %s\n' % (node,))

    return result


#===================================================================================================
# get_referrer_info
#===================================================================================================
def get_referrer_info(obj):
    try:
        obj_id = id(obj)

        import gc
        referrers = gc.get_referrers(obj)

        curr_frame = sys._getframe()
        frame_type = type(curr_frame)

        #Ignore this frame and any caller frame of this frame

        ignore_frames = {}  #Should be a set, but it's not available on all python versions.
        while curr_frame is not None:
            ignore_frames[curr_frame] = 1
            curr_frame = curr_frame.f_back


        ret = ['<xml>\n']

        ret.append('<for>\n')
        ret.append(pydevd_vars.varToXML(obj, 'searched', ' id="%s"' % (obj_id,)))
        ret.append('</for>\n')

        all_objects = None

        for r in referrers:
            try:
                if DictContains(ignore_frames, r):
                    continue  #Skip the references we may add ourselves
            except:
                pass  #Ok: unhashable type checked...

            if r is referrers:
                continue

            r_type = type(r)
            r_id = str(id(r))

            representation = str(r_type)

            found_as = ''
            if r_type == frame_type:
                for key, val in r.f_locals.items():
                    if val is obj:
                        found_as = key
                        break

            elif r_type == dict:
                # Try to check if it's a value in the dict (and under which key it was found)
                for key, val in r.items():
                    if val is obj:
                        found_as = key
                        break


                #Ok, there's one annoying thing: many times we find it in a dict from an instance,
                #but with this we don't directly have the class, only the dict, so, to workaround that
                #we iterate over all reachable objects ad check if one of those has the given dict.
                if all_objects is None:
                    all_objects = gc.get_objects()

                for obj in all_objects:
                    if getattr(obj, '__dict__', None) is r:
                        r = obj
                        r_type = type(obj)
                        r_id = str(id(r))
                        representation = str(r_type)
                        break

            elif r_type in (tuple, list):

                #Don't use enumerate() because not all Python versions have it.
                i = 0
                for x in r:
                    if x is obj:
                        found_as = '%s[%s]' % (r_type.__name__, i)
                        break
                    i += 1

            if found_as:
                found_as = ' found_as="%s"' % (pydevd_vars.makeValidXmlValue(found_as),)

            ret.append(pydevd_vars.varToXML(r, representation, ' id="%s"%s' % (r_id, found_as)))
    finally:
        #If we have any exceptions, don't keep dangling references from this frame to any of our objects.
        all_objects = None
        referrers = None
        obj = None
        r = None
        x = None
        key = None
        val = None
        curr_frame = None
        ignore_frames = None

    ret.append('</xml>')
    ret = ''.join(ret)
    return ret

