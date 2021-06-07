package eu.h2020.helios_social.modules.groupcommunications.mining;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Node;
import eu.h2020.helios_social.modules.contentawareprofiling.interestcategories.InterestCategories;
import eu.h2020.helios_social.modules.contentawareprofiling.interestcategories.InterestCategoriesHierarchy;
import eu.h2020.helios_social.modules.groupcommunications.api.mining.MathUtils;
import eu.h2020.helios_social.modules.groupcommunications_utils.battery.event.BatteryEvent;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.Event;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventListener;
import eu.h2020.helios_social.modules.groupcommunications_utils.nullsafety.NotNullByDefault;
import eu.h2020.helios_social.modules.groupcommunications_utils.settings.Settings;
import eu.h2020.helios_social.modules.groupcommunications_utils.settings.SettingsManager;
import eu.h2020.helios_social.modules.contentawareprofiling.model.ModelType;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.mining.ContentAwareProfilingType;
import eu.h2020.helios_social.modules.groupcommunications.api.mining.MiningManager;
import eu.h2020.helios_social.modules.socialgraphmining.SwitchableMiner;
import eu.h2020.helios_social.modules.socialgraphmining.diffusion.PPRMiner;
import mklab.JGNN.core.tensor.DenseTensor;

import static eu.h2020.helios_social.modules.groupcommunications.mining.MiningModule.COARSE_INTEREST_PROFILE_PERSONALIZER_NAME;
import static eu.h2020.helios_social.modules.groupcommunications.mining.MiningModule.FINE_INTEREST_PROFILE_PERSONALIZER_NAME;
import static eu.h2020.helios_social.modules.groupcommunications_utils.settings.SettingsConsts.PREF_CONTENT_PROFILING;
import static eu.h2020.helios_social.modules.groupcommunications_utils.settings.SettingsConsts.SETTINGS_NAMESPACE;
import static java.util.logging.Logger.getLogger;

@Immutable
@NotNullByDefault
public class MiningManagerImpl implements MiningManager, EventListener {
    private static String TAG = MiningManagerImpl.class.getName();
    private final static Logger LOG = getLogger(TAG);

    private final SwitchableMiner switchableMiner;
    private final ContextualEgoNetwork egoNetwork;
    private final SettingsManager settingsManager;
    private final WorkManager workManager;

    @Inject
    public MiningManagerImpl(SwitchableMiner switchableMiner,
                             ContextualEgoNetwork egoNetwork,
                             SettingsManager settingsManager,
                             WorkManager workManager) {
        this.switchableMiner = switchableMiner;
        this.egoNetwork = egoNetwork;
        this.settingsManager = settingsManager;
        this.workManager = workManager;
    }

    @Override
    public HashMap<Node, Double> getNextInteractionRecommendations(String egoContextId) {
        HashMap<Node, Double> recommendations =
                switchableMiner.recommendInteractions(egoNetwork.getOrCreateContext(egoContextId));

        int length = 0;
        int size = recommendations.size();
        if (size > 2 && size <= 4) {
            length = 1;
        } else if (size > 4 && size <= 6) {
            length = 2;
        } else if (size > 6) {
            length = 3;
        }

        return recommendations
                .entrySet()
                .stream()
                .sorted(Map.Entry.<Node, Double>comparingByValue()
                                .reversed())
                .limit(length)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new));
    }

    @Override
    public List getSmoothPersonalizedProfile(ContentAwareProfilingType profilingType) {
        if (profilingType == ContentAwareProfilingType.COARSE_INTEREST_RPOFILE) {
            DenseTensor smoothedPersonalization = (DenseTensor) ((PPRMiner) switchableMiner.getMiner(COARSE_INTEREST_PROFILE_PERSONALIZER_NAME)).getSmoothedPersonalization();
            List<Double> personalizedProfile = Arrays.stream(smoothedPersonalization.toArray()).boxed().collect(Collectors.toList());

            ArrayList<InterestCategories> interests = InterestCategoriesHierarchy.coarseCategories;
            List<Double> topk = MathUtils.findTopK(personalizedProfile, 3);
            if (topk.get(0) == 0) return new ArrayList();

            List<InterestCategories> topCategories = new ArrayList<>();
            topCategories.add(interests.get(personalizedProfile.indexOf(topk.get(0))));
            topCategories.add(interests.get(personalizedProfile.indexOf(topk.get(1))));
            topCategories.add(interests.get(personalizedProfile.indexOf(topk.get(2))));
            return topCategories;
        } else if (profilingType == ContentAwareProfilingType.FINE_INTEREST_PROFILE) {
            DenseTensor smoothedPersonalization = (DenseTensor) ((PPRMiner) switchableMiner.getMiner(FINE_INTEREST_PROFILE_PERSONALIZER_NAME)).getSmoothedPersonalization();
            List<Double> personalizedProfile = Arrays.stream(smoothedPersonalization.toArray()).boxed().collect(Collectors.toList());

            ArrayList<InterestCategories> interests = InterestCategoriesHierarchy.fineCategories;
            List<Double> topk = MathUtils.findTopK(personalizedProfile, 4);
            if (topk.get(0) == 0) return new ArrayList();

            List<InterestCategories> topCategories = new ArrayList<>();
            topCategories.add(interests.get(personalizedProfile.indexOf(topk.get(0))));
            topCategories.add(interests.get(personalizedProfile.indexOf(topk.get(1))));
            topCategories.add(interests.get(personalizedProfile.indexOf(topk.get(2))));
            topCategories.add(interests.get(personalizedProfile.indexOf(topk.get(3))));
            return topCategories;
        }
        throw new NotImplementedException(profilingType + " is not yet implemented");
    }


    @Override
    public void eventOccurred(Event e) {
        if (e instanceof BatteryEvent && ((BatteryEvent) e).isCharging()) {
            try {
                LOG.info("Battery is charging!");
                Settings s = settingsManager.getSettings(SETTINGS_NAMESPACE);
                ContentAwareProfilingType profilingType = ContentAwareProfilingType.fromValue(s.getInt(PREF_CONTENT_PROFILING, 0));

                if (profilingType == ContentAwareProfilingType.COARSE_INTEREST_RPOFILE) {
                    LOG.info("Selected Profiling Type: " + profilingType);
                    Constraints constraints = new Constraints.Builder()
                            .build();
                    WorkRequest request = new OneTimeWorkRequest.Builder(ProfilingWorker.class).setConstraints(constraints)
                            .setInputData(new Data.Builder().putString("MODEL", ModelType.COARSE.toString()).build()).build();

                    workManager.enqueueUniqueWork(
                            "ContentAwareProfiler",
                            ExistingWorkPolicy.KEEP,
                            (OneTimeWorkRequest) request
                    );

                    LOG.info("Work request with id " + request.getId() + " enqueued!");
                } else if (profilingType == ContentAwareProfilingType.FINE_INTEREST_PROFILE) {
                    LOG.info("Selected Profiling Type: " + profilingType);

                    Constraints constraints = new Constraints.Builder()
                            .build();
                    WorkRequest request = new OneTimeWorkRequest.Builder(ProfilingWorker.class).setConstraints(constraints)
                            .setInputData(new Data.Builder().putString("MODEL", ModelType.FINE.toString()).build()).build();

                    workManager.enqueueUniqueWork(
                            "ContentAwareProfiler",
                            ExistingWorkPolicy.KEEP,
                            (OneTimeWorkRequest) request
                    );

                    LOG.info("Work request with id " + request.getId() + " enqueued!");
                }
            } catch (DbException dbException) {
                dbException.printStackTrace();
            }
        }
    }

}
