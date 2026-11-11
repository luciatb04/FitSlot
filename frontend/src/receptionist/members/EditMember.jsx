import { useEffect, useMemo, useState } from "react";
import { Link, useParams, useNavigate } from "react-router-dom";
import api from "../../services/api";
import "./MemberRegistration.css";


const EMPTY_FORM = { firstName: "", lastName: "", email: "", telephone: "" };

function memberToForm(member) {
    return {
        firstName: member.firstName ?? "",
        lastName: member.lastName ?? "",
        email: member.email ?? "",
        telephone: member.telephone ?? ""
    };
}

function normalizedForm(form) {
    return {
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        telephone: form.telephone.trim()
    };
}

function apiErrorMessage(error) {
    const status = error.response?.status;
    if (status === 400) return "Please check the information you entered.";
    if (status === 401) return "Your session has expired. Please sign in again.";
    if (status === 403) return "You do not have permission to edit members.";
    if (status === 404) return "This member does not exist.";
    if (status === 409) return "This email address is already registered to another member.";
    return "The operation could not be completed. Please try again.";
}

export default function EditMember() {
    const { id } = useParams();
    const [form, setForm] = useState(EMPTY_FORM);
    const [original, setOriginal] = useState(EMPTY_FORM);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [loadError, setLoadError] = useState("");
    const [formError, setFormError] = useState("");
    const [success, setSuccess] = useState("");
    const [reloadKey, setReloadKey] = useState(0);
    const navigate = useNavigate();


    useEffect(() => {
        let active = true;

        async function loadMember() {
            setLoading(true);
            setLoadError("");
            setFormError("");
            setSuccess("");

            try {
                const response = await api.get(`/members/${id}`);
                if (active) {
                    const data = memberToForm(response.data);
                    setForm(data);
                    setOriginal(data);
                }
            } catch (error) {
                if (active) setLoadError(apiErrorMessage(error));
            } finally {
                if (active) setLoading(false);
            }
        }

        loadMember();
        return () => { active = false; };
    }, [id, reloadKey]);

    const hasChanges = useMemo(() => {
        const current = normalizedForm(form);
        const before = normalizedForm(original);
        return Object.keys(current).some((key) => current[key] !== before[key]);
    }, [form, original]);

    function handleChange(event) {
        const { name, value } = event.target;
        setForm((previous) => ({ ...previous, [name]: value }));
        setFormError("");
        setSuccess("");
    }

    function resetChanges() {
        setForm({ ...original });
        setFormError("");
        setSuccess("");
    }

    async function handleSubmit(event) {
        event.preventDefault();
        if (!hasChanges || saving) return;

        const values = normalizedForm(form);
        if (!values.firstName || !values.lastName || !values.email) {
            setFormError("First name, last name and email are required.");
            return;
        }

        setSaving(true);
        setFormError("");
        setSuccess("");
        try {
            const response = await api.put(`/members/${id}`, {
                ...values,
                telephone: values.telephone || null
            });
            const savedData = memberToForm(response.data);
            setForm(savedData);
            setOriginal(savedData);
            setSuccess("Member details updated successfully.");

            setTimeout(() => {
                navigate("/members");
            }, 1500);
        } catch (error) {
            setFormError(apiErrorMessage(error));
        } finally {
            setSaving(false);
        }
    }

    const fullName = `${original.firstName} ${original.lastName}`.trim();

    return (
        <main className="fm-page">
            <div className="fm-container fm-container-narrow">
                <nav className="fm-breadcrumb" aria-label="Breadcrumb">
                    <Link to="/members">Members</Link>
                    <span aria-hidden="true">/</span>
                    <span>Edit member</span>
                </nav>

                <header className="fm-page-header">
                    <div>
                        <p className="fm-eyebrow">MEMBER PROFILE</p>
                        <h1>Edit member</h1>
                        <p className="fm-subtitle">Update personal and contact information.</p>
                    </div>
                    <Link to="/members" className="fm-button fm-button-secondary">← Back to members</Link>
                </header>

                <section className="fm-card fm-edit-card">
                    {loading ? (
                        <p className="fm-empty" role="status">Loading member details...</p>
                    ) : loadError ? (
                        <div className="fm-alert fm-alert-error" role="alert">
                            {loadError} <button type="button" onClick={() => setReloadKey((key) => key + 1)}>Retry</button>
                        </div>
                    ) : (
                        <>
                            <div className="fm-profile-header">
                                <span className="fm-avatar fm-avatar-large" aria-hidden="true">
                                    {`${original.firstName?.[0] ?? ""}${original.lastName?.[0] ?? ""}`.toUpperCase() || "?"}
                                </span>
                                <div>
                                    <h2>{fullName || "Member profile"}</h2>
                                    <p className="fm-muted">Member ID #{id}</p>
                                </div>
                            </div>

                            {formError && <div className="fm-alert fm-alert-error" role="alert">{formError}</div>}
                            {success && <div className="fm-alert fm-alert-success" role="status">{success}</div>}

                            <form onSubmit={handleSubmit}>
                                <h3 className="fm-section-title">Personal information</h3>
                                <div className="fm-form-grid">
                                    <label className="fm-field">
                                        <span>First name <span aria-hidden="true">*</span></span>
                                        <input name="firstName" value={form.firstName} onChange={handleChange} autoComplete="given-name" required disabled={saving} />
                                    </label>
                                    <label className="fm-field">
                                        <span>Last name <span aria-hidden="true">*</span></span>
                                        <input name="lastName" value={form.lastName} onChange={handleChange} autoComplete="family-name" required disabled={saving} />
                                    </label>
                                </div>

                                <h3 className="fm-section-title fm-section-spacing">Contact details</h3>
                                <div className="fm-form-grid">
                                    <label className="fm-field">
                                        <span>Email address <span aria-hidden="true">*</span></span>
                                        <input name="email" type="email" value={form.email} onChange={handleChange} autoComplete="email" required disabled={saving} />
                                    </label>
                                    <label className="fm-field">
                                        <span>Telephone <span className="fm-optional">(optional)</span></span>
                                        <input name="telephone" type="tel" value={form.telephone} onChange={handleChange} autoComplete="tel" disabled={saving} />
                                    </label>
                                </div>

                                <footer className="fm-form-footer">
                                    <span className="fm-muted fm-footer-note">* Required fields</span>
                                    <div className="fm-form-actions">
                                        <button type="button" className="fm-button fm-button-secondary" onClick={resetChanges} disabled={!hasChanges || saving}>Reset changes</button>
                                        <button type="submit" className="fm-button fm-button-primary" disabled={!hasChanges || saving}>
                                            {saving ? "Saving..." : "Save changes"}
                                        </button>
                                    </div>
                                </footer>
                            </form>
                        </>
                    )}
                </section>
            </div>
        </main>
    );
}
