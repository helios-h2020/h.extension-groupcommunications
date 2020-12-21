package eu.h2020.helios_social.happ.helios.talk.db.lifecycle;

import eu.h2020.helios_social.happ.helios.talk.api.lifecycle.ShutdownManager;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
@NotNullByDefault
class ShutdownManagerImpl implements ShutdownManager {

	protected final Lock lock = new ReentrantLock();

	// The following are locking: lock
	protected final Map<Integer, Thread> hooks;
	private int nextHandle = 0;

	ShutdownManagerImpl() {
		hooks = new HashMap<>();
	}

	@Override
	public int addShutdownHook(Runnable r) {
		lock.lock();
		try {
			int handle = nextHandle++;
			Thread hook = createThread(r);
			hooks.put(handle, hook);
			Runtime.getRuntime().addShutdownHook(hook);
			return handle;
		} finally {
			lock.unlock();
		}

	}

	protected Thread createThread(Runnable r) {
		return new Thread(r, "ShutdownManager");
	}

	@Override
	public boolean removeShutdownHook(int handle) {
		lock.lock();
		try {
			Thread hook = hooks.remove(handle);
			if (hook == null) return false;
			else return Runtime.getRuntime().removeShutdownHook(hook);
		} finally {
			lock.unlock();
		}

	}
}
