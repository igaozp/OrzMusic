package team.orzmusic.com.orzmusic;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    // 声明全局变量
    private ArrayList<Music> musics = new ArrayList<>();
    private Bitmap bitmap = null;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private int index = 0;
    private int length = 0;
    private MainBroadcastReceiver mainBroadcastReceiver;
    public static final String MAIN_ACTIVITY = "main.activity";
    private boolean PlayOrOn = false;
    private boolean flag = false;
    private boolean isPlay;
    private int positionMain = 0;
    private Intent intent;

    // 声明组件
    private ListView musicListView;
    private ImageView bottomArtWork;
    private TextView bottomMusicName;
    private TextView bottomArtist;
    private Button bottomControl;
    private Button bottomNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 实例化相关组件
        musicListView = (ListView) findViewById(R.id.musicListView);
        bottomArtWork = (ImageView) findViewById(R.id.bottomArtWork);
        bottomMusicName = (TextView) findViewById(R.id.bottomMusicName);
        bottomArtist = (TextView) findViewById(R.id.bottomArtist);
        bottomControl = (Button) findViewById(R.id.bottomControl);
        bottomNext = (Button) findViewById(R.id.bottomNext);

        // 启动时自动扫描音乐并显示
        scanMusic();
        length = musics.size();
        MusicListAdapter musicListAdapter = new MusicListAdapter(MainActivity.this, R.layout.music_item, musics);
        musicListView.setAdapter(musicListAdapter);

        // 注册广播
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MAIN_ACTIVITY);
        mainBroadcastReceiver = new MainBroadcastReceiver();
        registerReceiver(mainBroadcastReceiver, intentFilter);

        // 向Service传递歌曲列表
        intent = new Intent(MainActivity.this, MusicService.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("Musics", musics);
        intent.putExtra("Play?", false);
        intent.putExtra("First?", true);
        intent.putExtras(bundle);
        startService(intent);

        // 控制播放暂停
        bottomControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(MainActivity.this, MusicService.class);
                if (PlayOrOn) {
                    intent.putExtra("Status", 5);
                    intent.putExtra("Position", positionMain);
                    bottomControl.setBackgroundResource(R.drawable.ic_play_arrow_36pt_2x);
                    PlayOrOn = false;
                } else {
                    intent.putExtra("Status", 6);
                    intent.putExtra("Position", positionMain);
                    bottomControl.setBackgroundResource(R.drawable.ic_pause_black_36dp);
                    PlayOrOn = true;
                }
                startService(intent);
            }
        });

        // 设置BottomBar Next按钮事件
        bottomNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 更新音乐下标
                index++;
                index %= length;
                // 向Service发送音乐下标和播放状态
                intent = new Intent(MainActivity.this, MusicService.class);
                intent.putExtra("Position", index);
                intent.putExtra("Play?", true);
                startService(intent);
                // 更新视图
//                bindBottomView(index);
            }
        });

        // 设置ListView item点击事件
        musicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                index = position;
                PlayOrOn = true;
                //向Service传递当前播放歌曲下标
                intent = new Intent(MainActivity.this, MusicService.class);
                intent.putExtra("Position", position);
                intent.putExtra("Status", 0);
                intent.putExtra("Play?", true);

                startService(intent);

                // 更新BottomBar视图
//                bindBottomView(position);
            }
        });

        bottomArtWork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(MainActivity.this, MusicPlayActivity.class);
                Bundle bundle = new Bundle();
                Music music = musics.get(index);
                bundle.putSerializable("Music", music);
                bundle.putSerializable("Musics", musics);
                bundle.putInt("Index", index);
                bundle.putInt("Process", mediaPlayer.getCurrentPosition());
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        // 自带勿动
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mainBroadcastReceiver);
        stopService(intent);
    }

    public void bindBottomView(int position) {
        bitmap = getBitmap(bitmap, MainActivity.this, position, 256);
        bottomArtWork.setImageBitmap(bitmap);
        bottomMusicName.setText(musics.get(position).getMusicName());
        bottomArtist.setText(musics.get(position).getArtist());
    }

    // 扫描音乐
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

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_musicList) {

        } else if (id == R.id.nav_playing) {
            Intent intent = new Intent(MainActivity.this, MusicPlayActivity.class);
            System.out.println("尝试跳转");
            startActivity(intent);
            System.out.println("也许跳转成功");
        } else if (id == R.id.nav_theme) {
            Toast.makeText(getApplicationContext(), "没做", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_setting) {
            Toast.makeText(getApplicationContext(), "没做", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_exit) {
            System.exit(0);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // 自定义音乐列表适配器
    public class MusicListAdapter extends ArrayAdapter<Music> {
        public MusicListAdapter(Context context, int resource, List<Music> objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.music_item, parent, false);
            }

            Music music = getItem(position);

            bitmap = getBitmap(bitmap, this.getContext(), position, 256);

            TextView textViewMusicName = (TextView) itemView.findViewById(R.id.musicNameText);
            TextView textViewArtist = (TextView) itemView.findViewById(R.id.artistText);
            TextView textViewDuration = (TextView) itemView.findViewById(R.id.musicDuration);
            ImageView imageView = (ImageView) itemView.findViewById(R.id.albumArtView);

            textViewMusicName.setText(music.getMusicName() + "");
            textViewArtist.setText(music.getArtist() + "");
            textViewDuration.setText(music.getSimpleTime() + "");
            imageView.setImageBitmap(bitmap);

            return itemView;
        }
    }

    // 时间转换
    private String convertTime(int time) {
        Date date = new Date(time);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss");
        return simpleDateFormat.format(date);
    }

    // 生成专辑封面位图
    public Bitmap getBitmap(Bitmap bitmap, Context context, int position, int size) {
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),
                    Uri.parse(musics.get(position).getAlbumArtWorkUri()));
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

    // 广播接收器
    public class MainBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            positionMain = intent.getIntExtra("Position", 0);
            isPlay = intent.getBooleanExtra("IsPlay", true);
            System.out.println("MainActivity---" + "Position" + positionMain);
            bindBottomView(positionMain);
        }
    }
}
