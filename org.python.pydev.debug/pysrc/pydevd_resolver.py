from types import *
import urllib
import sys

try:
    __setFalse = False
except:
    False = 0
    True = 1

class InspectStub:    
    def isbuiltin(self, args):       
        #return isinstance(args, types.BuiltinFunctionType)
        return False
    def isroutine(self, object):       
        return False
       
try:
    import inspect
except:
#    print "passou por except in import inspect"
    inspect = InspectStub()

try:
    import java.lang
except:
    pass

#types does not include a MethodWrapperType
try:
   MethodWrapperType = type([].__str__)   
except:
   MethodWrapperType = None


class Resolver:
    def resolve(self, var, attribute):
        #print "Resolver"
        return getattr(var, attribute)
#==========================================v=====================================
#        if MethodWrapperType:
#            return getattr(var, attribute)
#        else:
#            field = var.__class__.getDeclaredField( attribute )
#            print 'field',field
#            field.setAccessible( True )
#            return field.get( var )
#===============================================================================
    
    def getDictionary(self,var):
#        print "Resolver"
               
        if MethodWrapperType:
            return self._getPyDictionary(var)
        else:
            return self._getJyDictionary(var)        

    def _getJyDictionary(self,obj):
        ret = {}
        found = java.util.HashMap()
    
        if hasattr(obj, '__class__') and obj.__class__ ==  java.lang.Class:
            # print "dirObj"
            declaredMethods = obj.getDeclaredMethods()
            declaredFields = obj.getDeclaredFields()
            for i in range(len(declaredMethods)):
                name = declaredMethods[i].getName()
                # print 'method name', name
                ret[name] = declaredMethods[i].toString()
                found.put(name, 1)            
            
            for i in range(len(declaredFields)):
                name = declaredFields[i].getName()
                # print 'field name', name
                found.put(name, 1)
                #if declaredFields[i].isAccessible():
                declaredFields[i].setAccessible( True )
                #ret[name] = declaredFields[i].get( declaredFields[i] )
                try:
                    ret[name] = declaredFields[i].get( obj )
                except:
                    ret[name] = declaredFields[i].toString()
    
        #this simple dir does not always get all the info, that's why we have the part before
        #(e.g.: if we do a dir on String, some methods that are from other interfaces such as 
        #charAt don't appear)
#===============================================================================
#        d = []
#        if hasattr(obj, '__dict__'):
#            d = dir(obj)
#        print "d",d
#        for name in d:
#            print 'found.get( name )',found.get( name )
#            if not found.get(name):
#                try:
#                    print "name", name
#                    ret[name] = getattr(obj, name)
#                except:
#                    import traceback
#                    traceback.print_exc()
#                    #pass
#===============================================================================
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
        if filterSpecial:
            names = [n for n in names if not (n.startswith('__') and n.endswith('__') )]        
        if filterBuiltIn or filterFunction:
            nametemp = []
            for n in names:                
                attr = getattr(var, n)                
                if filterBuiltIn:
                    if inspect.isbuiltin(attr):                        
                        continue                
                if filterFunction:
                    isinst = False
                    if inspect.isroutine(attr) or isinstance(attr, MethodWrapperType): 
                        continue
                
                nametemp.append(n)
                
            names = nametemp
        
        if filterPrivate:
            names = [n for n in names if not (n.startswith('_') and not n.endswith('__') )]     
        
        for n in names:
            d[ n ] = getattr(var, n)        
        d['type'] = type(var).__name__
        return d        
                
class DictResolver(Resolver):
    def resolve(self, dict, key):
        return dict[key]
    
    def getDictionary(self, dict):
#        print "DictResolver"
        return dict

class TupleResolver(Resolver): #to enumerate tuples and lists
    def resolve(self, var, attribute):
        return var[int(attribute)]
    
    def getDictionary(self, var):
#        print "TupleResolver"
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
        # print "InstanceResolver"
        ret = {}        
        
        declaredFields = obj.__class__.getDeclaredFields()
        for i in range(len(declaredFields)):
            name = declaredFields[i].getName()
            declaredFields[i].setAccessible( True )            
            try:               
                ret[name] = declaredFields[i].get( obj )                
            except:
                import traceback
                traceback.print_exc()
                # I don't know why I'm getting an exception
                pass        
        
        ret['type'] = type(obj).__name__        
        return ret

class JyArrayResolver(Resolver):
    def resolve(self, var, attribute):
        # print 'var',var
        # print 'attribute',attribute        
        return var
    
    def getDictionary(self,obj):
        # print "PyArrayResolver"
        ret = {}       
        
        for i in range(len(obj)):
            try:        
                ret[ obj[i] ] = obj[i]
            except:
                import traceback
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
