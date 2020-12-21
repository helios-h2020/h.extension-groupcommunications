package eu.h2020.helios_social.happ.helios.talk.db.lifecycle;

import eu.h2020.helios_social.happ.helios.talk.api.crypto.SecretKey;
import eu.h2020.helios_social.happ.helios.talk.api.db.DataTooNewException;
import eu.h2020.helios_social.happ.helios.talk.api.db.DataTooOldException;
import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.happ.helios.talk.api.db.MigrationListener;
import eu.h2020.helios_social.happ.helios.talk.api.event.EventBus;
import eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager;
import eu.h2020.helios_social.happ.helios.talk.api.lifecycle.Service;
import eu.h2020.helios_social.happ.helios.talk.api.lifecycle.ServiceException;
import eu.h2020.helios_social.happ.helios.talk.api.lifecycle.event.LifecycleEvent;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager.LifecycleState.COMPACTING_DATABASE;
import static eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager.LifecycleState.MIGRATING_DATABASE;
import static eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager.LifecycleState.RUNNING;
import static eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager.LifecycleState.STARTING;
import static eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager.LifecycleState.STARTING_SERVICES;
import static eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager.LifecycleState.STOPPING;
import static eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager.StartResult.ALREADY_RUNNING;
import static eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager.StartResult.DATA_TOO_NEW_ERROR;
import static eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager.StartResult.DATA_TOO_OLD_ERROR;
import static eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager.StartResult.DB_ERROR;
import static eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager.StartResult.SERVICE_ERROR;
import static eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager.StartResult.SUCCESS;
import static eu.h2020.helios_social.happ.helios.talk.api.util.LogUtils.logDuration;
import static eu.h2020.helios_social.happ.helios.talk.api.util.LogUtils.logException;
import static eu.h2020.helios_social.happ.helios.talk.api.util.LogUtils.now;

@ThreadSafe
@NotNullByDefault
class LifecycleManagerImpl implements LifecycleManager, MigrationListener {

	private static final Logger LOG =
			getLogger(LifecycleManagerImpl.class.getName());

	private final DatabaseComponent db;
	private final EventBus eventBus;
	private final List<Service> services;
	private final List<OpenDatabaseHook> openDatabaseHooks;
	private final List<ExecutorService> executors;
	private final Semaphore startStopSemaphore = new Semaphore(1);
	private final CountDownLatch dbLatch = new CountDownLatch(1);
	private final CountDownLatch startupLatch = new CountDownLatch(1);
	private final CountDownLatch shutdownLatch = new CountDownLatch(1);

	private volatile LifecycleState state = STARTING;

	@Inject
	LifecycleManagerImpl(DatabaseComponent db, EventBus eventBus) {
		this.db = db;
		this.eventBus = eventBus;
		services = new CopyOnWriteArrayList<>();
		openDatabaseHooks = new CopyOnWriteArrayList<>();
		executors = new CopyOnWriteArrayList<>();
	}

	@Override
	public void registerService(Service s) {
		if (LOG.isLoggable(INFO))
			LOG.info("Registering service " + s.getClass().getSimpleName());
		services.add(s);
	}

	@Override
	public void registerOpenDatabaseHook(OpenDatabaseHook hook) {
		if (LOG.isLoggable(INFO)) {
			LOG.info("Registering open database hook "
					+ hook.getClass().getSimpleName());
		}
		openDatabaseHooks.add(hook);
	}

	@Override
	public void registerForShutdown(ExecutorService e) {
		LOG.info("Registering executor " + e.getClass().getSimpleName());
		executors.add(e);
	}

	@Override
	public StartResult startServices(SecretKey dbKey) {
		if (!startStopSemaphore.tryAcquire()) {
			LOG.info("Already starting or stopping");
			return ALREADY_RUNNING;
		}
		try {
			LOG.info("Opening database");
			long start = now();
			boolean reopened = db.open(dbKey, this);
			if (reopened) logDuration(LOG, "Reopening database", start);
			else logDuration(LOG, "Creating database", start);

			db.transaction(false, txn -> {
				for (OpenDatabaseHook hook : openDatabaseHooks) {
					long start1 = now();
					hook.onDatabaseOpened(txn);
					if (LOG.isLoggable(FINE)) {
						logDuration(LOG, "Calling open database hook "
								+ hook.getClass().getSimpleName(), start1);
					}
				}
			});

			LOG.info("Starting services");
			state = STARTING_SERVICES;
			dbLatch.countDown();
			eventBus.broadcast(new LifecycleEvent(STARTING_SERVICES));

			for (Service s : services) {
				start = now();
				s.startService();
				if (LOG.isLoggable(FINE)) {
					logDuration(LOG, "Starting service "
							+ s.getClass().getSimpleName(), start);
				}
			}

			state = RUNNING;
			startupLatch.countDown();
			eventBus.broadcast(new LifecycleEvent(RUNNING));
			return SUCCESS;
		} catch (DataTooOldException e) {
			logException(LOG, WARNING, e);
			return DATA_TOO_OLD_ERROR;
		} catch (DataTooNewException e) {
			logException(LOG, WARNING, e);
			return DATA_TOO_NEW_ERROR;
		} catch (DbException e) {
			logException(LOG, WARNING, e);
			return DB_ERROR;
		} catch (ServiceException e) {
			logException(LOG, WARNING, e);
			return SERVICE_ERROR;
		} finally {
			startStopSemaphore.release();
		}
	}

	@Override
	public void onDatabaseMigration() {
		state = MIGRATING_DATABASE;
		eventBus.broadcast(new LifecycleEvent(MIGRATING_DATABASE));
	}

	@Override
	public void onDatabaseCompaction() {
		state = COMPACTING_DATABASE;
		eventBus.broadcast(new LifecycleEvent(COMPACTING_DATABASE));
	}

	@Override
	public void stopServices() {
		try {
			startStopSemaphore.acquire();
		} catch (InterruptedException e) {
			LOG.warning("Interrupted while waiting to stop services");
			return;
		}
		try {
			LOG.info("Stopping services");
			state = STOPPING;
			eventBus.broadcast(new LifecycleEvent(STOPPING));
			for (Service s : services) {
				long start = now();
				s.stopService();
				if (LOG.isLoggable(FINE)) {
					logDuration(LOG, "Stopping service "
							+ s.getClass().getSimpleName(), start);
				}
			}
			for (ExecutorService e : executors) {
				if (LOG.isLoggable(FINE)) {
					LOG.fine("Stopping executor "
							+ e.getClass().getSimpleName());
				}
				e.shutdownNow();
			}
			long start = now();
			db.close();
			logDuration(LOG, "Closing database", start);
			shutdownLatch.countDown();
		} catch (DbException | ServiceException e) {
			logException(LOG, WARNING, e);
		} finally {
			startStopSemaphore.release();
		}
	}

	@Override
	public void waitForDatabase() throws InterruptedException {
		dbLatch.await();
	}

	@Override
	public void waitForStartup() throws InterruptedException {
		startupLatch.await();
	}

	@Override
	public void waitForShutdown() throws InterruptedException {
		shutdownLatch.await();
	}

	@Override
	public LifecycleState getLifecycleState() {
		return state;
	}
}
