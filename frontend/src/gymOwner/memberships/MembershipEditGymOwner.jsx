import React, { useCallback, useEffect, useState } from "react";
import tokenService from "../../services/token.service";
import "./membershipService.css";

const API_URL = "/api/membership-tiers";
const TIME_BANDS_URL = "/api/time-bands";

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
  const [bandConfigurations, setBandConfigurations] = useState({});
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");


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
      const [tiersResponse, bandsResponse] = await Promise.all([
        fetch(API_URL, { headers: getHeaders() }),
        fetch(TIME_BANDS_URL, { headers: getHeaders() })
      ]);

      if (!tiersResponse.ok || !bandsResponse.ok) {
        throw new Error("Could not load membership information");
      }

      const [tiersData, bandsData] = await Promise.all([
        tiersResponse.json(),
        bandsResponse.json()
      ]);

      if (!Array.isArray(tiersData) || !Array.isArray(bandsData)) {
        throw new Error("Invalid membership information");
      }

      setTiers(tiersData);

      const configurations = {};

      bandsData.forEach((band) => {
        configurations[band.timeBand] = band;
      });

      setBandConfigurations(configurations);

    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoadingTiers(false);
    }
  }, [getHeaders]);


  useEffect(() => {
    loadTiers();
  }, [loadTiers]);

  useEffect(() => {
    const configuration = bandConfigurations[timeBand];

    if (configuration) {
      setStartTime(configuration.startTime.slice(0, 5));
      setEndTime(configuration.endTime.slice(0, 5));
    } else {
      setStartTime("");
      setEndTime("");
    }
  }, [timeBand, bandConfigurations]);

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
      setError("Select at least one service and a time band.");
      return;
    }

    if (!startTime || !endTime || startTime >= endTime) {
      setError("Start time must be earlier than end time.");
      return;
    }

    setSaving(true);
    setError("");
    setMessage("");

    try {
      const response = await fetch(
        `${API_URL}/${selectedTier}/configuration`,
        {
          method: "PUT",
          headers: getHeaders(),
          body: JSON.stringify({
            allowedServices: services,
            timeBand: timeBand,
            startTime: startTime,
            endTime: endTime
          })
        }
      );

      if (!response.ok) {
        throw new Error(
          await getErrorMessage(response, "Could not save membership")
        );
      }

      const updatedTier = await response.json();

      // Update the edited membership card
      setTiers((current) =>
        current.map((tier) =>
          String(tier.id) === String(updatedTier.id)
            ? updatedTier
            : tier
        )
      );

      // Update the schedule for all memberships using this band
      setBandConfigurations((current) => ({
        ...current,
        [timeBand]: {
          ...current[timeBand],
          timeBand: timeBand,
          startTime: startTime,
          endTime: endTime
        }
      }));

      // Close the editing and creation forms
      setSelectedTier("");
      setShowCreateForm(false);

      setMessage("Membership configuration saved successfully.");

    } catch (requestError) {
      setError(
        requestError.message || "Could not save membership configuration"
      );
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

          {bandConfigurations[form.timeBand] && (
            <p className="membership-time-note">
              Schedule:{" "}
              {bandConfigurations[form.timeBand].startTime.slice(0, 5)}
              {" - "}
              {bandConfigurations[form.timeBand].endTime.slice(0, 5)}
            </p>
          )}


          <div className="membership-form-actions">
            <button
              className="membership-primary-button"
              type="submit"
              disabled={
                saving ||
                form.allowedServices.length === 0
              }
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

                  <div>
                    <dt>Access time</dt>
                    <dd>
                      {TIME_BANDS.find(
                        (band) => band.value === tier.timeBand
                      )?.label || tier.timeBand}

                      {bandConfigurations[tier.timeBand] && (
                        <span>
                          {" "}(
                          {bandConfigurations[tier.timeBand].startTime.slice(0, 5)}
                          {" - "}
                          {bandConfigurations[tier.timeBand].endTime.slice(0, 5)}
                          )
                        </span>
                      )}
                    </dd>
                  </div>
                </dl>
                <button
                  className="membership-secondary-button"
                  type="button"
                  aria-pressed={selectedTier === String(tier.id)}

                  onClick={() => {
                    setSelectedTier((current) =>
                      current === String(tier.id) ? "" : String(tier.id)
                    );
                    setError("");
                    setMessage("");
                  }}
                >
                  {selectedTier === String(tier.id)
                    ? "Cancel editing"
                    : "Edit membership"}
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

              <div className="membership-time-fields">
                <label className="membership-field">
                  Start time
                  <input
                    type="time"
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    required
                  />
                </label>

                <label className="membership-field">
                  End time
                  <input
                    type="time"
                    value={endTime}
                    onChange={(e) => setEndTime(e.target.value)}
                    required
                  />
                </label>
              </div>

              <p className="membership-time-note">
                Changes to these hours will affect all memberships
                using the selected time band.
              </p>

              <button
                className="membership-primary-button"
                type="button"
                onClick={handleSaveConfiguration}
                disabled={saving || loadingConfiguration || services.length === 0 || !timeBand}
              >
                {saving ? "Saving..." : "Save changes"}
              </button>
              <button
                className="membership-secondary-button"
                type="button"
                onClick={() => setSelectedTier("")}
                disabled={saving}
              >
                Cancel
              </button>
            </>
          )}
        </section>
      )}
    </main>
  );
}

export default MembershipServices;
