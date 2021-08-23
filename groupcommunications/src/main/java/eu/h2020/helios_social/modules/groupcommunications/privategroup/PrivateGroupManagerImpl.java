package eu.h2020.helios_social.modules.groupcommunications.privategroup;

import android.util.Log;

import com.github.javafaker.Faker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupMember;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroupMemberListInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroupNewMemberInfo;
import eu.h2020.helios_social.modules.groupcommunications_utils.Bytes;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfDictionary;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfEntry;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfList;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.Encoder;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.Parser;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Metadata;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.Contact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageTracker;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroupManager;
import eu.h2020.helios_social.modules.groupcommunications_utils.identity.IdentityManager;
import eu.h2020.helios_social.modules.groupcommunications_utils.lifecycle.IoExecutor;

import static eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionConstants.CONNECTIONS_RECEIVER_ID;
import static eu.h2020.helios_social.modules.groupcommunications.api.group.GroupConstants.GROUP_MEMBERS;
import static eu.h2020.helios_social.modules.groupcommunications.api.group.GroupConstants.GROUP_SHOW_TRUE_SELF;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FAKE_ID;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FUNNY_NAME;

public class PrivateGroupManagerImpl
        implements PrivateGroupManager<Transaction> {

    private final DatabaseComponent db;
    private final ContactManager contactManager;
    private final MessageTracker messageTracker;
    private final Encoder encoder;
    private final Parser parser;


    @Inject
    public PrivateGroupManagerImpl(DatabaseComponent db,
                                   ContactManager contactManager,
                                   MessageTracker messageTracker, Encoder encoder,
                                   Parser parser) {
        this.db = db;
        this.contactManager = contactManager;
        this.messageTracker = messageTracker;
        this.encoder = encoder;
        this.parser = parser;
    }

    @Override
    public void addPrivateGroup(PrivateGroup privateGroup)
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(false);
        try {
            addPrivateGroup(txn, privateGroup);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public void leavePrivateGroup(PrivateGroup privateGroup) {

    }

    @Override
    public PrivateGroup getPrivateGroup(String groupId)
            throws FormatException, DbException {
        Transaction txn = db.startTransaction(true);
        PrivateGroup privateGroup;
        try {
            privateGroup = getPrivateGroup(txn, groupId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return privateGroup;
    }

    @Override
    public Collection<PrivateGroup> getPrivateGroups(String contextId)
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(true);
        Collection<PrivateGroup> privateGroups = new ArrayList<>();
        try {
            Collection<Group> groups =
                    db.getGroups(txn, contextId, GroupType.PrivateGroup);
            for (Group g : groups) {
                privateGroups.add(parseToPrivateGroup(g));
            }
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }

        return privateGroups;
    }

    @Override
    public Collection<PrivateGroup> getPrivateGroups()
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(true);
        Collection<PrivateGroup> privateGroups = new ArrayList<>();
        try {
            Collection<Group> groups =
                    db.getGroups(txn, GroupType.PrivateGroup);
            for (Group g : groups) {
                privateGroups.add(parseToPrivateGroup(g));
            }
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }

        return privateGroups;
    }

    @Override
    public Collection<PrivateGroup> getPrivateGroups(Transaction txn)
            throws DbException, FormatException {
        Collection<PrivateGroup> privateGroups = new ArrayList<>();
        Collection<Group> groups =
                db.getGroups(txn, GroupType.PrivateGroup);
        for (Group g : groups) {
            privateGroups.add(parseToPrivateGroup(g));
        }

        return privateGroups;
    }

    @Override
    public Collection<GroupMember> getMembers(String groupId)
            throws DbException {
        Transaction txn = db.startTransaction(true);
        Collection<GroupMember> members;
        try {
            members = getMembers(txn, groupId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return members;
    }

    @Override
    public Collection<GroupMember> getMembers(Transaction txn, String groupId)
            throws DbException {
        Collection<GroupMember> groupMembers = db.getGroupMembers(txn,groupId);

        return groupMembers;
    }

    @Override
    public void addMember(GroupMember groupMember)
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(false);
        try {
            addMember(txn, groupMember);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public void addMember(Transaction txn, GroupMember groupMember)
            throws DbException {

        db.addGroupMember(txn,groupMember);
        Log.d("addMember","true");
    }

    @Override
    public void addPrivateGroup(Transaction txn,
                                PrivateGroup privateGroup)
            throws FormatException, DbException {
        BdfList descriptor =
                BdfList.of(privateGroup.getName(), privateGroup.getPassword(),
                        privateGroup.getOwner());
        db.addGroup(txn, privateGroup, encoder.encodeToByteArray(descriptor),
                GroupType.PrivateGroup);
        BdfDictionary meta = BdfDictionary.of(
                new BdfEntry(GROUP_MEMBERS, new BdfList()),
                new BdfEntry(PEER_FUNNY_NAME,
                        Faker.instance().name().username()),
                new BdfEntry(PEER_FAKE_ID,
                        UUID.randomUUID().toString()),
                new BdfEntry(GROUP_SHOW_TRUE_SELF, true)
        );
        db.mergeGroupMetadata(txn, privateGroup.getId(),
                encoder.encodeMetadata(meta));
        messageTracker.initializeGroupCount(txn, privateGroup.getId());
    }


    private PrivateGroup getPrivateGroup(Transaction txn,
                                         String groupId)
            throws FormatException, DbException {
        Group group = db.getGroup(txn, groupId);
        return parseToPrivateGroup(group);
    }

    private PrivateGroup parseToPrivateGroup(Group group)
            throws FormatException {
        BdfList list = parser.parseToList(group.getDescriptor());

        String name = list.getString(0);
        String password = list.getString(1);
        String owner = list.getString(2);
        return new PrivateGroup(group.getId(), group.getContextId(), name,
                password, owner);
    }

}
