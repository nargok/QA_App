package jp.techacademy.ryoichi.gokan.qa_app;

import java.io.Serializable;

/**
 * Created by houxianliangyi on 2017/07/25.
 */

public class Answer implements Serializable {
    private String mBody;
    private String mName;
    private String mUid;
    private String mAnswerUid;

    public Answer(String body, String name, String uid, String AnswerUid) {
        mBody = body;
        mName = name;
        mUid = uid;
        mAnswerUid = AnswerUid;
    }

    public String getmBody() {
        return mBody;
    }

    public String getmName() {
        return mName;
    }

    public String getmUid() {
        return mUid;
    }

    public String getmAnswerUid() {
        return mAnswerUid;
    }

}
