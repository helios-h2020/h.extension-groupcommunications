package eu.h2020.helios_social.modules.groupcommunications.attachment;

import android.app.Application;
import android.app.DownloadManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.attachment.AttachmentManager;
import eu.h2020.helios_social.modules.groupcommunications.api.attachment.PendingAttachmentsMemory;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Attachment;
import io.tus.android.client.TusAndroidUpload;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;

public class AttachmentManagerImpl implements AttachmentManager {
    private static Logger LOG = Logger.getLogger(AttachmentManagerImpl.class.getName());

    private final DownloadManager downloadManager;
    private final TusClient tusClient;
    private final Application app;
    private final MimeTypeMap mimeTypeMap;
    private final ConcurrentHashMap<Long, String> pendingAttachments;

    @Inject
    public AttachmentManagerImpl(Application app,
                                 @PendingAttachmentsMemory ConcurrentHashMap<Long, String> pendingAttachments,
                                 DownloadManager downloadManager,
                                 TusClient tusClient) {
        this.downloadManager = downloadManager;
        this.pendingAttachments = pendingAttachments;
        this.tusClient = tusClient;
        this.app = app;
        this.mimeTypeMap = MimeTypeMap.getSingleton();
    }

    @Override
    public void uploadAttachments(List<Attachment> attachments) {
        for (Attachment attachment : attachments) {
            try {
                LOG.info("Start copying attachment to external app storage");
                File appFile = FileUtils.saveFileIntoExternalStorageByUri(
                        app.getApplicationContext(),
                        Uri.parse(attachment.getUri()),
                        mimeTypeMap.getExtensionFromMimeType(attachment.getContentType())
                );

                TusUpload upload = new TusAndroidUpload(Uri.parse(attachment.getUri()), app);
                attachment.setUri(Uri.fromFile(appFile).toString());

                TusUploader uploader = tusClient.resumeOrCreateUpload(upload);
                long totalBytes = upload.getSize();
                long uploadedBytes = uploader.getOffset();

                // Upload file in 1MiB chunks
                uploader.setChunkSize(1024 * 1024);

                while (uploader.uploadChunk() > 0) {
                    uploadedBytes = uploader.getOffset();
                }

                uploader.finish();
                attachment.setUrl(uploader.getUploadURL().toString());
                LOG.info("file: " + attachment.getUri() + " uploaded! Available in URL: " + uploader.getUploadURL().toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void downloadAttachments(String messageId, List<Attachment> attachments) {
        for (Attachment a : attachments) {
            String path = "/" + a.getUrl().replaceAll(".*/", "") + "." + mimeTypeMap.getExtensionFromMimeType(a.getContentType());
            Uri uri = Uri.parse(a.getUrl());
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setMimeType(a.getContentType())
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                    .setAllowedOverRoaming(false)
                    .setDestinationInExternalFilesDir(app.getApplicationContext(), "/data", path);


            long downloadId = downloadManager.enqueue(request);
            LOG.info("Download ID: " + downloadId);
            File file = new File(app.getExternalFilesDir("/data"), path);
            a.setUri(Uri.fromFile(file).toString());
            pendingAttachments.put(downloadId, messageId);
            LOG.info("Enqueued downloads: " + pendingAttachments);

        }
    }

}
