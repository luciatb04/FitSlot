
import React, { useEffect, useMemo, useState } from "react";
import tokenService from "../services/token.service";
import "./ClassSchedule.css";

function getToday() {
  const today = new Date();

  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function formatDate(dateString) {
  if (!dateString) return "Date not available";

  const [year, month, day] = dateString.split("-").map(Number);
  const date = new Date(year, month - 1, day);

  return date.toLocaleDateString("en-GB", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

function formatTime(time) {
  return time ? time.substring(0, 5) : "--:--";
}

function formatRoom(room) {
  if (!room) return "Not assigned";

  return room
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export default function ClassSchedule() {
  const [classes, setClasses] = useState([]);
  const [selectedDate, setSelectedDate] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const today = getToday();

  // Retrieve scheduled classes and availability from the backend
  useEffect(() => {
    const controller = new AbortController();

    async function fetchClasses() {
      setLoading(true);
      setError(null);

      try {
        const token = tokenService.getLocalAccessToken();

        const response = await fetch("/api/v1/class-sessions", {
          method: "GET",
          headers: {
            Accept: "application/json",
            ...(token
              ? { Authorization: `Bearer ${token}` }
              : {}),
          },
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(
            `Failed to load class schedule (${response.status})`
          );
        }

        const data = await response.json();

        if (!Array.isArray(data)) {
          throw new Error("Invalid class schedule response");
        }

        setClasses(data);
      } catch (err) {
        if (err.name !== "AbortError") {
          setError(err.message);
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }

    fetchClasses();

    return () => controller.abort();
  }, [refreshKey]);

  // Display upcoming classes, sorted by date and time
  const filteredClasses = useMemo(() => {
    return classes
      .filter((session) => {
        if (!session.date || session.date < today) {
          return false;
        }

        return !selectedDate || session.date === selectedDate;
      })
      .sort((a, b) => {
        const dateComparison = a.date.localeCompare(b.date);

        if (dateComparison !== 0) {
          return dateComparison;
        }

        return (a.startTime || "").localeCompare(
          b.startTime || ""
        );
      });
  }, [classes, selectedDate, today]);

  function refreshSchedule() {
    setRefreshKey((previous) => previous + 1);
  }

  return (
    <main className="class-schedule-container">
      <div className="class-schedule-header">
        <div>
          <h1>Class Schedule</h1>
          <p>
            Explore upcoming classes and check available spots.
          </p>
        </div>

        <button
          type="button"
          className="class-schedule-refresh"
          onClick={refreshSchedule}
          disabled={loading}
        >
          Refresh availability
        </button>
      </div>

      <div className="class-schedule-filters">
        <label htmlFor="class-date">Filter by date</label>

        <input
          id="class-date"
          type="date"
          min={today}
          value={selectedDate}
          onChange={(event) =>
            setSelectedDate(event.target.value)
          }
        />

        {selectedDate && (
          <button
            type="button"
            className="class-schedule-clear"
            onClick={() => setSelectedDate("")}
          >
            Clear filter
          </button>
        )}
      </div>

      {loading && (
        <div className="class-schedule-message" role="status">
          Loading class schedule...
        </div>
      )}

      {!loading && error && (
        <div className="class-schedule-error" role="alert">
          <p>{error}</p>
          <button type="button" onClick={refreshSchedule}>
            Try again
          </button>
        </div>
      )}

      {!loading && !error && (
        <>
          <div className="class-schedule-results">
            <h2>Upcoming Classes</h2>
            <span>
              {filteredClasses.length} classes found
            </span>
          </div>

          {filteredClasses.length === 0 ? (
            <div className="class-schedule-message">
              No classes scheduled for the selected date.
            </div>
          ) : (
            <div className="class-schedule-grid">
              {filteredClasses.map((session) => {
                const availableSpots =
                  Number.isInteger(session.availableSpots)
                    ? Math.max(0, session.availableSpots)
                    : null;

                const isFull = availableSpots === 0;

                return (
                  <article
                    key={session.id}
                    className="class-schedule-card"
                  >
                    <div className="class-schedule-card-header">
                      <h3>{session.name}</h3>

                      <span
                        className={`class-schedule-badge ${
                          availableSpots === null
                            ? "unknown"
                            : isFull
                            ? "full"
                            : "available"
                        }`}
                      >
                        {availableSpots === null
                          ? "Availability unknown"
                          : isFull
                          ? "Full"
                          : `${availableSpots} spots left`}
                      </span>
                    </div>

                    <div className="class-schedule-details">
                      <p>
                        <strong>Date:</strong>{" "}
                        {formatDate(session.date)}
                      </p>

                      <p>
                        <strong>Time:</strong>{" "}
                        {formatTime(session.startTime)}
                        {" - "}
                        {formatTime(session.endTime)}
                      </p>

                      <p>
                        <strong>Room:</strong>{" "}
                        {formatRoom(session.room)}
                      </p>

                      <p>
                        <strong>Instructor:</strong>{" "}
                        {session.instructorName || "Not assigned"}
                      </p>

                      <p>
                        <strong>Maximum capacity:</strong>{" "}
                        {session.maxCapacity}
                      </p>
                    </div>

                    <div className="class-schedule-card-footer">
                      {availableSpots === null ? (
                        <span>Availability not provided</span>
                      ) : isFull ? (
                        <span className="class-schedule-full-text">
                          No spots available
                        </span>
                      ) : (
                        <span className="class-schedule-available-text">
                          Places available
                        </span>
                      )}
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </>
      )}
    </main>
  );
}
