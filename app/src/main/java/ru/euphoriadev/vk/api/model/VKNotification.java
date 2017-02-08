package ru.euphoriadev.vk.api.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class VKNotification implements Serializable {

    public static final String FOLLOW = "follow";
    public static final String FRIEND_ACCEPTED = "friend_accepted";
    public static final String MENTION = "mention";
    public static final String MENTION_COMMENTS = "mention_comments";
    public static final String WALL = "wall";
    public static final String COMMENT_POST = "comment_post";
    public static final String COMMENT_PHOTO = "comment_photo";
    public static final String COMMENT_VIDEO = "comment_video";
    public static final String REPLY_COMMENT = "reply_comment";//wall
    public static final String REPLY_COMMENT_PHOTO = "reply_comment_photo";
    public static final String REPLY_COMMENT_VIDEO = "reply_comment_video";
    public static final String REPLY_TOPIC = "reply_topic";
    public static final String LIKE_POST = "like_post";
    public static final String LIKE_COMMENT = "like_comment";
    public static final String LIKE_COMMENT_PHOTO = "like_comment_photo";
    public static final String LIKE_COMMENT_VIDEO = "like_comment_video";
    public static final String LIKE_COMMENT_TOPIC = "like_comment_topic";
    public static final String LIKE_PHOTO = "like_photo";
    public static final String LIKE_VIDEO = "like_video";
    public static final String COPY_POST = "copy_post";
    public static final String COPY_PHOTO = "copy_photo";
    public static final String COPY_VIDEO = "copy_video";
    private static final long serialVersionUID = 1L;
    public String type;
    public Long date;
    public Object parent;
    public Object feedback;
    public Object reply;
    public VKPhoto photo;//for type reply_comment_photo
    public VKVideo video;//for type reply_comment_video

    public static VKNotification parse(JSONObject o) {
        VKNotification n = null;
        try {
            n = new VKNotification();
            n.type = o.getString("type");
            n.date = o.optLong("date");
            if (n.type.equals(FOLLOW)) {
                JSONObject jfeedback = o.optJSONObject("feedback");//profiles
                n.parent = null;//empty
                if (jfeedback != null)
                    n.feedback = getProfiles(jfeedback);
            } else if (n.type.equals(FRIEND_ACCEPTED)) {
                JSONObject jfeedback = o.optJSONObject("feedback");//profiles
                n.parent = null;//empty
                if (jfeedback != null)
                    n.feedback = getProfiles(jfeedback);
            } else if (n.type.equals(MENTION)) {
                JSONObject jfeedback = o.optJSONObject("feedback");//post
                n.parent = null;//empty
                if (jfeedback != null)
                    n.feedback = VKWallMessage.parseForNotifications(jfeedback);
            } else if (n.type.equals(MENTION_COMMENTS)) {
                JSONObject jparent = o.optJSONObject("parent"); //post
                JSONObject jfeedback = o.optJSONObject("feedback");//comment
                if (jparent != null)
                    n.parent = VKWallMessage.parse(jparent);
                if (jfeedback != null)
                    n.feedback = VKComment.parseNotificationComment(jfeedback, false);
            } else if (n.type.equals(WALL)) {
                JSONObject jfeedback = o.optJSONObject("feedback");//post
                if (jfeedback != null)
                    n.feedback = VKWallMessage.parseForNotifications(jfeedback);
            } else if (n.type.equals(COMMENT_POST)) {
                JSONObject jparent = o.optJSONObject("parent"); //post
                JSONObject jfeedback = o.optJSONObject("feedback");//comment
                if (jparent != null)
                    n.parent = VKWallMessage.parse(jparent);
                if (jfeedback != null)
                    n.feedback = VKComment.parseNotificationComment(jfeedback, false);
            } else if (n.type.equals(COMMENT_PHOTO)) {
                JSONObject jparent = o.optJSONObject("parent"); //photo
                JSONObject jfeedback = o.optJSONObject("feedback");//comment
                if (jparent != null)
                    n.parent = VKPhoto.parse(jparent);
                if (jfeedback != null)
                    n.feedback = VKComment.parseNotificationComment(jfeedback, false);
            } else if (n.type.equals(COMMENT_VIDEO)) {
                JSONObject jparent = o.optJSONObject("parent"); //video
                JSONObject jfeedback = o.optJSONObject("feedback");//comment
                if (jparent != null)
                    n.parent = VKVideo.parse(jparent);
                if (jfeedback != null)
                    n.feedback = VKComment.parseNotificationComment(jfeedback, false);
            } else if (n.type.equals(REPLY_COMMENT)) {
                JSONObject jparent = o.optJSONObject("parent"); //comment
                JSONObject jfeedback = o.optJSONObject("feedback");//comment
                if (jparent != null)
                    n.parent = VKComment.parseNotificationComment(jparent, true);
                if (jfeedback != null)
                    n.feedback = VKComment.parseNotificationComment(jfeedback, false);
            } else if (n.type.equals(REPLY_COMMENT_PHOTO)) {
                JSONObject jparent = o.optJSONObject("parent"); //comment
                JSONObject jfeedback = o.optJSONObject("feedback");//comment
                if (jparent != null) {
                    n.parent = VKComment.parseNotificationComment(jparent, false);
                    if (jparent.has("photo"))
                        n.photo = VKPhoto.parse(jparent.optJSONObject("photo"));
                }
                if (jfeedback != null)
                    n.feedback = VKComment.parseNotificationComment(jfeedback, false);
            } else if (n.type.equals(REPLY_COMMENT_VIDEO)) {
                JSONObject jparent = o.optJSONObject("parent"); //comment
                JSONObject jfeedback = o.optJSONObject("feedback");//comment
                if (jparent != null) {
                    n.parent = VKComment.parseNotificationComment(jparent, false);
                    if (jparent.has("video"))
                        n.video = VKVideo.parse(jparent.optJSONObject("video"));
                }
                if (jfeedback != null)
                    n.feedback = VKComment.parseNotificationComment(jfeedback, false);
            } else if (n.type.equals(REPLY_TOPIC)) {
                JSONObject jparent = o.optJSONObject("parent"); //topic
                JSONObject jfeedback = o.optJSONObject("feedback");//comment
                if (jparent != null)
                    n.parent = GroupTopic.parseForNotifications(jparent);
                if (jfeedback != null)
                    n.feedback = VKComment.parseNotificationComment(jfeedback, false);
            } else if (n.type.equals(LIKE_POST)) {
                JSONObject jparent = o.optJSONObject("parent"); //post
                JSONObject jfeedback = o.optJSONObject("feedback");//profiles
                if (jparent != null)
                    n.parent = VKWallMessage.parse(jparent);
                if (jfeedback != null)
                    n.feedback = getProfiles(jfeedback);
            } else if (n.type.equals(LIKE_COMMENT)) {
                JSONObject jparent = o.optJSONObject("parent"); //comment
                JSONObject jfeedback = o.optJSONObject("feedback");//profiles
                if (jparent != null)
                    n.parent = VKComment.parseNotificationComment(jparent, true);
                if (jfeedback != null)
                    n.feedback = getProfiles(jfeedback);
            } else if (n.type.equals(LIKE_COMMENT_PHOTO)) {
                JSONObject jparent = o.optJSONObject("parent"); //comment
                JSONObject jfeedback = o.optJSONObject("feedback");//profiles
                if (jparent != null) {
                    n.parent = VKComment.parseNotificationComment(jparent, false);
                    if (jparent.has("photo"))
                        n.photo = VKPhoto.parse(jparent.optJSONObject("photo"));
                }
                if (jfeedback != null)
                    n.feedback = getProfiles(jfeedback);
            } else if (n.type.equals(LIKE_COMMENT_VIDEO)) {
                JSONObject jparent = o.optJSONObject("parent"); //comment
                JSONObject jfeedback = o.optJSONObject("feedback");//profiles
                if (jparent != null) {
                    n.parent = VKComment.parseNotificationComment(jparent, false);
                    if (jparent.has("video"))
                        n.video = VKVideo.parse(jparent.optJSONObject("video"));
                }
                if (jfeedback != null)
                    n.feedback = getProfiles(jfeedback);
            } else if (n.type.equals(LIKE_COMMENT_TOPIC)) {
                JSONObject jparent = o.optJSONObject("parent"); //comment
                JSONObject jfeedback = o.optJSONObject("feedback");//profiles
                if (jparent != null) {
                    n.parent = VKComment.parseNotificationComment(jparent, false);
                    //TODO
                    //if(jparent.has("topic"))
                    //    n.xxx=Xxx.parseArray(jparent.optJSONObject("topic"));
                }
                if (jfeedback != null)
                    n.feedback = getProfiles(jfeedback);
            } else if (n.type.equals(LIKE_PHOTO)) {
                JSONObject jparent = o.optJSONObject("parent"); //photo
                JSONObject jfeedback = o.optJSONObject("feedback");//profiles
                if (jparent != null)
                    n.parent = VKPhoto.parse(jparent);
                if (jfeedback != null)
                    n.feedback = getProfiles(jfeedback);
            } else if (n.type.equals(LIKE_VIDEO)) {
                JSONObject jparent = o.optJSONObject("parent"); //video
                JSONObject jfeedback = o.optJSONObject("feedback");//profiles
                if (jparent != null)
                    n.parent = VKVideo.parse(jparent);
                if (jfeedback != null)
                    n.feedback = getProfiles(jfeedback);
            } else if (n.type.equals(COPY_POST)) {
                JSONObject jparent = o.optJSONObject("parent"); //wall
                JSONObject jfeedback = o.optJSONObject("feedback");//copy
                if (jparent != null)
                    n.parent = VKWallMessage.parse(jparent);
                if (jfeedback != null)
                    n.feedback = getCopies(jfeedback);
            } else if (n.type.equals(COPY_PHOTO)) {
                JSONObject jparent = o.optJSONObject("parent"); //photo
                JSONObject jfeedback = o.optJSONObject("feedback");//copy
                if (jparent != null)
                    n.parent = VKPhoto.parse(jparent);
                if (jfeedback != null)
                    n.feedback = getCopies(jfeedback);
            } else if (n.type.equals(COPY_VIDEO)) {
                JSONObject jparent = o.optJSONObject("parent"); //video
                JSONObject jfeedback = o.optJSONObject("feedback");//copy
                if (jparent != null)
                    n.parent = VKVideo.parse(jparent);
                if (jfeedback != null)
                    n.feedback = getCopies(jfeedback);
            }
            JSONObject jreply = o.optJSONObject("reply");
            if (jreply != null)
                n.reply = Reply.parse(jreply);
        } catch (JSONException ex) {
            ex.printStackTrace();
            return null;
        }
        return n;
    }

    public static ArrayList<VKNotification> parseNotifications(JSONArray jnotifications) throws JSONException {
        ArrayList<VKNotification> notifications = new ArrayList<VKNotification>();
        for (int i = 0; i < jnotifications.length(); i++) {
            if (!(jnotifications.get(i) instanceof JSONObject))
                continue;
            JSONObject jgroup = (JSONObject) jnotifications.get(i);
            VKNotification n = VKNotification.parse(jgroup);
            if (n != null)
                notifications.add(n);
        }
        return notifications;
    }

    public static ArrayList<Object> getProfiles(JSONObject jfeedback) throws JSONException {
        ArrayList<Object> ids = new ArrayList<Object>();
        JSONArray items = jfeedback.optJSONArray("items");
        if (items == null)
            return ids;
        for (int i = 0; i < items.length(); i++) {
            JSONObject j_id = items.optJSONObject(i);
            if (j_id != null)
                ids.add(j_id.optLong("from_id"));
        }
        return ids;
    }

    public static ArrayList<Object> getCopies(JSONObject jfeedback) throws JSONException {
        ArrayList<Object> ids = new ArrayList<Object>();
        JSONArray items = jfeedback.optJSONArray("items");
        if (items == null)
            return ids;
        for (int i = 0; i < items.length(); i++) {
            JSONObject j_id = items.optJSONObject(i);
            if (j_id == null)
                continue;
            Long id = j_id.optLong("id");
            Long owner_id = j_id.optLong("from_id");
            if (id != null && owner_id != null) {
                IdsPair c = new IdsPair();
                c.id = id;
                c.owner_id = owner_id;
                ids.add(c);
            }
        }
        return ids;
    }

}
