package org.python.pydev.shared_core.partitioner;


public interface IRuleWithSubRules {

    public SubRuleToken[] evaluateSubRules(IFullScanner scanner);

}
