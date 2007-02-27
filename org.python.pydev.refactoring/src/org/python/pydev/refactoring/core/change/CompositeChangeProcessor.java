package org.python.pydev.refactoring.core.change;

import java.util.List;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;

public class CompositeChangeProcessor implements IChangeProcessor {

	private String name;

	private List<IChangeProcessor> processors;

	public CompositeChangeProcessor(String name, List<IChangeProcessor> processors) {
		this.name = name;
		this.processors = processors;
	}

	public Change createChange() {
		CompositeChange change = new CompositeChange(name);
		for (IChangeProcessor processor : processors) {
			change.add(processor.createChange());
		}

		return change;
	}

}
