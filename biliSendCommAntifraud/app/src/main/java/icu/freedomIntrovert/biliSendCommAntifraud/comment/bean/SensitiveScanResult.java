package icu.freedomIntrovert.biliSendCommAntifraud.comment.bean;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;

public class SensitiveScanResult {
    public SensitiveScanResult(int normalPosition, int unusualPosition) {
        this.normalPosition = normalPosition;
        this.unusualPosition = unusualPosition;
    }

    public SensitiveScanResult() {
    }

    public int normalPosition;
    public int unusualPosition;

    @NonNull
    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
