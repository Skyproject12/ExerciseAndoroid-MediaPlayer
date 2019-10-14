package com.example.mediaplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;


// alur dari proses adalah pengguna menekan button maka audio akan di baca menggunakan prepareAsync() tunggu sampai proses baca selesai metode start dijalankan pada onprepared
// media player start berfungsi untuk menstart media player ketika akan digunakan kembali
import java.io.IOException;
import java.lang.ref.WeakReference;


// service digunakan agar aplikasi saat ditutup lagu tetap berjalan, lalu pada saat di buka kembali akan mengikat data sebelumnnya
public class MediaService extends Service implements MediaPlayerCallback {
    private MediaPlayer mediaPlayer= null;
    private boolean isReady;
    public final static String ACTION_CREATE="com.dicoding.picodiploma.mysound.mediaservice.create";
    public final static String ACTION_DESTROY="com.dicoding.picodiploma.mysound.mediaservice.destroy";
    final String TAG= MediaService.class.getSimpleName();
    public final static int PLAY= 0;
    public final static int STOP=1;
    public MediaService() {
    }


    // ibinder giunaklan untuk mengikat data pada layanan
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    // onplay  method dari interface
    @Override
    public void onPlay() {
        // mengecek tingkat ready dari media player
        if (!isReady){
            // menyiapkan media player
            mediaPlayer.prepareAsync();
        }
        else {
            // ketika media player in play
            if (mediaPlayer.isPlaying()){
                mediaPlayer.pause();
                // show notif
                showNotif();
            }
            else {
                mediaPlayer.start();
            }
        }
    }


    // mmebantu dalam mengirim proses play or stop pada button
    private final Messenger messenger= new Messenger(new IncomingHandler(this));
    static class IncomingHandler extends Handler{
        private WeakReference<MediaPlayerCallback> mediaPlayerCallbackWeakReference;
        IncomingHandler(MediaPlayerCallback playerCallback){
            this.mediaPlayerCallbackWeakReference= new WeakReference<>(playerCallback);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                // if ststua PLAY
                // menangkap status yang dikirim dari mainactivity
                case PLAY:
                    // menangkap interface onPlay();
                    mediaPlayerCallbackWeakReference.get().onPlay();
                    break;
                // if status STOP
                case STOP:
                    // call interface onstop
                    mediaPlayerCallbackWeakReference.get().onStop();
                    break;
                    default:
                        super.handleMessage(msg);
            }

        }
    }

    // on stop method
    @Override
    public void onStop() {
        if (mediaPlayer.isPlaying() || isReady){
            mediaPlayer.stop();
            isReady= false;
            stopNotif();
        }
    }

    // start command
    @Override
    // mengecek commad mana yang akan di jalankan service
    public int onStartCommand(Intent intent, int flags, int startId) {
        // take action
        String action= intent.getAction();
        if(action!=null){
            switch (action){
                // make status action
                case ACTION_CREATE:
                    // if status action create and mediaplayer == null
                    if (mediaPlayer == null){
                        // akan menjalankan suatu media player
                        init();
                    }
                    break;
                    // if status action destroy and mediaplayer playing
                case ACTION_DESTROY:
                    if (!mediaPlayer.isPlaying()){
                        // akan mematikan media player
                        stopSelf();
                    }
                    break;
                    default:
                        break;
            }
        }
        return flags;
    }

    // initialization in android
    private void init() {
        mediaPlayer= new MediaPlayer();
        // set audio stream
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        // berfungsi mengambil musik yang akn digunakan
        AssetFileDescriptor afd= getApplicationContext().getResources().openRawResourceFd(R.raw.edsheerad);
        try {
            // set source
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());

        }
        catch (IOException e){
            e.printStackTrace();
        }
        // set prepared
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                isReady= true;
                mediaPlayer.start();
                showNotif();
            }
        });
        // set error
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });
    }

    // create notification
    void showNotif(){
        Intent notificationintent= new Intent(this, MainActivity.class);
        notificationintent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        PendingIntent pendingIntent= PendingIntent.getActivity(this, 0 , notificationintent, 0);
        String CHANNEL_DEFAULD_IMPORTANT="hannel_Test";
        int ONGOING_NOTIFICATION_ID=1;
        Notification notification= new NotificationCompat.Builder(this, CHANNEL_DEFAULD_IMPORTANT)
                .setContentTitle("Tes 1")
                .setContentText("Tes 2")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setTicker("Tes 3")
                .build();

        createChannel(CHANNEL_DEFAULD_IMPORTANT);
        // menjalankan sebuah foreground service dimana aplikasi akan berjalan pada foreground yang bergandengan dengan service
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    // cretae notification when version android o
    void createChannel(String CHANNEL_ID){
        NotificationManager notificationManager= (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel= new NotificationChannel(CHANNEL_ID, "Battery", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setShowBadge(false);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    void stopNotif(){
        // mematikan notif pada bagian foregrount atau yang sedang di jalankan di layar
        stopForeground(false);
    }
}
