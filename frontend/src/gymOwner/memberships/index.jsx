import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import tokenService from "../../services/token.service";
import "./membershipService.css";

const API_URL = "/api/membership-tiers";

const AVAILABLE_SERVICES = [
  { value: "WEIGHT_ROOM", label: "Weight Room" },
  { value: "CLASSES", label: "Group Classes" },
  { value: "POOL", label: "Swimming Pool" }
];

function MembershipServices() {
  const navigate = useNavigate();
  const [tiers, setTiers] = useState([]);
  const [selectedTier, setSelectedTier] = useState("");
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingServices, setLoadingServices] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const getHeaders = () => {
    const token = tokenService.getLocalAccessToken();

    return {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json"
    };
  };

  useEffect(() => {
    async function loadTiers() {
      try {
        const response = await fetch(API_URL, {
          headers: getHeaders()
        });

        if (!response.ok) {
          throw new Error("Could not load membership types");
        }

        const data = await response.json();
        setTiers(data);
      } catch (err) {
        setError(err.message);
      }
    }

    loadTiers();
  }, []);

  useEffect(() => {
    if (!selectedTier) return;

    let cancelled = false;

    async function loadServices() {
      setLoadingServices(true);
      setError("");
      setMessage("");
      setServices([]);

      try {
        const response = await fetch(
          `${API_URL}/${selectedTier}/services`,
          { headers: getHeaders() }
        );

        if (!response.ok) {
          throw new Error("Could not load services");
        }

        const data = await response.json();

        if (!cancelled) {
          setServices(data);
        }
      } catch (err) {
        if (!cancelled) setError(err.message);
      } finally {
        if (!cancelled) setLoadingServices(false);
      }
    }

    loadServices();

    return () => {
      cancelled = true;
    };
  }, [selectedTier]);

  const handleServiceChange = (service) => {
    setMessage("");

    setServices((previous) =>
      previous.includes(service)
        ? previous.filter((item) => item !== service)
        : [...previous, service]
    );
  };

  const handleSave = async () => {
    setLoading(true);
    setError("");
    setMessage("");

    try {
      const response = await fetch(
        `${API_URL}/${selectedTier}/services`,
        {
          method: "PUT",
          headers: getHeaders(),
          body: JSON.stringify(services)
        }
      );

      if (!response.ok) {
        throw new Error("Could not update services");
      }

      const updatedServices = await response.json();

      setServices(updatedServices);
      setMessage("membership saved succesfuly");
      window.setTimeout(() => navigate("/"), 3000);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="membership-services-container">
      <h2>Membership Services Management</h2>

      <p>
        Select a membership type and manage
        the facilities included in it.
      </p>

      <div className="membership-form">
        <label htmlFor="membership-tier">
          Membership Type
        </label>

        <select
          id="membership-tier"
          value={selectedTier}
          onChange={(e) => setSelectedTier(e.target.value)}
        >
          <option value="">Select a membership type</option>

          {tiers.map((tier) => (
            <option key={tier.id} value={tier.id}>
              {tier.name}
            </option>
          ))}
        </select>

        {selectedTier && !loadingServices && (
          <div className="services-section">
            <h3>Included Services</h3>

            {AVAILABLE_SERVICES.map((service) => (
              <label
                key={service.value}
                className="service-checkbox"
              >
                <input
                  type="checkbox"
                  checked={services.includes(service.value)}
                  onChange={() =>
                    handleServiceChange(service.value)
                  }
                />

                {service.label}
              </label>
            ))}

            <button
              className="save-services-btn"
              onClick={handleSave}
              disabled={loading}
            >
              {loading ? "Saving..." : "Save Changes"}
            </button>
          </div>
        )}

        {loadingServices && <p>Loading services...</p>}

        {message && (
          <p className="success-message" role="status">
            {message}
          </p>
        )}

        {error && (
          <p className="error-message" role="alert">
            {error}
          </p>
        )}
      </div>
    </div>
  );
}

export default MembershipServices;
