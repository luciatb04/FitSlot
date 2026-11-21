import React, { useEffect, useMemo, useState } from "react";
import tokenService from "../../services/token.service";
import "./CheckIn.css";

const API_BASE = "/api/v1";
const SERVICES = {
  WEIGHT_ROOM: "Weight room",
  CLASSES: "Classes",
  POOL: "Swimming pool",
};

const memberAccessInfoUrl = (id) => `${API_BASE}/access/members/${id}/check-in-info`;

function requestHeaders() {
  const token = tokenService.getLocalAccessToken();
  return {
    Accept: "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

async function requestJson(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: { ...requestHeaders(), ...options.headers },
  });
  const raw = await response.text();
  let body = null;
  if (raw) {
    try { body = JSON.parse(raw); } catch { /* body may be plain text */ }
  }
  if (!response.ok) {
    const message = body?.detail || body?.message || body?.error || body?.reason ||
      (raw && !raw.trim().startsWith("<") ? raw : null) ||
      `Request failed (${response.status}).`;
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }
  return body;
}

function displayDate(value) {
  if (!value) return "Not available";
  const parts = String(value).slice(0, 10).split("-");
  return parts.length === 3 ? `${parts[2]}/${parts[1]}/${parts[0]}` : String(value);
}

function displayTime(value) {
  return value ? String(value).slice(0, 5) : "--:--";
}

function memberLabel(member) {
  return `${member.firstName || ""} ${member.lastName || ""}`.trim() || `Member #${member.id}`;
}

function membershipActive(subscription) {
  const now = new Date();
  const today = [now.getFullYear(), String(now.getMonth() + 1).padStart(2, "0"), String(now.getDate()).padStart(2, "0")].join("-");
  return subscription?.status === "ACTIVE" &&
    (!subscription.startDate || subscription.startDate <= today) &&
    (!subscription.endDate || subscription.endDate >= today);
}

function SubscriptionCard({ subscription }) {
  const services = Array.isArray(subscription.allowedServices) ? subscription.allowedServices : [];
  return (
    <div className="checkin-subscription">
      <div className="checkin-subscription-title">
        <strong>{subscription.tierName || "Membership"}</strong>
        <span className={`checkin-pill ${subscription.status === "ACTIVE" ? "checkin-pill-active" : "checkin-pill-neutral"}`}>
          {subscription.status || "Unknown status"}
        </span>
      </div>
      <dl className="checkin-facts">
        <div><dt>Membership expiry</dt><dd>{displayDate(subscription.endDate)}</dd></div>
        <div><dt>Allowed time band</dt><dd>{subscription.startTime && subscription.endTime
          ? `${displayTime(subscription.startTime)} – ${displayTime(subscription.endTime)}`
          : "Not configured"}</dd></div>
        <div><dt>Remaining entries</dt><dd>{subscription.remainingEntries == null ? "Unlimited / not applicable" : subscription.remainingEntries}</dd></div>
      </dl>
      <div className="checkin-services">
        <span className="checkin-muted">Included services:</span>
        {services.length ? services.map((service) => (
          <span key={service} className="checkin-service-tag">{SERVICES[service] || service}</span>
        )) : <span>None listed</span>}
      </div>
    </div>
  );
}

