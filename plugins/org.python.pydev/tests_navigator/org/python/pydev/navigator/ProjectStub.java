package org.python.pydev.navigator;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;

public class ProjectStub implements IProject, IWorkbenchAdapter{

    private File projectRoot;

    private Map<File, IResource> cache = new HashMap<File, IResource>();

    private IPythonNature nature;

    private IContainer parent;

    private boolean addNullChild;

    private List<Object> additionalChildren;
    
    public ProjectStub(File file, IPythonNature nature) {
        this(file, nature, false);
    }
    
    public ProjectStub(File file, IPythonNature nature, boolean addNullChild) {
        this(file, nature, addNullChild, new ArrayList<Object>());
    }
    
    public ProjectStub(File file, IPythonNature nature, boolean addNullChild, List<Object> additionalChildren) {
        Assert.isTrue(file.exists() && file.isDirectory());
        this.projectRoot = file;
        this.nature = nature;
        this.addNullChild = addNullChild;
        this.additionalChildren = additionalChildren;
    }
    
    public IResource getResource(File parentFile) {
        if(parentFile.equals(projectRoot)){
            return this;
        }
        
        IResource r = cache.get(parentFile);
        if(r == null){
            if(parentFile.isFile()){
                r = new FileStub(this, parentFile);
            }else{
                r = new FolderStub(this, parentFile);
            }
            cache.put(parentFile, r);
        }
        return r;
    }
    
    public IContainer getFolder(File parentFile) {
        return (IContainer) getResource(parentFile);
    }
    
    public void setParent(IContainer parent) {
        this.parent = parent;
    }
    public IContainer getParent() {
        return this.parent;
    }

    @Override
    public String toString() {
        return "ProjectStub:"+this.projectRoot;
    }

    

