package com.example.mediaplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

// setAction berfungsi untuk mengatur action yang akn digunakna pada service
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG= MainActivity.class.getSimpleName();
    private Button buttonPlay;
    private Button buttonStop;
    private MediaPlayer mediaPlayer= null;
    private boolean isReady;
    private Messenger serviceMessage= null;
    private Intent boundServiceIntent;
    private boolean serviceBound= false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonPlay= findViewById(R.id.button_play);
        buttonStop= findViewById(R.id.button_stop);
        buttonPlay.setOnClickListener(this);
        buttonStop.setOnClickListener(this);
        boundServiceIntent= new Intent(MainActivity.this, MediaService.class);
        // berfungsi membuat dan mengaitkan kelas service sehingga ketika di buat di service musik yang diputar tidak akan berpengaruh dengan mainactivity
        boundServiceIntent.setAction(MediaService.ACTION_CREATE);
        startService(boundServiceIntent);
        // berfungsi mengaitkan kelas service
        bindService(boundServiceIntent, serviceConnection, BIND_AUTO_CREATE);
//        init();
    }

    private ServiceConnection serviceConnection= new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceMessage= new Messenger(service);
            serviceBound= true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceMessage= null;
            serviceBound=false;
        }
    };

    @Override
    public void onClick(View v) {
        int id= v.getId();
        switch (id){
            // jika button play ditekan
            case R.id.button_play:
                if(!serviceBound) return;
                try {

                    // menagkap message yang dikirim dari service berupa interface onplay
                    serviceMessage.send(Message.obtain(null, MediaService.PLAY, 0, 0));
                }
                catch (RemoteException e){
                    e.printStackTrace();
                }
                break;

                // jika button stop ditekan
            case R.id.button_stop:
                if (!serviceBound)return;
                try {

                    // menangkap message yang dikirim service berupa interface onstop
                    serviceMessage.send(Message.obtain(null, MediaService.STOP, 0,0));
                }
                catch (RemoteException e){
                    e.printStackTrace();
                }
                break;
                default:
                    break;
        }
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        boundServiceIntent.setAction(MediaService.ACTION_DESTROY);
        startService(boundServiceIntent);
        super.onDestroy();
    }
}
