package jp.techacademy.ryoichi.gokan.qa_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private int mGenre = 0;

    private DatabaseReference mDatabaseReference;
    private DatabaseReference mGenreRef;
    private ListView mListView;
    private ArrayList<Question> mQuestionArrayList;
    private QuestionListAdapter mAdapter;

    // Firebaseのデータで追加・変更があったことをキャッチする
    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override // 質問が追加されたときの処理
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();
            Log.d("QA App", "ジャンル: " + String.valueOf(mGenre));

            // メニューでお気に入り以外を選択した場合
            if (mGenre != 5) {
                String title = (String) map.get("title");
                String body = (String) map.get("body");
                String name = (String) map.get("name");
                String uid = (String) map.get("uid");
                String imageString = (String) map.get("image");
                byte[] bytes;
                if (imageString != null) {
                    bytes = Base64.decode(imageString, Base64.DEFAULT);
                } else {
                    bytes = new byte[0];
                }

                ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
                HashMap answerMap = (HashMap) map.get("answers");
                if (answerMap != null) {
                    for (Object key : answerMap.keySet()) {
                        HashMap temp = (HashMap) answerMap.get((String) key);
                        String answerBody = (String) temp.get("body");
                        String answerName = (String) temp.get("name");
                        String answerUid = (String) temp.get("uid");
                        Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                        answerArrayList.add(answer);
                    }
                }

                // お気に入りの設定
                // 一旦お気に入りのリストを生成して
                // お気に入りデータを取得した時にnullではなかったら、FireBaseのデータをセットする
                ArrayList<String> favoritesArrayList = new ArrayList<String>();
                ArrayList<String> favorites = new ArrayList<String>();
                favorites = (ArrayList<String>) map.get("favorites");

                if (favorites != null) {
                    favoritesArrayList = favorites;
                }

                Question question = new Question(title, body, name, uid, dataSnapshot.getKey(), mGenre, bytes, answerArrayList, favoritesArrayList);
                mQuestionArrayList.add(question);
                mAdapter.notifyDataSetChanged();

            } else {
                // keyを取得する
                for (Object key : map.keySet()) {
                    String dataKey = key.toString();
                    // コンテンツを取得する
                    HashMap<String, String> hash = (HashMap<String, String>) map.get(dataKey);
                    Question question = setArrayData(dataKey, hash);

                    ArrayList<String> favoriteDatas = question.getFavorites();
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    for (String favorite : favoriteDatas) {
                        if (favorite.equals(uid)) {
                            mQuestionArrayList.add(question);
                            break;
                        }
                    }
                    mAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override // 質問に対して回答が投稿されたとき
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            // 変更があったQuestionを探す
            for (Question question: mQuestionArrayList) {
                if (dataSnapshot.getKey().equals(question.getQuestionUid())) {
                    // このアプリで変更がある可能性があるのは回答(Answer)のみ
                    // 回答のArrayListをいったんクリアして、取得した回答を設定する
                    question.getAnswers().clear();
                    HashMap answerMap = (HashMap) map.get("answers");
                    if (answerMap != null) {
                        for (Object key : answerMap.keySet()) {
                            HashMap temp = (HashMap) answerMap.get((String) key);
                            String answerBody = (String) temp.get("body");
                            String answerName = (String) temp.get("name");
                            String answerUid = (String) temp.get("uid");
                            Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                            question.getAnswers().add(answer);
                        }
                    }

                    // お気に入りが登録されていれば一旦クリアして、FireBaseのお気に入りデータに入れ替える
                    if (question.getFavorites() != null) {
                        question.getFavorites().clear();
                    }
                    ArrayList<String> favoritesArrayList = (ArrayList<String>) map.get("favorites");
                    question.setFavorites(favoritesArrayList);

                    mAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // ジャンルを選択していない場合(mGemre = 0)はエラーを表示するだけ
                if (mGenre == 0) {
                    Snackbar.make(view, "ジャンルを選択してください", Snackbar.LENGTH_LONG).show();

                    return;
                }

                // ログイン済のユーザーを取得する
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                // ログインしていなければログイン画面に遷移させる
                if (user == null) {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(getApplicationContext(), QuestionSendActivity.class);
                    intent.putExtra("genre", mGenre);
                    startActivity(intent);
                }
            }
        });

        // ナビゲーションドロワーの設定
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, mToolbar, R.string.app_name, R.string.app_name);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_hobby) {
                    mToolbar.setTitle("趣味");
                    mGenre = 1;
                } else if (id == R.id.nav_life) {
                    mToolbar.setTitle("生活");
                    mGenre = 2;
                } else if (id == R.id.nav_health) {
                    mToolbar.setTitle("健康");
                    mGenre = 3;
                } else if (id == R.id.nav_compter) {
                    mToolbar.setTitle("コンピューター");
                    mGenre = 4;
                } else if (id == R.id.nav_favorite) {
                    mToolbar.setTitle("お気に入り");
                    mGenre = 5;
                }

                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

                drawer.closeDrawer(GravityCompat.START);

                // 質問のリストをクリアして再度Adapterにセットし、AdapterをListViewにセットし直す
                mQuestionArrayList.clear();
                mAdapter.setQuestionArrayList(mQuestionArrayList);
                mListView.setAdapter(mAdapter);

                // 選択したジャンルにリスナーを登録する
                if (mGenreRef != null) {
                    mGenreRef.removeEventListener(mEventListener);
                }

                FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                if (mGenre != 5) {
                    // お気に入り以外を選んだ場合は、対象のジャンルの質問を取得する
                    mGenreRef = mDatabaseReference.child(Const.ContentsPATH).child(String.valueOf(mGenre));
                    fab.setVisibility(View.VISIBLE);
                } else {
                    // お気に入りを選んだ場合は、すべての質問を取得する
                    mGenreRef = mDatabaseReference.child(Const.ContentsPATH);
                    // お気に入りを選択した場合に質問投稿ボタンを使用不可にする
                    fab.setVisibility(View.INVISIBLE);
                }

                mGenreRef.addChildEventListener(mEventListener);

                return true;
            }

        });

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // ListViewの準備
        mListView = (ListView) findViewById(R.id.listView);
        mAdapter = new QuestionListAdapter(this);
        mQuestionArrayList = new ArrayList<Question>();
        mAdapter.notifyDataSetChanged();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Questionのインスタンスを渡して質問詳細画面を起動する
                Intent intent = new Intent(getApplicationContext(), QuestionDetailActivity.class);
                intent.putExtra("question", mQuestionArrayList.get(position));
                startActivity(intent);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Questionを返すようにしようと検討中　ジャンルを選ぶとtitle以下すべての値の取得ができていないため
    private Question setArrayData(String dataKey, HashMap map) {
        String title = (String) map.get("title");
        String body = (String) map.get("body");
        String name = (String) map.get("name");
        String uid = (String) map.get("uid");
        String imageString = (String) map.get("image");
        String genre = (String) map.get("genre");
        int genreInt = Integer.valueOf(genre);
        byte[] bytes;
        if (imageString != null) {
            bytes = Base64.decode(imageString, Base64.DEFAULT);
        } else {
            bytes = new byte[0];
        }

        ArrayList<Answer> answerArrayList = new ArrayList<Answer>();
        HashMap answerMap = (HashMap) map.get("answers");
        if (answerMap != null) {
            for (Object key : answerMap.keySet()) {
                HashMap temp = (HashMap) answerMap.get((String) key);
                String answerBody = (String) temp.get("body");
                String answerName = (String) temp.get("name");
                String answerUid = (String) temp.get("uid");
                Answer answer = new Answer(answerBody, answerName, answerUid, (String) key);
                answerArrayList.add(answer);
            }
        }

        ArrayList<String> favoritesArrayList = new ArrayList<String>();
        ArrayList<String> favorites = new ArrayList<String>();
        favorites = (ArrayList<String>) map.get("favorites");

        if (favorites != null) {
            favoritesArrayList = favorites;
        }

        Question question = new Question(title, body, name, uid, dataKey, genreInt, bytes, answerArrayList, favoritesArrayList);
        return question;
    }
}
