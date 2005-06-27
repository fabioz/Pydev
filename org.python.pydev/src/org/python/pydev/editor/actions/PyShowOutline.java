/*
 * Created on Jul 20, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;

/**
 * @author Fabio Zadrozny
 */
public class PyShowOutline extends PyAction{

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
        //TODO: Still trying to find out how to do this...
        //As I couldn't find a good way to do it, I added a bug to see
        //if the interface provided by Java can be generalized...
        //
        //The bug can be found at:
        //
        //https://bugs.eclipse.org/bugs/show_bug.cgi?id=70419
        
        
        
//        System.out.println("Show outline information...");
//        InformationPresenter informationPresenter = getInformationPresenter();
//        informationPresenter.setInformationProvider(getInformationProvider(), "Python Outline Content");
//        informationPresenter.showInformation();
//        System.out.println("Show outline information...OK");
    }

//    /**
//     * @return
//     */
//    private IInformationProvider getInformationProvider() {
//        return new IInformationProvider(){
//
//            public IRegion getSubject(ITextViewer textViewer, int offset) {
//                return new IRegion(){
//
//                    public int getLength() {
//                        return 20;
//                    }
//
//                    public int getOffset() {
//                        return 20;
//                    }};
//            }
//
//            public String getInformation(ITextViewer textViewer, IRegion subject) {
//                return "Python Outline Content";
//            }};
//    }
//
//    /**
//     * 
//     * @return
//     */
//    private InformationPresenter getInformationPresenter() {
//         return new InformationPresenter(getInformationControlCreator());
//    }
//
//    /**
//     * @return
//     */
//    private IInformationControlCreator getInformationControlCreator() {
//        return new IInformationControlCreator (){
//
//            public IInformationControl createInformationControl(Shell parent) {
//                return new PyInformationControl(parent);
//            }};
//    }
//    
//    class PyInformationControl extends DefaultInformationControl{
//
//        /**
//         * @param parent
//         */
//        public PyInformationControl(Shell parent) {
//            super(parent);
//            setInformation("Python Outline Content");
//        }
//        
//    }

}
