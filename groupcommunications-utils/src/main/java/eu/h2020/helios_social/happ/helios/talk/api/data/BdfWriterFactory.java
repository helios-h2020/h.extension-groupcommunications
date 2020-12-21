package eu.h2020.helios_social.happ.helios.talk.api.data;

import java.io.OutputStream;

import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface BdfWriterFactory {

	BdfWriter createWriter(OutputStream out);
}
