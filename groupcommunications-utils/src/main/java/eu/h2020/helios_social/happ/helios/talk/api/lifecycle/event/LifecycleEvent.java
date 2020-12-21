package eu.h2020.helios_social.happ.helios.talk.api.lifecycle.event;

import eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager.LifecycleState;

import eu.h2020.helios_social.happ.helios.talk.api.event.Event;

/**
 * An event that is broadcast when the app enters a new lifecycle state.
 */
public class LifecycleEvent extends Event {

	private final LifecycleState state;

	public LifecycleEvent(LifecycleState state) {
		this.state = state;
	}

	public LifecycleState getLifecycleState() {
		return state;
	}
}
