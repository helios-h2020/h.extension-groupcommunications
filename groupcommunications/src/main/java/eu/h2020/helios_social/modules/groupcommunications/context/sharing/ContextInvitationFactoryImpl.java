package eu.h2020.helios_social.modules.groupcommunications.context.sharing;

import javax.inject.Inject;

import eu.h2020.helios_social.core.context.Context;
import eu.h2020.helios_social.happ.helios.talk.api.system.Clock;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.context.ContextType;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInvitationFactory;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.GeneralContextProxy;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.LocationContextProxy;

public class ContextInvitationFactoryImpl implements ContextInvitationFactory<Context> {

	private final Clock clock;

	@Inject
	public ContextInvitationFactoryImpl(Clock clock) {
		this.clock = clock;
	}

	@Override
	public ContextInvitation createOutgoingContextInvitation(ContactId contactId,
			Context context) {
		if (context instanceof GeneralContextProxy) {
			GeneralContextProxy contextProxy = (GeneralContextProxy) context;
			return new ContextInvitation(contactId, context.getId(),
					context.getName(), ContextType.GENERAL,
					contextProxy.toJson(), clock.currentTimeMillis(), false);
		} else if (context instanceof LocationContextProxy) {
			LocationContextProxy contextProxy = (LocationContextProxy) context;
			return new ContextInvitation(contactId, context.getId(),
					context.getName(), ContextType.LOCATION,
					contextProxy.toJson(), clock.currentTimeMillis(), false);
		} else {
			//TODO: TEMPORAL & SPATIOTEMPORAL CONTEXTS
			return null;
		}

	}

	@Override
	public ContextInvitation createIncomingContextInvitation(ContactId contactId,
			ContextInfo contextInfo) {
		return new ContextInvitation(contactId, contextInfo.getContextId(),
				contextInfo.getName(), contextInfo.getContextType(),
				contextInfo.jsonContext(), contextInfo.getTimestamp(), true);
	}
}
