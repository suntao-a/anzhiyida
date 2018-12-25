package com.azyd.face.ui.module;

import com.azyd.face.base.IProguardKeeper;
import com.azyd.face.base.ResponseBase;

import java.util.List;

/**
 * @author suntao
 * @creat-time 2018/12/25 on 16:07
 * $describe$
 */
public class Compare1nReponse extends ResponseBase implements IProguardKeeper {
    Content content;

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public class Content{
        List<PersonInfo> personInfo;
        String suspectedScore;
        String confirmScore;

        public List<PersonInfo> getPersonInfo() {
            return personInfo;
        }

        public void setPersonInfo(List<PersonInfo> personInfo) {
            this.personInfo = personInfo;
        }

        public String getSuspectedScore() {
            return suspectedScore;
        }

        public void setSuspectedScore(String suspectedScore) {
            this.suspectedScore = suspectedScore;
        }

        public String getConfirmScore() {
            return confirmScore;
        }

        public void setConfirmScore(String confirmScore) {
            this.confirmScore = confirmScore;
        }
    }
}
