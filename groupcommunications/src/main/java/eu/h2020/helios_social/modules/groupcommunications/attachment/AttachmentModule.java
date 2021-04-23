package eu.h2020.helios_social.modules.groupcommunications.attachment;


import android.app.Application;
import android.app.DownloadManager;
import android.content.IntentFilter;

import com.google.common.cache.Cache;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.modules.groupcommunications.api.attachment.AttachmentManager;
import eu.h2020.helios_social.modules.groupcommunications.api.attachment.PendingAttachmentsMemory;

import static android.content.Context.DOWNLOAD_SERVICE;

@Module
public class AttachmentModule {

    private final ConcurrentHashMap<Long, String> pendingAttachments;

    public AttachmentModule() {
        pendingAttachments = new ConcurrentHashMap<>();
    }

    @Provides
    @Singleton
    AttachmentManager providesAttachmentManager(AttachmentManagerImpl attachmentManager,
                                                Application application,
                                                DownloadActionsReceiver downloadActionsReceiver) {
        application.registerReceiver(
                downloadActionsReceiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        );
        return attachmentManager;
    }

    @Provides
    @Singleton
    DownloadManager providesDownloadManager(Application application) {
        return (DownloadManager) application.getSystemService(DOWNLOAD_SERVICE);
    }

    @Provides
    @Singleton
    @PendingAttachmentsMemory
    ConcurrentHashMap<Long, String> providesPendingAttachmentsMemory() {
        return pendingAttachments;
    }
}
