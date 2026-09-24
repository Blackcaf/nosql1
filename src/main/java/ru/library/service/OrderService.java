package ru.library.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.library.exception.ConflictException;
import ru.library.exception.NotFoundException;
import ru.library.exception.ValidationException;
import ru.library.kv.Versioned;
import ru.library.model.*;
import ru.library.repo.EventRepository;
import ru.library.repo.OrderRepository;
import ru.library.web.dto.OrderRequest;
import ru.library.web.dto.PlaceOrderResult;

@Service
public class OrderService {
  private final EventRepository events;
  private final OrderRepository orders;
  private final AtomicLong conflicts = new AtomicLong();

  public OrderService(EventRepository events, OrderRepository orders) {
    this.events = events;
    this.orders = orders;
  }

  @Transactional
  public PlaceOrderResult placeOrder(OrderRequest req, String managerLogin) {
    if (req.seats() <= 0) throw new ValidationException("Количество мест должно быть > 0");

    Versioned<Event> v =
        events
            .findForUpdate(req.eventId())
            .orElseThrow(() -> new NotFoundException("Событие не найдено: " + req.eventId()));
    Event e = v.value();

    if (e.status() != EventStatus.OPEN)
      throw new ValidationException("Регистрация на событие закрыта");
    if (e.freeSeats() < req.seats())
      throw new ValidationException(
          "Недостаточно мест: доступно " + e.freeSeats() + ", запрошено " + req.seats());

    Event updated = e.withFreeSeats(e.freeSeats() - req.seats());
    Order order =
        new Order(
            UUID.randomUUID().toString(),
            e.id(),
            req.readerCard(),
            req.readerName(),
            req.seats(),
            e.price().multiply(BigDecimal.valueOf(req.seats())),
            OrderStatus.CONFIRMED,
            managerLogin,
            LocalDateTime.now());

    if (!events.updateIfUnchanged(updated, v.modRevision())) {
      conflicts.incrementAndGet();
      throw new ConflictException("Событие изменилось параллельно, повторите запрос");
    }
    orders.save(order);
    return new PlaceOrderResult(order, updated);
  }

  @Transactional
  public Order cancel(String orderId) {
    Order o =
        orders
            .find(orderId)
            .orElseThrow(() -> new NotFoundException("Заказ не найден: " + orderId));
    if (o.status() == OrderStatus.CANCELLED) return o;

    Versioned<Event> v =
        events
            .findForUpdate(o.eventId())
            .orElseThrow(() -> new NotFoundException("Событие не найдено: " + o.eventId()));
    Event updated = v.value().withFreeSeats(v.value().freeSeats() + o.seats());

    if (!events.updateIfUnchanged(updated, v.modRevision())) {
      conflicts.incrementAndGet();
      throw new ConflictException("Событие изменилось параллельно, повторите запрос");
    }
    orders.updateStatus(o.id(), OrderStatus.CANCELLED);
    return o.withStatus(OrderStatus.CANCELLED);
  }

  public Optional<Order> find(String id) {
    return orders.find(id);
  }

  public List<Order> all() {
    return orders.findAll();
  }

  public List<Order> byEvent(String eventId) {
    return orders.findByEvent(eventId);
  }

  public long conflicts() {
    return conflicts.get();
  }

  public void resetConflicts() {
    conflicts.set(0);
  }
}