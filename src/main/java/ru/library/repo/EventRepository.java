package ru.library.repo;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.library.kv.Versioned;
import ru.library.model.Event;
import ru.library.model.EventStatus;
import ru.library.model.EventType;

@Repository
public class EventRepository {
  private final JdbcTemplate jdbc;

  public EventRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private Event map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
    return new Event(
        rs.getString("id"),
        rs.getString("title"),
        EventType.valueOf(rs.getString("type")),
        rs.getTimestamp("date_time").toLocalDateTime(),
        rs.getString("hall"),
        rs.getInt("total_seats"),
        rs.getInt("free_seats"),
        rs.getBigDecimal("price"),
        EventStatus.valueOf(rs.getString("status")),
        rs.getString("description"));
  }

  public void save(Event e) {
    jdbc.update(
        "INSERT INTO"
            + " events(id,title,type,date_time,hall,total_seats,free_seats,price,status,description,version)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,1)",
        e.id(),
        e.title(),
        e.type().name(),
        Timestamp.valueOf(e.dateTime()),
        e.hall(),
        e.totalSeats(),
        e.freeSeats(),
        e.price(),
        e.status().name(),
        e.description());
  }

  public Optional<Versioned<Event>> find(String id) {
    return jdbc
        .query(
            "SELECT * FROM events WHERE id=?",
            (rs, n) -> new Versioned<>(map(rs, n), rs.getLong("version"), rs.getLong("version"), 0),
            id)
        .stream()
        .findFirst();
  }

  public Optional<Versioned<Event>> findForUpdate(String id) {
    return jdbc
        .query(
            "SELECT * FROM events WHERE id=? FOR UPDATE",
            (rs, n) -> new Versioned<>(map(rs, n), rs.getLong("version"), rs.getLong("version"), 0),
            id)
        .stream()
        .findFirst();
  }

  public List<Versioned<Event>> findAll() {
    return jdbc.query(
        "SELECT * FROM events ORDER BY date_time",
        (rs, n) -> new Versioned<>(map(rs, n), rs.getLong("version"), rs.getLong("version"), 0));
  }

  public boolean updateIfUnchanged(Event e, long expectedRevision) {
    return jdbc.update(
            "UPDATE events SET"
                + " title=?,type=?,date_time=?,hall=?,total_seats=?,free_seats=?,price=?,status=?,description=?,version=version+1"
                + " WHERE id=? AND version=?",
            e.title(),
            e.type().name(),
            Timestamp.valueOf(e.dateTime()),
            e.hall(),
            e.totalSeats(),
            e.freeSeats(),
            e.price(),
            e.status().name(),
            e.description(),
            e.id(),
            expectedRevision)
        == 1;
  }

  public void delete(String id) {
    jdbc.update("DELETE FROM events WHERE id=?", id);
  }
}