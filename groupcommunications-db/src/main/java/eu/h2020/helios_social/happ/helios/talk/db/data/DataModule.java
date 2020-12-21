package eu.h2020.helios_social.happ.helios.talk.db.data;

import eu.h2020.helios_social.happ.helios.talk.api.data.BdfReaderFactory;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfWriterFactory;
import eu.h2020.helios_social.happ.helios.talk.api.data.Encoder;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.happ.helios.talk.api.data.Parser;

@Module
public class DataModule {

	@Provides
	BdfReaderFactory provideBdfReaderFactory() {
		return new BdfReaderFactoryImpl();
	}

	@Provides
	BdfWriterFactory provideBdfWriterFactory() {
		return new BdfWriterFactoryImpl();
	}

	@Provides
	Parser provideMetaDataParser(BdfReaderFactory bdfReaderFactory) {
		return new ParserImpl(bdfReaderFactory);
	}

	@Provides
	Encoder provideMetaDataEncoder(BdfWriterFactory bdfWriterFactory) {
		return new EncoderImpl(bdfWriterFactory);
	}

}
