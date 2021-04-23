package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.contentawareprofiling.profile.ContentAwareProfile;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.Query;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryForwarder;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryForwarderManager;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.forwarders.FloodingQueryForwarder;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.forwarders.KRandomWalkQueryForwarder;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.forwarders.RandomWalkQueryForwarder;

public class QueryForwarderManagerImpl implements QueryForwarderManager {

    private final ContactManager contactManager;
    private HashSet<Class<? extends QueryForwarder>> queryForwarders = new HashSet<>();

    @Inject
    public QueryForwarderManagerImpl(ContactManager contactManager) {
        this.contactManager = contactManager;
        queryForwarders.add(FloodingQueryForwarder.class);
        queryForwarders.add(RandomWalkQueryForwarder.class);
        queryForwarders.add(KRandomWalkQueryForwarder.class);
    }

    @Override
    public <Forwarder extends QueryForwarder> List<PeerId> getNextHops(Class<Forwarder> forwarderClass, PeerId peerId, Query query) throws DbException {
        QueryForwarder queryForwarder = createForwarder(forwarderClass);
        return queryForwarder.forward(peerId, query);
    }

    @Override
    public <Forwarder extends QueryForwarder> List<PeerId> getNextHops(PeerId peerId, Query query) throws DbException {
        QueryForwarder queryForwarder = createForwarder(KRandomWalkQueryForwarder.class);
        return queryForwarder.forward(peerId, query);
    }

    @Override
    public void addForwarder(Class<? extends QueryForwarder> forwarder) {
        queryForwarders.add(forwarder);
    }

    public QueryForwarder createForwarder(Class<? extends QueryForwarder> queryForwaderClass) {
        if (queryForwaderClass.getName().equals(RandomWalkQueryForwarder.class.getName()))
            return new RandomWalkQueryForwarder(contactManager);
        else if (queryForwaderClass.getName().equals(KRandomWalkQueryForwarder.class.getName()))
            return new RandomWalkQueryForwarder(contactManager);
        else if (queryForwaderClass.getName().equals(FloodingQueryForwarder.class.getName()))
            return new FloodingQueryForwarder(contactManager);
        else return null;

        //FIX it throws: java.lang.NoSuchMethodException:
        // KRandomWalkQueryForwarder.<init> [interface eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager]
        /*try {
            Constructor<? extends QueryForwarder> constructor =
                    queryForwaderClass.getDeclaredConstructor(ContactManager.class);
            return constructor.newInstance(contactManager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;*/
    }
}
