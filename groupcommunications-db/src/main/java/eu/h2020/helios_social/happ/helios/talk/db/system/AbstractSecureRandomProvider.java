package eu.h2020.helios_social.happ.helios.talk.db.system;

import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;
import eu.h2020.helios_social.happ.helios.talk.api.system.SecureRandomProvider;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;

import javax.annotation.concurrent.Immutable;

import static java.net.NetworkInterface.getNetworkInterfaces;
import static java.util.Collections.list;

@Immutable
@NotNullByDefault
public abstract class AbstractSecureRandomProvider implements SecureRandomProvider {

	// Contribute whatever slightly unpredictable info we have to the pool
	protected void writeToEntropyPool(DataOutputStream out) throws IOException {
		out.writeLong(System.currentTimeMillis());
		out.writeLong(System.nanoTime());
		out.writeLong(Runtime.getRuntime().freeMemory());
		Enumeration<NetworkInterface> ifaces = getNetworkInterfaces();
		if (ifaces != null) {
			for (NetworkInterface i : list(ifaces)) {
				for (InetAddress a : list(i.getInetAddresses()))
					out.write(a.getAddress());
				byte[] hardware = i.getHardwareAddress();
				if (hardware != null) out.write(hardware);
			}
		}
		for (Entry<String, String> e : System.getenv().entrySet()) {
			out.writeUTF(e.getKey());
			out.writeUTF(e.getValue());
		}
		Properties properties = System.getProperties();
		for (String key : properties.stringPropertyNames())
			out.writeUTF(properties.getProperty(key));
	}
}
