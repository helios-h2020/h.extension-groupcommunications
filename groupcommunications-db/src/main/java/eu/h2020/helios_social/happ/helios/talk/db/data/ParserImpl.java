package eu.h2020.helios_social.happ.helios.talk.db.data;

import eu.h2020.helios_social.happ.helios.talk.api.data.BdfDictionary;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfList;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfReader;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfReaderFactory;
import eu.h2020.helios_social.happ.helios.talk.api.data.Parser;
import eu.h2020.helios_social.happ.helios.talk.api.db.Metadata;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map.Entry;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import static eu.h2020.helios_social.happ.helios.talk.api.data.BdfDictionary.NULL_VALUE;
import static eu.h2020.helios_social.happ.helios.talk.api.db.Metadata.REMOVE;

@Immutable
@NotNullByDefault
class ParserImpl implements Parser {

	private final BdfReaderFactory bdfReaderFactory;

	@Inject
	ParserImpl(BdfReaderFactory bdfReaderFactory) {
		this.bdfReaderFactory = bdfReaderFactory;
	}

	@Override
	public BdfDictionary parseMetadata(Metadata m) throws FormatException {
		BdfDictionary d = new BdfDictionary();
		try {
			for (Entry<String, byte[]> e : m.entrySet()) {
				// Special case: if key is being removed, value is null
				if (e.getValue() == REMOVE) d.put(e.getKey(), NULL_VALUE);
				else d.put(e.getKey(), parseValue(e.getValue()));
			}
		} catch (FormatException e) {
			throw e;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return d;
	}

	@Override
	public BdfList parseToList(byte[] b) throws FormatException {
		ByteArrayInputStream in = new ByteArrayInputStream(b, 0, b.length);
		BdfReader reader = bdfReaderFactory.createReader(in);
		try {
			BdfList list = reader.readList();
			if (!reader.eof()) throw new FormatException();
			return list;
		} catch (FormatException e) {
			throw e;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Object parseValue(byte[] b) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(b);
		BdfReader reader = bdfReaderFactory.createReader(in);
		Object o = parseObject(reader);
		if (!reader.eof()) throw new FormatException();
		return o;
	}

	private Object parseObject(BdfReader reader) throws IOException {
		if (reader.hasNull()) return NULL_VALUE;
		if (reader.hasBoolean()) return reader.readBoolean();
		if (reader.hasLong()) return reader.readLong();
		if (reader.hasDouble()) return reader.readDouble();
		if (reader.hasString()) return reader.readString();
		if (reader.hasRaw()) return reader.readRaw();
		if (reader.hasList()) return reader.readList();
		if (reader.hasDictionary()) return reader.readDictionary();
		throw new FormatException();
	}
}
