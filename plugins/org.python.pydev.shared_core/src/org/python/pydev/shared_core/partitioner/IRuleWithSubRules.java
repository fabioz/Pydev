package org.python.pydev.shared_core.partitioner;

import org.eclipse.jface.text.rules.IPredicateRule;

public interface IRuleWithSubRules {

    IPredicateRule[] getSubRules();

    public SubRuleToken[] evaluateSubRules(IFullScanner scanner);

}