    public void build(int kind, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void build(int kind, String builderName, Map args, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void close(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void create(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void create(IProjectDescription description, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void delete(boolean deleteContent, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IContentTypeMatcher getContentTypeMatcher() throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IProjectDescription getDescription() throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IFile getFile(String name) {
        throw new RuntimeException("Not impl");
        
    }

    public IFolder getFolder(String name) {
        throw new RuntimeException("Not impl");
        
    }

    public IProjectNature getNature(String natureId) throws CoreException {
        if(nature == null){
            throw new RuntimeException("not expected");
        }
        return nature;
    }

    public IPath getPluginWorkingLocation(IPluginDescriptor plugin) {
        throw new RuntimeException("Not impl");
        
    }

    public IProject[] getReferencedProjects() throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IProject[] getReferencingProjects() {
        throw new RuntimeException("Not impl");
        
    }

    public IPath getWorkingLocation(String id) {
        throw new RuntimeException("Not impl");
        
    }

    public boolean hasNature(String natureId) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isNatureEnabled(String natureId) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isOpen() {
        return true;
        
    }

    public void move(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void open(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void open(int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setDescription(IProjectDescription description, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setDescription(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public boolean exists(IPath path) {
        throw new RuntimeException("Not impl");
        
    }

    public IFile[] findDeletedMembersWithHistory(int depth, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IResource findMember(String name) {
        throw new RuntimeException("Not impl");
        
    }

    public IResource findMember(IPath path) {
        throw new RuntimeException("Not impl");
        
    }

    public IResource findMember(String name, boolean includePhantoms) {
        throw new RuntimeException("Not impl");
        
    }

    public IResource findMember(IPath path, boolean includePhantoms) {
        throw new RuntimeException("Not impl");
        
    }

    public String getDefaultCharset() throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public String getDefaultCharset(boolean checkImplicit) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IFile getFile(IPath path) {
        throw new RuntimeException("Not impl");
        
    }

    public IFolder getFolder(IPath path) {
        throw new RuntimeException("Not impl");
        
    }

    public IResource[] members() throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IResource[] members(boolean includePhantoms) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IResource[] members(int memberFlags) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setDefaultCharset(String charset) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setDefaultCharset(String charset, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void accept(IResourceVisitor visitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void clearHistory(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void copy(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void copy(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IMarker createMarker(String type) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IResourceProxy createProxy() {
        throw new RuntimeException("Not impl");
        
    }

    public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public boolean exists() {
        throw new RuntimeException("Not impl");
        
    }

    public IMarker findMarker(long id) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public String getFileExtension() {
        throw new RuntimeException("Not impl");
        
    }

    public IPath getFullPath() {
        return Path.fromOSString(REF.getFileAbsolutePath(this.projectRoot));
    }

    public long getLocalTimeStamp() {
        throw new RuntimeException("Not impl");
        
    }

    public IPath getLocation() {
        return Path.fromOSString(REF.getFileAbsolutePath(this.projectRoot));
    }

    public URI getLocationURI() {
        throw new RuntimeException("Not impl");
        
    }

    public IMarker getMarker(long id) {
        throw new RuntimeException("Not impl");
        
    }

    public long getModificationStamp() {
        throw new RuntimeException("Not impl");
        
    }

    public String getName() {
        throw new RuntimeException("Not impl");
        
    }


    public String getPersistentProperty(QualifiedName key) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IProject getProject() {
        return this;
        
    }

    public IPath getProjectRelativePath() {
        throw new RuntimeException("Not impl");
        
    }

    public IPath getRawLocation() {
        throw new RuntimeException("Not impl");
        
    }

    public URI getRawLocationURI() {
        throw new RuntimeException("Not impl");
        
    }

    public ResourceAttributes getResourceAttributes() {
        throw new RuntimeException("Not impl");
        
    }

    public Object getSessionProperty(QualifiedName key) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public int getType() {
        throw new RuntimeException("Not impl");
        
    }

    public IWorkspace getWorkspace() {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isAccessible() {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isDerived() {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isLinked() {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isLinked(int options) {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isLocal(int depth) {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isPhantom() {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isReadOnly() {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isSynchronized(int depth) {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isTeamPrivateMember() {
        throw new RuntimeException("Not impl");
        
    }

    public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void move(IProjectDescription description, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void revertModificationStamp(long value) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setDerived(boolean isDerived) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public long setLocalTimeStamp(long value) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setReadOnly(boolean readOnly) {
        throw new RuntimeException("Not impl");
        
    }

    public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void touch(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public Object getAdapter(Class adapter) {
        if (adapter == IWorkbenchAdapter.class){
            return this;
        }
        throw new RuntimeException("Not impl");
        
    }

    public boolean contains(ISchedulingRule rule) {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isConflicting(ISchedulingRule rule) {
        throw new RuntimeException("Not impl");
        
    }

    public int findMaxProblemSeverity(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    
    private HashMap<Object, Object[]> stubsCache = new HashMap<Object, Object[]>();
    
    //workbench adapter
    public Object[] getChildren(Object o) {
        Object[] found = stubsCache.get(o);
        if(found != null){
            return found;
        }
        
        File folder = null; 
        if(o instanceof ProjectStub){
            ProjectStub projectStub = (ProjectStub) o;
            folder = projectStub.projectRoot;
        }else{
            throw new RuntimeException("Shouldn't happen");
        }
        ArrayList<Object> ret = new ArrayList<Object>();
        for(File file:folder.listFiles()){
            String lower = file.getName().toLowerCase();
            if(lower.equals("cvs") || lower.equals(".svn")){
                continue;
            }
            if(file.isDirectory()){
                ret.add(new FolderStub(this, file));
            }else{
                ret.add(new FileStub(this, file));
            }
        }
        if(addNullChild){
            ret.add(null);
        }
        ret.addAll(this.additionalChildren);
        return ret.toArray();
    }

    public ImageDescriptor getImageDescriptor(Object object) {
        throw new RuntimeException("Not implemented");
    }

    public String getLabel(Object o) {
        throw new RuntimeException("Not implemented");
    }

    public Object getParent(Object o) {
        throw new RuntimeException("Not implemented");
    }

    public void create(IProjectDescription description, int updateFlags,
            IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        
    }

    public Map getPersistentProperties() throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    public Map getSessionProperties() throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isDerived(int options) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isHidden() {
        // TODO Auto-generated method stub
        return false;
    }

    public void setHidden(boolean isHidden) throws CoreException {
        // TODO Auto-generated method stub
        
    }

    public boolean isHidden(int options) {
        throw new RuntimeException("Not implemented");
    }

    public boolean isTeamPrivateMember(int options) {
        throw new RuntimeException("Not implemented");
    }


}
