package eu.h2020.helios_social.modules.groupcommunications.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.core.context.Context;
import eu.h2020.helios_social.core.context.ext.LocationContext;
import eu.h2020.helios_social.core.context.ext.TimeContext;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.SpatioTemporalContext;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfDictionary;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfEntry;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfList;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.Encoder;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.Parser;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Metadata;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.context.ContextType;
import eu.h2020.helios_social.modules.groupcommunications.api.context.DBContext;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.GeneralContextProxy;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.LocationContextProxy;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.TimeContextProxy;

import static eu.h2020.helios_social.modules.groupcommunications.context.ContextConstants.CONTEXT_END_TIME;
import static eu.h2020.helios_social.modules.groupcommunications.context.ContextConstants.CONTEXT_FORUMS;
import static eu.h2020.helios_social.modules.groupcommunications.context.ContextConstants.CONTEXT_KEY_MEMBERS;
import static eu.h2020.helios_social.modules.groupcommunications.context.ContextConstants.CONTEXT_LAT;
import static eu.h2020.helios_social.modules.groupcommunications.context.ContextConstants.CONTEXT_LNG;
import static eu.h2020.helios_social.modules.groupcommunications.context.ContextConstants.CONTEXT_PRIVATE_GROUPS;
import static eu.h2020.helios_social.modules.groupcommunications.context.ContextConstants.CONTEXT_RADIUS;
import static eu.h2020.helios_social.modules.groupcommunications.context.ContextConstants.CONTEXT_REPEAT;
import static eu.h2020.helios_social.modules.groupcommunications.context.ContextConstants.CONTEXT_START_TIME;
import static java.util.logging.Logger.getLogger;


public class ContextManagerImpl implements ContextManager<Transaction> {
    private static final Logger LOG =
            getLogger(ContextManagerImpl.class.getName());

    private final DatabaseComponent db;
    private final Encoder encoder;
    private final Parser parser;

    @Inject
    ContextManagerImpl(DatabaseComponent db, Encoder encoder,
                       Parser parser) {
        this.db = db;
        this.encoder = encoder;
        this.parser = parser;
    }

