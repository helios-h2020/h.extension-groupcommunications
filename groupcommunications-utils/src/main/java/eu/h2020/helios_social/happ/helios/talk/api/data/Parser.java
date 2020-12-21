package eu.h2020.helios_social.happ.helios.talk.api.data;

import eu.h2020.helios_social.happ.helios.talk.api.db.Metadata;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;

@NotNullByDefault
public interface Parser {

	BdfDictionary parseMetadata(Metadata m) throws FormatException;

	BdfList parseToList(byte[] b) throws FormatException;
}
