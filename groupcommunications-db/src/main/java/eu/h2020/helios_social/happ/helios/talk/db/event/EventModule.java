package eu.h2020.helios_social.happ.helios.talk.db.event;

import eu.h2020.helios_social.happ.helios.talk.api.event.EventBus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class EventModule {

	@Provides
	@Singleton
	EventBus provideEventBus(EventBusImpl eventBus) {
		return eventBus;
	}
}
