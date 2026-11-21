import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getClassSessions } from "../../services/classSessionService";
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

export default function ClassSessionList() {
  const [sessions, setSessions] = useState([]);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;

    async function loadSessions() {
      setLoading(true);
      setError("");

      try {
        const data = await getClassSessions();

        if (active) {
          setSessions(Array.isArray(data) ? data : []);
        }
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
          session.room
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
              View and manage gym class schedules and rooms.
            </p>
          </div>
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

          <div className="fm-toolbar">
            <label className="fm-search-field">
              <span className="fm-sr-only">
                Search classes
              </span>

              <input
                type="search"
                placeholder="Search by name, date, room or ID..."
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
            <div className="fm-alert fm-alert-error" role="alert">
              {error}
              <button
                type="button"
                onClick={() => setReloadKey((key) => key + 1)}
              >
                Retry
              </button>
            </div>
          )}

          {loading ? (
            <p className="fm-empty">Loading classes...</p>
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
                      <th>Capacity</th>
                      <th>Actions</th>
                    </tr>
                  </thead>

                  <tbody>
                    {pagedSessions.map((session) => (
                      <tr key={session.id}>
                        <td>{session.name}</td>
                        <td>{session.date}</td>
                        <td>
                          {session.startTime?.slice(0, 5)}
                          {" - "}
                          {session.endTime?.slice(0, 5)}
                        </td>
                        <td>
                          {session.room?.replaceAll("_", " ")}
                        </td>
                        <td>{session.maxCapacity}</td>
                        <td>
                          <Link
                            to={`/manage-classes/${session.id}/edit`}
                            className="fm-button fm-button-edit"
                          >
                            Edit
                          </Link>
                        </td>
                      </tr>
                    ))}
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
                    onClick={() => setPage(currentPage - 1)}
                  >
                    Previous
                  </button>

                  <button
                    type="button"
                    disabled={currentPage === totalPages}
                    onClick={() => setPage(currentPage + 1)}
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
