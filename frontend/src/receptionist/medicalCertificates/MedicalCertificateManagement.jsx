import { useEffect, useState } from "react";
import api from "../../services/api";
import "./medicalCertificates.css";

const emptyForm = { issueDate: "", expiryDate: "" };

function getErrorMessage(error, fallback) {
  return error.response?.data?.message || fallback;
}

export default function MedicalCertificateManagement() {
  const [members, setMembers] = useState([]);
  const [memberId, setMemberId] = useState("");
  const [certificates, setCertificates] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [loadingMembers, setLoadingMembers] = useState(true);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

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

  function handleMemberChange(event) {
    setMemberId(event.target.value);
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

        <label className="certificate-member-select">
          Select member
          <select disabled={loadingMembers} onChange={handleMemberChange} value={memberId}>
            <option value="">{loadingMembers ? "Loading members..." : "Choose a member"}</option>
            {members.map((member) => (
              <option key={member.id} value={member.id}>
                {member.firstName} {member.lastName} ({member.email})
              </option>
            ))}
          </select>
        </label>

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