    @Override
    public void addContext(Context context) throws DbException {
        Transaction txn = db.startTransaction(false);
        try {
            if (context instanceof LocationContextProxy) {
                addContext(txn, (LocationContextProxy) context);
            } else if (context instanceof GeneralContextProxy) {
                addContext(txn, (GeneralContextProxy) context);
            } else if (context instanceof SpatioTemporalContext) {
                addContext(txn, (SpatioTemporalContext) context);
            }
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public void addPendingContext(ContextInvitation contextInvitation)
            throws DbException {
        Transaction txn = db.startTransaction(false);
        try {
            db.addContextInvitation(txn, contextInvitation);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public Collection<ContextInvitation> getPendingContextInvitations()
            throws DbException {
        Transaction txn = db.startTransaction(false);
        Collection<ContextInvitation> contextInvitations;
        try {
            contextInvitations = db.getPendingContextInvitations(txn);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return contextInvitations;
    }

    @Override
    public Collection<ContextInvitation> getPendingContextInvitations(Transaction txn)
            throws DbException {
        return db.getPendingContextInvitations(txn);
    }

    @Override
    public void removePendingContext(String pendingContextId)
            throws DbException {
        Transaction txn = db.startTransaction(false);
        try {
            db.removePendingContext(txn, pendingContextId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public void removeContextInvitation(ContactId contactId,
                                        String pendingContextId)
            throws DbException {
        Transaction txn = db.startTransaction(false);
        try {
            db.removeContextInvitation(txn, contactId, pendingContextId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public boolean contextExists(String contextId) throws DbException {
        Transaction txn = db.startTransaction(true);
        boolean exists;
        try {
            exists = db.containsContext(txn, contextId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return exists;
    }

    @Override
    public void addContext(Transaction txn, GeneralContextProxy context)
            throws DbException {
        try {
            DBContext dbContext =
                    new DBContext(context.getId(), context.getName(),
                            context.getColor(), ContextType.GENERAL, context.getPrivateName());
            db.addContext(txn, dbContext);
            BdfDictionary meta = BdfDictionary.of(
                    new BdfEntry(CONTEXT_KEY_MEMBERS, new BdfList()),
                    new BdfEntry(CONTEXT_PRIVATE_GROUPS, new BdfList()),
                    new BdfEntry(CONTEXT_FORUMS, new BdfList())
            );
            db.mergeContextMetadata(txn, context.getId(), encoder.encodeMetadata(meta));
        } catch (FormatException e) {
            throw new DbException(e);
        }
    }

    @Override
    public void addContext(Transaction txn, LocationContextProxy context)
            throws DbException {
        try {
            DBContext dbContext =
                    new DBContext(context.getId(), context.getName(),
                            context.getColor(), ContextType.LOCATION, context.getPrivateName());
            db.addContext(txn, dbContext);
            BdfDictionary meta = BdfDictionary.of(
                    new BdfEntry(CONTEXT_LAT, context.getLat()),
                    new BdfEntry(CONTEXT_LNG, context.getLon()),
                    new BdfEntry(CONTEXT_RADIUS, context.getRadius()),
                    new BdfEntry(CONTEXT_KEY_MEMBERS, new BdfList()),
                    new BdfEntry(CONTEXT_PRIVATE_GROUPS, new BdfList()),
                    new BdfEntry(CONTEXT_FORUMS, new BdfList())
            );
            db.mergeContextMetadata(txn, context.getId(), encoder.encodeMetadata(meta));
        } catch (FormatException e) {
            throw new DbException(e);
        }
    }

    @Override
    public void addContext(Transaction txn, SpatioTemporalContext context)
            throws DbException {
        try {
            DBContext dbContext =
                    new DBContext(context.getId(), context.getName(),
                            context.getColor(), ContextType.SPATIOTEMPORAL, context.getPrivateName());
            db.addContext(txn, dbContext);

            BdfDictionary meta = BdfDictionary.of(
                    new BdfEntry(CONTEXT_LAT, context.getLat()),
                    new BdfEntry(CONTEXT_LNG, context.getLon()),
                    new BdfEntry(CONTEXT_RADIUS, context.getRadius()),
                    new BdfEntry(CONTEXT_START_TIME, context.getStartTime()),
                    new BdfEntry(CONTEXT_END_TIME, context.getEndTime()),
                    new BdfEntry(CONTEXT_REPEAT, context.getRepeat()),
                    new BdfEntry(CONTEXT_KEY_MEMBERS, new BdfList()),
                    new BdfEntry(CONTEXT_PRIVATE_GROUPS, new BdfList()),
                    new BdfEntry(CONTEXT_FORUMS, new BdfList())
            );
            db.mergeContextMetadata(txn, context.getId(), encoder.encodeMetadata(meta));
        } catch (FormatException e) {
            throw new DbException(e);
        }
    }

    @Override
    public void removeContext(String contextId) throws DbException {
        db.transaction(false, txn -> {
            db.removeContext(txn, contextId);
        });
    }

    @Override
    public Collection<DBContext> getContexts() throws DbException {
        Transaction txn = db.startTransaction(true);
        Collection<DBContext> contexts = new ArrayList<>();
        try {
            contexts = db.getContexts(txn);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return contexts;
    }


    @Override
    public Context getContext(String contextId)
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(true);
        Context context = null;
        try {
            context = getContext(txn, contextId,false);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return context;
    }

    @Override
    public Context getContext(String contextId, boolean hidePrivateName) throws DbException, FormatException {
        Transaction txn = db.startTransaction(true);
        Context context = null;
        try {
            context = getContext(txn, contextId, hidePrivateName);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return context;
    }

    @Override
    public Integer getContextColor(String contextId) throws DbException {
        return db.transactionWithResult(true, txn ->
                db.getContextColor(txn, contextId));
    }

    @Override
    public String getContextPrivateName(String contextId) throws DbException {
        return db.transactionWithResult(true, txn ->
                db.getContext(txn, contextId).getPrivateName());
    }

    @Override
    public String getContextName(String contextId) throws DbException {
        return db.transactionWithResult(true, txn ->
                db.getContext(txn, contextId).getName());
    }

    @Override
    public void setContextName(String contextId, String name) throws DbException {
        db.transaction(false, txn -> {
            db.setContextName(txn, contextId, name);
        });
    }

    @Override
    public void setContextPrivateName(String contextId, String name)  throws DbException {
        db.transaction(false, txn -> {
            db.setContextPrivateName(txn, contextId, name);
        });
    }


    @Override
    public boolean isMember(Transaction txn, String contextId, ContactId cid)
            throws DbException, FormatException {
        for (ContactId member : getMembers(txn, contextId)) {
            if (member.equals(cid)) return true;
        }
        return false;
    }

    @Override
    public boolean isMember(String contextId, ContactId cid)
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(true);
        try {
            boolean canBeShared = this.isMember(txn, contextId, cid);
            db.commitTransaction(txn);
            return canBeShared;
        } finally {
            db.endTransaction(txn);
        }

    }

    @Override
    public Collection<ContextInvitation> getOutgoingContextInvitations(String contextId)
            throws DbException {
        Transaction txn = db.startTransaction(true);
        Collection<ContextInvitation> contextInvitation;
        try {
            contextInvitation =
                    db.getPendingContextInvitations(txn, contextId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return contextInvitation;
    }

    @Override
    public boolean belongsToContext(ContactId contactId, String contextId)
            throws DbException {
        Transaction txn = db.startTransaction(true);
        Group group;
        try {
            group = db.getContactGroup(txn, contactId, contextId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return group != null;
    }

    @Override
    public ContextCount getStats(String contextId) throws DbException,
            FormatException {
        Transaction txn = db.startTransaction(true);
        ContextCount stats;
        try {
            stats = getStats(txn, contextId);
            db.commitTransaction(txn);

        } finally {
            db.endTransaction(txn);
        }
        return stats;
    }

    @Override
    public void addMember(String contextId, ContactId cid)
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(false);
        try {
            addMember(txn, contextId, cid);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public void addMember(Transaction txn, String contextId, ContactId a)
            throws DbException, FormatException {
        Metadata metadata = db.getContextMetadata(txn, contextId);
        BdfDictionary meta = parser.parseMetadata(metadata);
        BdfList members = meta.getList(CONTEXT_KEY_MEMBERS);
        members.add(a.getId().getBytes());
        db.mergeContextMetadata(txn, contextId, encoder.encodeMetadata(meta));
    }

    @Override
    public Collection<ContactId> getMembers(String contextId)
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(true);
        Collection<ContactId> members;
        try {
            members = getMembers(txn, contextId);
            db.commitTransaction(txn);

        } finally {
            db.endTransaction(txn);
        }
        return members;
    }

    @Override
    public Collection<String> getPrivateGroups(String contextId)
            throws DbException, FormatException {
        Collection<String> groupIds;
        Transaction txn = db.startTransaction(true);
        try {
            groupIds = getPrivateGroups(txn, contextId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return groupIds;
    }

    @Override
    public Collection<String> getForums(String contextId)
            throws DbException, FormatException {
        Collection<String> forumIds;
        Transaction txn = db.startTransaction(true);
        try {
            forumIds = getForums(txn, contextId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return forumIds;
    }

    @Override
    public void addForum(Transaction txn, String contextId, String forumId)
            throws DbException, FormatException {
        Metadata metadata = db.getContextMetadata(txn, contextId);
        BdfDictionary meta = parser.parseMetadata(metadata);
        BdfList groups = meta.getList(CONTEXT_FORUMS);
        groups.add(forumId.getBytes());
        db.mergeContextMetadata(txn, contextId, encoder.encodeMetadata(meta));
    }

    @Override
    public void addForum(String contextId, String forumId)
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(false);
        try {
            addForum(txn, contextId, forumId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public void addPrivateGroup(String contextId, String groupId)
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(false);
        try {
            addPrivateGroup(txn, contextId, groupId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public void addPrivateGroup(Transaction txn, String contextId,
                                String groupId)
            throws DbException, FormatException {
        Metadata metadata = db.getContextMetadata(txn, contextId);
        BdfDictionary meta = parser.parseMetadata(metadata);
        BdfList groups = meta.getList(CONTEXT_PRIVATE_GROUPS);
        groups.add(groupId.getBytes());
        db.mergeContextMetadata(txn, contextId, encoder.encodeMetadata(meta));
    }

    @Override
    public Collection<ContactId> getMembers(Transaction txn, String contextId)
            throws DbException, FormatException {
        Metadata metadata = db.getContextMetadata(txn, contextId);
        BdfDictionary meta = parser.parseMetadata(metadata);
        BdfList list = meta.getList(CONTEXT_KEY_MEMBERS);
        HashSet<ContactId> members = new HashSet<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            String id = list.getString(i);
            members.add(new ContactId(id));
        }
        return members;
    }

    @Override
    public int pendingIncomingContextInvitations() throws DbException {
        Transaction txn = db.startTransaction(true);
        int pendingContextInvitations = 0;
        try {
            pendingContextInvitations = db.countPendingContextInvitations(txn, true);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return pendingContextInvitations;
    }

    @Override
    public int pendingOutgoingContextInvitations() throws DbException {
        Transaction txn = db.startTransaction(true);
        int pendingContextInvitations = 0;
        try {
            pendingContextInvitations = db.countPendingContextInvitations(txn, false);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return pendingContextInvitations;
    }

    private Collection<String> getPrivateGroups(Transaction txn,
                                                String contextId)
            throws DbException, FormatException {
        Metadata metadata = db.getContextMetadata(txn, contextId);
        BdfDictionary meta = parser.parseMetadata(metadata);
        BdfList list = meta.getList(CONTEXT_PRIVATE_GROUPS);
        HashSet<String> groups = new HashSet<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            String id = list.getString(i);
            groups.add(id);
        }
        return groups;

    }

    private Collection<String> getForums(Transaction txn, String contextId)
            throws DbException, FormatException {
        Metadata metadata = db.getContextMetadata(txn, contextId);
        BdfDictionary meta = parser.parseMetadata(metadata);
        BdfList list = meta.getList(CONTEXT_FORUMS);
        HashSet<String> forums = new HashSet<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            String id = list.getString(i);
            forums.add(id);
        }
        return forums;
    }

    private ContextCount getStats(Transaction txn, String contextId)
            throws DbException, FormatException {
        Metadata metadata = db.getContextMetadata(txn, contextId);
        BdfDictionary meta = parser.parseMetadata(metadata);
        BdfList members = meta.getList(CONTEXT_KEY_MEMBERS);
        BdfList groups = meta.getList(CONTEXT_PRIVATE_GROUPS);
        BdfList forums = meta.getList(CONTEXT_FORUMS);

        return new ContextCount(
                members.size(),
                groups.size(),
                forums.size()
        );
    }

    private Context getContext(Transaction txn, String contextId, boolean hidePrivateName)
            throws DbException, FormatException {
        DBContext dbContext = db.getContext(txn, contextId);

        if (dbContext.getContextType().equals(ContextType.GENERAL)) {
            if (hidePrivateName){
                return new GeneralContextProxy(dbContext.getId(),
                        dbContext.getName(), dbContext.getColor(), false, dbContext.getName());
            }
            else {
                return new GeneralContextProxy(dbContext.getId(),
                        dbContext.getName(), dbContext.getColor(), false, dbContext.getPrivateName());
            }
        } else if (dbContext.getContextType()
                .equals(ContextType.LOCATION)) {
            Metadata metadata = db.getContextMetadata(txn, contextId);
            BdfDictionary meta = parser.parseMetadata(metadata);
            Double lat = meta.getDouble(CONTEXT_LAT);
            Double lng = meta.getDouble(CONTEXT_LNG);
            Double radius = meta.getDouble(CONTEXT_RADIUS);
            return new LocationContextProxy(dbContext.getId(),
                    dbContext.getName(), dbContext.getColor(), lat, lng,
                    radius, dbContext.getPrivateName());
        }  else if (dbContext.getContextType()
                .equals(ContextType.SPATIOTEMPORAL)) {
            Metadata metadata = db.getContextMetadata(txn, contextId);
            BdfDictionary meta = parser.parseMetadata(metadata);
            Double lat = meta.getDouble(CONTEXT_LAT);
            Double lng = meta.getDouble(CONTEXT_LNG);
            Double radius = meta.getDouble(CONTEXT_RADIUS);
            long startTime = meta.getLong(CONTEXT_START_TIME);
            long endTime = meta.getLong(CONTEXT_END_TIME);
            int repeat = meta.getInteger(CONTEXT_REPEAT);

            return new SpatioTemporalContext(dbContext.getId(),
                    dbContext.getName(), dbContext.getColor(), dbContext.getPrivateName(), new LocationContext(dbContext.getName(), lat, lng,
                    radius), new TimeContext(null, dbContext.getName(), startTime,endTime,repeat));
        }else {
            //TODO: Temporal & Spatiotemporal Contexts
            return null;
        }
    }

    @Override
    public int countUnreadMessagesInContext(String contextId) throws DbException {
        Transaction txn = db.startTransaction(true);
        int unreadMessages = 0;
        try {
            unreadMessages = db.countUnreadMessagesInContext(txn, contextId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return unreadMessages;
    }

    @Override
    public Collection<LocationContextProxy> getLocationContexts()
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(true);
        Collection<LocationContextProxy> locationContextProxies = null;
        try {
            Collection<DBContext> contexts = db.getContexts(txn);
            locationContextProxies = new ArrayList<>();
            for (DBContext context: contexts){
                if (context.getContextType()
                        .equals(ContextType.LOCATION)) {
                    Metadata metadata = db.getContextMetadata(txn, context.getId());
                    BdfDictionary meta = parser.parseMetadata(metadata);
                    Double lat = meta.getDouble(CONTEXT_LAT);
                    Double lng = meta.getDouble(CONTEXT_LNG);
                    Double radius = meta.getDouble(CONTEXT_RADIUS);
                    locationContextProxies.add( new LocationContextProxy(context.getId(),
                            context.getName(), context.getColor(), lat, lng,
                            radius, context.getPrivateName()));
                }
            }
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }

        return locationContextProxies;
    }

    @Override
    public Collection<SpatioTemporalContext> getSpatiotemporalContexts()
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(true);
        Collection<SpatioTemporalContext> spatioTemporalContexts = null;
        try {
            Collection<DBContext> contexts = db.getContexts(txn);
            spatioTemporalContexts = new ArrayList<>();
            for (DBContext context: contexts){
                if (context.getContextType()
                        .equals(ContextType.SPATIOTEMPORAL)) {
                    Metadata metadata = db.getContextMetadata(txn, context.getId());
                    BdfDictionary meta = parser.parseMetadata(metadata);
                    Double lat = meta.getDouble(CONTEXT_LAT);
                    Double lng = meta.getDouble(CONTEXT_LNG);
                    Double radius = meta.getDouble(CONTEXT_RADIUS);
                    long startTime = meta.getLong(CONTEXT_START_TIME);
                    long endTime = meta.getLong(CONTEXT_END_TIME);
                    int repeat = meta.getInteger(CONTEXT_REPEAT);
                    spatioTemporalContexts.add( new SpatioTemporalContext(context.getId(),
                            context.getName(), context.getColor(), context.getPrivateName(), new LocationContext(context.getName(), lat, lng,
                            radius), new TimeContext(null, context.getName(), startTime,endTime,repeat)));
                }
            }
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }

        return spatioTemporalContexts;
    }
}
