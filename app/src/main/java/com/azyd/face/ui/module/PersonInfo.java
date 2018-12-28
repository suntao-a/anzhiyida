package com.azyd.face.ui.module;

import com.azyd.face.base.IProguardKeeper;

/**
 * @author suntao
 * @creat-time 2018/12/25 on 16:11
 * $describe$
 */
public class PersonInfo implements IProguardKeeper {
    String id;
    String personRegioncode;
    int score;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPersonRegioncode() {
        return personRegioncode;
    }

    public void setPersonRegioncode(String personRegioncode) {
        this.personRegioncode = personRegioncode;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
