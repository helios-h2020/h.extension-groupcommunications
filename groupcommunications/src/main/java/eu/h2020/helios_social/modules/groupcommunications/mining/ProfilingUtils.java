package eu.h2020.helios_social.modules.groupcommunications.mining;

import eu.h2020.helios_social.modules.contentawareprofiling.model.ModelType;
import eu.h2020.helios_social.modules.contentawareprofiling.profile.CoarseInterestsProfile;
import eu.h2020.helios_social.modules.contentawareprofiling.profile.ContentAwareProfile;
import eu.h2020.helios_social.modules.contentawareprofiling.profile.DMLProfile;
import eu.h2020.helios_social.modules.contentawareprofiling.profile.FineInterestsProfile;

public class ProfilingUtils {

    public static Class<? extends ContentAwareProfile> getProfileClass(ModelType modelType) {
        if (modelType.equals(ModelType.COARSE)) return CoarseInterestsProfile.class;
        else if (modelType.equals(ModelType.FINE)) return FineInterestsProfile.class;
        else return DMLProfile.class;
    }
}
