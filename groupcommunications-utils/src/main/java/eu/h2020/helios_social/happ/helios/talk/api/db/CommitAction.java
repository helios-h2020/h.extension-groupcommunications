package eu.h2020.helios_social.happ.helios.talk.api.db;

import eu.h2020.helios_social.happ.helios.talk.api.event.EventExecutor;

/**
 * An action that's taken when a {@link Transaction} is committed.
 */
public interface CommitAction {

	void accept(Visitor visitor);

	interface Visitor {

		@EventExecutor
		void visit(EventAction a);

		@EventExecutor
		void visit(TaskAction a);
	}
}
