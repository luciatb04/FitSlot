import { useEffect, useState } from "react";
import api from "../../services/api";
import "./checkIn.css";

function getErrorMessage(error, fallback) {
  return error.response?.data?.message || fallback;
}

export default function CheckIn() {
  const [members, setMembers] = useState([]);
  const [memberId, setMemberId] = useState("");
  const [serviceAccessed, setServiceAccessed] = useState("WEIGHT_ROOM");
  const [loadingMembers, setLoadingMembers] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);

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

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setResult(null);
    setSubmitting(true);

    try {
      const response = await api.post("/access/check-in", {
        memberId: Number(memberId),
        serviceAccessed
      });
      setResult(response.data);
    } catch (requestError) {
      setError(getErrorMessage(requestError, "The access check could not be completed."));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="check-in-page">
      <section className="check-in-card" aria-labelledby="check-in-title">
        <h1 id="check-in-title">Member check-in</h1>
        <p>Access is granted only when the member's latest medical certificate is current.</p>

        {error && <div className="check-in-result check-in-denied" role="alert">{error}</div>}
        {result && (
          <div className="check-in-result check-in-allowed" role="status">
            Access allowed. Record #{result.accessRecordId} was created.
          </div>
        )}

        <form className="check-in-form" onSubmit={handleSubmit}>
          <label>
            Member
            <select
              disabled={loadingMembers}
              onChange={(event) => setMemberId(event.target.value)}
              required
              value={memberId}
            >
              <option value="">{loadingMembers ? "Loading members..." : "Choose a member"}</option>
              {members.map((member) => (
                <option key={member.id} value={member.id}>
                  {member.firstName} {member.lastName} ({member.email})
                </option>
              ))}
            </select>
          </label>

          <label>
            Service
            <select
              onChange={(event) => setServiceAccessed(event.target.value)}
              value={serviceAccessed}
            >
              <option value="WEIGHT_ROOM">Weight room</option>
              <option value="POOL">Pool</option>
              <option value="CLASSES">Classes</option>
            </select>
          </label>

          <button className="check-in-submit" disabled={loadingMembers || submitting} type="submit">
            {submitting ? "Checking..." : "Check access"}
          </button>
        </form>
      </section>
    </main>
  );
}
