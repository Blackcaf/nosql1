package ru.library.repo;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.library.model.Order;
import ru.library.model.OrderStatus;

@Repository
public class OrderRepository {
  private final JdbcTemplate jdbc;

  public OrderRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private Order map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
    return new Order(
        rs.getString("id"),
        rs.getString("event_id"),
        rs.getString("reader_card"),
        rs.getString("reader_name"),
        rs.getInt("seats"),
        rs.getBigDecimal("total_price"),
        OrderStatus.valueOf(rs.getString("status")),
        rs.getString("manager_login"),
        rs.getTimestamp("created_at").toLocalDateTime());
  }

  public Optional<Order> find(String id) {
    return jdbc.query("SELECT * FROM orders WHERE id=?", (rs, n) -> map(rs, n), id).stream()
        .findFirst();
  }

  public List<Order> findAll() {
    return jdbc.query("SELECT * FROM orders ORDER BY created_at DESC", (rs, n) -> map(rs, n));
  }

  public List<Order> findByEvent(String eventId) {
    return jdbc.query(
        "SELECT * FROM orders WHERE event_id=? ORDER BY created_at DESC",
        (rs, n) -> map(rs, n),
        eventId);
  }

  public void save(Order o) {
    jdbc.update(
        "INSERT INTO"
            + " orders(id,event_id,reader_card,reader_name,seats,total_price,status,manager_login,created_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?)",
        o.id(),
        o.eventId(),
        o.readerCard(),
        o.readerName(),
        o.seats(),
        o.totalPrice(),
        o.status().name(),
        o.managerLogin(),
        Timestamp.valueOf(o.createdAt()));
  }

  public void updateStatus(String id, OrderStatus status) {
    jdbc.update("UPDATE orders SET status=? WHERE id=?", status.name(), id);
  }
}