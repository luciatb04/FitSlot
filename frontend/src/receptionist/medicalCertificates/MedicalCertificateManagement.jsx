import { useEffect, useMemo, useState } from "react";
import api from "../../services/api";
import "./medicalCertificates.css";

const emptyForm = { issueDate: "", expiryDate: "" };
const MEMBER_PAGE_SIZE = 10;

function getErrorMessage(error, fallback) {
  return error.response?.data?.message || fallback;
}

export default function MedicalCertificateManagement() {
  const [members, setMembers] = useState([]);
  const [memberId, setMemberId] = useState("");
  const [memberSearch, setMemberSearch] = useState("");
  const [memberPage, setMemberPage] = useState(1);
  const [certificates, setCertificates] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [loadingMembers, setLoadingMembers] = useState(true);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const filteredMembers = useMemo(() => {
    const query = memberSearch.trim().toLocaleLowerCase();

    return members
      .filter((member) => {
        if (!query) return true;

        return [
          member.id,
          member.firstName,
          member.lastName,
          member.email,
          member.telephone
        ]
          .filter((value) => value != null)
          .join(" ")
          .toLocaleLowerCase()
          .includes(query);
      })
      .sort((a, b) =>
        `${a.lastName ?? ""} ${a.firstName ?? ""}`.localeCompare(
          `${b.lastName ?? ""} ${b.firstName ?? ""}`
        )
      );
  }, [members, memberSearch]);

  const totalMemberPages = Math.max(
    1,
    Math.ceil(filteredMembers.length / MEMBER_PAGE_SIZE)
  );
  const currentMemberPage = Math.min(memberPage, totalMemberPages);
  const pagedMembers = filteredMembers.slice(
    (currentMemberPage - 1) * MEMBER_PAGE_SIZE,
    currentMemberPage * MEMBER_PAGE_SIZE
  );

  const selectedMember = members.find(
    (member) => String(member.id) === memberId
  );

  useEffect(() => {
    let active = true;
    api.get("/members")
      .then(({ data }) => {
        if (active) setMembers(data);
      })
      .catch((requestError) => {
        if (active) setError(getErrorMessage(requestError, "Members could not be loaded."));
      })
      .finally(() => {
        if (active) setLoadingMembers(false);
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!memberId) {
      setCertificates([]);
      setLoadingHistory(false);
      return undefined;
    }

    let active = true;
    setLoadingHistory(true);
    setError("");
    api.get(`/members/${memberId}/medical-certificates`)
      .then(({ data }) => {
        if (active) setCertificates(data);
      })
      .catch((requestError) => {
        if (active) {
          setCertificates([]);
          setError(getErrorMessage(requestError, "Certificate history could not be loaded."));
        }
      })
      .finally(() => {
        if (active) setLoadingHistory(false);
      });

    return () => {
      active = false;
    };
  }, [memberId]);

  function chooseMember(member) {
    setMemberId(String(member.id));
    setMemberSearch("");
    setMemberPage(1);
    setCertificates([]);
    setForm(emptyForm);
    setEditingId(null);
    setError("");
    setSuccess("");
  }

  function changeMember() {
    setMemberId("");
    setMemberSearch("");
    setMemberPage(1);
    setCertificates([]);
    setForm(emptyForm);
    setEditingId(null);
    setError("");
    setSuccess("");
  }

  function handleFormChange(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setError("");
    setSuccess("");
  }

  function startCorrection(certificate) {
    setEditingId(certificate.id);
    setForm({ issueDate: certificate.issueDate, expiryDate: certificate.expiryDate });
    setError("");
    setSuccess("");
  }

  function cancelCorrection() {
    setEditingId(null);
    setForm(emptyForm);
    setError("");
  }

  async function refreshHistory() {
    const { data } = await api.get(`/members/${memberId}/medical-certificates`);
    setCertificates(data);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSuccess("");
    setSaving(true);

    try {
      const request = { issueDate: form.issueDate, expiryDate: form.expiryDate };
      if (editingId === null) {
        await api.post(`/members/${memberId}/medical-certificates`, request);
        setSuccess("Certificate renewal was registered.");
      } else {
        await api.put(`/members/${memberId}/medical-certificates/${editingId}`, request);
        setSuccess("Certificate dates were corrected.");
      }
      setForm(emptyForm);
      setEditingId(null);
      await refreshHistory();
    } catch (requestError) {
      setError(getErrorMessage(requestError, "The certificate could not be saved."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="certificate-page">
      <section className="certificate-card" aria-labelledby="certificate-title">
        <h1 id="certificate-title">Medical certificates</h1>
        <p>Register renewals, correct certificate dates, and review the member's certificate history.</p>

        {error && <div className="certificate-alert certificate-alert-error" role="alert">{error}</div>}
        {success && <div className="certificate-alert certificate-alert-success" role="status">{success}</div>}

        {selectedMember ? (
          <div className="certificate-selected-member">
            <div>
              <strong>
                {selectedMember.firstName} {selectedMember.lastName}
              </strong>
              <span>{selectedMember.email}</span>
              <span>Member #{selectedMember.id}</span>
            </div>
            <button
              className="certificate-secondary-button"
              onClick={changeMember}
              type="button"
            >
              Change member
            </button>
          </div>
        ) : (
          <div className="certificate-member-picker">
            <label htmlFor="certificate-member-search">Search members</label>
            <div className="certificate-search-control">
              <span aria-hidden="true">⌕</span>
              <input
                autoComplete="off"
                disabled={loadingMembers}
                id="certificate-member-search"
                onChange={(event) => {
                  setMemberSearch(event.target.value);
                  setMemberPage(1);
                }}
                placeholder="Search by name, email, phone or ID..."
                type="search"
                value={memberSearch}
              />
            </div>
            {loadingMembers && (
              <p role="status">Loading members...</p>
            )}
            {!loadingMembers && (
              <section
                aria-label="Member directory"
                className="certificate-member-directory"
              >
                <div className="certificate-member-directory-header">
                  <h2>Members</h2>
                  <span>
                    {filteredMembers.length} result{filteredMembers.length === 1 ? "" : "s"}
                  </span>
                </div>

                {filteredMembers.length === 0 ? (
                  <p className="certificate-member-empty">
                    {memberSearch.trim()
                      ? "No members match your search."
                      : "There are no registered members yet."}
                  </p>
                ) : (
                  <>
                    <div className="certificate-member-table-scroll">
                      <table className="certificate-member-table">
                        <thead>
                          <tr>
                            <th scope="col">Member</th>
                            <th scope="col">Email</th>
                            <th scope="col">Telephone</th>
                            <th scope="col">ID</th>
                            <th scope="col">
                              <span className="certificate-sr-only">Actions</span>
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          {pagedMembers.map((member) => {
                            const fullName = `${member.firstName ?? ""} ${member.lastName ?? ""}`.trim();
                            const initials = `${member.firstName?.[0] ?? ""}${member.lastName?.[0] ?? ""}`.toUpperCase();
                            return (
                              <tr key={member.id}>
                                <td>
                                  <div className="certificate-member-person">
                                    <span aria-hidden="true" className="certificate-member-avatar">
                                      {initials || "?"}
                                    </span>
                                    <span>{fullName || "Unnamed member"}</span>
                                  </div>
                                </td>
                                <td>{member.email || "—"}</td>
                                <td>{member.telephone || "—"}</td>
                                <td>#{member.id}</td>
                                <td>
                                  <button
                                    aria-label={`Select ${fullName || `member ${member.id}`}`}
                                    className="certificate-select-member-button"
                                    onClick={() => chooseMember(member)}
                                    type="button"
                                  >
                                    Select
                                  </button>
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>

                    <footer className="certificate-member-pagination">
                      <span>
                        Showing {(currentMemberPage - 1) * MEMBER_PAGE_SIZE + 1}–
                        {Math.min(currentMemberPage * MEMBER_PAGE_SIZE, filteredMembers.length)}
                        {" "}of {filteredMembers.length}
                      </span>
                      <div>
                        <button
                          disabled={currentMemberPage === 1}
                          onClick={() => setMemberPage(currentMemberPage - 1)}
                          type="button"
                        >
                          Previous
                        </button>
                        <span>Page {currentMemberPage} of {totalMemberPages}</span>
                        <button
                          disabled={currentMemberPage === totalMemberPages}
                          onClick={() => setMemberPage(currentMemberPage + 1)}
                          type="button"
                        >
                          Next
                        </button>
                      </div>
                    </footer>
                  </>
                )}
              </section>
            )}
          </div>
        )}

        {memberId && (
          <>
            <section className="certificate-history" aria-labelledby="certificate-history-title">
              <h2 id="certificate-history-title">Certificate history</h2>
              {loadingHistory ? (
                <p>Loading certificate history...</p>
              ) : certificates.length === 0 ? (
                <p>No medical certificates have been registered for this member.</p>
              ) : (
                <ul className="certificate-list">
                  {certificates.map((certificate, index) => {
                    const isCurrent = index === 0;
                    return (
                      <li className="certificate-list-item" key={certificate.id}>
                        <div>
                          <strong>{isCurrent ? "Current certificate" : "Previous certificate"}</strong>
                          <span>
                            Issued {certificate.issueDate} · Expires {certificate.expiryDate}
                          </span>
                          <span className={certificate.validity === "VALID" ? "certificate-valid" : "certificate-expired"}>
                            {certificate.validity === "VALID"
                              ? "Within date range"
                              : certificate.validity === "EXPIRED"
                                ? "Expired"
                                : "Not yet valid"}
                            {!isCurrent && " — does not determine access"}
                          </span>
                        </div>
                        <button
                          className="certificate-secondary-button"
                          onClick={() => startCorrection(certificate)}
                          type="button"
                        >
                          Correct dates
                        </button>
                      </li>
                    );
                  })}
                </ul>
              )}
            </section>

            <section aria-labelledby="certificate-form-title">
              <h2 id="certificate-form-title">
                {editingId === null ? "Register certificate renewal" : "Correct certificate dates"}
              </h2>
              <form className="certificate-form" onSubmit={handleSubmit}>
                <label>
                  Issue date
                  <input
                    name="issueDate"
                    onChange={handleFormChange}
                    required
                    type="date"
                    value={form.issueDate}
                  />
                </label>
                <label>
                  Expiry date
                  <input
                    name="expiryDate"
                    onChange={handleFormChange}
                    required
                    type="date"
                    value={form.expiryDate}
                  />
                </label>
                <div className="certificate-form-actions">
                  <button className="certificate-primary-button" disabled={saving} type="submit">
                    {saving ? "Saving..." : editingId === null ? "Register renewal" : "Save correction"}
                  </button>
                  {editingId !== null && (
                    <button className="certificate-secondary-button" onClick={cancelCorrection} type="button">
                      Cancel
                    </button>
                  )}
                </div>
              </form>
            </section>
          </>
        )}
      </section>
    </main>
  );
}
