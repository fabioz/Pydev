from pydevd_constants import DictContains
import sys
import pydevd_vars

#===================================================================================================
# print_referrers
#===================================================================================================
def print_referrers(obj):
    print get_referrer_info(obj)
    return 'ok'


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

        ignore_frames = {} #Should be a set, but it's not available on all python versions.
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
                pass #Ok: unhashable type checked...

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
                    i+=1

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

