package eu.h2020.helios_social.modules.groupcommunications.group.sharing;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.happ.helios.talk.api.event.Event;
import eu.h2020.helios_social.happ.helios.talk.api.event.EventBus;
import eu.h2020.helios_social.happ.helios.talk.api.event.EventListener;
import eu.h2020.helios_social.happ.helios.talk.api.identity.Identity;
import eu.h2020.helios_social.happ.helios.talk.api.identity.IdentityManager;
import eu.h2020.helios_social.happ.helios.talk.api.lifecycle.IoExecutor;
import eu.h2020.helios_social.happ.helios.talk.api.sync.event.JoinGroupEvent;
import eu.h2020.helios_social.happ.helios.talk.api.sync.event.LeaveGroupEvent;
import eu.h2020.helios_social.happ.helios.talk.api.system.Clock;
import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.membership.MembershipInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupManager;
import eu.h2020.helios_social.modules.groupcommunications.api.group.sharing.GroupInvitationType;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.ResponseInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.group.sharing.SharingGroupManager;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.FORUM_MEMBERSHIP_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.GROUP_INVITE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.GROUP_INVITE_RESPONSE_PROTOCOL;

public class SharingGroupManagerImpl implements SharingGroupManager,
		EventListener {

	private final GroupManager groupManager;
	private final CommunicationManager communicationManager;
	private final IdentityManager identityManager;
	private final DatabaseComponent db;
	private final Executor ioExecutor;
	private final EventBus eventBus;
	private final Clock clock;

	@Inject
	public SharingGroupManagerImpl(DatabaseComponent db,
			@IoExecutor Executor ioExecutor,
			GroupManager groupManager, IdentityManager identityManager,
			CommunicationManager communicationManager, EventBus eventBus,
			Clock clock) {
		this.db = db;
		this.ioExecutor = ioExecutor;
		this.groupManager = groupManager;
		this.identityManager = identityManager;
		this.clock = clock;
		this.communicationManager = communicationManager;
		this.eventBus = eventBus;
		this.eventBus.addListener(this);
	}

	@Override
	public void sendGroupInvitation(GroupInvitation groupInvitation)
			throws DbException {
		groupManager.addGroupInvitation(groupInvitation);
		GroupInfo groupInfo = new GroupInfo(
				groupInvitation.getContextId(),
				groupInvitation.getGroupId(),
				groupInvitation.getName(),
				groupInvitation.getGroupInvitationType(),
				groupInvitation.getJson(),
				groupInvitation.getTimestamp()
		);
		try {
			communicationManager.sendDirectMessage(GROUP_INVITE_PROTOCOL,
					groupInvitation.getContactId(), groupInfo);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void acceptGroupInvitation(GroupInvitation groupInvitation)
			throws DbException, FormatException {
		Transaction txn = db.startTransaction(false);
		try {
			if (groupInvitation.getGroupInvitationType()
					.equals(GroupInvitationType.PrivateGroup)) {
				PrivateGroup privateGroup = new Gson()
						.fromJson(groupInvitation.getJson(),
								PrivateGroup.class);
				groupManager.addGroup(txn, privateGroup);

				try {
					communicationManager
							.sendDirectMessage(GROUP_INVITE_RESPONSE_PROTOCOL,
									groupInvitation.getContactId(),
									new ResponseInfo(
											ResponseInfo.Response.ACCEPT,
											groupInvitation.getGroupId(),
											groupInvitation
													.getGroupInvitationType()));
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				} catch (TimeoutException e) {
					e.printStackTrace();
				}
				db.removeGroupInvitation(txn, groupInvitation.getContactId(),
						groupInvitation.getGroupId());
				communicationManager
						.subscribe(privateGroup.getId(),
								privateGroup.getPassword());
			} else if (groupInvitation.getGroupInvitationType()
					.equals(GroupInvitationType.Forum)) {
				Forum forum = new Gson()
						.fromJson(groupInvitation.getJson(),
								Forum.class);
				groupManager.addGroup(txn, forum);

				try {
					communicationManager
							.sendDirectMessage(GROUP_INVITE_RESPONSE_PROTOCOL,
									groupInvitation.getContactId(),
									new ResponseInfo(
											ResponseInfo.Response.ACCEPT,
											groupInvitation.getGroupId(),
											groupInvitation
													.getGroupInvitationType()));

				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				} catch (TimeoutException e) {
					e.printStackTrace();
				}
				db.removeGroupInvitation(txn, groupInvitation.getContactId(),
						groupInvitation.getGroupId());
				communicationManager.subscribe(
						forum.getId(),
						forum.getPassword());
				notifyModeratorsForJoiningForum(txn, forum);
			}
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void rejectGroupInvitation(GroupInvitation groupInvitation)
			throws DbException {
		try {
			communicationManager
					.sendDirectMessage(GROUP_INVITE_RESPONSE_PROTOCOL,
							groupInvitation.getContactId(), new ResponseInfo(
									ResponseInfo.Response.REJECT,
									groupInvitation.getGroupId(),
									groupInvitation.getGroupInvitationType()));
			groupManager.removeGroupInvitation(groupInvitation.getContactId(),
					groupInvitation.getGroupId());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void eventOccurred(Event e) {
		if (e instanceof JoinGroupEvent) {
			communicationManager
					.subscribe(((JoinGroupEvent) e).getGroupId(),
							((JoinGroupEvent) e).getPassword());
		} else if (e instanceof LeaveGroupEvent) {
			try {
				if (((LeaveGroupEvent) e).getGroupType()
						.equals(GroupType.PrivateGroup)) {
					PrivateGroup group = (PrivateGroup) groupManager
							.getGroup(((LeaveGroupEvent) e).getGroupId(),
									((LeaveGroupEvent) e).getGroupType());
					communicationManager
							.unsubscribe(group.getId(),
									group.getPassword());
				}
			} catch (FormatException ex) {
				ex.printStackTrace();
			} catch (DbException ex) {
				ex.printStackTrace();
			}
		}
	}

	private void notifyModeratorsForJoiningForum(Transaction txn, Forum forum)
			throws DbException, FormatException {
		Identity identity = identityManager.getIdentity();
		String fakeId = (String) groupManager.getFakeIdentity(
				txn,
				forum.getId()).getSecond();
		MembershipInfo membershipInfo =
				new MembershipInfo(
						forum.getId(),
						new PeerId(identity.getNetworkId(), fakeId),
						forum.getDefaultMemberRole(),
						identity.getAlias(),
						clock.currentTimeMillis());
		membershipInfo.setAction(MembershipInfo.Action.JOIN_FORUM);
		for (String moderator : forum.getModerators()) {
			notify(new PeerId(moderator), membershipInfo);
		}
	}

	private void notify(PeerId peerId, MembershipInfo membershipInfo) {
		ioExecutor.execute(() -> {
			try {
				communicationManager.sendDirectMessage(
						FORUM_MEMBERSHIP_PROTOCOL,
						peerId,
						membershipInfo);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
		});
	}
}
