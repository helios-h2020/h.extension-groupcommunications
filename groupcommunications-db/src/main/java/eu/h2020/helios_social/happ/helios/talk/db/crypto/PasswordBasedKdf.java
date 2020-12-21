package eu.h2020.helios_social.happ.helios.talk.db.crypto;

import eu.h2020.helios_social.happ.helios.talk.api.crypto.SecretKey;

interface PasswordBasedKdf {

	int chooseCostParameter();

	SecretKey deriveKey(String password, byte[] salt, int cost);
}
