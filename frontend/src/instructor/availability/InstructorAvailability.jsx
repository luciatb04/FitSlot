
import { useEffect, useMemo, useState } from "react";
import api from "../../services/api";
import "./InstructorAvailability.css";

const BASE_URL = "/instructors/me/availability";

const DAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

// Format a local date as YYYY-MM-DD
function toISODate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

// Parse without UTC timezone conversion
function fromISODate(value) {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day, 12);
}

function addDays(date, days) {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

function getMonday(date) {
  const result = new Date(date);
  const day = result.getDay();

  result.setDate(result.getDate() - ((day + 6) % 7));
  return result;
}

function getInitialWeek() {
  // Show next week initially
  return toISODate(addDays(getMonday(new Date()), 7));
}

function formatDate(value) {
  return fromISODate(value).toLocaleDateString("en-GB", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric"
  });
}

function formatShortDate(date) {
  return date.toLocaleDateString("en-GB", {
    day: "numeric",
    month: "short"
  });
}

function formatTime(time) {
  return time?.slice(0, 5) ?? "";
}

function getErrorMessage(error) {
  const status = error.response?.status;
  const message = error.response?.data?.message;

  if (status === 400) {
    return message || "Invalid availability period.";
  }

  if (status === 401) {
    return "Your session has expired. Please sign in again.";
  }

  if (status === 403) {
    return "You do not have permission to manage availability.";
  }

  if (status === 404) {
    return "Availability or instructor not found.";
  }

  if (status === 409) {
    return message ||
      "This time slot overlaps with an existing availability.";
  }

  return "An unexpected error occurred. Please try again.";
}

