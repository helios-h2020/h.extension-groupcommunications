package eu.h2020.helios_social.modules.groupcommunications.context;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.context.proxy.GeneralContextProxy;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.LocationContextProxy;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.TimeContextProxy;


public class ContextFactoryImpl implements ContextFactory {

	@Inject
	public ContextFactoryImpl() {
	}

	@Override
	public GeneralContextProxy createContext(@NotNull String name,
			@NotNull int color) {
		return new GeneralContextProxy(UUID.randomUUID().toString(), name,
				color, true);
	}

	@Override
	public LocationContextProxy createLocationContext(@NotNull String name,
			@NotNull int color, double lat, double lng, int radius) {
		return new LocationContextProxy(UUID.randomUUID().toString(), name,
				color, lat, lng, radius);
	}

	@Override
	public TimeContextProxy createTimeContext(@NotNull String name, int color,
			long startTime,
			long endTime) {
		return new TimeContextProxy(UUID.randomUUID().toString(), name, color,
				startTime, endTime);
	}

}
