package eu.h2020.helios_social.modules.groupcommunications;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.crypto.CryptoException;

import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.inject.Inject;

import eu.h2020.helios_social.core.messaging.HeliosConnectionInfo;
import eu.h2020.helios_social.core.messaging.HeliosIdentityInfo;
import eu.h2020.helios_social.core.messaging.HeliosMessage;
import eu.h2020.helios_social.core.messaging.HeliosMessageListener;
import eu.h2020.helios_social.core.messaging.HeliosMessagingException;
import eu.h2020.helios_social.core.messaging.HeliosTopic;
import eu.h2020.helios_social.core.messaging.ReliableHeliosMessagingNodejsLibp2pImpl;
import eu.h2020.helios_social.core.messaging.HeliosEgoTag;
import eu.h2020.helios_social.core.messaging.HeliosMessagingReceiver;
import eu.h2020.helios_social.core.messaging.HeliosNetworkAddress;
import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.Contact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupManager;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageAndKey;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;
import eu.h2020.helios_social.modules.groupcommunications.db.crypto.security.HeliosCryptoManager;
import eu.h2020.helios_social.modules.groupcommunications.messaging.GroupMessageListener;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications_utils.identity.Identity;
import eu.h2020.helios_social.modules.groupcommunications_utils.identity.IdentityManager;
import eu.h2020.helios_social.modules.groupcommunications_utils.lifecycle.LifecycleManager;
import eu.h2020.helios_social.modules.groupcommunications_utils.lifecycle.Service;
import eu.h2020.helios_social.modules.groupcommunications_utils.lifecycle.ServiceException;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.APP_TAG;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.PRIVATE_MESSAGE_PROTOCOL;
import static java.util.logging.Logger.getLogger;
public class ReliableCommunicationManagerImpl implements CommunicationManager<HeliosMessagingReceiver>, Service,
        LifecycleManager.OpenDatabaseHook {

    private static final Logger LOG =
            getLogger(ReliableCommunicationManagerImpl.class.getName());

    private ReliableHeliosMessagingNodejsLibp2pImpl heliosMessaging;
    private Context appContext;
    private HeliosConnectionInfo connectionInfo;
    private HeliosIdentityInfo identityInfo;
    private IdentityManager identityManager;
    private GroupManager groupManager;
    private ContactManager contactManager;
    private GroupMessageListener privateGroupMessageListener;
    private ArrayList<HeliosTopic> groups;
    private HashMap<String, HeliosMessagingReceiver> receivers;
    private HandlerThread handlerThread;
    private Handler handler;
    private Boolean needRestart = false;

    @Inject
    public ReliableCommunicationManagerImpl(Application app,
                                            IdentityManager identityManager,
                                            GroupManager groupManager,
                                            ContactManager contactManager,
                                            GroupMessageListener privateGroupMessageListener) {
        this.appContext = app.getApplicationContext();
        this.identityManager = identityManager;
        this.connectionInfo = getConnectionInfo();
        this.heliosMessaging = ReliableHeliosMessagingNodejsLibp2pImpl.getInstance();
        this.heliosMessaging.setHeartbeatInterval(30000);
        this.privateGroupMessageListener = privateGroupMessageListener;
        this.groupManager = groupManager;
        this.groups = new ArrayList();
        this.receivers = new HashMap();
        this.contactManager = contactManager;
        this.handlerThread = new HandlerThread("ReliableCommunicationManager");
        this.handlerThread.start();
        this.handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public void startService() throws ServiceException {
        heliosMessaging.setContext(appContext);
        heliosMessaging.setFilterJoinMsg(false);
        try {
            heliosMessaging.connect(connectionInfo, identityInfo);
        } catch (HeliosMessagingException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
        LOG.info("PEER ID: " + heliosMessaging.getPeerId());
        LOG.info("CURRENT PEER ID: " + identityManager.getIdentity().getNetworkId());

        if (identityManager.getIdentity().getNetworkId() == null) {
            try {
                identityManager.setNetworkId(heliosMessaging.getPeerId());
            } catch (DbException e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
                e.printStackTrace();
            }
        }

        for (Map.Entry<String, HeliosMessagingReceiver> receiver : receivers
                .entrySet()) {
            LOG.info(receiver.getValue().getClass().getSimpleName() +
                             " has been registered to Communication Manager");
            heliosMessaging.getDirectMessaging()
                    .addReceiver(receiver.getKey(), receiver.getValue());
        }
        //subscribe the user to a default topic to allow receiving offline messages
        try {
            heliosMessaging.subscribe(new HeliosTopic("helios-talk-app-topic", ""), new HeliosMessageListener() {
                @Override
                public void showMessage(HeliosTopic heliosTopic, HeliosMessage heliosMessage) {
                    LOG.info(heliosMessage.getMessage());
//                    try {
//                        Thread.sleep(500);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                }
            });
            //as soon as she has subscribe send a message to notify that she has arrived
            heliosMessaging.publish(new HeliosTopic("helios-talk-app-topic", ""), new HeliosMessage("{\"message\":\"join\"}"));
        } catch (HeliosMessagingException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }

        for (HeliosTopic groupTopic : groups) {
            LOG.info("Subscribed in: " + groupTopic.getTopicName() +
                             " topic.");
            try {
                heliosMessaging.subscribe(groupTopic, privateGroupMessageListener);
            } catch (HeliosMessagingException e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
                e.printStackTrace();
            }
        }

        LOG.info("Is helios peer connected? " + heliosMessaging.isConnected());

        sendOnlineStatusToAllContacts(30000);

        heliosMessaging.announceTag(APP_TAG);
        heliosMessaging.observeTag(APP_TAG);
    }

    @Override
    public void stopService() throws ServiceException {
        try {
            heliosMessaging.disconnect(connectionInfo, identityInfo);
        } catch (HeliosMessagingException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
        heliosMessaging.stop();
        handlerThread.quitSafely();
    }

    @Override
    public List<ContactId> getOnlineContacts(Collection<ContactId> contactIds) {
        List<HeliosNetworkAddress> peers = contactIds.stream().map(contactId -> {
            HeliosNetworkAddress heliosNetworkAddress = new HeliosNetworkAddress();
            heliosNetworkAddress.setNetworkId(contactId.getId());
            return heliosNetworkAddress;
        }).collect(Collectors.toList());

        List<HeliosEgoTag> onlineContacts = heliosMessaging.getCurrentOnlineStatus(new ArrayList(peers));
        return onlineContacts.stream().map(heliosEgo -> {
            return new ContactId(heliosEgo.getNetworkId());
        }).collect(Collectors.toList());
    }

    @Override
    public List<PeerId> getOnlinePeers(Collection<PeerId> peerIds) {
        List<HeliosNetworkAddress> peers = peerIds.stream().map(peerId -> {
            HeliosNetworkAddress heliosNetworkAddress = new HeliosNetworkAddress();
            heliosNetworkAddress.setNetworkId(peerId.getId());
            return heliosNetworkAddress;
        }).collect(Collectors.toList());

        List<HeliosEgoTag> onlineContacts = heliosMessaging.getCurrentOnlineStatus(new ArrayList(peers));
        return onlineContacts.stream().map(heliosEgo -> {
            return new PeerId(heliosEgo.getNetworkId());
        }).collect(Collectors.toList());
    }

    public void addReceiver(String protocolId,
                            HeliosMessagingReceiver messagingReceiver) {
        heliosMessaging.getDirectMessaging()
                .addReceiver(protocolId, messagingReceiver);
    }

    @Override
    public void sendDirectMessage(String protocolId, ContactId contactId,
                                  AbstractMessage message) {
        HeliosNetworkAddress heliosNetworkAddress = new HeliosNetworkAddress();
        heliosNetworkAddress.setNetworkId(contactId.getId());

        LOG.info(
                "send direct message!: " + heliosNetworkAddress.getNetworkId());
        try {
            if(protocolId.equals(PRIVATE_MESSAGE_PROTOCOL)){

                // get contact's public key
                Contact contact = contactManager.getContact(contactId);
                if (contact== null){
                    heliosMessaging.sendTo(
                            heliosNetworkAddress,
                            protocolId,
                            message.toJson().getBytes()
                    );
                    return;
                }
                PublicKey publicKey = contact.getPublicKey();
                HeliosCryptoManager heliosCryptoManager = HeliosCryptoManager.getInstance();

                LOG.info("messageAndKeyJson" + Arrays.toString(publicKey.getEncoded()));

                try {
                    // encrypt message
                    byte [] encryptedMessage = heliosCryptoManager.encryptMessage(publicKey,message).toJson().getBytes();
                    heliosMessaging.sendTo(heliosNetworkAddress, protocolId, encryptedMessage);
                } catch (CryptoException e) {
                    e.printStackTrace();
                    // do not encrpyt message, send it as it is.
                    // heliosMessaging.sendTo(heliosNetworkAddress, protocolId, message.toJson().getBytes());
                }

            } else{
                    heliosMessaging.sendTo(
                        heliosNetworkAddress,
                        protocolId,
                        message.toJson().getBytes()
                );
            }
        } catch (RuntimeException | DbException  e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Override
    public void sendDirectMessage(String protocolId, PeerId peerId,
                                  AbstractMessage message) {
        HeliosNetworkAddress heliosNetworkAddress = new HeliosNetworkAddress();
        heliosNetworkAddress.setNetworkId(peerId.getId());

        LOG.info(
                "send direct message to peer!: " +
                        heliosNetworkAddress.getNetworkId());
        try {
            heliosMessaging.sendTo(
                    heliosNetworkAddress,
                    protocolId,
                    message.toJson().getBytes()
            );
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Override
    public void sendGroupMessage(String groupId, String password,
                                 AbstractMessage message) {
        try {
            heliosMessaging.publish(
                    new HeliosTopic(groupId, password),
                    new HeliosMessage(message.toJson())
            );
        } catch (HeliosMessagingException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Override
    public void registerReceiver(String protocolId,
                                 HeliosMessagingReceiver receiver) {
        receivers.put(protocolId, receiver);
    }

    @Override
    public void announceTag(String tag) {
        heliosMessaging.announceTag(tag);
    }

    @Override
    public void observeTag(String tag) {
        heliosMessaging.observeTag(tag);
    }

    @Override
    public void unannounceTag(String tag) {
        heliosMessaging.unannounceTag(tag);
    }

    @Override
    public void unobserveTag(String tag) {
        heliosMessaging.unobserveTag(tag);
    }

    @Override
    public void subscribe(String groupId, String password) {
        try {
            heliosMessaging.subscribe(new HeliosTopic(groupId, password),
                                      privateGroupMessageListener);
        } catch (HeliosMessagingException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Override
    public void unsubscribe(String groupId, String password) {
        try {
            heliosMessaging.unsubscribe(new HeliosTopic(groupId, password));
        } catch (HeliosMessagingException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private HeliosConnectionInfo getConnectionInfo() {
        return new HeliosConnectionInfo();
    }

    @Override
    public void onDatabaseOpened(Transaction txn) throws DbException {
        Identity i = identityManager.getIdentity();
        this.identityInfo = new HeliosIdentityInfo(i.getAlias(), i.getId());
        LOG.info("identity info: \n" + i);

        try {
            Collection<Group> groupCollection = groupManager.getGroups(txn);
            for (Group group : groupCollection) {
                if (group instanceof PrivateGroup) {
                    PrivateGroup pg = (PrivateGroup) group;
                    groups.add(new HeliosTopic(group.getId(),
                                               pg.getPassword()));
                } else {
                    Forum f = (Forum) group;
                    groups.add(new HeliosTopic(group.getId(),
                                               f.getPassword()));
                }
            }
        } catch (FormatException e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Override
    public void sendOnlineStatusToAllContacts(long delay) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                List<HeliosNetworkAddress> addresses = null;
                try {
                    addresses = contactManager.getContacts()
                            .stream()
                            .map(contact -> {
                                HeliosNetworkAddress heliosNetworkAddress = new HeliosNetworkAddress();
                                heliosNetworkAddress.setNetworkId(contact.getId().getId());
                                return heliosNetworkAddress;
                            })
                            .collect(Collectors.toList());
                    LOG.info("Sending online status to addresses: " + addresses + " ...");
                    //heliosMessaging.sendOnlineStatusTo(addresses);
                    Iterator var2 = addresses.iterator();
                    while(var2.hasNext()) {
                        HeliosNetworkAddress address = (HeliosNetworkAddress)var2.next();
                        LOG.info("Sending online status to addresses: " + address + " ...");
                        heliosMessaging.sendOnlineStatusTo(address);
                        Thread.sleep(1000);
                    }
                } catch (DbException | InterruptedException e) {
                    LOG.log(Level.SEVERE, e.getMessage(), e);
                    e.printStackTrace();
                }
            }
        }, delay);
    }

    @Override
    public boolean isConnected() {
        return heliosMessaging.isConnected();
    }

    @Override
    public void stop() {
        try {
            stopService();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(){
        try {
            startService();
        } catch (ServiceException e) {
            e.printStackTrace();
        }
    }
    @Override
    public Boolean getNeedRestart() {
        return needRestart;
    }
    @Override
    public void setNeedRestart(Boolean needRestart) {
        this.needRestart = needRestart;
    }
}