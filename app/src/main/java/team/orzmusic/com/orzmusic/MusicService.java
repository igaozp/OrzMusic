package team.orzmusic.com.orzmusic;

import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.telecom.Call;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MusicService extends Service {
    MediaPlayer player = new MediaPlayer();
    Music music;
    ArrayList<Music> musics = new ArrayList<>();
    int position = 0;
    int STATUS = 0;
    int length = 0;
    boolean playOrNo = false;

    public MusicService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        scanMusic();
        player.reset();
        length = musics.size();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        // 获取音乐信息
        position = intent.getIntExtra("Position", 0);
        STATUS = intent.getIntExtra("Status", 0);
        playOrNo = intent.getBooleanExtra("Play?", false);
        final boolean firstOrOn = intent.getBooleanExtra("First?", false);

        music = musics.get(position);

        switch (STATUS) {
            case 0: // 默认
                if (playOrNo) {
                    if (player.isPlaying()) {
                        player.reset();
                        playMusic(musics.get(position).getMusicData());
                    } else {
                        playMusic(musics.get(position).getMusicData());
                    }
                }
                break;
            case 1: // 列表循环

                break;
            case 2: // 随机播放

                break;
            case 3: // 下一首
                player.reset();
                playMusic(musics.get(position).getMusicData());
                break;
            case 4: // 上一首
                player.reset();
                playMusic(musics.get(position).getMusicData());
                break;
            case 5: // 暂停
                player.pause();
                break;
            case 6: // 开始
                if (!player.isPlaying()) {
                    player.start();
                }
                break;
            case 7: // 拖动滚动条
                player.seekTo(intent.getIntExtra("Process", player.getCurrentPosition()));
                break;
        }

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                position++;
                position %= length;
                if (!firstOrOn) {
                    mp.reset();
                    try {
                        player.setDataSource(musics.get(position).getMusicData());
                        player.prepare();
                        player.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // 向主播放界面发送广播
        new Thread() {
            @Override
            public void run() {
                Intent intentPlayer = new Intent();
                intentPlayer.setAction(MusicPlayActivity.MUSIC_PLAYER);
                while (true) {
                    intentPlayer.putExtra("Process", player.getCurrentPosition());
                    intentPlayer.putExtra("Position", position);
                    intentPlayer.putExtra("Duration", music.getDuration());
                    intentPlayer.putExtra("IsPlay", player.isPlaying());
                    try {
                        SendServiceBroadCast(intentPlayer);
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        // 向开始界面发送广播
        new Thread() {
            @Override
            public void run() {
                Intent intentMain = new Intent();
                intentMain.setAction(MainActivity.MAIN_ACTIVITY);
                while (true) {
                    intentMain.putExtra("Position", position);
                    intentMain.putExtra("IsPlay", player.isPlaying());
                    try {
                        SendServiceBroadCast(intentMain);
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        if (player.isPlaying()) {
            player.stop();
        }
        player.release();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    public void playMusic(String path) {
        try {
            player.setDataSource(path);
            player.prepare();
            player.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void SendServiceBroadCast(Intent intent) throws InterruptedException {
        sendBroadcast(intent);
//        Toast.makeText(MusicService.this, "Sent! Did you receive?", Toast.LENGTH_SHORT).show();
        System.out.println("Sending for MusicPlayer");
    }

    public void SendMainActivityBroadCast(Intent intent) throws InterruptedException {
        sendBroadcast(intent);
        System.out.println("Sending for MainActivity");
    }

    public void scanMusic() {
        final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final String[] cursor_cols = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION
        };
        final String where = MediaStore.Audio.Media.IS_MUSIC + "=1";
        final Cursor cursor = this.getContentResolver().query(uri, cursor_cols, where, null, null);

        while (cursor.moveToNext()) {
            Music music = new Music();

            // 获取音乐信息
            Long ID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
            String artlist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            String albumName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            String musicName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String musicData = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
            int albumId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
            int duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
            Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
            String albumArtWorkUri = ContentUris.withAppendedId(sArtworkUri, albumId).toString();
            String simpleTime = convertTime(duration);

            // 设置音乐信息
            music.setID(ID);
            music.setMusicName(musicName);
            music.setArtist(artlist);
            music.setAlbumName(albumName);
            music.setDuration(duration);
            music.setAlbumID(albumId);
            music.setAlbumArtWorkUri(albumArtWorkUri);
            music.setSimpleTime(simpleTime);
            music.setMusicData(musicData);
            // 将音乐添加到音乐列表
            musics.add(music);
        }
    }

    private String convertTime(int time) {
        Date date = new Date(time);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss");
        return simpleDateFormat.format(date);
    }
}
