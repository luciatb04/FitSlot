import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../../services/api";
import tokenService from "../../services/token.service";
import { createClassSession } from "../../services/classSessionService";
import "./ClassSession.css";

const ROOMS = [
  { value: "WEIGHT_ROOM", label: "Weight Room" },
  { value: "CLASS_ROOM_1", label: "Class Room 1" },
  { value: "CLASS_ROOM_2", label: "Class Room 2" },
  { value: "POOL", label: "Swimming Pool" }
];

const INITIAL_FORM = {
  name: "",
  date: "",
  room: "",
  startTime: "",
  endTime: "",
  maxCapacity: "",
  gymId: ""
};

function getErrorMessage(error) {
  const status = error.response?.status;

  if (status === 400) {
    return "Invalid class details. Check all fields and try again.";
  }

  if (status === 401) {
    return "Your session has expired. Please sign in again.";
  }

  if (status === 403) {
    return "You do not have permission to create classes.";
  }

  if (status === 409) {
    return "Scheduling conflict: the selected room is already occupied during this time.";
  }

  return "Unable to create the class. Please try again.";
}

export default function CreateClassSession() {
  const navigate = useNavigate();
  const [form, setForm] = useState(INITIAL_FORM);
  const [gyms, setGyms] = useState([]);
  const [loadingGyms, setLoadingGyms] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    async function loadGyms() {
      try {
        const userId = tokenService.getUser()?.id;
        if (!userId) {
          throw new Error("Unable to identify the signed-in gym owner.");
        }

        const response = await api.get("/gyms", {
          params: { userId }
        });
        if (!Array.isArray(response.data)) {
          throw new Error("Invalid gym list response.");
        }

        if (active) {
          setGyms(response.data);
          if (response.data.length === 1) {
            setForm((previous) => ({
              ...previous,
              gymId: String(response.data[0].id)
            }));
          }
        }
      } catch (requestError) {
        if (active) {
          setError(
            requestError.response
              ? getErrorMessage(requestError)
              : requestError.message
          );
        }
      } finally {
        if (active) {
          setLoadingGyms(false);
        }
      }
    }

    loadGyms();
    return () => {
      active = false;
    };
  }, []);

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((previous) => ({ ...previous, [name]: value }));
    setError("");
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (saving) return;

    if (form.startTime >= form.endTime) {
      setError("End time must be later than start time.");
      return;
    }

    setSaving(true);
    setError("");

    try {
      await createClassSession({
        ...form,
        maxCapacity: Number(form.maxCapacity),
        gymId: Number(form.gymId)
      });
      navigate("/manage-classes");
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="fm-page">
      <div className="fm-container">
        <header className="fm-page-header">
          <div>
            <p className="fm-eyebrow">GYM OWNER / CLASSES</p>
            <h1>Create Class Schedule</h1>
            <p className="fm-subtitle">
              Set the class details, date, room and time.
            </p>
          </div>

          <Link
            to="/manage-classes"
            className="fm-button fm-button-secondary"
          >
            Back to classes
          </Link>
        </header>

        <section className="fm-card" aria-label="Create class">
          {error && (
            <div className="fm-alert fm-alert-error" role="alert">
              {error}
            </div>
          )}

          {!loadingGyms && gyms.length === 0 && !error && (
            <div className="fm-alert fm-alert-error" role="alert">
              Create a gym before scheduling a class.
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="class-form-grid">
              <div className="class-form-field">
                <label htmlFor="class-name">Class name</label>
                <input
                  id="class-name"
                  name="name"
                  value={form.name}
                  onChange={handleChange}
                  required
                  disabled={saving}
                />
              </div>

              <div className="class-form-field">
                <label htmlFor="class-gym">Gym</label>
                <select
                  id="class-gym"
                  name="gymId"
                  value={form.gymId}
                  onChange={handleChange}
                  required
                  disabled={saving || loadingGyms || gyms.length === 0}
                >
                  <option value="">
                    {loadingGyms ? "Loading gyms..." : "Select a gym"}
                  </option>
                  {gyms.map((gym) => (
                    <option key={gym.id} value={gym.id}>
                      {gym.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="class-form-field">
                <label htmlFor="class-date">Class date</label>
                <input
                  id="class-date"
                  type="date"
                  name="date"
                  value={form.date}
                  onChange={handleChange}
                  required
                  disabled={saving}
                />
              </div>

              <div className="class-form-field">
                <label htmlFor="class-room">Room</label>
                <select
                  id="class-room"
                  name="room"
                  value={form.room}
                  onChange={handleChange}
                  required
                  disabled={saving}
                >
                  <option value="">Select a room</option>
                  {ROOMS.map((room) => (
                    <option key={room.value} value={room.value}>
                      {room.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="class-form-field">
                <label htmlFor="class-start-time">Start time</label>
                <input
                  id="class-start-time"
                  type="time"
                  name="startTime"
                  value={form.startTime}
                  onChange={handleChange}
                  required
                  disabled={saving}
                />
              </div>

              <div className="class-form-field">
                <label htmlFor="class-end-time">End time</label>
                <input
                  id="class-end-time"
                  type="time"
                  name="endTime"
                  value={form.endTime}
                  onChange={handleChange}
                  required
                  disabled={saving}
                />
              </div>

              <div className="class-form-field">
                <label htmlFor="class-capacity">Maximum capacity</label>
                <input
                  id="class-capacity"
                  type="number"
                  name="maxCapacity"
                  min="1"
                  step="1"
                  value={form.maxCapacity}
                  onChange={handleChange}
                  required
                  disabled={saving}
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
                disabled={
                  saving || loadingGyms || gyms.length === 0
                }
              >
                {saving ? "Creating..." : "Create class"}
              </button>
            </div>
          </form>
        </section>
      </div>
    </main>
  );
}
