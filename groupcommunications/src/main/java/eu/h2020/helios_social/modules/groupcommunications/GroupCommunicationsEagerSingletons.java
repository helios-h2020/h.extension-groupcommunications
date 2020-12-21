package eu.h2020.helios_social.modules.groupcommunications;

import eu.h2020.helios_social.modules.groupcommunications.context.ContextModule;

public interface GroupCommunicationsEagerSingletons {

	void inject(GroupCommunicationsModule.EagerSingletons init);

	void inject(ContextModule.EagerSingletons init);

	class Helper {

		public static void injectEagerSingletons(
				GroupCommunicationsEagerSingletons c) {
			c.inject(new ContextModule.EagerSingletons());
			c.inject(new GroupCommunicationsModule.EagerSingletons());
		}
	}
}
