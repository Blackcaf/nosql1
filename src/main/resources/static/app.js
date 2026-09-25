const $ = (id) => document.getElementById(id);
let auth = null;
let events = [];
let language = "ru";

const TEXT = {
  ru: {
    login: "Логин",
    password: "Пароль",
    loginTitle: "Вход менеджера",
    loginButton: "Войти",
    events: "События",
    orders: "Заказы",
    drafts: "Временные заявки",
    settings: "Настройки",
    upcoming: "Предстоящие события",
    refresh: "Обновить",
    order: "Оформить заказ",
    draft: "Временная заявка",
    noEvents: "Событий пока нет.",
    noOrders: "Оформленных заказов пока нет.",
    noDrafts: "Активных временных заявок нет.",
    seats: "мест",
    free: "Свободно",
    views: "просмотров",
    absent: "Описание отсутствует.",
    reader: "Читатель",
    event: "Событие",
    amount: "Сумма",
    status: "Статус",
    date: "Дата",
    actions: "Действия",
    cancel: "Отменить",
    extend: "Продлить",
    save: "Сохранить настройки",
    reset: "Сбросить",
    language: "Язык",
    theme: "Тема",
    light: "Светлая",
    dark: "Тёмная",
    confirm: "Подтвердить заказ",
    reserve: "Зарезервировать на 5 минут",
    card: "Номер читательского билета",
    name: "ФИО читателя",
    quantity: "Количество мест",
    closed: "Закрыть",
  },
  en: {
    login: "Login",
    password: "Password",
    loginTitle: "Manager sign in",
    loginButton: "Sign in",
    events: "Events",
    orders: "Orders",
    drafts: "Temporary requests",
    settings: "Settings",
    upcoming: "Upcoming events",
    refresh: "Refresh",
    order: "Place order",
    draft: "Temporary request",
    noEvents: "There are no events yet.",
    noOrders: "There are no orders yet.",
    noDrafts: "There are no active temporary requests.",
    seats: "seats",
    free: "Free",
    views: "views",
    absent: "No description.",
    reader: "Reader",
    event: "Event",
    amount: "Amount",
    status: "Status",
    date: "Date",
    actions: "Actions",
    cancel: "Cancel",
    extend: "Extend",
    save: "Save settings",
    reset: "Reset",
    language: "Language",
    theme: "Theme",
    light: "Light",
    dark: "Dark",
    confirm: "Confirm order",
    reserve: "Reserve for 5 minutes",
    card: "Library card number",
    name: "Reader name",
    quantity: "Number of seats",
    closed: "Close",
  },
};
const STATUS_EN = {
  OPEN: "Registration open",
  PLANNED: "Planned",
  CLOSED: "Registration closed",
  CANCELLED: "Cancelled",
  CONFIRMED: "Confirmed",
};
const statusLabel = (status) =>
  (language === "en" ? STATUS_EN[status] : STATUS_RU[status]) || status;
const t = (key) => TEXT[language][key] || key;

