/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Dec 9, 2006
 * @author Fabio
 */
package com.python.pydev.refactoring.refactorer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;

/**
 * Refactorer used to find the references given some refactoring request.
 *
 * @author Fabio
 */
public class RefactorerFindReferences {

    /**
     * If this field is not null, the return will be forced without actually doing
     * a search in files.
     *
     * This is intended to help in testing features that depend on the search.
     */
    public static ArrayList<Tuple<List<ModulesKey>, IPythonNature>> FORCED_RETURN;

    /**
     * Find the references that may have the text we're looking for.
     *
     * @param request the request with the info for the find
     * @return an array of IFile with the files that may have the references we're
     * interested about (note that those may not actually contain the matches we're
     * interested in -- it is just a helper to refine our search).
     */
    public List<Tuple<List<ModulesKey>, IPythonNature>> findPossibleReferences(RefactoringRequest request)
            throws OperationCanceledException {
        String initialName = request.initialName;
        List<Tuple<List<ModulesKey>, IPythonNature>> ret = request.getPossibleReferences(initialName);
        if (ret != null) {
            return ret;
        }

        if (FORCED_RETURN != null) {
            ret = new ArrayList<Tuple<List<ModulesKey>, IPythonNature>>();

            for (Tuple<List<ModulesKey>, IPythonNature> f : FORCED_RETURN) {
                //only for testing purposes
                for (ModulesKey k : f.o1) {
                    String object = FileUtils.getFileContents(k.file);
                    if (object.indexOf(request.initialName) != -1) {
                        ret.add(new Tuple<List<ModulesKey>, IPythonNature>(Arrays.asList(k), f.o2));
                    }
                }
            }
            return ret;
        }

        ret = new ArrayList<Tuple<List<ModulesKey>, IPythonNature>>();

        try {

            try {
                IProject project = request.nature.getProject();
                List<Tuple<AbstractAdditionalTokensInfo, IPythonNature>> infoAndNature = null;
                if (project == null) {
                    if (request.nature instanceof SystemPythonNature) {
                        SystemPythonNature systemPythonNature = (SystemPythonNature) request.nature;
                        int interpreterType = systemPythonNature.getInterpreterType();
                        List<IPythonNature> naturesRelatedTo = PythonNature.getPythonNaturesRelatedTo(interpreterType);
                        infoAndNature = new ArrayList<Tuple<AbstractAdditionalTokensInfo, IPythonNature>>();

                        for (IPythonNature iPythonNature : naturesRelatedTo) {
                            if (iPythonNature.getProject() != null && iPythonNature.getProject().isAccessible()) {
                                AbstractAdditionalTokensInfo o1 = AdditionalProjectInterpreterInfo
                                        .getAdditionalInfoForProject(iPythonNature);
                                if (o1 != null) {
                                    infoAndNature
                                            .add(new Tuple<AbstractAdditionalTokensInfo, IPythonNature>(o1,
                                                    iPythonNature));
                                }
                            }
                        }
                    }
                } else {
                    infoAndNature = AdditionalProjectInterpreterInfo
                            .getAdditionalInfoAndNature(request.nature, false, true, true);
                }

                if (infoAndNature == null || infoAndNature.size() == 0) {
                    return ret;
                }

                //long initial = System.currentTimeMillis();
                request.getMonitor().beginTask("Find possible references", infoAndNature.size());
                request.getMonitor().setTaskName("Find possible references");
                try {
                    for (Tuple<AbstractAdditionalTokensInfo, IPythonNature> tuple : infoAndNature) {
                        try {
                            SubProgressMonitor sub = new SubProgressMonitor(request.getMonitor(), 1);
                            request.pushMonitor(sub);
                            if (tuple.o1 != null && tuple.o2 != null) {
                                List<ModulesKey> modulesWithToken = tuple.o1.getModulesWithToken(
                                        request.nature.getProject(), initialName, sub);

                                if (sub.isCanceled()) {
                                    break;
                                }
                                ret.add(new Tuple<List<ModulesKey>, IPythonNature>(modulesWithToken, tuple.o2));
                            }
                        } finally {
                            request.popMonitor().done();
                        }
                    }
                } finally {
                    request.getMonitor().done();
                }
                //System.out.println("Total: " + ((System.currentTimeMillis() - initial) / 1000.));
            } catch (MisconfigurationException e) {
                throw new RuntimeException(e);
            }

        } catch (OperationCanceledException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        request.setPossibleReferences(initialName, ret);
        return ret;
    }

}
