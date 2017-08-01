package jp.techacademy.ryoichi.gokan.qa_app;

import com.google.firebase.auth.FirebaseAuth;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by houxianliangyi on 2017/07/25.
 */

public class Question implements Serializable {
    private String mTitle;
    private String mBody;
    private String mName;
    private String mUid;
    private String mQuestionUid;
    private int mGenre;
    private byte[] mBitmapArray;
    private ArrayList<Answer> mAnswerArrayList;
    private ArrayList<String> mFavoritesArrayList; // お気に入り登録用
    private Boolean mFavoriteFlag = false;

    public String getTitle() {
        return mTitle;
    }

    public String getBody() {
        return mBody;
    }

    public String getName() {
        return mName;
    }

    public String getUid() {
        return mUid;
    }

    public String getQuestionUid() {
        return mQuestionUid;
    }

    public int getGenre() {
        return mGenre;
    }

    public byte[] getImageBytes() {
        return mBitmapArray;
    }

    public ArrayList<Answer> getAnswers() {
        return mAnswerArrayList;
    }

    public ArrayList<String> getmFavorites() { // お気に入り
        return mFavoritesArrayList;
    }

    public Boolean getmFavoriteFlag() {

        // お気に入りにしたユーザーの中に、自分自身のIDがあればお気に入り登録済と判定する
        for (String uid: mFavoritesArrayList){
            if (uid == FirebaseAuth.getInstance().getCurrentUser().getUid()) {
                mFavoriteFlag = true;
                break;
            }
        }
        return mFavoriteFlag;
    }

    public Question(String title, String body, String name, String uid, String questionUid, int genre, byte[] bytes, ArrayList<Answer> answers, ArrayList<String> favorites) {
        mTitle = title;
        mBody = body;
        mName = name;
        mUid = uid;
        mQuestionUid = questionUid;
        mGenre = genre;
        mBitmapArray = bytes.clone();
        mAnswerArrayList = answers;
        mFavoritesArrayList = favorites; // お気に入り
    }




}
