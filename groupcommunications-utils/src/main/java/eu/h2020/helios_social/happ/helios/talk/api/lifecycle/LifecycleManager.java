package eu.h2020.helios_social.happ.helios.talk.api.lifecycle;

import java.util.concurrent.ExecutorService;

import eu.h2020.helios_social.happ.helios.talk.api.crypto.SecretKey;
import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;

/**
 * Manages the lifecycle of the app: opening and closing the
 * {@link DatabaseComponent} starting and stopping {@link Service Services},
 * and shutting down {@link ExecutorService ExecutorServices}.
 */
@NotNullByDefault
public interface LifecycleManager {

	/**
	 * The result of calling {@link #startServices(SecretKey)}.
	 */
	enum StartResult {
		ALREADY_RUNNING,
		DB_ERROR,
		DATA_TOO_OLD_ERROR,
		DATA_TOO_NEW_ERROR,
		SERVICE_ERROR,
		SUCCESS
	}

	/**
	 * The state the lifecycle can be in.
	 * Returned by {@link #getLifecycleState()}
	 */
	enum LifecycleState {

		STARTING, MIGRATING_DATABASE, COMPACTING_DATABASE, STARTING_SERVICES,
		RUNNING, STOPPING;

		public boolean isAfter(LifecycleState state) {
			return ordinal() > state.ordinal();
		}
	}

	/**
	 * Registers a hook to be called after the database is opened and before
	 * {@link Service services} are started. This method should be called
	 * before {@link #startServices(SecretKey)}.
	 */
	void registerOpenDatabaseHook(OpenDatabaseHook hook);

	/**
	 * Registers a {@link Service} to be started and stopped. This method
	 * should be called before {@link #startServices(SecretKey)}.
	 */
	void registerService(Service s);

	/**
	 * Registers an {@link ExecutorService} to be shut down. This method
	 * should be called before {@link #startServices(SecretKey)}.
	 */
	void registerForShutdown(ExecutorService e);

	/**
	 * Opens the {@link DatabaseComponent} using the given key and starts any
	 * registered {@link Service Services}.
	 */
	StartResult startServices(SecretKey dbKey);

	/**
	 * Stops any registered {@link Service Services}, shuts down any
	 * registered {@link ExecutorService ExecutorServices}, and closes the
	 * {@link DatabaseComponent}.
	 */
	void stopServices();

	/**
	 * Waits for the {@link DatabaseComponent} to be opened before returning.
	 */
	void waitForDatabase() throws InterruptedException;

	/**
	 * Waits for the {@link DatabaseComponent} to be opened and all registered
	 * {@link Service Services} to start before returning.
	 */
	void waitForStartup() throws InterruptedException;

	/**
	 * Waits for all registered {@link Service Services} to stop, all
	 * registered {@link ExecutorService ExecutorServices} to shut down, and
	 * the {@link DatabaseComponent} to be closed before returning.
	 */
	void waitForShutdown() throws InterruptedException;

	/**
	 * Returns the current state of the lifecycle.
	 */
	LifecycleState getLifecycleState();

	interface OpenDatabaseHook {
		/**
		 * Called when the database is being opened, before
		 * {@link #waitForDatabase()} returns.
		 *
		 * @param txn A read-write transaction
		 */
		void onDatabaseOpened(Transaction txn) throws DbException;
	}
}
