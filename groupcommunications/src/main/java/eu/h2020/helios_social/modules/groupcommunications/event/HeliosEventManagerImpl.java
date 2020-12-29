package eu.h2020.helios_social.modules.groupcommunications.event;

import java.util.Collection;

import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications.api.event.HeliosEvent;
import eu.h2020.helios_social.modules.groupcommunications.api.event.HeliosEventManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;

public class HeliosEventManagerImpl implements HeliosEventManager {

    private final DatabaseComponent db;

    public HeliosEventManagerImpl(DatabaseComponent db) {
        this.db = db;
    }

    @Override
    public void addEvent(HeliosEvent event) throws DbException {
        Transaction txn = db.startTransaction(false);
        try {
            db.addEvent(txn, event);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public boolean containsEvent(String eventId) throws DbException {
        Transaction txn = db.startTransaction(false);
        boolean exists;
        try {
            exists = db.containsEvent(txn, eventId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return exists;
    }

    @Override
    public void removeEvent(String eventId) throws DbException {
        Transaction txn = db.startTransaction(false);
        try {
            db.removeEvent(txn, eventId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public HeliosEvent getEvent(String eventId) throws DbException {
        Transaction txn = db.startTransaction(true);
        HeliosEvent event;
        try {
            event = db.getEvent(txn, eventId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return event;
    }

    @Override
    public Collection<HeliosEvent> getEvents(String contextId) throws DbException {
        Transaction txn = db.startTransaction(true);
        Collection<HeliosEvent> events;
        try {
            events = db.getEvents(txn, contextId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return events;
    }
}
