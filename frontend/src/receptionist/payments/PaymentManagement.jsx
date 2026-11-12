import { useEffect, useMemo, useState } from "react";
import api from "../../services/api";
import tokenService from "../../services/token.service";
import "./paymentManagement.css";

function todayAsInputValue() {
  const now = new Date();
  const localDate = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
  return localDate.toISOString().slice(0, 10);
}

function getErrorMessage(error, fallback) {
  return error.response?.data?.message || error.message || fallback;
}

export default function PaymentManagement() {
  const [members, setMembers] = useState([]);
  const [tiers, setTiers] = useState([]);
  const [memberId, setMemberId] = useState("");
  const [tierId, setTierId] = useState("");
  const [startDate, setStartDate] = useState(todayAsInputValue);
  const [endDate, setEndDate] = useState("");
  const [initialStatus, setInitialStatus] = useState("PAID");
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    let active = true;

    async function loadOptions() {
      try {
        const [membersResponse, tiersResponse] = await Promise.all([
          api.get("/members"),
          fetch("/api/membership-tiers", {
            headers: {
              Authorization: `Bearer ${tokenService.getLocalAccessToken()}`,
              "Content-Type": "application/json"
            }
          })
        ]);
        if (!tiersResponse.ok) {
          throw new Error("Membership types could not be loaded.");
        }
        const tierData = await tiersResponse.json();
        if (!active) return;
        setMembers(membersResponse.data);
        setTiers(tierData);
      } catch (requestError) {
        if (active) setError(getErrorMessage(requestError, "Payment information could not be loaded."));
      } finally {
        if (active) setLoading(false);
      }
    }

    loadOptions();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!memberId) {
      setPayments([]);
      return undefined;
    }

    let active = true;
    setLoadingHistory(true);
    setError("");
    api.get("/payments", { params: { memberId } })
      .then(({ data }) => {
        if (active) setPayments(data);
      })
      .catch((requestError) => {
        if (active) {
          setPayments([]);
          setError(getErrorMessage(requestError, "Payment history could not be loaded."));
        }
      })
      .finally(() => {
        if (active) setLoadingHistory(false);
      });

    return () => {
      active = false;
    };
  }, [memberId]);

  const selectedTier = useMemo(
    () => tiers.find((tier) => String(tier.id) === tierId),
    [tierId, tiers]
  );
  const isEntryTier = selectedTier?.entryQuota != null;

  function handleMemberChange(event) {
    setMemberId(event.target.value);
    setSuccess("");
    setError("");
  }

  function handleTierChange(event) {
    setTierId(event.target.value);
    setEndDate("");
    setSuccess("");
    setError("");
  }

  async function refreshHistory() {
    if (!memberId) return;
    const { data } = await api.get("/payments", { params: { memberId } });
    setPayments(data);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if (!selectedTier || saving) return;
    setSaving(true);
    setError("");
    setSuccess("");

    try {
      await api.post("/payments", {
        memberId: Number(memberId),
        tierId: Number(tierId),
        startDate,
        endDate: isEntryTier ? endDate : null,
        status: initialStatus
      });
      setSuccess("Payment and membership subscription were registered.");
      setEndDate("");
      await refreshHistory();
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Payment could not be registered."));
    } finally {
      setSaving(false);
    }
  }

  async function handleStatusChange(payment, status) {
    setError("");
    setSuccess("");
    setSaving(true);
    try {
      await api.put(`/payments/${payment.id}/status`, { status });
      setSuccess(`Payment marked as ${status.toLowerCase()}.`);
      await refreshHistory();
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Payment status could not be updated."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="payment-page">
      <section className="payment-card" aria-labelledby="payment-title">
        <h1 id="payment-title">Member payments</h1>
        <p>Register a membership payment and review the member's payment history.</p>

        {error && <div className="payment-alert payment-alert-error" role="alert">{error}</div>}
        {success && <div className="payment-alert payment-alert-success" role="status">{success}</div>}

        <form className="payment-form" onSubmit={handleSubmit}>
          <label>
            Member
            <select
              disabled={loading}
              onChange={handleMemberChange}
              required
              value={memberId}
            >
              <option value="">{loading ? "Loading members..." : "Choose a member"}</option>
              {members.map((member) => (
                <option key={member.id} value={member.id}>
                  {member.firstName} {member.lastName} ({member.email})
                </option>
              ))}
            </select>
          </label>

          <label>
            Membership type
            <select
              disabled={loading || tiers.length === 0}
              onChange={handleTierChange}
              required
              value={tierId}
            >
              <option value="">{loading ? "Loading membership types..." : "Choose a membership"}</option>
              {tiers.map((tier) => (
                <option key={tier.id} value={tier.id}>
                  {tier.name} — {Number(tier.price).toFixed(2)} €
                </option>
              ))}
            </select>
          </label>

          {selectedTier && (
            <p className="payment-price">
              Full payment amount: <strong>{Number(selectedTier.price).toFixed(2)} €</strong>
            </p>
          )}

          <label>
            Subscription start date
            <input
              onChange={(event) => setStartDate(event.target.value)}
              required
              type="date"
              value={startDate}
            />
          </label>

          {isEntryTier && (
            <label>
              Subscription end date
              <input
                min={startDate}
                onChange={(event) => setEndDate(event.target.value)}
                required
                type="date"
                value={endDate}
              />
            </label>
          )}

          <label>
            Initial payment status
            <select
              onChange={(event) => setInitialStatus(event.target.value)}
              value={initialStatus}
            >
              <option value="PAID">Paid</option>
              <option value="PENDING">Pending</option>
            </select>
          </label>

          <button
            className="payment-primary-button"
            disabled={loading || saving || !memberId || !selectedTier || (isEntryTier && !endDate)}
            type="submit"
          >
            {saving ? "Saving..." : "Register payment"}
          </button>
        </form>

        <section className="payment-history" aria-labelledby="payment-history-title">
          <h2 id="payment-history-title">Payment history</h2>
          {!memberId ? (
            <p>Select a member to view their payment history.</p>
          ) : loadingHistory ? (
            <p role="status">Loading payment history...</p>
          ) : payments.length === 0 ? (
            <p>No payments have been registered for this member.</p>
          ) : (
            <ul className="payment-list">
              {payments.map((payment) => (
                <li className="payment-list-item" key={payment.id}>
                  <div>
                    <strong>{payment.tierName} — {Number(payment.amount).toFixed(2)} €</strong>
                    <span>
                      {payment.startDate} to {payment.endDate} · Payment {payment.status.toLowerCase()}
                    </span>
                    {payment.remainingEntries != null && (
                      <span>{payment.remainingEntries} entries remaining</span>
                    )}
                  </div>
                  {payment.status === "PENDING" && (
                    <button
                      className="payment-secondary-button"
                      disabled={saving}
                      onClick={() => handleStatusChange(payment, "PAID")}
                      type="button"
                    >
                      Mark as paid
                    </button>
                  )}
                  {payment.status === "PAID" && (
                    <button
                      className="payment-secondary-button"
                      disabled={saving}
                      onClick={() => handleStatusChange(payment, "REFUNDED")}
                      type="button"
                    >
                      Refund payment
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      </section>
    </main>
  );
}
