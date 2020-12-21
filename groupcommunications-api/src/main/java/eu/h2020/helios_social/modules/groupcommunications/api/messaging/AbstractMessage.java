package eu.h2020.helios_social.modules.groupcommunications.api.messaging;

import com.google.gson.Gson;

public interface AbstractMessage {

    default String toJson() {
        return new Gson().toJson(this);
    }
}
