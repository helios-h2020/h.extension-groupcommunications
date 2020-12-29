package eu.h2020.helios_social.modules.groupcommunications.mining;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.modules.groupcommunications.api.mining.MiningManager;
import eu.h2020.helios_social.modules.socialgraphmining.GNN.GNNMiner;
import eu.h2020.helios_social.modules.socialgraphmining.SocialGraphMiner;
import eu.h2020.helios_social.modules.socialgraphmining.SwitchableMiner;

@Module
public class MiningModule {

    @Provides
    @Singleton
    SocialGraphMiner getSocialGraphMiner(ContextualEgoNetwork egoNetwork) {
        return new GNNMiner(egoNetwork);
    }

    @Provides
    @Singleton
    MiningManager provideMiningManager(
            MiningManagerImpl miningManager) {
        return miningManager;
    }
}
