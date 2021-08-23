package eu.h2020.helios_social.modules.groupcommunications.context;

import org.jetbrains.annotations.NotNull;

import eu.h2020.helios_social.core.context.Context;
import eu.h2020.helios_social.core.context.ext.LocationContext;
import eu.h2020.helios_social.core.context.ext.TimeContext;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.GeneralContextProxy;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.LocationContextProxy;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.TimeContextProxy;

public interface ContextFactory {

	GeneralContextProxy createContext(String name,
			int hexColor, @NotNull String privateName);

	LocationContextProxy createLocationContext(String name,
			int color, double lat, double lng, int radius, @NotNull String privateName);

	TimeContextProxy createTimeContext(@NotNull String name,
			int hexColor, long startTime,
			long endTime);
}
