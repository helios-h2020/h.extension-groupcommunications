package eu.h2020.helios_social.happ.helios.talk.db.data;

import eu.h2020.helios_social.happ.helios.talk.api.data.BdfWriter;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfWriterFactory;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;

import java.io.OutputStream;

import javax.annotation.concurrent.Immutable;

@Immutable
@NotNullByDefault
class BdfWriterFactoryImpl implements BdfWriterFactory {

	@Override
	public BdfWriter createWriter(OutputStream out) {
		return new BdfWriterImpl(out);
	}
}
