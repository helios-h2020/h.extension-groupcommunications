package eu.h2020.helios_social.modules.groupcommunications.api.utils;

public abstract class Identifier {

    private final String identifier;

    public Identifier(String id) {
        this.identifier = id;
    }

    public String getId() {
        return identifier;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Identifier && (this.identifier == ((Identifier) o).getId());
    }

}
