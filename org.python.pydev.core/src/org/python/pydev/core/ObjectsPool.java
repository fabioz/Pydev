/*
 * Created on Apr 9, 2006
 */
package org.python.pydev.core;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author Fabio
 */
public class ObjectsPool {

    public Map pool = new WeakHashMap();
    
    /**
     * Retorna um objeto que seja igual ao passado e guarda ele no Pool.
     * 
     * Ex.: se um Integer com valor 1 for requisitado do Pool, ele vai verificar se já existe um e retorná-lo.
     * Se não existe, o objeto passado será colocado no pool e retornado.
     * 
     * @param o
     * @return
     */
    public Object getFromPool(Object o){
        Class class_ = o.getClass();
        WeakHashMap weakHashMap;

        if(pool.containsKey(class_)){
            weakHashMap = (WeakHashMap) pool.get(class_);
        }else{
            weakHashMap = new WeakHashMap();
            pool.put(class_, weakHashMap);
        }
        
        if(weakHashMap.containsKey(o)){
            WeakReference w = (WeakReference)weakHashMap.get(o);
            return w.get();
        }else{
            weakHashMap.put(o, new WeakReference(o));
            return o;
        }
    }
}
