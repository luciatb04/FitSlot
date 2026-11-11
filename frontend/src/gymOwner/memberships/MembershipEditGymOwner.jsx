import React, { useCallback, useEffect, useState } from "react";
import tokenService from "../../services/token.service";
import "./membershipService.css";

const API_URL = "/api/membership-tiers";

const AVAILABLE_SERVICES = [
  { value: "WEIGHT_ROOM", label: "Weight Room" },
  { value: "CLASSES", label: "Group Classes" },
  { value: "POOL", label: "Swimming Pool" }
];

const TIME_BANDS = [
  { value: "FULL_DAY", label: "Full day" },
  { value: "MORNINGS_ONLY", label: "Mornings only" },
  { value: "AFTERNOONS_ONLY", label: "Afternoons only" },
  { value: "EVENINGS_ONLY", label: "Evenings only" }
];

const EMPTY_FORM = {
  name: "",
  price: "",
  planType: "duration",
  durationDays: "",
  entryQuota: "",
  allowedServices: [],
  timeBand: "FULL_DAY"
};

async function getErrorMessage(response, fallback) {
  const body = await response.text();

  if (body) {
    try {
      const error = JSON.parse(body);
      if (error.message) return error.message;
    } catch {
      return body;
    }
  }

  return response.statusText || fallback;
}