function setLanguage(value) {
  language = value === "en" ? "en" : "ru";
  document.documentElement.lang = language;
  $("login-panel").querySelector("h2").textContent = t("loginTitle");
  document.querySelector(".brand h1").textContent =
    language === "en" ? "Events and requests" : "События и заявки";
  $("login-form").querySelectorAll("label")[0].firstChild.textContent =
    t("login");
  $("login-form").querySelectorAll("label")[1].firstChild.textContent =
    t("password");
  $("login-form").querySelector("button").textContent = t("loginButton");
  document.querySelector('[data-section="events-section"]').textContent =
    t("events");
  document.querySelector('[data-section="orders-section"]').textContent =
    t("orders");
  document.querySelector('[data-section="drafts-section"]').textContent =
    t("drafts");
  document.querySelector('[data-section="settings-section"]').textContent =
    t("settings");
  document.querySelector("#events-section h2").textContent = t("upcoming");
  document.querySelector("#orders-section h2").textContent =
    language === "en" ? "Placed orders" : "Оформленные заказы";
  document.querySelector("#drafts-section h2").textContent =
    language === "en" ? "Temporary requests" : "Временные заявки";
  document
    .querySelectorAll(".section-heading .secondary")
    .forEach((button) => (button.textContent = t("refresh")));
  $("settings-section").querySelector("h2").textContent = t("settings");
  document.querySelector(".topbar .eyebrow").textContent =
    language === "en" ? "LIBRARY REQUESTS" : "LIBRARY REQUESTS";
  document.querySelector("#events-section .eyebrow").textContent =
    language === "en" ? "CATALOG" : "КАТАЛОГ";
  document.querySelector("#orders-section .eyebrow").textContent =
    language === "en" ? "MANAGER DESK" : "РАБОЧИЙ СТОЛ МЕНЕДЖЕРА";
  document.querySelector("#drafts-section .eyebrow").textContent =
    language === "en" ? "ETCD LEASE" : "АРЕНДА ETCD";
  document.querySelector("#settings-section .eyebrow").textContent =
    language === "en" ? "PROFILE" : "ПРОФИЛЬ";
  const labels = $("settings-form").querySelectorAll("label");
  labels[0].firstChild.textContent = t("language");
  labels[1].firstChild.textContent = t("theme");
  $("setting-theme").options[0].textContent = t("light");
  $("setting-theme").options[1].textContent = t("dark");
  $("settings-form").querySelector("button[type=submit]").textContent =
    t("save");
  $("reset-settings").textContent = t("reset");
  $("order-dialog").querySelector("h2").textContent = t("order");
  $("draft-dialog").querySelector("h2").textContent = t("draft");
  document
    .querySelectorAll(".dialog-form .close")
    .forEach((button) => button.setAttribute("aria-label", t("closed")));
  $("order-form").querySelector("button[type=submit]").textContent =
    t("confirm");
  $("draft-form").querySelector("button[type=submit]").textContent =
    t("reserve");
  document.querySelector(".account #logout").textContent =
    language === "en" ? "Log out" : "Выйти";
  document.querySelector("#order-dialog .eyebrow").textContent =
    language === "en" ? "NEW ORDER" : "НОВЫЙ ЗАКАЗ";
  document.querySelector("#draft-dialog .eyebrow").textContent =
    language === "en" ? "LEASE · 5 MINUTES" : "АРЕНДА · 5 МИНУТ";
  document.querySelectorAll(".dialog-form label")[0].firstChild.textContent =
    t("card");
  document.querySelectorAll(".dialog-form label")[1].firstChild.textContent =
    t("name");
  document.querySelectorAll(".dialog-form label")[2].firstChild.textContent =
    t("quantity");
  if (auth) {
    loadEvents().catch((e) => message(e.message, true));
  }
}

function message(text, error = false) {
  const el = $("notice");
  el.textContent = text;
  el.className = `notice${error ? " error" : ""}`;
  el.classList.remove("hidden");
  window.setTimeout(() => el.classList.add("hidden"), 5000);
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
      ...(auth ? { Authorization: `Basic ${auth}` } : {}),
    },
  });
  if (!response.ok) {
    let detail = `Ошибка ${response.status}`;
    try {
      detail = (await response.json()).message || detail;
    } catch (_) {}
    throw new Error(detail);
  }
  return response.status === 204 ? null : response.json();
}

function formatDate(value) {
  return value
    ? new Date(value).toLocaleString(language === "en" ? "en-US" : "ru-RU", {
        dateStyle: "medium",
        timeStyle: "short",
      })
    : "—";
}
function money(value) {
  return `${Number(value || 0).toLocaleString("ru-RU")} ₽`;
}
function empty(target, text) {
  $(target).innerHTML = `<p class="empty">${text}</p>`;
}
const EVENT_TRANSLATIONS = {
  "Встреча с автором: Евгений Водолазкин": "Author meeting: Evgeny Vodolazkin",
  "Лекция «История книгопечатания»": "Lecture: The history of printing",
  "Читательский клуб: Достоевский": "Reading club: Dostoevsky",
  "Презентация нового романа": "Presentation of a new novel",
  "Бесплатно, по записи": "Free, registration required",
  "Обсуждение «Идиота»": "Discussion of The Idiot",
};
function translateEvent(value) {
  return language === "en" ? EVENT_TRANSLATIONS[value] || value : value;
}

const STATUS_RU = {
  OPEN: "Открыта регистрация",
  PLANNED: "Запланировано",
  CLOSED: "Регистрация закрыта",
  CANCELLED: "Отменено",
  CONFIRMED: "Подтверждён",
};

function applyTheme(theme) {
  document.documentElement.dataset.theme = theme === "dark" ? "dark" : "light";
}

