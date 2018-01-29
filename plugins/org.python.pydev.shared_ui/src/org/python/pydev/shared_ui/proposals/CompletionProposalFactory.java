package org.python.pydev.shared_ui.proposals;

public class CompletionProposalFactory {

    private static ICompletionProposalFactory factory;

    public static ICompletionProposalFactory get() {
        return factory;
    }

    public static void set(ICompletionProposalFactory factoryParam) {
        factory = factoryParam;
    }

}
