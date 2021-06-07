package eu.h2020.helios_social.modules.groupcommunications.mining;

import android.app.Application;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.modules.contentawareprofiling.ContentAwareProfileManager;
import eu.h2020.helios_social.modules.contentawareprofiling.profile.CoarseInterestsProfile;
import eu.h2020.helios_social.modules.contentawareprofiling.profile.FineInterestsProfile;
import eu.h2020.helios_social.modules.contentawareprofiling.profile.Interest;
import eu.h2020.helios_social.modules.groupcommunications.api.mining.MiningManager;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventBus;
import eu.h2020.helios_social.modules.socialgraphmining.GNN.GNNMiner;
import eu.h2020.helios_social.modules.socialgraphmining.SwitchableMiner;
import eu.h2020.helios_social.modules.socialgraphmining.diffusion.PPRMiner;
import mklab.JGNN.core.tensor.DenseTensor;

@Module
public class MiningModule {
    public static String COARSE_INTEREST_PROFILE_PERSONALIZER_NAME = PPRMiner.class.getName() + "_" + CoarseInterestsProfile.class.getName();
    public static String FINE_INTEREST_PROFILE_PERSONALIZER_NAME = PPRMiner.class.getName() + "_" + FineInterestsProfile.class.getName();

    public static class EagerSingletons {
        @Inject
        MiningManager miningManager;
    }

    @Provides
    @Singleton
    SwitchableMiner getSwitchableMiner(ContextualEgoNetwork egoNetwork) {
        SwitchableMiner switchableMiner = new SwitchableMiner(egoNetwork);
        switchableMiner.createMiner(GNNMiner.class.getName(), GNNMiner.class);
        DenseTensor coarseInterestPersonalization = egoNetwork.getEgo()
                .getOrCreateInstance(
                        COARSE_INTEREST_PROFILE_PERSONALIZER_NAME + "personalization",
                        () -> {
                            return new DenseTensor(15);
                        }
                );
        switchableMiner.registerMiner(COARSE_INTEREST_PROFILE_PERSONALIZER_NAME,
                                      new PPRMiner(
                                              COARSE_INTEREST_PROFILE_PERSONALIZER_NAME,
                                              egoNetwork,
                                              coarseInterestPersonalization
                                      )
        );


        DenseTensor fineInterestPersonalization = egoNetwork.getEgo()
                .getOrCreateInstance(
                        FINE_INTEREST_PROFILE_PERSONALIZER_NAME + "personalization", () -> {
                            return new DenseTensor(42);
                        }
                );

        switchableMiner.registerMiner(FINE_INTEREST_PROFILE_PERSONALIZER_NAME,
                                      new PPRMiner(
                                              FINE_INTEREST_PROFILE_PERSONALIZER_NAME,
                                              egoNetwork,
                                              fineInterestPersonalization
                                      ));

        switchableMiner.setActiveMiner(GNNMiner.class.getName());
        egoNetwork.save();
        return switchableMiner;
    }

    @Provides
    @Singleton
    MiningManager provideMiningManager(EventBus eventBus,
                                       MiningManagerImpl miningManager) {
        eventBus.addListener(miningManager);
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
