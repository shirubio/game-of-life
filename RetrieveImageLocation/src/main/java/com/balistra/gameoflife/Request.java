package com.balistra.gameoflife;

public class Request {
    private String imageKey;

    public String getImageKey() {
        return imageKey;
    }

    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
    }

    @Override
    public String toString() {
        return "Request [imageKey=" + imageKey + "]";
    }
}
