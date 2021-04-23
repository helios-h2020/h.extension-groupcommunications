package eu.h2020.helios_social.modules.groupcommunications.mining;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Node;
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
import eu.h2020.helios_social.modules.socialgraphmining.SocialGraphMiner;

import static eu.h2020.helios_social.modules.groupcommunications_utils.settings.SettingsConsts.PREF_CONTENT_PROFILING;
import static eu.h2020.helios_social.modules.groupcommunications_utils.settings.SettingsConsts.SETTINGS_NAMESPACE;
import static java.util.logging.Logger.getLogger;

@Immutable
@NotNullByDefault
public class MiningManagerImpl implements MiningManager, EventListener {
    private static String TAG = MiningManagerImpl.class.getName();
    private final static Logger LOG = getLogger(TAG);

    private final SocialGraphMiner socialGraphMiner;
    private final ContextualEgoNetwork egoNetwork;
    private final SettingsManager settingsManager;
    private final WorkManager workManager;

    @Inject
    public MiningManagerImpl(SocialGraphMiner socialGraphMiner,
                             ContextualEgoNetwork egoNetwork,
                             SettingsManager settingsManager,
                             WorkManager workManager) {
        this.socialGraphMiner = socialGraphMiner;
        this.egoNetwork = egoNetwork;
        this.settingsManager = settingsManager;
        this.workManager = workManager;
    }

    @Override
    public SocialGraphMiner getSocialGraphMiner() {
        return socialGraphMiner;
    }

    @Override
    public HashMap<Node, Double> getNextInteractionRecommendations(String egoContextId) {
        HashMap<Node, Double> recommendations =
                socialGraphMiner.recommendInteractions(egoNetwork.getOrCreateContext(egoContextId));

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

                    workManager.beginUniqueWork(
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
