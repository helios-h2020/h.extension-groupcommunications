package eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries;

import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import eu.h2020.helios_social.core.context.Context;
import eu.h2020.helios_social.core.contextualegonetwork.Node;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.mining.MiningManager;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.Query;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryForwarder;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryForwarderManager;
import eu.h2020.helios_social.modules.groupcommunications.context.ContextManager;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.forwarders.BiasedWalkQueryForwarder;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.forwarders.FloodingQueryForwarder;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.forwarders.KBiasedWalkQueryForwarder;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.forwarders.KRandomWalkQueryForwarder;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.queries.forwarders.RandomWalkQueryForwarder;

public class QueryForwarderManagerImpl implements QueryForwarderManager {

    private final ContactManager contactManager;
    private HashSet<Class<? extends QueryForwarder>> queryForwarders = new HashSet<>();
    private Context currentContext;
    private ContextManager contextManager;
    private MiningManager miningManager;
    @Inject
    public QueryForwarderManagerImpl(ContactManager contactManager, ContextManager contextManager, MiningManager miningManager) {
        this.contactManager = contactManager;
        this.contextManager = contextManager;
        this.miningManager = miningManager;
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
        currentContext = getCurrentContext(query);
        QueryForwarder queryForwarder = createForwarder(KBiasedWalkQueryForwarder.class);
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
            return new KRandomWalkQueryForwarder(contactManager);
        else if (queryForwaderClass.getName().equals(FloodingQueryForwarder.class.getName()))
            return new FloodingQueryForwarder(contactManager);
        else if (queryForwaderClass.getName().equals(BiasedWalkQueryForwarder.class.getName()))
            return new BiasedWalkQueryForwarder(contactManager, getProbabilities());
        else if (queryForwaderClass.getName().equals(KBiasedWalkQueryForwarder.class.getName()))
            return new KBiasedWalkQueryForwarder(contactManager, getProbabilities(),3);
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

    public HashMap<PeerId, Double> getProbabilities(){
        Log.d("currentContext", currentContext.getName() + "%" + currentContext.getId());

        HashMap<Node,Double> probabilities =  miningManager.
                getInteractionRecommendationsProbabilities(currentContext.getName() + "%" + currentContext.getId());
        Log.d("probabilities", String.valueOf(probabilities.entrySet()));

        HashMap<PeerId,Double> newHashMap = new HashMap<>();
        for (Map.Entry<Node, Double> prob : probabilities.entrySet()) {
            PeerId id = new PeerId(prob.getKey().getId());
            newHashMap.put(id, prob.getValue());
        }
        Log.d("newProbabilities", String.valueOf(newHashMap.entrySet()));
        return newHashMap;
    }


    public Context getCurrentContext(Query query) {
        try {
            return contextManager.getContext(query.getContextId());
        } catch (DbException | FormatException e) {
            e.printStackTrace();
            return null;
        }
    }
}