async function loadEvents() {
  events = await api("/api/events");
  const target = $("events");
  if (!events.length) return empty("events", t("noEvents"));
  target.innerHTML = events
    .map((view) => {
      const e = view.event;
      const pct = e.totalSeats
        ? Math.round((1 - e.freeSeats / e.totalSeats) * 100)
        : 0;
      return `<article class="card">
            <div class="card-top">
                <span class="badge ${e.status === "OPEN" ? "ok" : e.status === "CANCELLED" ? "danger" : ""}">${statusLabel(e.status)}</span>
                <span class="muted" style="font-size:12.5px" data-views="${e.id}">${view.views} ${t("views")}</span>
            </div>
            <h3>${translateEvent(e.title)}</h3>
            <div class="card-meta">${formatDate(e.dateTime)} · ${e.hall}</div>
            <div class="seat-bar" title="Занято ${pct}%"><span style="width:${pct}%"></span></div>
            <div class="card-meta">${t("free")} <strong>${e.freeSeats}</strong> ${t("seats")} ${e.totalSeats}</div>
            <p class="card-description">${translateEvent(e.description) || t("absent")}</p>
            <p class="price">${money(e.price)} <small class="muted">${language === "en" ? "per seat" : "за место"}</small></p>
            <div class="actions">
                <button onclick="openOrder('${e.id}')">${t("order")}</button>
                <button class="secondary" onclick="openDraft('${e.id}')">${t("draft")}</button>
            </div>
        </article>`;
    })
    .join("");
}

async function loadOrders() {
  const orders = await api("/api/orders");
  if (!orders.length) return empty("orders", t("noOrders"));
  $("orders").innerHTML =
    `<table><thead><tr><th>${t("reader")}</th><th>${t("event")}</th><th>${t("seats")}</th><th>${t("amount")}</th><th>${t("status")}</th><th>${t("date")}</th><th></th></tr></thead><tbody>
        ${orders.map((o) => `<tr><td>${o.readerName}<br><small class="muted">${o.readerCard}</small></td><td>${eventTitle(o.eventId)}</td><td>${o.seats}</td><td>${money(o.totalPrice)}</td><td><span class="pill ${o.status === "CONFIRMED" ? "ok" : "danger"}">${statusLabel(o.status)}</span></td><td>${formatDate(o.createdAt)}</td><td>${o.status === "CONFIRMED" ? `<button class="danger" onclick="cancelOrder('${o.id}')">${t("cancel")}</button>` : ""}</td></tr>`).join("")}
    </tbody></table>`;
}

async function loadDrafts() {
  const drafts = await api("/api/drafts");
  if (!drafts.length) return empty("drafts", t("noDrafts"));
  $("drafts").innerHTML =
    `<table><thead><tr><th>${t("reader")}</th><th>${t("event")}</th><th>${t("seats")}</th><th>${t("date")}</th><th>${t("actions")}</th></tr></thead><tbody>
        ${drafts.map((d) => `<tr><td>${d.readerName}<br><small class="muted">${d.readerCard}</small></td><td>${eventTitle(d.eventId)}</td><td>${d.seats}</td><td>${formatDate(d.createdAt)}</td><td><button class="secondary" onclick="extendDraft('${d.id}')">${t("extend")}</button> <button class="danger" onclick="cancelDraft('${d.id}')">${t("cancel")}</button></td></tr>`).join("")}
    </tbody></table>`;
}

function eventTitle(id) {
  return translateEvent(
    events.find((v) => v.event.id === id)?.event.title || id,
  );
}
async function cancelOrder(id) {
  if (!confirm("Отменить заказ?")) return;
  try {
    await api(`/api/orders/${id}/cancel`, { method: "POST" });
    message("Заказ отменён.");
    await Promise.all([loadOrders(), loadEvents()]);
  } catch (e) {
    message(e.message, true);
  }
}
async function extendDraft(id) {
  try {
    const result = await api(`/api/drafts/${id}/extend`, { method: "POST" });
    message(`Срок заявки продлён. Осталось секунд: ${result.ttl}`);
  } catch (e) {
    message(e.message, true);
  }
}
async function cancelDraft(id) {
  if (!confirm("Отменить временную заявку?")) return;
  try {
    await api(`/api/drafts/${id}`, { method: "DELETE" });
    message("Временная заявка отменена.");
    await loadDrafts();
  } catch (e) {
    message(e.message, true);
  }
}

function clearDialogError(id) {
  $(id).textContent = "";
  $(id).classList.add("hidden");
}
function dialogError(id, error) {
  $(id).textContent = error.message;
  $(id).classList.remove("hidden");
}
async function countView(id) {
  const view = await api(`/api/events/${id}`);
  const counter = document.querySelector(`[data-views="${id}"]`);
  if (counter) counter.textContent = `${view.views} ${t("views")}`;
}
async function openOrder(id) {
  const e = events.find((v) => v.event.id === id)?.event;
  clearDialogError("order-error");
  $("order-event-id").value = id;
  $("order-event").textContent =
    `${translateEvent(e.title)} · ${t("free").toLowerCase()}: ${e.freeSeats}`;
  $("order-dialog").showModal();
  try {
    await countView(id);
  } catch (error) {
    message(error.message, true);
  }
}
async function openDraft(id) {
  const e = events.find((v) => v.event.id === id)?.event;
  clearDialogError("draft-error");
  $("draft-event-id").value = id;
  $("draft-event").textContent = `${translateEvent(e.title)} · 5 min`;
  $("draft-dialog").showModal();
  try {
    await countView(id);
  } catch (error) {
    message(error.message, true);
  }
}

