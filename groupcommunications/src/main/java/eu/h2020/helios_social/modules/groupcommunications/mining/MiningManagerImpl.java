package eu.h2020.helios_social.modules.groupcommunications.mining;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Node;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;
import eu.h2020.helios_social.modules.groupcommunications.api.mining.MiningManager;
import eu.h2020.helios_social.modules.socialgraphmining.GNN.GNNMiner;
import eu.h2020.helios_social.modules.socialgraphmining.SocialGraphMiner;
import eu.h2020.helios_social.modules.socialgraphmining.SwitchableMiner;

@Immutable
@NotNullByDefault
public class MiningManagerImpl implements MiningManager {

    private final SocialGraphMiner socialGraphMiner;
    private final ContextualEgoNetwork egoNetwork;

    @Inject
    MiningManagerImpl(SocialGraphMiner socialGraphMiner, ContextualEgoNetwork egoNetwork) {
        this.socialGraphMiner = socialGraphMiner;
        this.egoNetwork = egoNetwork;
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
}
