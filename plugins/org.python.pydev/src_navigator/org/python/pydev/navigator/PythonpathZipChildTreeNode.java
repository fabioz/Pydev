package org.python.pydev.navigator;

import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.structure.TreeNode;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.navigator.elements.ISortedElement;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

/**
 * This class represents a file or folder that's inside a zip file.
 */
public class PythonpathZipChildTreeNode extends TreeNode<LabelAndImage> implements ISortedElement{

	/**
	 * Identifies whether we already calculated the children
	 */
	private boolean calculated = false;
	
	/**
	 * Is this a file for a directory?
	 */
	public final boolean isDir;

	/**
	 * Helper structure to get data from the zip
	 */
	public final ZipStructure zipStructure;

	/**
	 * The path inside the zip for this node
	 */
	public final String zipPath;

	/**
	 * Marks whether this is a python package (has __init__) or not.
	 */
	private boolean isPackage;

	/**
	 * If this is a dir, these are the contents from this dir.
	 */
	private List<String> dirContents;
	
	/**
	 * @param zipStructure helper to deal with zip
	 * @param zipPath the path in the zip for this node
	 * @param icon if not provided, it'll be calculated
	 * @param isPythonpathRoot identifies whether we're directly in the root of the zip file
	 */
	public PythonpathZipChildTreeNode(TreeNode<LabelAndImage> parent, ZipStructure zipStructure, String zipPath, Image icon, boolean isPythonpathRoot) {
		super(parent, new LabelAndImage(getLabel(zipPath), icon));
		this.zipStructure = zipStructure;
		this.zipPath = zipPath;
		this.isDir = StringUtils.endsWith(zipPath, '/');
		if(isDir){
			dirContents = zipStructure.contents(zipPath);
			//This one can only be a package if its parent is a root or if it's also a package.
			if(isPythonpathRoot){
				isPackage = true;
				
			}else if(parent instanceof PythonpathZipChildTreeNode && ((PythonpathZipChildTreeNode)parent).isPackage){
				for (String s : dirContents) {
					if(PythonPathHelper.isValidInitFile(s)){
						isPackage=true;
						break;
					}
				}
				
			}
		}
		
		//Update the icon if it wasn't received.
		if(icon == null){
			ImageCache imageCache = PydevPlugin.getImageCache();
			if(isDir){
				if(isPackage){
					this.getData().o2 = imageCache.get(UIConstants.FOLDER_PACKAGE_ICON);
				}else{
					this.getData().o2 = imageCache.get(UIConstants.FOLDER_ICON);
				}
			}else{
				if(PythonPathHelper.isValidSourceFile(zipPath)){
					this.getData().o2 = imageCache.get(UIConstants.PY_FILE_ICON);
				}else{
					this.getData().o2 = imageCache.get(UIConstants.FILE_ICON);
				}
			}
		}
	}
	
	/**
	 * @return the label for the passed zip path.
	 * 
	 * E.g.: 
	 * For /dir/foo/file.py, this will return 'file.py'
	 * For /dir/foo/dir2/, this will return 'dir2'
	 */
	private static String getLabel(String zipPath) {
		if(StringUtils.endsWith(zipPath, '/')){
			zipPath = zipPath.substring(0, zipPath.length()-1); //remove last char
		}
		
		int lastIndexOf = zipPath.lastIndexOf('/');
		if(lastIndexOf == -1){
			return zipPath;
		}else{
			return zipPath.substring(lastIndexOf+1);
		}
	}

	public boolean hasChildren() {
		return isDir && dirContents != null && dirContents.size() > 0;
	}

	public int getRank() {
		return isDir?ISortedElement.RANK_PYTHON_FOLDER:ISortedElement.RANK_PYTHON_FILE;
	}
	
	
	public synchronized List<TreeNode<LabelAndImage>> getChildren() {
		if(!calculated){
			this.calculated = true;
			if(isDir && dirContents != null){
				for(String childPath:dirContents){
					new PythonpathZipChildTreeNode(this, zipStructure, childPath, null, false);
				}
			}
		}
		return super.getChildren();
	}


}
