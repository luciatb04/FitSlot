import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import "./ClassSession.css";
import {
  getClassSession,
  updateClassSession
} from "../../services/classSessionService";
import "../../receptionist/members/memberRegistration.css";

const ROOMS = [
  { value: "WEIGHT_ROOM", label: "Weight Room" },
  { value: "CLASS_ROOM_1", label: "Class Room 1" },
  { value: "CLASS_ROOM_2", label: "Class Room 2" },
  { value: "POOL", label: "Swimming Pool" }
];

const INITIAL_FORM = {
  date: "",
  startTime: "",
  endTime: "",
  room: ""
};

function formatTime(value) {
  return value?.slice(0, 5) ?? "";
}

function roomLabel(value) {
  return ROOMS.find((room) => room.value === value)?.label
    ?? value
    ?? "Not assigned";
}

function getErrorMessage(error) {
  const status = error.response?.status;

  if (status === 400) {
    return "Invalid schedule. Please check the entered information.";
  }

  if (status === 401) {
    return "Your session has expired. Please sign in again.";
  }

  if (status === 403) {
    return "You do not have permission to edit this class.";
  }

  if (status === 404) {
    return "The selected class no longer exists.";
  }

  if (status === 409) {
    return "Scheduling conflict: the selected room is already occupied during this time.";
  }

  return "Unable to save changes. Please try again.";
}

export default function EditClassSession() {
  const { id } = useParams();

  const [session, setSession] = useState(null);
  const [form, setForm] = useState(INITIAL_FORM);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [validationError, setValidationError] = useState("");

  useEffect(() => {
    let active = true;

    async function loadSession() {
      setLoading(true);
      setError("");
      setSuccess("");

      try {
        const data = await getClassSession(id);

        if (!active) return;

        setSession(data);

        setForm({
          date: data.date ?? "",
          startTime: formatTime(data.startTime),
          endTime: formatTime(data.endTime),
          room: data.room ?? ""
        });
      } catch (requestError) {
        if (active) {
          setError(getErrorMessage(requestError));
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadSession();

    return () => {
      active = false;
    };
  }, [id]);

  function handleChange(event) {
    const { name, value } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: value
    }));

    setValidationError("");
    setError("");
    setSuccess("");
  }

  function validateForm() {
    if (
      !form.date ||
      !form.startTime ||
      !form.endTime ||
      !form.room
    ) {
      return "All fields are required.";
    }

    if (form.startTime >= form.endTime) {
      return "End time must be later than start time.";
    }

    return "";
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (saving) return;

    const validationMessage = validateForm();

    if (validationMessage) {
      setValidationError(validationMessage);
      return;
    }

    setSaving(true);
    setError("");
    setSuccess("");
    setValidationError("");

    try {
      const updatedSchedule = {
        date: form.date,
        startTime: form.startTime,
        endTime: form.endTime,
        room: form.room
      };

      await updateClassSession(id, updatedSchedule);

      setSession((previous) => ({
        ...previous,
        ...updatedSchedule
      }));

      setSuccess("Class schedule updated successfully.");
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <main className="fm-page">
        <div className="fm-container">
          <p className="fm-empty" role="status">
            Loading class information...
          </p>
        </div>
      </main>
    );
  }

  if (!session) {
    return (
      <main className="fm-page">
        <div className="fm-container">
          <div className="fm-alert fm-alert-error" role="alert">
            {error || "Class not found."}
          </div>

          <Link
            to="/manage-classes"
            className="fm-button fm-button-secondary"
          >
            Back to classes
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className="fm-page">
      <div className="fm-container">

        <header className="fm-page-header">
          <div>
            <p className="fm-eyebrow">
              GYM OWNER / CLASSES
            </p>

            <h1>Edit Class Schedule</h1>

            <p className="fm-subtitle">
              Update the date, time and room of a scheduled class.
            </p>
          </div>

          <Link
            to="/manage-classes"
            className="fm-button fm-button-secondary"
          >
            Back to classes
          </Link>
        </header>

        <section
          className="fm-card"
          aria-label="Current class information"
        >
          <div className="fm-card-top">
            <div>
              <h2>Current class information</h2>
              <p className="fm-muted">
                Review the existing schedule before making changes.
              </p>
            </div>
          </div>

          <div className="class-info-grid">
            <div>
              <strong>Class</strong>
              <p>{session.name}</p>
            </div>

            <div>
              <strong>Class ID</strong>
              <p>#{session.id}</p>
            </div>

            <div>
              <strong>Date</strong>
              <p>{session.date}</p>
            </div>

            <div>
              <strong>Time</strong>
              <p>
                {formatTime(session.startTime)}
                {" - "}
                {formatTime(session.endTime)}
              </p>
            </div>

            <div>
              <strong>Room</strong>
              <p>{roomLabel(session.room)}</p>
            </div>

            <div>
              <strong>Maximum capacity</strong>
              <p>{session.maxCapacity ?? "—"}</p>
            </div>
          </div>
        </section>

        <section
          className="fm-card"
          aria-label="Edit class schedule"
        >
          <div className="fm-card-top">
            <div>
              <h2>Modify schedule</h2>
              <p className="fm-muted">
                Select the new date, time and room.
              </p>
            </div>
          </div>

          {success && (
            <div
              className="fm-alert fm-alert-success"
              role="status"
            >
              {success}
            </div>
          )}

          {error && (
            <div
              className="fm-alert fm-alert-error"
              role="alert"
            >
              {error}
            </div>
          )}

          {validationError && (
            <div
              className="fm-alert fm-alert-error"
              role="alert"
            >
              {validationError}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="class-form-grid">

              <div className="class-form-field">
                <label htmlFor="class-date">
                  Class date
                </label>

                <input
                  id="class-date"
                  type="date"
                  name="date"
                  value={form.date}
                  onChange={handleChange}
                  disabled={saving}
                  required
                />
              </div>

              <div className="class-form-field">
                <label htmlFor="class-room">
                  Room
                </label>

                <select
                  id="class-room"
                  name="room"
                  value={form.room}
                  onChange={handleChange}
                  disabled={saving}
                  required
                >
                  <option value="">
                    Select a room
                  </option>

                  {ROOMS.map((room) => (
                    <option
                      key={room.value}
                      value={room.value}
                    >
                      {room.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="class-form-field">
                <label htmlFor="class-start-time">
                  Start time
                </label>

                <input
                  id="class-start-time"
                  type="time"
                  name="startTime"
                  value={form.startTime}
                  onChange={handleChange}
                  disabled={saving}
                  required
                />
              </div>

              <div className="class-form-field">
                <label htmlFor="class-end-time">
                  End time
                </label>

                <input
                  id="class-end-time"
                  type="time"
                  name="endTime"
                  value={form.endTime}
                  onChange={handleChange}
                  disabled={saving}
                  required
                />
              </div>
            </div>

            <div className="class-form-actions">
              <Link
                to="/manage-classes"
                className="fm-button fm-button-secondary"
              >
                Cancel
              </Link>

              <button
                type="submit"
                className="fm-button fm-button-edit"
                disabled={saving}
              >
                {saving ? "Saving..." : "Save changes"}
              </button>
            </div>
          </form>
        </section>

      </div>
    </main>
  );
}
