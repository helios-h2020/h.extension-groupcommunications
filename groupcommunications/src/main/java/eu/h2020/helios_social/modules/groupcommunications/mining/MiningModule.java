package eu.h2020.helios_social.modules.groupcommunications.mining;

import android.app.Application;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.modules.contentawareprofiling.ContentAwareProfileManager;
import eu.h2020.helios_social.modules.groupcommunications.api.mining.MiningManager;
import eu.h2020.helios_social.modules.socialgraphmining.GNN.GNNMiner;
import eu.h2020.helios_social.modules.socialgraphmining.SocialGraphMiner;

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

    @Provides
    @Singleton
    ContentAwareProfileManager provideContentAwareProfileManager(@NotNull Application app,
                                                                 ContextualEgoNetwork egoNetwork) {
        return new ContentAwareProfileManager(app.getApplicationContext(), egoNetwork);
    }

    @Provides
    @Singleton
    WorkManager provideWorkManager(Application app, CustomWorkerFactory customWorkerFactory) {
        Configuration config = new Configuration.Builder()
                .setWorkerFactory(customWorkerFactory)
                .build();
        WorkManager.initialize(app, config);
        return WorkManager.getInstance(app);
    }
}
