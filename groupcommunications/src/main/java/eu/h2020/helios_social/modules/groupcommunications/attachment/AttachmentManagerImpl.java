package eu.h2020.helios_social.modules.groupcommunications.attachment;

import android.app.Application;
import android.app.DownloadManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.attachment.AttachmentManager;
import eu.h2020.helios_social.modules.groupcommunications.api.attachment.PendingAttachmentsMemory;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Attachment;
import io.tus.android.client.TusAndroidUpload;
import io.tus.java.client.ProtocolException;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusExecutor;
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
    public boolean validateAttachments(List<Attachment> attachments) {
        for (Attachment attachment : attachments) {
            try {
                InputStream inputStream = app.getContentResolver().openInputStream(Uri.parse(attachment.getUri()));
                inputStream.close();
            } catch (FileNotFoundException ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
                return false;
            } catch (IOException ioException) {
                LOG.log(Level.SEVERE, ioException.getMessage(), ioException);
                ioException.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public void storeOutgoingAttachmentsToExternalStorage(List<Attachment> attachments) {
        for (Attachment attachment : attachments) {
            try {
                LOG.info("Start copying attachment to external app storage");
                File appFile = FileUtils.saveFileIntoExternalStorageByUri(
                        app.getApplicationContext(),
                        Uri.parse(attachment.getUri()),
                        mimeTypeMap.getExtensionFromMimeType(attachment.getContentType())
                );

                attachment.setUri(Uri.fromFile(appFile).toString());
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void uploadAttachments(List<Attachment> attachments) {
        for (Attachment attachment : attachments) {
            try {
                File file = new File(Uri.parse(attachment.getUri()).getPath());
                final TusUpload upload = new TusUpload(file);
                TusExecutor executor = new TusExecutor() {
                    @Override
                    protected void makeAttempt() throws ProtocolException, IOException {
                        // First try to resume an upload. If that's not possible we will create a new
                        // upload and get a TusUploader in return. This class is responsible for opening
                        // a connection to the remote server and doing the uploading.
                        TusUploader uploader = tusClient.resumeOrCreateUpload(upload);

                        // Upload the file in chunks of 1KB sizes.
                        uploader.setChunkSize(1024 * 1024);

                        // Upload the file as long as data is available. Once the
                        // file has been fully uploaded the method will return -1
                        do {
                            // Calculate the progress using the total size of the uploading file and
                            // the current offset.
                            long totalBytes = upload.getSize();
                            long bytesUploaded = uploader.getOffset();
                            double progress = (double) bytesUploaded / totalBytes * 100;

                            System.out.printf("Upload at %06.2f%%.\n", progress);
                        } while (uploader.uploadChunk() > -1);

                        // Allow the HTTP connection to be closed and cleaned up
                        uploader.finish();

                        LOG.info("Upload finished.available at " + uploader.getUploadURL().toString());
                        attachment.setUrl(uploader.getUploadURL().toString());
                    }
                };
                executor.makeAttempts();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
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
