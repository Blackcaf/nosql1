package ru.library.web.dto;

public record OrderRequest(
    String eventId, String readerCard, String readerName, int seats, String draftId) {
  public OrderRequest(String eventId, String readerCard, String readerName, int seats) {
    this(eventId, readerCard, readerName, seats, null);
  }
}
