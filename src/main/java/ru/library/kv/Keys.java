package ru.library.kv;

import org.springframework.stereotype.Component;
import ru.library.config.AppProperties;

@Component
public class Keys {
  private final String p;

  public Keys(AppProperties props) {
    this.p = props.kv().keyPrefix();
  }

  public String eventViews(String id) {
    return p + "/events/" + id + "/views";
  }

  public String drafts() {
    return p + "/drafts/";
  }

  public String draft(String id) {
    return p + "/drafts/" + id;
  }

  public String draftsByEvent(String e) {
    return p + "/idx/drafts-by-event/" + e + "/";
  }

  public String draftByEvent(String e, String d) {
    return draftsByEvent(e) + d;
  }

  public String usersPrefix() {
    return p + "/users/";
  }

  public String userSettings(String login) {
    return p + "/users/" + login + "/settings";
  }

  public static String lastSegment(String key) {
    return key.substring(key.lastIndexOf('/') + 1);
  }
}