package ru.library.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserSettings(String login, String language, String theme) {
  public static UserSettings defaults(String login) {
    return new UserSettings(login, "ru", "light");
  }
}
