package eu.h2020.helios_social.modules.groupcommunications.utils;

import java.io.File;

public class ContextualEgoNetworkConfig implements InternalStorageConfig {

	private final File storageDir;

	public ContextualEgoNetworkConfig(File storageDir) {
		this.storageDir = storageDir;
	}

	@Override
	public File getStorageDir() {
		return this.storageDir;
	}

}
