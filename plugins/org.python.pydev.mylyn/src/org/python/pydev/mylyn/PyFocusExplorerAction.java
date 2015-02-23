package org.python.pydev.mylyn;

import java.util.ArrayList;
import java.util.List;

import  org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.mylyn.context.ui.InterestFilter;
import org.eclipse.mylyn.resources.ui.FocusCommonNavigatorAction;
import  org.eclipse.ui.IViewPart;
import  org.eclipse.ui.navigator.CommonNavigator;

public class PyFocusExplorerAction extends FocusCommonNavigatorAction {

  public PyFocusExplorerAction() {
      super(new InterestFilter(), true, true, true);
  }

  protected PyFocusExplorerAction(InterestFilter filter) {
      super(filter, true, true, true);
  }

  @Override
  public List<StructuredViewer> getViewers() {
      List<StructuredViewer> viewers = new ArrayList<StructuredViewer>();

      IViewPart view = super.getPartForAction();
      if (view instanceof CommonNavigator) {
          CommonNavigator navigator = (CommonNavigator) view;
          viewers.add(navigator.getCommonViewer());
      }
      return viewers;
  }

}