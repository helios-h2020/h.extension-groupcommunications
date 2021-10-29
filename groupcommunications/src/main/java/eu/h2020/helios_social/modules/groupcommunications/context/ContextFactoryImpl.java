package eu.h2020.helios_social.modules.groupcommunications.context;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import javax.inject.Inject;

import eu.h2020.helios_social.core.context.ext.LocationContext;
import eu.h2020.helios_social.core.context.ext.TimeContext;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.GeneralContextProxy;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.LocationContextProxy;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.SpatioTemporalContext;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.TimeContextProxy;


public class ContextFactoryImpl implements ContextFactory {

	@Inject
	public ContextFactoryImpl() {
	}

	@Override
	public GeneralContextProxy createContext(String name,
			@NotNull int color, @NotNull String privateName) {
		return new GeneralContextProxy(UUID.randomUUID().toString(), name,
				color, true, privateName);
	}

	@Override
	public LocationContextProxy createLocationContext(String name,
			@NotNull int color, double lat, double lng, int radius, @NotNull  String privateName) {
		return new LocationContextProxy(UUID.randomUUID().toString(), name,
				color, lat, lng, radius, privateName);
	}

	@Override
	public TimeContextProxy createTimeContext(@NotNull String name, int color,
			long startTime,
			long endTime) {
		return new TimeContextProxy(UUID.randomUUID().toString(), name, color,
				startTime, endTime);
	}

	@Override
	public SpatioTemporalContext createSpatioTemporalContext(@NotNull String name, int color,
															 long startTime, long endTime, int repeat,
															 double lat, double lng, int radius,
															 @NotNull  String privateName) {
		LocationContext contextA = new LocationContext(name, lat, lng, radius);
		TimeContext contextB = new TimeContext(null,name, startTime, endTime, repeat);
		return new SpatioTemporalContext(UUID.randomUUID().toString(), name, color, privateName,
				contextA, contextB);
	}

}
