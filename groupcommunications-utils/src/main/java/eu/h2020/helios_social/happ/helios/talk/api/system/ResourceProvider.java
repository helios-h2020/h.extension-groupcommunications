package eu.h2020.helios_social.happ.helios.talk.api.system;

import java.io.InputStream;

import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface ResourceProvider {

	InputStream getResourceInputStream(String name, String extension);
}
