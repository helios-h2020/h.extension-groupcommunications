package eu.h2020.helios_social.modules.groupcommunications.group;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications_utils.identity.IdentityManager;
import eu.h2020.helios_social.modules.groupcommunications.api.context.utils.Datespan;
import eu.h2020.helios_social.modules.groupcommunications.api.context.utils.Daytime;
import eu.h2020.helios_social.modules.groupcommunications.api.context.utils.Season;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMemberRole;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.LocationForum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.SeasonalForum;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupFactory;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;
import eu.h2020.helios_social.modules.groupcommunications.api.utils.password.PasswordGenerator;

public class GroupFactoryImpl implements GroupFactory {

    private IdentityManager identityManager;
    private PasswordGenerator passwordGenerator;

    @Inject
    public GroupFactoryImpl(IdentityManager identityManager) {
        this.identityManager = identityManager;
        this.passwordGenerator = new PasswordGenerator();
    }

    @Override
    public PrivateGroup createPrivateGroup(@NotNull String name,
                                           String contextId) {
        return new PrivateGroup(UUID.randomUUID().toString(), contextId, name,
                passwordGenerator.generateRandomPassword(8),
                identityManager.getIdentity().getNetworkId());
    }

    @Override
    public Forum createNamedForum(@NotNull String name,
                                  String contextId,
                                  @NotNull GroupType forumType,
                                  List<String> tags,
                                  ForumMemberRole defaultRole) {
        List<String> moderators = new ArrayList();
        moderators.add(identityManager.getIdentity().getNetworkId());
        return new Forum(UUID.randomUUID().toString(), contextId, name,
                passwordGenerator.generateRandomPassword(8),
                moderators, forumType, tags, defaultRole);
    }

    @Override
    public LocationForum createLocationForum(@NotNull String name,
                                             String contextId,
                                             GroupType forumType,
                                             List<String> tags,
                                             ForumMemberRole defaultRole,
                                             double lat, double lon, int radius) {
        List<String> moderators = new ArrayList();
        moderators.add(identityManager.getIdentity().getNetworkId());
        return new LocationForum(UUID.randomUUID().toString(), contextId, name,
                passwordGenerator.generateRandomPassword(8),
                moderators, forumType, tags, defaultRole, lat, lon, radius);
    }

    @Override
    public SeasonalForum<Season> createSeasonalForum(@NotNull String name,
                                                     String contextId,
                                                     @NotNull GroupType forumType,
                                                     List<String> tags,
                                                     ForumMemberRole defaultRole,
                                                     @NotNull Season season) {
        List<String> moderators = new ArrayList();
        moderators.add(identityManager.getIdentity().getNetworkId());
        return new SeasonalForum<Season>(UUID.randomUUID().toString(), contextId, name,
                passwordGenerator.generateRandomPassword(8),
                moderators, forumType, tags, defaultRole, season);
    }

    @Override
    public SeasonalForum<Datespan> createSeasonalForum(@NotNull String name,
                                                       String contextId,
                                                       @NotNull GroupType forumType,
                                                       List<String> tags,
                                                       ForumMemberRole defaultRole,
                                                       @NotNull Datespan season) {
        List<String> moderators = new ArrayList();
        moderators.add(identityManager.getIdentity().getNetworkId());
        return new SeasonalForum<Datespan>(UUID.randomUUID().toString(), contextId, name,
                passwordGenerator.generateRandomPassword(8),
                moderators, forumType, tags, defaultRole, season);
    }

    @Override
    public SeasonalForum<Daytime> createSeasonalForum(@NotNull String name, String contextId,
                                                      @NotNull GroupType forumType,
                                                      List<String> tags,
                                                      ForumMemberRole defaultRole,
                                                      @NotNull Daytime season) {
        List<String> moderators = new ArrayList();
        moderators.add(identityManager.getIdentity().getNetworkId());
        return new SeasonalForum<Daytime>(UUID.randomUUID().toString(), contextId, name,
                passwordGenerator.generateRandomPassword(8),
                moderators, forumType, tags, defaultRole, season);
    }
}
