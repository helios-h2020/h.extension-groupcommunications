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
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.mining.MiningManager;
import eu.h2020.helios_social.modules.groupcommunications_utils.lifecycle.LifecycleManager;
import eu.h2020.helios_social.modules.groupcommunications_utils.settings.Settings;
import eu.h2020.helios_social.modules.groupcommunications_utils.settings.SettingsManager;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventBus;
import eu.h2020.helios_social.modules.socialgraphmining.GNN.GNNMiner;
import eu.h2020.helios_social.modules.socialgraphmining.combination.WeightedMiner ;
import eu.h2020.helios_social.modules.socialgraphmining.diffusion.PPRMiner;
import eu.h2020.helios_social.modules.socialgraphmining.heuristics.RepeatAndReplyMiner;
import mklab.JGNN.core.tensor.DenseTensor;

import static eu.h2020.helios_social.modules.groupcommunications_utils.settings.SettingsConsts.PREF_RECOMMENDATION_MINER;
import static eu.h2020.helios_social.modules.groupcommunications_utils.settings.SettingsConsts.PREF_SHARE_PREFS;
import static eu.h2020.helios_social.modules.groupcommunications_utils.settings.SettingsConsts.SETTINGS_NAMESPACE;

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
    WeightedMiner  getSwitchableMiner(ContextualEgoNetwork egoNetwork, SettingsManager settingsManager) {
        WeightedMiner  switchableMiner = new WeightedMiner (egoNetwork);
        switchableMiner.createMiner(RepeatAndReplyMiner.class.getName(), RepeatAndReplyMiner.class);
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
        egoNetwork.save();
        return switchableMiner;
    }

    @Provides
    @Singleton
    MiningManager provideMiningManager(EventBus eventBus, LifecycleManager lifecycleManager,
                                       MiningManagerImpl miningManager) {
        lifecycleManager.registerOpenDatabaseHook(miningManager);
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
        try {
            WorkManager.initialize(app, config);
        } catch (Exception e){
            return WorkManager.getInstance(app);
        }
        return WorkManager.getInstance(app);
    }
}
