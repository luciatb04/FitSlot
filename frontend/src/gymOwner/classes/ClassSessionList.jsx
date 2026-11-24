
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  deleteClassSession,
  getClassSessions
} from "../../services/classSessionService";
import "./ClassSession.css";

const PAGE_SIZE = 10;

function getErrorMessage(error) {
  const status = error.response?.status;

  if (status === 401) {
    return "Your session has expired. Please sign in again.";
  }

  if (status === 403) {
    return "You do not have permission to view classes.";
  }

  return "Unable to load classes. Please try again.";
}

function formatTime(value) {
  return value?.slice(0, 5) || "--:--";
}

function formatRoom(room) {
  return room?.replaceAll("_", " ") || "Not specified";
}

export default function ClassSessionList() {
  const [sessions, setSessions] = useState([]);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [deletingId, setDeletingId] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;

    async function loadSessions() {
      setLoading(true);
      setError("");

      try {
        const data = await getClassSessions();

        if (active) {
          if (!Array.isArray(data)) {
            throw new Error("Invalid class list response");
          }

          setSessions(data);
        }
      } catch (requestError) {
        if (active) {
          setError(getErrorMessage(requestError));
          setSessions([]);
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    loadSessions();

    return () => {
      active = false;
    };
  }, [reloadKey]);

  const filteredSessions = useMemo(() => {
    const query = search.trim().toLowerCase();

    return sessions
      .filter((session) =>
        [
          session.id,
          session.name,
          session.date,
          session.room,
          session.instructorName
        ]
          .filter((value) => value != null)
          .join(" ")
          .toLowerCase()
          .includes(query)
      )
      .sort((a, b) =>
        `${a.date} ${a.startTime}`.localeCompare(
          `${b.date} ${b.startTime}`
        )
      );
  }, [sessions, search]);

  async function handleDelete(session) {
    if (
      !window.confirm(
        `Delete the schedule for "${session.name}"? This cannot be undone.`
      )
    ) {
      return;
    }

    setDeletingId(session.id);
    setActionError("");
    try {
      await deleteClassSession(session.id);
      setReloadKey((key) => key + 1);
    } catch (requestError) {
      setActionError(
        requestError.response?.status === 409
          ? "This class has reservations and cannot be deleted."
          : requestError.response?.status === 404
            ? "This class no longer exists. Refresh the schedule."
            : "Unable to delete this class. Please try again."
      );
    } finally {
      setDeletingId(null);
    }
  }

  const totalPages = Math.max(
    1,
    Math.ceil(filteredSessions.length / PAGE_SIZE)
  );

  const currentPage = Math.min(page, totalPages);

  const pagedSessions = filteredSessions.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE
  );

  return (
    <main className="fm-page">
      <div className="fm-container">

        <header className="fm-page-header">
          <div>
            <p className="fm-eyebrow">
              GYM OWNER / CLASSES
            </p>

            <h1>Class Management</h1>

            <p className="fm-subtitle">
              View class schedules, assigned instructors,
              and manage substitutions.
            </p>
          </div>
          <Link
            to="/manage-classes/new"
            className="fm-button fm-button-edit"
          >
            Create class
          </Link>
        </header>

        <section className="fm-card">

          <div className="fm-card-top">
            <div>
              <h2>Scheduled classes</h2>

              <p className="fm-muted">
                {loading
                  ? "Loading..."
                  : `${sessions.length} scheduled classes`}
              </p>
            </div>

            <button
              className="fm-button fm-button-secondary"
              type="button"
              onClick={() => setReloadKey((key) => key + 1)}
              disabled={loading}
            >
              Refresh
            </button>
          </div>

          {actionError && (
            <div className="fm-alert fm-alert-error" role="alert">
              {actionError}
            </div>
          )}

          <div className="fm-toolbar">

            <label className="fm-search-field">
              <span className="fm-sr-only">
                Search classes
              </span>

              <input
                type="search"
                placeholder="Search by class, instructor, date or room..."
                value={search}
                onChange={(event) => {
                  setSearch(event.target.value);
                  setPage(1);
                }}
              />
            </label>

            <span className="fm-count">
              {filteredSessions.length} results
            </span>
          </div>

          {error && (
            <div
              className="fm-alert fm-alert-error"
              role="alert"
            >
              {error}

              <button
                type="button"
                onClick={() =>
                  setReloadKey((key) => key + 1)
                }
              >
                Retry
              </button>
            </div>
          )}

          {loading ? (
            <p className="fm-empty">
              Loading classes...
            </p>
          ) : !error && filteredSessions.length === 0 ? (
            <p className="fm-empty">
              {search
                ? "No classes match your search."
                : "No scheduled classes found."}
            </p>
          ) : !error && (
            <>
              <div className="fm-table-scroll">
                <table className="fm-table">
                  <thead>
                    <tr>
                      <th>Class</th>
                      <th>Date</th>
                      <th>Time</th>
                      <th>Room</th>
                      <th>Bookings / capacity</th>
                      <th>Instructor</th>
                      <th>Actions</th>
                    </tr>
                  </thead>

                  <tbody>
                    {pagedSessions.map((session) => {
                      // Distinguish missing API data from
                      // a class that has no assigned instructor.
                      const hasInstructorInfo =
                        Object.prototype.hasOwnProperty.call(
                          session,
                          "instructorId"
                        );

                      const assigned =
                        hasInstructorInfo &&
                        session.instructorId != null;

                      return (
                        <tr key={session.id}>

                          <td>
                            <strong>{session.name}</strong>
                          </td>

                          <td>{session.date}</td>

                          <td>
                            {formatTime(session.startTime)}
                            {" - "}
                            {formatTime(session.endTime)}
                          </td>

                          <td>
                            {formatRoom(session.room)}
                          </td>

                          <td>
                            {session.confirmedBookings ?? "—"}
                            {" / "}
                            {session.maxCapacity}
                          </td>

                          {/* Instructor column */}
                          <td>
                            {!hasInstructorInfo ? (
                              <span className="class-instructor-unknown">
                                Instructor data unavailable
                              </span>
                            ) : assigned ? (
                              <div className="class-instructor-cell">
                                <span className="class-instructor-name">
                                  {session.instructorName ||
                                    `Instructor #${session.instructorId}`}
                                </span>

                                <span className="class-instructor-status assigned">
                                  Assigned
                                </span>
                              </div>
                            ) : (
                              <div className="class-instructor-cell">
                                <span className="class-instructor-warning">
                                  An instructor must be assigned
                                </span>

                                <span className="class-instructor-status unassigned">
                                  Unassigned
                                </span>
                              </div>
                            )}
                          </td>

                          {/* Actions */}
                          <td>
                            <div className="class-session-actions">

                              <Link
                                to={`/manage-classes/${session.id}/edit`}
                                className="fm-button fm-button-edit"
                              >
                                Edit
                              </Link>

                              <button
                                type="button"
                                className="fm-button class-button-delete"
                                onClick={() => handleDelete(session)}
                                disabled={deletingId === session.id}
                                aria-label={`Delete ${session.name}`}
                              >
                                {deletingId === session.id
                                  ? "Deleting..."
                                  : "Delete"}
                              </button>

                              {hasInstructorInfo ? (
                                <Link
                                  to={`/manage-classes/${session.id}/substitute`}
                                  className={
                                    "fm-button " +
                                    (assigned
                                      ? "class-button-substitute"
                                      : "class-button-assign")
                                  }
                                >
                                  {assigned
                                    ? "Substitute Instructor"
                                    : "Assign Instructor"}
                                </Link>
                              ) : (
                                <span className="class-instructor-unknown">
                                  Assignment unavailable
                                </span>
                              )}

                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              <footer className="fm-table-footer">
                <span>
                  Page {currentPage} of {totalPages}
                </span>

                <div className="fm-pagination">
                  <button
                    type="button"
                    disabled={currentPage === 1}
                    onClick={() =>
                      setPage(currentPage - 1)
                    }
                  >
                    Previous
                  </button>

                  <button
                    type="button"
                    disabled={currentPage === totalPages}
                    onClick={() =>
                      setPage(currentPage + 1)
                    }
                  >
                    Next
                  </button>
                </div>
              </footer>
            </>
          )}
        </section>
      </div>
    </main>
  );
}
