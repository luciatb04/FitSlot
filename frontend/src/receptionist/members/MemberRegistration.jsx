import { useState } from "react";
import api from "../../services/api";
import "./memberRegistration.css";

const initialForm = {
  firstName: "",
  lastName: "",
  email: "",
  telephone: ""
};

export default function MemberRegistration() {
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setError("");
    setSuccess("");
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setSuccess("");
    setIsSubmitting(true);

    try {
      const response = await api.post("/members", {
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        telephone: form.telephone.trim() || null
      });
      setSuccess(`${response.data.firstName} ${response.data.lastName} was registered with an active profile.`);
      setForm(initialForm);
    } catch (requestError) {
      const status = requestError.response?.status;
      const responseMessage = requestError.response?.data?.message;
      setError(
        status === 403
          ? "Your account does not have permission to register members. Sign out and sign in with a receptionist account."
          : status === 401
            ? "Your session has expired. Sign in again to register members."
            : responseMessage ||
            "The member could not be registered. Please check the information and try again."
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="member-registration-page">
      <section className="member-registration-card" aria-labelledby="member-registration-title">
        <h1 id="member-registration-title">Register a new member</h1>
        <p>Enter the member's personal and contact information.</p>

        {error && <div className="member-registration-error" role="alert">{error}</div>}
        {success && <div className="member-registration-success" role="status">{success}</div>}

        <form onSubmit={handleSubmit}>
          <div className="member-registration-fields">
            <label>
              First name
              <input
                autoComplete="given-name"
                name="firstName"
                onChange={handleChange}
                required
                value={form.firstName}
              />
            </label>

            <label>
              Last name
              <input
                autoComplete="family-name"
                name="lastName"
                onChange={handleChange}
                required
                value={form.lastName}
              />
            </label>

            <label>
              Email
              <input
                autoComplete="email"
                name="email"
                onChange={handleChange}
                required
                type="email"
                value={form.email}
              />
            </label>

            <label>
              Telephone (optional)
              <input
                autoComplete="tel"
                name="telephone"
                onChange={handleChange}
                type="tel"
                value={form.telephone}
              />
            </label>
          </div>

          <button className="member-registration-submit" disabled={isSubmitting} type="submit">
            {isSubmitting ? "Registering..." : "Register member"}
          </button>
        </form>
      </section>
    </main>
  );
}
