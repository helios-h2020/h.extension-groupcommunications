package eu.h2020.helios_social.modules.groupcommunications;

import com.github.javafaker.HarryPotter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import eu.h2020.helios_social.modules.groupcommunications.api.event.HeliosEvent;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMemberRole;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.LocationForum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.SeasonalForum;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryResponse;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.Queryable;
import eu.h2020.helios_social.modules.groupcommunications.api.resourcediscovery.queries.QueryableDeserializer;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {

        LocationForum forum = new LocationForum(UUID.randomUUID().toString(), "All", "All", "123",
                new ArrayList<>(), GroupType.PublicForum,
                new ArrayList<>(),
                ForumMemberRole.EDITOR,
                2.2, 256.3, 1000);

        System.out.println(forum instanceof LocationForum);

        QueryResponse queryResponse = new QueryResponse<>(UUID.randomUUID().toString());
        HashMap<Queryable, Double> results = new HashMap<>();
        results.put(forum, 1.0);
        queryResponse.appendResults(results);

        QueryableDeserializer deserializer = new QueryableDeserializer("queryableType");
        deserializer.registerQueryableType("Forum", Forum.class);
        deserializer.registerQueryableType("LocationForum", LocationForum.class);
        deserializer.registerQueryableType("SeasonalForum", SeasonalForum.class);
        deserializer.registerQueryableType("HeliosEvent", HeliosEvent.class);

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Queryable.class, deserializer)
                .create();

        QueryResponse<Queryable> qr = gson.fromJson(queryResponse.toJson(), new TypeToken<QueryResponse<Queryable>>() {
        }.getType());

        System.out.println(qr.getEntities().entrySet().iterator().next().getValue().getClass());


        assertEquals(4, 2 + 2);
    }
}
