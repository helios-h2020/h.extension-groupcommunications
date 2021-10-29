package eu.h2020.helios_social.modules.groupcommunications.mining;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.contentawareprofiling.ContentAwareProfileManager;
import eu.h2020.helios_social.modules.contentawareprofiling.Image;
import eu.h2020.helios_social.modules.contentawareprofiling.model.ModelType;
import eu.h2020.helios_social.modules.contentawareprofiling.profile.ContentAwareProfile;
import eu.h2020.helios_social.modules.groupcommunications.R;
//import eu.h2020.helios_social.modules.socialgraphmining.SwitchableMiner;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventBus;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.ProfilingStateEvent;
import eu.h2020.helios_social.modules.socialgraphmining.diffusion.PPRMiner;

import static java.util.logging.Logger.getLogger;

/**
 * ProfilingWorker is responsible for running the Profiling Module in the background. The profiling
 * is performed in batches of 500 pictures.
 */
public class ProfilingWorker extends Worker {
    private static String TAG = ProfilingWorker.class.getName();
    private final static Logger LOG = getLogger(TAG);

    private static final String MODEL = "MODEL";
    private static final String CHANNEL_ID = "PROFILING";
    private static final int NOTIFICATION_ID = 0;
    private ModelType modelType;

    private ContentAwareProfileManager profileManager;
    private Class<? extends ContentAwareProfile> profileClass;
    private PPRMiner pprMiner;
    //private EventBus eventBus;
    public ProfilingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams,
                           ContentAwareProfileManager profileManager/*, EventBus eventBus*/) {
        super(context, workerParams);
        try {
            this.modelType = ModelType.valueOf(workerParams.getInputData().getString(MODEL));
        } catch (NullPointerException ex) {
            this.modelType = ModelType.COARSE;
        }
        this.profileClass = ProfilingUtils.getProfileClass(modelType);
        this.profileManager = profileManager;
//        this.eventBus = eventBus;
    }

    @NonNull
    @Override
    public Result doWork() {
        ArrayList<Image> images = getImages();

        //execute profiling in batches of 500 pictures
        int it = images.size() / 500;
        LOG.info(modelType.name() + " Profiling is executed in batches of 500.");

        if (images.size() == 0) return Result.success();

        int progress = 0;
        //eventBus.broadcast(new ProfilingStateEvent("Analysing"));
        setForegroundAsync(createForegroundInfo(0, "Analyzing your collection of images"));
        for (int i = 0; i <= it; i++) {
            int endIndex = i == it ? images.size() : (i + 1) * 500;
            ArrayList<Image> imgs = new ArrayList(images.subList(i * 500, endIndex));
            try {
                profileManager.updateOrCreateProfile(profileClass, imgs);
            } catch (Exception ex) {
                Result.failure();
            }
            progress = (endIndex * 100) / images.size();

        }
        setForegroundAsync(createForegroundInfo(progress, "Analyzing your collection of images"));
        return Result.success();
    }

    @NonNull
    private ForegroundInfo createForegroundInfo(@NonNull int progress, String desc) {
        Context context = getApplicationContext();
        String title = context.getString(R.string.profiling_title);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(desc);
        }

        //show progress of the profiling as a notification
        NotificationCompat.Builder nbuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title + ": " + desc)
                .setSmallIcon(R.drawable.ic_profiler)
                .setPriority(NotificationManager.IMPORTANCE_HIGH);
        if (progress < 100) {
            nbuilder.setOngoing(true);
        } else {
            nbuilder.setContentText("completed").setOngoing(false);
            //eventBus.broadcast(new ProfilingStateEvent("Completed"));
        }

        Notification notification = nbuilder.build();

        return new ForegroundInfo(NOTIFICATION_ID, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(String desc) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getApplicationContext().getString(R.string.profiling_title);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(desc);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager =
                    getApplicationContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * @return the collection of images from the device
     */
    private ArrayList<Image> getImages() {
        //eventBus.broadcast(new ProfilingStateEvent("Collecting info"));
        setForegroundAsync(createForegroundInfo(0, "Collecting info for your collection of " +
                "images"));
        ArrayList<Image> images = new ArrayList();

        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN
        };
        String selection = MediaStore.Images.Media.HEIGHT +
                " >= ?";
        String[] selectionArgs = new String[]{"0"};
        String sortOrder = MediaStore.Video.Media.DISPLAY_NAME + " ASC";

        try (Cursor cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )) {
            // Cache column indices.
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            int dateTaken = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN);

            int totalImages = cursor.getCount();
            int count = 0;
            while (cursor.moveToNext()) {
                // Get values of columns for a given video.
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);

                Uri photoUri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id + "");

                try {
                    InputStream stream =
                            getApplicationContext().getContentResolver().openInputStream(photoUri);
                    float[] latlng = new float[2];
                    if (stream != null) {
                        ExifInterface exifInterface = new ExifInterface(stream);

                        exifInterface.getLatLong(latlng);
                        String date = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);

                        String date2 =
                                exifInterface.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED);
                    }

                    Image image = new Image(photoUri, name, new Long(dateTaken), latlng[1],
                                            latlng[0]);

                    images.add(image);
                    stream.close();

                } catch (FileNotFoundException f) {
                }
                count++;
            }
            setForegroundAsync(createForegroundInfo(100, "Collecting info for your collection of " +
                    "images"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return images;
    }
}
