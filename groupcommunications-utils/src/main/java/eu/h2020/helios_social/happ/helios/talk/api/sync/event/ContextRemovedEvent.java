package eu.h2020.helios_social.happ.helios.talk.api.sync.event;

import javax.annotation.Nullable;

import eu.h2020.helios_social.happ.helios.talk.api.event.Event;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;
import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 * An event that is broadcast when a context is removed from the database.
 */
@Immutable
@NotNullByDefault
public class ContextRemovedEvent extends Event {
	@Nullable
	private final String contextId;

	public ContextRemovedEvent(String contextId) {
		this.contextId = contextId;
	}

	public String getContextId() {
		return contextId;
	}
}
