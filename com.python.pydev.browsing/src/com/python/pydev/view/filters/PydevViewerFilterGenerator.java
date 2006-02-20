package com.python.pydev.view.filters;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.python.pydev.view.copiedfromeclipsesrc.FilterDescriptor;

public class PydevViewerFilterGenerator {
	private static List<Class> filters;	
	
	static {
		filters = new ArrayList<Class>();
		
		filters.add( DotResourceFilter.class );
		filters.add( PycFilter.class );
		filters.add( EmptyPackageFilter.class );
		filters.add( PythonFileFilter.class );
		filters.add( NonPydevProjectsFilter.class );
		filters.add( NonSharedProjectFilter.class );
		filters.add( ClosedProjectFilter.class );		
	}
	
	public static FilterDescriptor[] getBuiltinViewrFilter() {
		ArrayList<FilterDescriptor> list = new ArrayList<FilterDescriptor>();
		
		for( int i=0; i<filters.size(); i++ ) {
			Class cl = filters.get(i);
			Constructor cons;
			try {
				cons = cl.getConstructor();
				AbstractViewerFilter filter = (AbstractViewerFilter)cons.newInstance();
				list.add( new FilterDescriptor(filter) );
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}		    
		}
		
		return list.toArray( new FilterDescriptor[0] );
	}
}
