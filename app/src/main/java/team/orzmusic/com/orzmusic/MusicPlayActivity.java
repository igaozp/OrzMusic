package team.orzmusic.com.orzmusic;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MusicPlayActivity extends AppCompatActivity {
    // 全局变量
    Bitmap bitmap = null;
    Music music = new Music();
    ArrayList<Music> musics = new ArrayList<>();
    MediaPlayer mediaPlayer = new MediaPlayer();

    int duration = 0;
    int index = 0;
    int position = 0;
    int process;
    boolean isPlaying;
    int length;

    // 声明组件
    private ImageView mainArtWork;
    private TextView mainMusicName;
    private TextView mainArtist;
    private TextView mainAlbum;
    private Button mainMusicControl;
    private Button mainNext;
    private Button mainPrev;
    private SeekBar seekBarProcess;
    private TextView currentDuration;
    private TextView maxDuration;

    private MusicPlayActivity.MyBroadcastReceiver receiver;
    public static final String MUSIC_PLAYER = "music.player";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_play);

        // 获取组件
        mainArtWork = (ImageView) findViewById(R.id.mainArtWork);
        mainMusicName = (TextView) findViewById(R.id.mainMusicName);
        mainArtist = (TextView) findViewById(R.id.mainArtist);
        mainAlbum = (TextView) findViewById(R.id.mainAlbumName);
        mainMusicControl = (Button) findViewById(R.id.mainMusicControl);
        mainNext = (Button) findViewById(R.id.mainNext);
        mainPrev = (Button) findViewById(R.id.mainPrev);
        seekBarProcess = (SeekBar) findViewById(R.id.seekBarProcess);
        currentDuration = (TextView) findViewById(R.id.currentDuration);
        maxDuration = (TextView) findViewById(R.id.maxDuration);

        // 设置 ListView 的高度
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        mainArtWork.setMaxHeight(displayMetrics.widthPixels);

        // 扫描音乐
        scanMusic();
        length = musics.size();
        music = musics.get(position);

        // 注册广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MUSIC_PLAYER);
        receiver = new MusicPlayActivity.MyBroadcastReceiver();
        registerReceiver(receiver, intentFilter);

        // 同步进度条
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    seekBarProcess.setProgress(process);
                    seekBarProcess.setMax(duration);
//                    currentDuration.setText(music.getSimpleTime());
//                    maxDuration.setText(music.getSimpleTime());
                    System.out.println("Process----" + process);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        // 生成专辑封面Bitmap
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    bitmap = getBitmap(bitmap, MusicPlayActivity.this, 1024, music.getAlbumArtWorkUri());
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        // 设置进度条拖动方法
        seekBarProcess.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int process = seekBar.getProgress();
                Intent intent = new Intent(MusicPlayActivity.this, MusicService.class);
                intent.putExtra("Process", process);
                intent.putExtra("Status", 7);
                intent.putExtra("Position", position);
                startService(intent);
            }
        });
        // 控制音乐的播放&暂停
        mainMusicControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MusicPlayActivity.this, MusicService.class);
                if (isPlaying) {
                    mainMusicControl.setBackgroundResource(R.drawable.ic_play_circle_filled_black_36dp);
                    isPlaying = false;
                    intent.putExtra("Status", 5);
                    intent.putExtra("Position", position);
                    startService(intent);
                } else {
                    mainMusicControl.setBackgroundResource(R.drawable.ic_pause_circle_filled_black_36dp);
                    isPlaying = true;
                    intent.putExtra("Status", 6);
                    intent.putExtra("Position", position);
                    startService(intent);
                }
            }
        });
        // 上一首
        mainPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                position--;
                position %= length;
                Intent intent = new Intent(MusicPlayActivity.this, MusicService.class);
                intent.putExtra("Status", 4);
                intent.putExtra("Position", position);
                startService(intent);
            }
        });
        // 下一首
        mainNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                position++;
                position %= length;
                Intent intent = new Intent(MusicPlayActivity.this, MusicService.class);
                intent.putExtra("Status", 3);
                intent.putExtra("Position", position);
                startService(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    // 生成专辑封面位图
    public Bitmap getBitmap(Bitmap bitmap, Context context, int size, String path) {
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),
                    Uri.parse(path));
            bitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);
        } catch (FileNotFoundException exception) {
            exception.printStackTrace();
            bitmap = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_music_note_white_36pt_3x);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
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

    public void bindView(Music music, String path) {
        mainArtist.setText(music.getArtist());
        mainMusicName.setText(music.getMusicName());
        mainAlbum.setText(music.getAlbumName());
        mainArtWork.setImageBitmap(bitmap);
        currentDuration.setText(convertTime(process));
        maxDuration.setText(convertTime(duration));
    }

    // 广播接收器
    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            process = intent.getIntExtra("Process", 0);
            position = intent.getIntExtra("Position", 0);
            isPlaying = intent.getBooleanExtra("IsPlay", true);
            duration = intent.getIntExtra("Duration", 0);

            // 更新音乐和相关视图
            music = musics.get(position);
            bindView(music, music.getAlbumArtWorkUri());
        }
    }
}
