package eu.h2020.helios_social.modules.groupcommunications;

import android.app.Application;
import android.content.Context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.core.messaging.HeliosConnectionInfo;
import eu.h2020.helios_social.core.messaging.HeliosIdentityInfo;
import eu.h2020.helios_social.core.messaging.HeliosMessage;
import eu.h2020.helios_social.core.messaging.HeliosTopic;
import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosMessagingNodejsLibp2p;
import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosMessagingReceiver;
import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosNetworkAddress;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.happ.helios.talk.api.identity.Identity;
import eu.h2020.helios_social.happ.helios.talk.api.identity.IdentityManager;
import eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupManager;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;
import eu.h2020.helios_social.happ.helios.talk.api.lifecycle.Service;
import eu.h2020.helios_social.happ.helios.talk.api.lifecycle.ServiceException;
import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroupManager;
import eu.h2020.helios_social.modules.groupcommunications.privategroup.GroupMessageListener;
import kotlin.Unit;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.APP_TAG;
import static java.util.logging.Logger.getLogger;

public class CommunicationManagerImpl
		implements CommunicationManager<HeliosMessagingReceiver>, Service,
		LifecycleManager.OpenDatabaseHook {

	private static final Logger LOG =
			getLogger(CommunicationManagerImpl.class.getName());

	private HeliosMessagingNodejsLibp2p heliosMessaging;
	private Context appContext;
	private HeliosConnectionInfo connectionInfo;
	private HeliosIdentityInfo identityInfo;
	private IdentityManager identityManager;
	private GroupManager groupManager;
	private GroupMessageListener privateGroupMessageListener;
	private ArrayList<HeliosTopic> groups;
	private HashMap<String, HeliosMessagingReceiver> receivers;

	@Inject
	public CommunicationManagerImpl(Application app,
			IdentityManager identityManager,
			GroupManager groupManager,
			GroupMessageListener privateGroupMessageListener) {
		this.appContext = app.getApplicationContext();
		this.identityManager = identityManager;
		this.connectionInfo = getConnectionInfo();
		this.heliosMessaging = HeliosMessagingNodejsLibp2p.getInstance();
		this.privateGroupMessageListener = privateGroupMessageListener;
		this.groupManager = groupManager;
		this.groups = new ArrayList();
		this.receivers = new HashMap();
	}

	@Override
	public void startService() throws ServiceException {
		heliosMessaging.setContext(appContext);
		heliosMessaging.connect(connectionInfo, identityInfo);

		if (identityManager.getIdentity().getNetworkId() == null) {
			try {
				identityManager.setNetworkId(heliosMessaging.getPeerId());
			} catch (DbException e) {
				e.printStackTrace();
			}
		}

		for (Map.Entry<String, HeliosMessagingReceiver> receiver : receivers
				.entrySet()) {
			LOG.info(receiver.getValue().getClass().getSimpleName() +
					" has been registered to Communication Manager");
			heliosMessaging.getDirectMessaging()
					.addReceiver(receiver.getKey(), receiver.getValue());
		}

		for (HeliosTopic groupTopic : groups) {
			LOG.info("Subscribed in: " + groupTopic.getTopicName() +
					" topic.");
			heliosMessaging.subscribe(groupTopic, privateGroupMessageListener);
		}

		heliosMessaging.announceTag(APP_TAG);
		heliosMessaging.observeTag(APP_TAG);
	}

	@Override
	public void stopService() throws ServiceException {
		heliosMessaging.stop();
	}

	public void addDirectMessageReceiver(String protocolId,
			HeliosMessagingReceiver messagingReceiver) {
		heliosMessaging.getDirectMessaging()
				.addReceiver(protocolId, messagingReceiver);
	}

	@Override
	public void sendDirectMessage(String protocolId, ContactId contactId,
			AbstractMessage message)
			throws InterruptedException, ExecutionException, TimeoutException {
		HeliosNetworkAddress heliosNetworkAddress = new HeliosNetworkAddress();
		heliosNetworkAddress.setNetworkId(contactId.getId());
		LOG.info(
				"send direct message!: " + heliosNetworkAddress.getNetworkId());
		Future<Unit> f = heliosMessaging.getDirectMessaging().sendToFuture(
				heliosNetworkAddress,
				protocolId,
				message.toJson().getBytes()
		);
		f.get(10000, TimeUnit.MILLISECONDS);
	}

	@Override
	public void sendDirectMessage(String protocolId, PeerId peerId,
			AbstractMessage message)
			throws InterruptedException, ExecutionException, TimeoutException {
		HeliosNetworkAddress heliosNetworkAddress = new HeliosNetworkAddress();
		heliosNetworkAddress.setNetworkId(peerId.getId());
		LOG.info(
				"send direct message to peer!: " +
						heliosNetworkAddress.getNetworkId());
		Future<Unit> f = heliosMessaging.getDirectMessaging().sendToFuture(
				heliosNetworkAddress,
				protocolId,
				message.toJson().getBytes()
		);
		f.get(10000, TimeUnit.MILLISECONDS);
	}

	@Override
	public void sendGroupMessage(String groupId, String password,
			AbstractMessage message) {
		heliosMessaging.publish(
				new HeliosTopic(groupId, password),
				new HeliosMessage(message.toJson())
		);
	}

	@Override
	public void registerReceiver(String protocolId,
			HeliosMessagingReceiver receiver) {
		receivers.put(protocolId, receiver);
	}

	@Override
	public void announceTag(String tag) {
		heliosMessaging.announceTag(tag);
	}

	@Override
	public void observeTag(String tag) {
		heliosMessaging.observeTag(tag);
	}

	@Override
	public void unannounceTag(String tag) {
		heliosMessaging.unannounceTag(tag);
	}

	@Override
	public void unobserveTag(String tag) {
		heliosMessaging.unobserveTag(tag);
	}

	@Override
	public void subscribe(String groupId, String password) {
		heliosMessaging.subscribe(new HeliosTopic(groupId, password),
				privateGroupMessageListener);
	}

	@Override
	public void unsubscribe(String groupId, String password) {
		heliosMessaging.unsubscribe(new HeliosTopic(groupId, password));
	}

	private HeliosConnectionInfo getConnectionInfo() {
		return new HeliosConnectionInfo();
	}

	@Override
	public void onDatabaseOpened(Transaction txn) throws DbException {
		Identity i = identityManager.getIdentity();
		this.identityInfo = new HeliosIdentityInfo(i.getAlias(), i.getId());

		try {
			Collection<Group> groupCollection = groupManager.getGroups(txn);
			for (Group group : groupCollection) {
				if (group instanceof PrivateGroup) {
					PrivateGroup pg = (PrivateGroup) group;
					groups.add(new HeliosTopic(group.getId(),
							pg.getPassword()));
				} else {
					Forum f = (Forum) group;
					groups.add(new HeliosTopic(group.getId(),
							f.getPassword()));
				}
			}
		} catch (FormatException e) {
			e.printStackTrace();
		}
	}
}
