package eu.h2020.helios_social.modules.groupcommunications.mining;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Worker;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;

import java.lang.reflect.Constructor;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.modules.contentawareprofiling.ContentAwareProfileManager;
import eu.h2020.helios_social.modules.contentawareprofiling.data.CNNModelData;
import eu.h2020.helios_social.modules.contentawareprofiling.data.DMLModelData;
import eu.h2020.helios_social.modules.contentawareprofiling.miners.CoarseInterestProfileMiner;
import eu.h2020.helios_social.modules.contentawareprofiling.miners.DMLProfileMiner;
import eu.h2020.helios_social.modules.contentawareprofiling.miners.FineInterestProfileMiner;
import eu.h2020.helios_social.modules.socialgraphmining.SocialGraphMiner;
import eu.h2020.helios_social.modules.socialgraphmining.SwitchableMiner;

import static java.util.logging.Logger.getLogger;

/**
 * CustomWorkerFactory is responsible for initializing workers and pass additional variables to
 * workers
 */
public class CustomWorkerFactory extends WorkerFactory {
    private static String TAG = CustomWorkerFactory.class.getName();
    private final static Logger LOG = getLogger(TAG);

    private ContentAwareProfileManager profileManager;

    @Inject
    public CustomWorkerFactory(ContentAwareProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Nullable
    @Override
    public Worker createWorker(@NonNull Context appContext, @NonNull String workerClassName,
                               @NonNull WorkerParameters workerParameters) {
        try {
            Worker worker;
            FineInterestProfileMiner.class.getDeclaredConstructors();
            CoarseInterestProfileMiner.class.getDeclaredConstructors();
            DMLProfileMiner.class.getDeclaredConstructors();
            CNNModelData.class.getDeclaredConstructors();
            DMLModelData.class.getDeclaredConstructors();
            //allows custom construction of ProfilingWorker
            if (workerClassName.equals(ProfilingWorker.class.getName())) {
                Constructor<? extends Worker> constructor = Class.forName(workerClassName)
                        .asSubclass(Worker.class).getDeclaredConstructor(Context.class,
                                                                         WorkerParameters.class,
                                                                         ContentAwareProfileManager.class);
                worker = constructor.newInstance(appContext, workerParameters, profileManager);
            } else {
                //Default construction of other workers
                Constructor<? extends Worker> constructor = Class.forName(workerClassName)
                        .asSubclass(Worker.class).getDeclaredConstructor(Context.class,
                                                                         WorkerParameters.class);
                worker = constructor.newInstance(appContext, workerParameters);
            }
            return worker;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