function MembershipServices() {
  const [tiers, setTiers] = useState([]);
  const [selectedTier, setSelectedTier] = useState("");
  const [services, setServices] = useState([]);
  const [timeBand, setTimeBand] = useState("");
  const [form, setForm] = useState(EMPTY_FORM);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [loadingTiers, setLoadingTiers] = useState(true);
  const [saving, setSaving] = useState(false);
  const [loadingConfiguration, setLoadingConfiguration] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const getHeaders = useCallback(() => {
    const token = tokenService.getLocalAccessToken();
    return {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      "Content-Type": "application/json"
    };
  }, []);

  const loadTiers = useCallback(async () => {
    setLoadingTiers(true);
    setError("");

    try {
      const response = await fetch(API_URL, { headers: getHeaders() });
      if (!response.ok) {
        throw new Error(await getErrorMessage(response, "Could not load membership types"));
      }

      const data = await response.json();
      if (!Array.isArray(data)) {
        throw new Error("The server returned an invalid membership list");
      }
      setTiers(data);
    } catch (requestError) {
      setError(requestError.message || "Could not load membership types");
    } finally {
      setLoadingTiers(false);
    }
  }, [getHeaders]);

  useEffect(() => {
    loadTiers();
  }, [loadTiers]);

  useEffect(() => {
    if (!selectedTier) {
      setServices([]);
      setTimeBand("");
      return undefined;
    }

    let cancelled = false;
    async function loadConfiguration() {
      setLoadingConfiguration(true);
      setError("");
      setMessage("");
      setServices([]);
      setTimeBand("");

      try {
        const [servicesResponse, timeBandResponse] = await Promise.all([
          fetch(`${API_URL}/${selectedTier}/services`, { headers: getHeaders() }),
          fetch(`${API_URL}/${selectedTier}/time-band`, { headers: getHeaders() })
        ]);

        if (!servicesResponse.ok) {
          throw new Error(await getErrorMessage(servicesResponse, "Could not load membership services"));
        }
        if (!timeBandResponse.ok) {
          throw new Error(await getErrorMessage(timeBandResponse, "Could not load access time band"));
        }

        const [loadedServices, loadedTimeBand] = await Promise.all([
          servicesResponse.json(),
          timeBandResponse.json()
        ]);
        if (!Array.isArray(loadedServices) || !TIME_BANDS.some((band) => band.value === loadedTimeBand)) {
          throw new Error("The server returned an invalid membership configuration");
        }

        if (!cancelled) {
          setServices(loadedServices);
          setTimeBand(loadedTimeBand);
        }
      } catch (requestError) {
        if (!cancelled) {
          setError(requestError.message || "Could not load membership configuration");
        }
      } finally {
        if (!cancelled) {
          setLoadingConfiguration(false);
        }
      }
    }

    loadConfiguration();
    return () => {
      cancelled = true;
    };
  }, [getHeaders, selectedTier]);

  function updateForm(event) {
    const { name, value } = event.target;
    setForm((current) => {
      if (name === "planType") {
        return {
          ...current,
          planType: value,
          durationDays: "",
          entryQuota: ""
        };
      }
      return { ...current, [name]: value };
    });
  }

  function toggleFormService(service) {
    setForm((current) => ({
      ...current,
      allowedServices: current.allowedServices.includes(service)
        ? current.allowedServices.filter((item) => item !== service)
        : [...current.allowedServices, service]
    }));
  }

  function resetCreateForm() {
    setForm(EMPTY_FORM);
    setShowCreateForm(false);
  }

  async function handleCreate(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setMessage("");

    const request = {
      name: form.name.trim(),
      price: Number(form.price),
      // The API contract accepts only one limit at a time.
      durationDays: form.planType === "duration" ? Number(form.durationDays) : null,
      entryQuota: form.planType === "entries" ? Number(form.entryQuota) : null,
      allowedServices: form.allowedServices,
      timeBand: form.timeBand
    };

    try {
      const response = await fetch(API_URL, {
        method: "POST",
        headers: getHeaders(),
        body: JSON.stringify(request)
      });

      if (!response.ok) {
        throw new Error(await getErrorMessage(response, "Could not create membership type"));
      }

      const createdTier = await response.json();
      setTiers((current) => [...current, createdTier].sort((a, b) => a.name.localeCompare(b.name)));
      resetCreateForm();
      setMessage("Membership type created successfully.");
    } catch (requestError) {
      setError(requestError.message || "Could not create membership type");
    } finally {
      setSaving(false);
    }
  }

  function toggleSelectedService(service) {
    setMessage("");
    setServices((current) => (
      current.includes(service)
        ? current.filter((item) => item !== service)
        : [...current, service]
    ));
  }

  async function handleSaveConfiguration() {
    if (!selectedTier || services.length === 0 || !timeBand) {
      setError("Select at least one service and an access time band.");
      return;
    }

    setSaving(true);
    setError("");
    setMessage("");
    let servicesSaved = false;

    try {
      const servicesResponse = await fetch(`${API_URL}/${selectedTier}/services`, {
        method: "PUT",
        headers: getHeaders(),
        body: JSON.stringify(services)
      });
      if (!servicesResponse.ok) {
        throw new Error(await getErrorMessage(servicesResponse, "Could not update services"));
      }
      const updatedServices = await servicesResponse.json();
      servicesSaved = true;
      setTiers((current) => current.map((tier) => (
        String(tier.id) === selectedTier
          ? { ...tier, allowedServices: updatedServices }
          : tier
      )));

      const timeBandResponse = await fetch(`${API_URL}/${selectedTier}/time-band`, {
        method: "PUT",
        headers: getHeaders(),
        body: JSON.stringify(timeBand)
      });
      if (!timeBandResponse.ok) {
        throw new Error(await getErrorMessage(timeBandResponse, "Could not update access time band"));
      }
      const updatedTimeBand = await timeBandResponse.json();

      setTimeBand(updatedTimeBand);
      setTiers((current) => current.map((tier) => (
        String(tier.id) === selectedTier
          ? { ...tier, allowedServices: updatedServices, timeBand: updatedTimeBand }
          : tier
      )));
      setMessage("Membership configuration saved successfully.");
    } catch (requestError) {
      const failure = requestError.message || "Could not save membership configuration";
      // These settings use separate endpoints, so a failed second request can leave a partial save.
      setError(servicesSaved ? `Services were saved, but the access time was not: ${failure}` : failure);
    } finally {
      setSaving(false);
    }
  }

  function formatLimit(tier) {
    if (tier.durationDays != null) return `${tier.durationDays} days`;
    if (tier.entryQuota != null) return `${tier.entryQuota} entries`;
    return "Not configured";
  }

  return (
    <main className="membership-services-container">
      <header className="membership-page-header">
        <div>
          <h1>Membership types</h1>
          <p>View plans and manage their services and access time.</p>
        </div>
        <button
          className="membership-primary-button"
          type="button"
          aria-expanded={showCreateForm}
          aria-controls="create-membership-form"
          onClick={() => {
            setShowCreateForm((visible) => !visible);
            setError("");
            setMessage("");
          }}
        >
          {showCreateForm ? "Cancel" : "Create membership"}
        </button>
      </header>

      {message && <p className="success-message" role="status">{message}</p>}
      {error && <p className="error-message" role="alert">{error}</p>}

      {showCreateForm && (
        <form id="create-membership-form" className="membership-form" onSubmit={handleCreate}>
          <h2>Create a membership type</h2>
          <label className="membership-field" htmlFor="tier-name">
            Name
            <input
              id="tier-name"
              name="name"
              value={form.name}
              onChange={updateForm}
              required
              maxLength={100}
            />
          </label>

          <label className="membership-field" htmlFor="tier-price">
            Price
            <input
              id="tier-price"
              name="price"
              type="number"
              min="0.01"
              step="0.01"
              value={form.price}
              onChange={updateForm}
              required
            />
          </label>

          <fieldset className="membership-fieldset">
            <legend>Membership limit</legend>
            <label>
              <input
                type="radio"
                name="planType"
                value="duration"
                checked={form.planType === "duration"}
                onChange={updateForm}
              />
              By duration
            </label>
            <label>
              <input
                type="radio"
                name="planType"
                value="entries"
                checked={form.planType === "entries"}
                onChange={updateForm}
              />
              By entries
            </label>
          </fieldset>

          {form.planType === "duration" ? (
            <label className="membership-field" htmlFor="tier-duration">
              Duration (days)
              <input
                id="tier-duration"
                name="durationDays"
                type="number"
                min="1"
                step="1"
                value={form.durationDays}
                onChange={updateForm}
                required
              />
            </label>
          ) : (
            <label className="membership-field" htmlFor="tier-quota">
              Number of entries
              <input
                id="tier-quota"
                name="entryQuota"
                type="number"
                min="1"
                step="1"
                value={form.entryQuota}
                onChange={updateForm}
                required
              />
            </label>
          )}

          <fieldset className="membership-fieldset">
            <legend>Included services (select at least one)</legend>
            {AVAILABLE_SERVICES.map((service) => (
              <label key={service.value}>
                <input
                  type="checkbox"
                  checked={form.allowedServices.includes(service.value)}
                  onChange={() => toggleFormService(service.value)}
                />
                {service.label}
              </label>
            ))}
          </fieldset>

          <label className="membership-field" htmlFor="tier-time-band">
            Access time
            <select
              id="tier-time-band"
              name="timeBand"
              value={form.timeBand}
              onChange={updateForm}
              required
            >
              {TIME_BANDS.map((band) => (
                <option key={band.value} value={band.value}>{band.label}</option>
              ))}
            </select>
          </label>

          <div className="membership-form-actions">
            <button
              className="membership-primary-button"
              type="submit"
              disabled={saving || form.allowedServices.length === 0}
            >
              {saving ? "Creating..." : "Save membership"}
            </button>
            <button
              className="membership-secondary-button"
              type="button"
              onClick={resetCreateForm}
              disabled={saving}
            >
              Cancel
            </button>
          </div>
        </form>
      )}

      <section className="membership-list-section" aria-labelledby="membership-list-title">
        <h2 id="membership-list-title">Available memberships</h2>
        {loadingTiers ? (
          <p role="status">Loading membership types...</p>
        ) : tiers.length === 0 ? (
          <p>No membership types have been created yet.</p>
        ) : (
          <div className="membership-tier-grid">
            {tiers.map((tier) => (
              <article className="membership-tier-card" key={tier.id}>
                <h3>{tier.name}</h3>
                <p className="membership-tier-price">{Number(tier.price).toFixed(2)} €</p>
                <dl>
                  <div><dt>Limit</dt><dd>{formatLimit(tier)}</dd></div>
                  <div>
                    <dt>Services</dt>
                    <dd>{(tier.allowedServices || []).join(", ") || "None"}</dd>
                  </div>
                  <div><dt>Access time</dt><dd>{tier.timeBand}</dd></div>
                </dl>
                <button
                  className="membership-secondary-button"
                  type="button"
                  aria-pressed={selectedTier === String(tier.id)}
                  onClick={() => setSelectedTier(String(tier.id))}
                >
                  {selectedTier === String(tier.id) ? "Selected" : "Edit services and time"}
                </button>
              </article>
            ))}
          </div>
        )}
      </section>

      {selectedTier && (
        <section className="membership-form" aria-labelledby="edit-membership-title">
          <h2 id="edit-membership-title">Edit services and access time</h2>
          {loadingConfiguration ? (
            <p role="status">Loading membership configuration...</p>
          ) : (
            <>
              <fieldset className="membership-fieldset">
                <legend>Included services (select at least one)</legend>
                {AVAILABLE_SERVICES.map((service) => (
                  <label key={service.value}>
                    <input
                      type="checkbox"
                      checked={services.includes(service.value)}
                      onChange={() => toggleSelectedService(service.value)}
                    />
                    {service.label}
                  </label>
                ))}
              </fieldset>

              <fieldset className="membership-fieldset">
                <legend>Access time</legend>
                {TIME_BANDS.map((band) => (
                  <label key={band.value}>
                    <input
                      type="radio"
                      name="edit-time-band"
                      value={band.value}
                      checked={timeBand === band.value}
                      onChange={() => setTimeBand(band.value)}
                    />
                    {band.label}
                  </label>
                ))}
              </fieldset>
              <button
                className="membership-primary-button"
                type="button"
                onClick={handleSaveConfiguration}
                disabled={saving || loadingConfiguration || services.length === 0 || !timeBand}
              >
                {saving ? "Saving..." : "Save changes"}
              </button>
            </>
          )}
        </section>
      )}
    </main>
  );
}

export default MembershipServices;