async function loadSettings() {
  const s = await api("/api/settings/me");
  $("setting-language").value = language;
  $("setting-theme").value = s.theme || "light";
}

async function enter(login, password) {
  auth = btoa(`${login}:${password}`);
  await api("/api/events");
  $("login-panel").classList.add("hidden");
  $("app").classList.remove("hidden");
  $("logout").classList.remove("hidden");
  $("current-user").textContent = login;
  await loadEvents();
}

$("login-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    await enter($("login").value, $("password").value);
    message("Вы вошли как менеджер.");
  } catch (e) {
    auth = null;
    message("Не удалось войти: проверьте логин и пароль.", true);
  }
});
$("logout").addEventListener("click", () => {
  auth = null;
  $("app").classList.add("hidden");
  $("login-panel").classList.remove("hidden");
  $("logout").classList.add("hidden");
  $("current-user").textContent = "Менеджер не авторизован";
  applyTheme("light");
});
$("refresh-events").addEventListener("click", () =>
  loadEvents().catch((e) => message(e.message, true)),
);
$("refresh-orders").addEventListener("click", () =>
  loadOrders().catch((e) => message(e.message, true)),
);
$("refresh-drafts").addEventListener("click", () =>
  loadDrafts().catch((e) => message(e.message, true)),
);
$("close-order").addEventListener("click", () => $("order-dialog").close());
$("close-draft").addEventListener("click", () => $("draft-dialog").close());
$("setting-language").addEventListener("change", (event) =>
  setLanguage(event.target.value),
);

$("order-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  clearDialogError("order-error");
  try {
    await api("/api/orders", {
      method: "POST",
      body: JSON.stringify({
        eventId: $("order-event-id").value,
        readerCard: $("reader-card").value,
        readerName: $("reader-name").value,
        seats: Number($("order-seats").value),
      }),
    });
    $("order-dialog").close();
    event.target.reset();
    message(
      language === "en"
        ? "Order placed successfully."
        : "Заказ успешно оформлен.",
    );
    await Promise.all([loadEvents(), loadOrders()]);
  } catch (e) {
    dialogError("order-error", e);
  }
});
$("draft-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  clearDialogError("draft-error");
  try {
    const result = await api("/api/drafts", {
      method: "POST",
      body: JSON.stringify({
        eventId: $("draft-event-id").value,
        readerCard: $("draft-card").value,
        readerName: $("draft-name").value,
        seats: Number($("draft-seats").value),
      }),
    });
    $("draft-dialog").close();
    event.target.reset();
    message(
      language === "en"
        ? `Request saved for ${result.ttlSeconds} seconds.`
        : `Заявка сохранена на ${result.ttlSeconds} секунд.`,
    );
    await loadDrafts();
  } catch (e) {
    dialogError("draft-error", e);
  }
});
$("settings-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const selectedLanguage = $("setting-language").value;
    const selectedTheme = $("setting-theme").value;
    await api("/api/settings/me", {
      method: "PUT",
      body: JSON.stringify({
        language: selectedLanguage,
        theme: selectedTheme,
      }),
    });
    applyTheme(selectedTheme);
    setLanguage(selectedLanguage);
    message(language === "en" ? "Settings saved." : "Настройки сохранены.");
  } catch (e) {
    message(e.message, true);
  }
});
$("reset-settings").addEventListener("click", async () => {
  try {
    await api("/api/settings/me", { method: "DELETE" });
    applyTheme("light");
    await loadSettings();
    message(language === "en" ? "Settings reset." : "Настройки сброшены.");
  } catch (e) {
    message(e.message, true);
  }
});
document.querySelectorAll(".tab").forEach((tab) =>
  tab.addEventListener("click", async () => {
    document
      .querySelectorAll(".tab")
      .forEach((t) => t.classList.remove("active"));
    tab.classList.add("active");
    document
      .querySelectorAll(".page-section")
      .forEach((s) => s.classList.add("hidden"));
    $(tab.dataset.section).classList.remove("hidden");
    try {
      if (tab.dataset.section === "orders-section") await loadOrders();
      if (tab.dataset.section === "drafts-section") await loadDrafts();
      if (tab.dataset.section === "settings-section") await loadSettings();
    } catch (e) {
      message(e.message, true);
    }
  }),
);
