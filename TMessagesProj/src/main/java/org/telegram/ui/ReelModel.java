package org.telegram.ui;

public class ReelModel {
    public String id;
    public String author;
    public String caption;
    public String videoUrl;
    public int likeCount;
    public int commentCount;

    public ReelModel(String id, String author, String caption, String videoUrl, int likeCount, int commentCount) {
        this.id = id;
        this.author = author;
        this.caption = caption;
        this.videoUrl = videoUrl;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
    }
}
