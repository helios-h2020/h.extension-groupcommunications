package eu.h2020.helios_social.modules.groupcommunications.api.forum;


import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.context.utils.Datespan;
import eu.h2020.helios_social.modules.groupcommunications.api.context.utils.Daytime;
import eu.h2020.helios_social.modules.groupcommunications.api.context.utils.Season;

/**
 * A class for creating new sharable Forums
 */
public interface ForumFactory {

    /**
     * creates a sharable Forum with a given name
     *
     * @param name      of the forum
     * @param contextId identifier of context the forum lives in
     * @param forumType PUBLIC, PROTECTED, SECRET
     * @param tags      a collection of strings describing the forum
     * @return the created forum
     */
    Forum createForum(@NotNull String name, String contextId,
		    @NotNull GroupType forumType,
		    Collection<String> tags);

    /**
     * creates a sharable Location Forum with a given name
     *
     * @param name      of the forum
     * @param forumType PUBLIC, PROTECTED, SECRET
     * @param tags      a collection of strings describing the forum
     * @param lat       latitude of the location assigned to forum
     * @param lon       longitude of the location assigned to forum
     * @return the created forum
     */
    LocationForum createLocationForum(@NotNull String name,
		    GroupType forumType,
		    Collection<String> tags, double lat, double lon);

    /**
     * creates a sharable Location Forum with a given name
     *
     * @param name      of the forum
     * @param forumType PUBLIC, PROTECTED, SECRET
     * @param tags      a collection of strings describing the forum
     * @param season    season the forum is active
     * @return the created forum
     */
    SeasonalForum<Season> createSeasonalForum(@NotNull String name,
		    @NotNull GroupType forumType,
		    Collection<String> tags,
		    @NotNull Season season);

    /**
     * creates a sharable Location Forum with a given name
     *
     * @param name      of the forum
     * @param forumType PUBLIC, PROTECTED, SECRET
     * @param tags      a collection of strings describing the forum
     * @param season    season the forum is active
     * @return the created forum
     */
    SeasonalForum<Datespan> createSeasonalForum(@NotNull String name,
		    @NotNull GroupType forumType,
		    Collection<String> tags,
		    @NotNull Datespan season);

    /**
     * creates a sharable Location Forum with a given name
     *
     * @param name      of the forum
     * @param forumType PUBLIC, PROTECTED, SECRET
     * @param tags      a collection of strings describing the forum
     * @param season    season the forum is active
     * @return the created forum
     */
    SeasonalForum<Daytime> createSeasonalForum(@NotNull String name,
		    String contextId,
		    @NotNull GroupType forumType,
		    Collection<String> tags,
		    @NotNull Daytime season);


}
