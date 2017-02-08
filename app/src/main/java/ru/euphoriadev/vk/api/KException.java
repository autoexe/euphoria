package ru.euphoriadev.vk.api;

@SuppressWarnings("serial")
public class KException extends Exception {
    public int error_code;
    public String url;
    //for captcha
    public String captcha_img;
    public String captcha_sid;
    //for "Validation required" error
    public String redirect_uri;

    KException(int code, String message, String url) {
        super(message);
        this.error_code = code;
        this.url = url;
    }
}