export default function CheckIn() {
  const [members, setMembers] = useState([]);
  const [loadingMembers, setLoadingMembers] = useState(true);
  const [membersError, setMembersError] = useState("");
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState(null);
  const [accessInfo, setAccessInfo] = useState(null);
  const [loadingInfo, setLoadingInfo] = useState(false);
  const [infoError, setInfoError] = useState("");
  const [service, setService] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    (async () => {
      try {
        setLoadingMembers(true);
        const data = await requestJson(`${API_BASE}/members`, { signal: controller.signal });
        if (!Array.isArray(data)) throw new Error("Unexpected member list response.");
        setMembers(data);
      } catch (error) {
        if (error.name !== "AbortError") setMembersError(error.message);
      } finally {
        if (!controller.signal.aborted) setLoadingMembers(false);
      }
    })();
    return () => controller.abort();
  }, []);

  const results = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return [];
    return members.filter((member) =>
      String(member.id).includes(needle) ||
      memberLabel(member).toLowerCase().includes(needle)
    ).slice(0, 12);
  }, [members, query]);

  useEffect(() => {
    if (!selected) return undefined;
    const controller = new AbortController();
    (async () => {
      setLoadingInfo(true);
      setInfoError("");
      setAccessInfo(null);
      setService("");
      try {
        const data = await requestJson(memberAccessInfoUrl(selected.id), { signal: controller.signal });
        if (!data || !Array.isArray(data.subscriptions)) {
          throw new Error("The member access information response is incomplete.");
        }
        setAccessInfo(data);
      } catch (error) {
        if (error.name !== "AbortError") {
          setInfoError(error.status === 404
            ? "The check-in information endpoint is not yet available. The backend must provide the member subscription, time band and medical certificate details."
            : error.message);
        }
      } finally {
        if (!controller.signal.aborted) setLoadingInfo(false);
      }
    })();
    return () => controller.abort();
  }, [selected]);

  const eligibleSubscriptions = useMemo(() =>
    (accessInfo?.subscriptions || []).filter(membershipActive), [accessInfo]);
  const availableServices = useMemo(() => [...new Set(
    eligibleSubscriptions.flatMap((subscription) => subscription.allowedServices || [])
  )], [eligibleSubscriptions]);

  function chooseMember(member) {
    setSelected(member);
    setQuery(memberLabel(member));
    setResult(null);
  }

  async function submitCheckIn(event) {
    event.preventDefault();
    if (!selected || !service || submitting || !availableServices.includes(service)) return;
    setSubmitting(true);
    setResult(null);
    try {
      const data = await requestJson(`${API_BASE}/access/check-in`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ memberId: selected.id, serviceAccessed: service }),
      });
      setResult({ success: true, message: `Access allowed. Check-in #${data?.accessRecordId ?? "registered"}.` });
      try {
        const updated = await requestJson(memberAccessInfoUrl(selected.id));
        if (updated && Array.isArray(updated.subscriptions)) setAccessInfo(updated);
      } catch { /* Do not hide a successful check-in due to a refresh failure. */ }
    } catch (error) {
      setResult({ success: false, message: error.message });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="checkin-page">
      <div className="checkin-heading">
        <h1>Member Check-in</h1>
        <p>Search for a member, review their access conditions, and register their entrance.</p>
      </div>

      <section className="checkin-panel" aria-label="Member search">
        <label htmlFor="checkin-search">Search by name, surname or member ID</label>
        <input
          id="checkin-search"
          value={query}
          onChange={(event) => {
            setQuery(event.target.value);
            setSelected(null);
            setAccessInfo(null);
            setResult(null);
          }}
          placeholder="Enter a name or member ID"
          autoComplete="off"
        />
        {loadingMembers && <p className="checkin-muted" role="status">Loading members...</p>}
        {membersError && <p className="checkin-alert checkin-alert-error" role="alert">{membersError}</p>}
        {!selected && query.trim() && !loadingMembers && !membersError && (
          <div className="checkin-results" role="listbox" aria-label="Matching members">
            {results.length ? results.map((member) => (
              <button type="button" key={member.id} className="checkin-result" onClick={() => chooseMember(member)}>
                <span>{memberLabel(member)}</span><small>ID #{member.id} · {member.status}</small>
              </button>
            )) : <p className="checkin-muted">No matching members.</p>}
          </div>
        )}
      </section>

      {selected && (
        <section className="checkin-panel" aria-label="Selected member details">
          <div className="checkin-member-header">
            <div><h2>{memberLabel(selected)}</h2><span className="checkin-muted">Member #{selected.id}</span></div>
            <span className={`checkin-pill ${selected.status === "ACTIVE" ? "checkin-pill-active" : "checkin-pill-neutral"}`}>
              {selected.status}
            </span>
          </div>

          {loadingInfo && <p role="status">Loading subscription and certificate details...</p>}
          {infoError && <p role="alert" className="checkin-alert checkin-alert-error">{infoError}</p>}
          {accessInfo && (
            <>
              <h3>Medical certificate</h3>
              <dl className="checkin-facts">
                <div><dt>Expiry date</dt><dd>{displayDate(accessInfo.medicalCertificate?.expiryDate)}</dd></div>
                <div><dt>Status</dt><dd>{accessInfo.medicalCertificate?.validity || "Not available"}</dd></div>
              </dl>

              <h3>Membership information</h3>
              {accessInfo.subscriptions.length ? accessInfo.subscriptions.map((subscription, index) => (
                <SubscriptionCard key={subscription.id ?? index} subscription={subscription} />
              )) : <p className="checkin-muted">No subscriptions found.</p>}

              <form onSubmit={submitCheckIn} className="checkin-form">
                <label htmlFor="checkin-service">Service to access</label>
                <select id="checkin-service" value={service} onChange={(event) => setService(event.target.value)} required>
                  <option value="">Select an included service</option>
                  {availableServices.map((item) => (
                    <option value={item} key={item}>{SERVICES[item] || item}</option>
                  ))}
                </select>
                <p className="checkin-muted">Only services included in an active membership are selectable. The backend makes the final access decision.</p>
                <button type="submit" className="checkin-submit" disabled={submitting || !service || selected.status !== "ACTIVE"}>
                  {submitting ? "Registering..." : "Register check-in"}
                </button>
              </form>
            </>
          )}
          {result && (
            <div className={`checkin-alert ${result.success ? "checkin-alert-success" : "checkin-alert-error"}`} role="alert">
              {result.message}
            </div>
          )}
        </section>
      )}
    </main>
  );
}