export default function AvailabilityInstructor() {

  const [weekStart, setWeekStart] = useState(getInitialWeek);

  const [form, setForm] = useState(() => ({
    date: getInitialWeek(),
    startTime: "",
    endTime: ""
  }));

  const [availabilities, setAvailabilities] = useState([]);
  const [editingId, setEditingId] = useState(null);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  const [error, setError] = useState("");
  const [loadError, setLoadError] = useState("");
  const [success, setSuccess] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  const busy = saving || deletingId !== null;

  // Calculate the seven dates of the selected week
  const weekDates = useMemo(() => {
    const monday = fromISODate(weekStart);

    return Array.from({ length: 7 }, (_, index) => {
      const date = addDays(monday, index);

      return {
        date: toISODate(date),
        day: DAYS[index],
        dayNumber: date.getDate(),
        fullDate: date
      };
    });
  }, [weekStart]);

  const weekLabel =
    `${formatShortDate(weekDates[0].fullDate)} – ` +
    `${formatShortDate(weekDates[6].fullDate)} ` +
    `${weekDates[6].fullDate.getFullYear()}`;

  // Retrieve availability for the selected week
  useEffect(() => {
    let active = true;

    async function loadAvailability() {
      setLoading(true);
      setLoadError("");

      try {
        const response = await api.get(BASE_URL, {
          params: { weekStart }
        });

        if (active) {
          if (!Array.isArray(response.data)) {
            throw new Error("Invalid availability response");
          }

          setAvailabilities(response.data);
        }
      } catch (requestError) {
        if (active) {
          setLoadError(getErrorMessage(requestError));
          setAvailabilities([]);
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadAvailability();

    return () => {
      active = false;
    };
  }, [weekStart, reloadKey]);

  // Group availability by exact date
  const slotsByDate = useMemo(() => {
    const result = {};

    weekDates.forEach(({ date }) => {
      result[date] = [];
    });

    availabilities.forEach((slot) => {
      if (result[slot.date]) {
        result[slot.date].push(slot);
      }
    });

    Object.values(result).forEach((slots) => {
      slots.sort((a, b) =>
        a.startTime.localeCompare(b.startTime)
      );
    });

    return result;
  }, [availabilities, weekDates]);

  const selectedSlots = slotsByDate[form.date] ?? [];

  // Navigate to another week
  function changeWeek(direction) {
    if (busy) return;

    const nextMonday = addDays(
      fromISODate(weekStart),
      direction * 7
    );

    const newWeek = toISODate(nextMonday);

    setWeekStart(newWeek);
    setForm({
      date: newWeek,
      startTime: "",
      endTime: ""
    });
    setEditingId(null);
    setError("");
    setSuccess("");
  }

  // Navigate to the current week
  function goToCurrentWeek() {
    if (busy) return;

    const today = new Date();

    setWeekStart(toISODate(getMonday(today)));
    setForm({
      date: toISODate(today),
      startTime: "",
      endTime: ""
    });

    setEditingId(null);
    setError("");
    setSuccess("");
  }

  // Select an exact date
  function selectDate(date) {
    if (busy) return;

    setForm((previous) => ({
      ...previous,
      date
    }));

    setError("");
    setSuccess("");
  }

  function handleChange(event) {
    const { name, value } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: value
    }));

    setError("");
    setSuccess("");
  }

  function validateForm() {
    if (!form.date || !form.startTime || !form.endTime) {
      return "All fields are required.";
    }

    if (form.startTime >= form.endTime) {
      return "End time must be later than start time.";
    }

    const overlapping = availabilities.some((slot) => {

      if (String(slot.id) === String(editingId)) {
        return false;
      }

      return (
        slot.date === form.date &&
        form.startTime < formatTime(slot.endTime) &&
        form.endTime > formatTime(slot.startTime)
      );
    });

    if (overlapping) {
      return "This time slot overlaps with an existing availability.";
    }

    return "";
  }

  // Create or update availability
  async function handleSubmit(event) {
    event.preventDefault();

    if (busy || loading || loadError) return;

    const validationError = validateForm();

    if (validationError) {
      setError(validationError);
      setSuccess("");
      return;
    }

    setSaving(true);
    setError("");
    setSuccess("");

    try {
      const payload = {
        date: form.date,
        startTime: form.startTime,
        endTime: form.endTime
      };

      if (editingId !== null) {

        await api.put(
          `${BASE_URL}/${editingId}`,
          payload
        );

        setSuccess("Availability updated successfully.");

      } else {

        await api.post(BASE_URL, payload);

        setSuccess("Availability added successfully.");
      }

      setEditingId(null);
      setForm({
        date: form.date,
        startTime: "",
        endTime: ""
      });

      // Retrieve the saved records from the database
      setReloadKey((key) => key + 1);

    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  // Edit a slot
  function handleEdit(slot) {
    if (busy) return;

    setEditingId(slot.id);

    setForm({
      date: slot.date,
      startTime: formatTime(slot.startTime),
      endTime: formatTime(slot.endTime)
    });

    setError("");
    setSuccess("");

    document.getElementById("availability-form")
      ?.scrollIntoView({
        behavior: "smooth",
        block: "center"
      });
  }

  function handleCancel() {
    setEditingId(null);
    setForm({
      date: form.date,
      startTime: "",
      endTime: ""
    });

    setError("");
    setSuccess("");
  }

  // Delete a slot
  async function handleDelete(id) {
    if (busy || loading) return;

    if (!window.confirm(
      "Are you sure you want to delete this availability?"
    )) {
      return;
    }

    setDeletingId(id);
    setError("");
    setSuccess("");

    try {
      await api.delete(`${BASE_URL}/${id}`);

      if (editingId === id) {
        setEditingId(null);
        setForm({
          date: form.date,
          startTime: "",
          endTime: ""
        });
      }

      setSuccess("Availability deleted successfully.");
      setReloadKey((key) => key + 1);

    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <main className="availability-page">
      <div className="availability-container">

        <header className="availability-header">
          <div>
            <p className="availability-eyebrow">
              INSTRUCTOR / AVAILABILITY
            </p>

            <h1>My Availability</h1>

            <p>
              Select a week and manage when you are available
              to teach classes.
            </p>
          </div>

          <button
            type="button"
            className="availability-button secondary"
            onClick={() => setReloadKey((key) => key + 1)}
            disabled={busy || loading}
          >
            Refresh
          </button>
        </header>

        {success && (
          <div
            className="availability-alert success"
            role="status"
          >
            {success}
          </div>
        )}

        {error && (
          <div
            className="availability-alert error"
            role="alert"
          >
            {error}
          </div>
        )}

        {loadError && (
          <div
            className="availability-alert error"
            role="alert"
          >
            {loadError}
            <button
              type="button"
              onClick={() => setReloadKey((key) => key + 1)}
            >
              Retry
            </button>
          </div>
        )}

        {/* Weekly calendar */}
        <section className="availability-card">

          <div className="availability-section-header">
            <h2>Weekly Availability</h2>
            <p>
              Choose a specific date to view or edit your availability.
            </p>
          </div>

          <div className="availability-week-navigation">
            <button
              type="button"
              className="availability-button secondary"
              onClick={() => changeWeek(-1)}
              disabled={busy}
            >
              ← Previous
            </button>

            <div className="availability-week-title">
              <strong>{weekLabel}</strong>

              <button
                type="button"
                onClick={goToCurrentWeek}
                disabled={busy}
              >
                This week
              </button>
            </div>

            <button
              type="button"
              className="availability-button secondary"
              onClick={() => changeWeek(1)}
              disabled={busy}
            >
              Next →
            </button>
          </div>

          <div className="availability-calendar-scroll">
            <div className="availability-calendar">
              {weekDates.map(({ date, day, dayNumber }) => {
                const selected = form.date === date;
                const count = (slotsByDate[date] ?? []).length;

                return (
                  <button
                    key={date}
                    type="button"
                    aria-pressed={selected}
                    className={
                      "availability-calendar-day" +
                      (selected ? " selected" : "")
                    }
                    onClick={() => selectDate(date)}
                    disabled={busy}
                  >
                    <span>{day}</span>
                    <strong>{dayNumber}</strong>
                    <small>
                      {count === 0
                        ? "No slots"
                        : `${count} slot${count === 1 ? "" : "s"}`}
                    </small>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Selected day's availability */}
          {!loading && !loadError && (
            <div className="availability-day-detail">

              <h3>{formatDate(form.date)}</h3>

              <p className="availability-date-note">
                Availability for this specific date only.
              </p>

              {selectedSlots.length === 0 ? (
                <p className="availability-empty">
                  You have not added any availability for this date.
                </p>
              ) : (
                selectedSlots.map((slot) => (
                  <div
                    className="availability-slot"
                    key={slot.id}
                  >
                    <div className="availability-slot-time">
                      {formatTime(slot.startTime)}
                      {" – "}
                      {formatTime(slot.endTime)}
                    </div>

                    <div className="availability-slot-actions">
                      <button
                        type="button"
                        onClick={() => handleEdit(slot)}
                        disabled={busy}
                      >
                        Edit
                      </button>

                      <button
                        type="button"
                        className="delete"
                        onClick={() => handleDelete(slot.id)}
                        disabled={busy}
                      >
                        {deletingId === slot.id
                          ? "Deleting..."
                          : "Delete"}
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {loading && (
            <p role="status">Loading weekly availability...</p>
          )}
        </section>

        {/* Availability form */}
        <section
          id="availability-form"
          className="availability-card"
        >
          <div className="availability-section-header">
            <h2>
              {editingId !== null
                ? "Edit Availability"
                : "Add Availability"}
            </h2>

            <p>
              {editingId !== null
                ? "Update the selected time slot."
                : "Set the hours you are available on the selected date."}
            </p>
          </div>

          <div className="availability-selected-date">
            <strong>{formatDate(form.date)}</strong>
            <p>
              Use the calendar above to change the date.
              {editingId !== null &&
                " You can also move this slot to another day of the selected week."}
            </p>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="availability-form-grid">
              <div className="availability-field">
                <label htmlFor="startTime">
                  Start time
                </label>

                <input
                  id="startTime"
                  type="time"
                  name="startTime"
                  value={form.startTime}
                  onChange={handleChange}
                  disabled={busy || loading || !!loadError}
                  required
                />
              </div>

              <div className="availability-field">
                <label htmlFor="endTime">
                  End time
                </label>

                <input
                  id="endTime"
                  type="time"
                  name="endTime"
                  value={form.endTime}
                  onChange={handleChange}
                  disabled={busy || loading || !!loadError}
                  required
                />
              </div>
            </div>

            <div className="availability-form-actions">
              {editingId !== null && (
                <button
                  type="button"
                  className="availability-button secondary"
                  onClick={handleCancel}
                  disabled={busy}
                >
                  Cancel
                </button>
              )}

              <button
                type="submit"
                className="availability-button primary"
                disabled={busy || loading || !!loadError}
              >
                {saving
                  ? "Saving..."
                  : editingId !== null
                    ? "Save Changes"
                    : "Add Availability"}
              </button>
            </div>
          </form>
        </section>

      </div>
    </main>
  );
}
