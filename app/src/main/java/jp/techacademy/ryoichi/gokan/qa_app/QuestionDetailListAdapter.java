package jp.techacademy.ryoichi.gokan.qa_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import static jp.techacademy.ryoichi.gokan.qa_app.R.id.parent;

/**
 * Created by houxianliangyi on 2017/07/26.
 */

public class QuestionDetailListAdapter extends BaseAdapter implements DatabaseReference.CompletionListener {
    private final static int TYPE_QUESTION = 0;
    private final static int TYPE_ANSWER = 1;

    private LayoutInflater mLayoutInflater = null;
    private Question mQuestion;

    private ArrayList<String> data = new ArrayList<String>();

    private Button mFavoriteButton;


    public QuestionDetailListAdapter(Context context, Question question) {
        mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mQuestion = question;
    }

    @Override
    public int getCount() {
        return 1 + mQuestion.getAnswers().size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_QUESTION;
        } else {
            return TYPE_ANSWER;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public Object getItem(int position) {
        return mQuestion;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (getItemViewType(position) == TYPE_QUESTION) {
            // 質問欄
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.list_question_detail, parent, false);
            }
            String body = mQuestion.getBody();
            String name = mQuestion.getName();

            TextView bodyTextView = (TextView) convertView.findViewById(R.id.bodyTextView);
            bodyTextView.setText(body);

            TextView nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);
            nameTextView.setText(name);

            byte[] bytes = mQuestion.getImageBytes();
            if (bytes.length != 0) {
                Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length).copy(Bitmap.Config.ARGB_8888, true);
                ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
                imageView.setImageBitmap(image);
            }

            // お気に入りの列を取得する
            data = mQuestion.getFavorites();

            // お気に入りボタンの見た目on/offを設定する
            mFavoriteButton = convertView.findViewById(R.id.favoriteButton);

            if (mQuestion.getFavoriteFlag() == true) {
                mFavoriteButton.setText("★お気に入り");
            } else {
                mFavoriteButton.setText("☆お気に入り");
            }

            mFavoriteButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    Log.d("QAApp", "Favorite button was clicked");

                    DatabaseReference dataBaseReference = FirebaseDatabase.getInstance().getReference();
                    DatabaseReference favoriteRef = dataBaseReference.child(Const.ContentsPATH).child(String.valueOf(mQuestion.getGenre())).child(mQuestion.getQuestionUid()).child(Const.FavoritePATH);
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    // まだお気に入り登録をしていない場合はuidを登録する
                    if (mQuestion.getFavoriteFlag() == false ) {
                        // お気に入り登録がないときはどうしているのか
                        data.add(uid);
                        // お気に入りボタンのみためを変更する
                        mFavoriteButton.setText("★お気に入り");
                    } else {
                        // もうお気に入り登録済の場合は、uidを削除する
                        int index = -1;
                        for (String favoriteUid: data){
                            // 文字列と文字列の比較は==を使ってはならない
                            if (favoriteUid.equals(uid)) {
                                index = data.indexOf(favoriteUid);
                                break;
                            }
                        }
                        data.remove(index);
                        // お気に入りボタンのみためを変更する
                        mFavoriteButton.setText("☆お気に入り");
                    }
                    favoriteRef.setValue(data, QuestionDetailListAdapter.this);
                }
            });

        } else {
            // 回答欄
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.list_answer, parent, false);
            }

            Answer answer = mQuestion.getAnswers().get(position -1);
            String body = answer.getBody();
            String name = answer.getName();

            TextView bodyTextView = (TextView) convertView.findViewById(R.id.bodyTextView);
            bodyTextView.setText(body);

            TextView nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);
            nameTextView.setText(name);
        }

        return convertView;
    }

    @Override
    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

        if (databaseError == null) {
            Log.d("QAApp", "お気に入り登録に成功しました");

        } else {
            Log.d("QAApp", "お気に入り登録に失敗しました");

        }
    }
}
