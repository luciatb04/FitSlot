import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../services/api";
import "./memberRegistration.css";

const PAGE_SIZE = 10;

function apiErrorMessage(error) {
  const status = error.response?.status;
  if (status === 401) return "Your session has expired. Please sign in again.";
  if (status === 403) return "You do not have permission to view members.";
  return "Unable to load members. Please try again.";
}

export default function MemberList() {
  const [members, setMembers] = useState([]);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;

    async function loadMembers() {
      setLoading(true);
      setError("");
      try {
        const response = await api.get("/members");
        if (active) setMembers(Array.isArray(response.data) ? response.data : []);
      } catch (requestError) {
        if (active) setError(apiErrorMessage(requestError));
      } finally {
        if (active) setLoading(false);
      }
    }

    loadMembers();
    return () => { active = false; };
  }, [reloadKey]);

  const filteredMembers = useMemo(() => {
    const query = search.trim().toLocaleLowerCase();
    return members
      .filter((member) => {
        const searchable = [
          member.id,
          member.firstName,
          member.lastName,
          member.email,
          member.telephone
        ].filter((value) => value != null).join(" ").toLocaleLowerCase();
        return searchable.includes(query);
      })
      .sort((a, b) =>
        `${a.lastName ?? ""} ${a.firstName ?? ""}`.localeCompare(
          `${b.lastName ?? ""} ${b.firstName ?? ""}`
        )
      );
  }, [members, search]);

  const totalPages = Math.max(1, Math.ceil(filteredMembers.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const pagedMembers = filteredMembers.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE
  );

  function handleSearch(event) {
    setSearch(event.target.value);
    setPage(1);
  }

  return (
    <main className="fm-page">
      <div className="fm-container">
        <header className="fm-page-header">
          <div>
            <p className="fm-eyebrow">RECEPTION / MEMBERS</p>
            <h1>Members</h1>
            <p className="fm-subtitle">View and manage members' contact details.</p>
          </div>
          <Link to="/NewMember" className="fm-button fm-button-primary">
            <span aria-hidden="true">＋</span> Register member
          </Link>
        </header>

        <section className="fm-card" aria-label="Registered members">
          <div className="fm-card-top">
            <div>
              <h2>Member directory</h2>
              <p className="fm-muted">{loading ? "Loading..." : `${members.length} registered member${members.length === 1 ? "" : "s"}`}</p>
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
              <span className="fm-sr-only">Search members</span>
              <span className="fm-search-symbol" aria-hidden="true">⌕</span>
              <input
                type="search"
                placeholder="Search by name, email, phone or ID..."
                value={search}
                onChange={handleSearch}
              />
            </label>
            <span className="fm-count">
              {filteredMembers.length} result{filteredMembers.length === 1 ? "" : "s"}
            </span>
          </div>

          {error && (
            <div className="fm-alert fm-alert-error" role="alert">
              {error} <button type="button" onClick={() => setReloadKey((key) => key + 1)}>Retry</button>
            </div>
          )}

          {loading ? (
            <p className="fm-empty" role="status">Loading members...</p>
          ) : !error && filteredMembers.length === 0 ? (
            <p className="fm-empty">
              {search ? "No members match your search." : "There are no registered members yet."}
            </p>
          ) : !error && (
            <>
              <div className="fm-table-scroll">
                <table className="fm-table">
                  <thead>
                    <tr>
                      <th scope="col">Member</th>
                      <th scope="col">Email</th>
                      <th scope="col">Telephone</th>
                      <th scope="col">ID</th>
                      <th scope="col"><span className="fm-sr-only">Actions</span></th>
                    </tr>
                  </thead>
                  <tbody>
                    {pagedMembers.map((member) => {
                      const fullName = `${member.firstName ?? ""} ${member.lastName ?? ""}`.trim();
                      const initials = `${member.firstName?.[0] ?? ""}${member.lastName?.[0] ?? ""}`.toUpperCase();
                      return (
                        <tr key={member.id}>
                          <td>
                            <div className="fm-person">
                              <span className="fm-avatar" aria-hidden="true">{initials || "?"}</span>
                              <span className="fm-person-name">{fullName || "Unnamed member"}</span>
                            </div>
                          </td>
                          <td>{member.email || <span className="fm-muted">—</span>}</td>
                          <td>{member.telephone || <span className="fm-muted">—</span>}</td>
                          <td><span className="fm-id">#{member.id}</span></td>
                          <td className="fm-action-cell">
                            <Link
                              to={`/members/${member.id}/edit`}
                              className="fm-button fm-button-edit"
                            >
                              Edit
                            </Link>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              <footer className="fm-table-footer">
                <span>
                  Showing {(currentPage - 1) * PAGE_SIZE + 1}–{Math.min(currentPage * PAGE_SIZE, filteredMembers.length)} of {filteredMembers.length}
                </span>
                <div className="fm-pagination">
                  <button type="button" disabled={currentPage === 1} onClick={() => setPage(currentPage - 1)}>Previous</button>
                  <span>Page {currentPage} of {totalPages}</span>
                  <button type="button" disabled={currentPage === totalPages} onClick={() => setPage(currentPage + 1)}>Next</button>
                </div>
              </footer>
            </>
          )}
        </section>
      </div>
    </main>
  );
}
