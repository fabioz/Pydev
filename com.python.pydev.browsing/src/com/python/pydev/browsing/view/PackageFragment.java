package com.python.pydev.browsing.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;

public class PackageFragment {
	private List<IFolder> packageFragment;
	private List<Object> children;
	
	public PackageFragment() {
		packageFragment = new ArrayList<IFolder>();
		children = new ArrayList<Object>();
	}
	
	public void insertFragment( IFolder fragment ) {
		packageFragment.add(fragment);
	}
	
	public void insertChildren( Object child ) {
		this.children.add(child);
	}
	
	public String getText(){
		String text = "";
		for( IFolder folder : packageFragment ) {
			text += folder.getName() + ".";
		}
		return text.substring(0, text.length()-1);
	}
	
	public boolean belongToThisFragment( IFolder otherFolder ) {
		IFolder folder = packageFragment.get( packageFragment.size()-1 );
		
		String otherFolderPath = otherFolder.getProjectRelativePath().toPortableString(); 
		String folderPath = folder.getProjectRelativePath().toPortableString();
		
		boolean result = otherFolderPath.contains(folderPath);
		return otherFolderPath.contains(folderPath);
	}
	
	public Object[] getChildren() {
		return children.toArray();
	}
}
