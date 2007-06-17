import StringIO
import traceback

try:
    __setFalse = False
except:
    False = 0
    True = 1

class InspectStub:    
    def isbuiltin(self, _args):       
        return False
    def isroutine(self, object):       
        return False
       
try:
    import inspect
except:
    inspect = InspectStub()

try:
    import java.lang #@UnresolvedImport
except:
    pass

#types does not include a MethodWrapperType
try:
   MethodWrapperType = type([].__str__)   
except:
   MethodWrapperType = None


class Resolver:

    def resolve(self, var, attribute):
        return getattr(var, attribute)
    
    def getDictionary(self,var):
        if MethodWrapperType:
            return self._getPyDictionary(var)
        else:
            return self._getJyDictionary(var)        

    def _getJyDictionary(self,obj):
        ret = {}
        found = java.util.HashMap()
    
        original = obj
        if hasattr(obj, '__class__') and obj.__class__ ==  java.lang.Class:
            
            #get info about superclasses
            classes = []
            classes.append(obj)
            c = obj.getSuperclass()
            while c != None:
                classes.append(c)
                c = c.getSuperclass()
            
            #get info about interfaces
            interfs = []
            for obj in classes:
                interfs.extend(obj.getInterfaces())
            classes.extend(interfs)
                
            #now is the time when we actually get info on the declared methods and fields
            for obj in classes:

                declaredMethods = obj.getDeclaredMethods()
                declaredFields = obj.getDeclaredFields()
                for i in range(len(declaredMethods)):
                    name = declaredMethods[i].getName()
                    ret[name] = declaredMethods[i].toString()
                    found.put(name, 1)            
                
                for i in range(len(declaredFields)):
                    name = declaredFields[i].getName()
                    found.put(name, 1)
                    #if declaredFields[i].isAccessible():
                    declaredFields[i].setAccessible( True )
                    #ret[name] = declaredFields[i].get( declaredFields[i] )
                    try:
                        ret[name] = declaredFields[i].get( original )
                    except:
                        ret[name] = declaredFields[i].toString()
    
        #this simple dir does not always get all the info, that's why we have the part before
        #(e.g.: if we do a dir on String, some methods that are from other interfaces such as 
        #charAt don't appear)
        try:
            d = dir(original)
            for name in d:
                if found.get(name) is not 1:
                    ret[name] = getattr(original, name)
        except:
            #sometimes we're unable to do a dir
            pass

        ret['type'] = type(obj).__name__
        
        return ret

    def _getPyDictionary(self,var):
        filterPrivate = False
        filterSpecial = True
        filterFunction = True
        filterBuiltIn = True
        
        names = dir(var)
        d = {}
        
        #Be aware that the order in which the filters are applied attempts to 
        #optimize the operation by removing as many items as possible in the 
        #first filters, leaving fewer items for later filters

        if filterBuiltIn or filterFunction:
            for n in names:                
                if filterSpecial:
                    if n.startswith('__') and n.endswith('__'):
                        continue
                    
                if filterPrivate:
                    if n.startswith('_') or n.endswith('__'):
                        continue

                try:
                    attr = getattr(var, n)                
    
                    #filter builtins?
                    if filterBuiltIn:
                        if inspect.isbuiltin(attr):                        
                            continue                
                    
                    #filter functions?
                    if filterFunction:
                        if inspect.isroutine(attr) or isinstance(attr, MethodWrapperType): 
                            continue
                except:
                    #if some error occurs getting it, let's put it to the user.
                    strIO = StringIO.StringIO()
                    traceback.print_exc(file = strIO)
                    attr = strIO.getvalue()
                
                d[ n ] = attr        

        d['type'] = type(var).__name__
        return d        
                
class DictResolver(Resolver):
    def resolve(self, dict, key):
        return dict[key]
    
    def getDictionary(self, dict):
        return dict

class TupleResolver(Resolver): #to enumerate tuples and lists
    def resolve(self, var, attribute):
        return var[int(attribute)]
    
    def getDictionary(self, var):
        #return dict( [ (i, x) for i, x in enumerate(var) ] )
        # modified 'cause jython does not have enumerate support
        d = {}
        for i, item in zip(range(len(var)), var):
            d[ i ] = item        
        return d
        
class InstanceResolver(Resolver):
    def resolve(self, var, attribute):        
        field = var.__class__.getDeclaredField( attribute )
        field.setAccessible( True )
        return field.get( var )
    
    def getDictionary(self,obj):
        ret = {}        
        
        declaredFields = obj.__class__.getDeclaredFields()
        for i in range(len(declaredFields)):
            name = declaredFields[i].getName()
            try:               
                declaredFields[i].setAccessible( True )            
                ret[name] = declaredFields[i].get( obj )                
            except:
                traceback.print_exc()
        
        ret['type'] = type(obj).__name__        
        return ret


class JyArrayResolver(Resolver):
    def resolve(self, var, _attribute):
        return var
    
    def getDictionary(self,obj):
        ret = {}       
        
        for i in range(len(obj)):
            try:        
                ret[ obj[i] ] = obj[i]
            except:
                traceback.print_exc()
                # I don't know why I'm getting an exception
                pass        
        
        ret['type'] = type(obj).__name__        
        return ret

defaultResolver = Resolver()
dictResolver = DictResolver()
tupleResolver = TupleResolver()
instanceResolver = InstanceResolver()
jyArrayResolver = JyArrayResolver()
