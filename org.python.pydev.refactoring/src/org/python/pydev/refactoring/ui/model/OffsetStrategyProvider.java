package org.python.pydev.refactoring.ui.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.offsetstrategy.IOffsetStrategy;
import org.python.pydev.refactoring.ui.UITexts;

public class OffsetStrategyProvider implements IStructuredContentProvider {

	private List<OffsetStrategyModel> strategies;

	private String initLabel;

	private String beginLabel;

	private String endLabel;

	public OffsetStrategyProvider(int strategyOption) {
		strategies = new ArrayList<OffsetStrategyModel>();
		initLabel();
		initStrategies(strategyOption);
	}

	public OffsetStrategyProvider(AbstractScopeNode<?> scopeAdapter, int strategyOption) {
		strategies = new ArrayList<OffsetStrategyModel>();
		strategyOption = updateLabel(scopeAdapter, strategyOption);
		initStrategies(strategyOption);
	}

	private void initLabel() {
		initLabel = UITexts.offsetStrategyAfterInit;
		beginLabel = UITexts.offsetStrategyBegin;
		endLabel = UITexts.offsetStrategyEnd;
	}

	private int updateLabel(AbstractScopeNode<?> scopeAdapter, int strategyOption) {
		if (scopeAdapter != null) {

			if (scopeAdapter.getNodeBodyIndent() == 0) {
				beginLabel = UITexts.offsetStrategyBeginModule;
				endLabel = UITexts.offsetStrategyEndModule;
				if ((strategyOption & IOffsetStrategy.AFTERINIT) == IOffsetStrategy.AFTERINIT) {
					strategyOption &= ~IOffsetStrategy.AFTERINIT;
				}
			} else
				initLabel();
		}
		return strategyOption;
	}

	private void initStrategies(int strategyOption) {
		setStrategy(strategyOption, IOffsetStrategy.AFTERINIT, initLabel);
		setStrategy(strategyOption, IOffsetStrategy.BEGIN, beginLabel);
		setStrategy(strategyOption, IOffsetStrategy.END, endLabel);
	}

	private void setStrategy(int strategyOption, int id, String label) {
		if ((strategyOption & id) == id) {
			strategies.add(new OffsetStrategyModel(id, label));
		}
	}

	public OffsetStrategyModel get(int i) {
		return this.strategies.get(i);
	}

	public Object[] getElements(Object inputElement) {

		return strategies.toArray();
	}

	public void dispose() {
		this.strategies = null;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

}
