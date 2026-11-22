
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api from "../../services/api";
import "./SubstituteInstructor.css";

function formatTime(value) {
    return value ? value.slice(0, 5) : "--:--";
}

function formatDate(value) {
    if (!value) return "Date not available";

    const [year, month, day] = value.split("-").map(Number);
    const date = new Date(year, month - 1, day, 12);

    return date.toLocaleDateString("en-GB", {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric"
    });
}

function formatRoom(value) {
    if (!value) return "Room not available";

    return value
        .replaceAll("_", " ")
        .toLowerCase()
        .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function getErrorMessage(error) {
    const status = error.response?.status;
    const message =
        error.response?.data?.message ||
        error.response?.data?.detail;

    if (status === 400) {
        return message || "Invalid instructor selection.";
    }

    if (status === 401) {
        return "Your session has expired. Please sign in again.";
    }

    if (status === 403) {
        return "You are not authorized to manage this class.";
    }

    if (status === 404) {
        return "The class or instructor could not be found.";
    }

    if (status === 409) {
        return message ||
            "The selected instructor is no longer available.";
    }

    return "An unexpected error occurred. Please try again.";
}

export default function SubstituteInstructor() {
    const { id } = useParams();

    const [classSession, setClassSession] = useState(null);
    const [instructors, setInstructors] = useState([]);
    const [selectedInstructorId, setSelectedInstructorId] =
        useState("");

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [completed, setCompleted] = useState(false);

    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [reloadKey, setReloadKey] = useState(0);

    useEffect(() => {
        let active = true;

        async function loadData() {
            setLoading(true);
            setError("");
            setSuccess("");
            setCompleted(false);
            setSelectedInstructorId("");

            try {
                const [sessionResponse, instructorsResponse] =
                    await Promise.all([
                        api.get(`/class-sessions/${id}`),
                        api.get(
                            `/class-sessions/${id}/available-instructors`
                        )
                    ]);

                if (!Array.isArray(instructorsResponse.data)) {
                    throw new Error("Invalid instructor response");
                }

                if (active) {
                    setClassSession(sessionResponse.data);
                    setInstructors(instructorsResponse.data);
                }
            } catch (requestError) {
                if (active) {
                    setError(getErrorMessage(requestError));
                    setClassSession(null);
                    setInstructors([]);
                }
            } finally {
                if (active) {
                    setLoading(false);
                }
            }
        }

        loadData();

        return () => {
            active = false;
        };
    }, [id, reloadKey]);

    const selectedInstructor = instructors.find(
        (instructor) =>
            String(instructor.id) === selectedInstructorId
    );

    async function handleSubmit(event) {
        event.preventDefault();

        if (
            !selectedInstructor ||
            saving ||
            loading ||
            completed
        ) {
            return;
        }

        const instructorName =
            `${selectedInstructor.firstName} ` +
            `${selectedInstructor.lastName}`;

        const confirmed = window.confirm(
            `Do you want to assign ${instructorName} ` +
            `to the class "${classSession.name}"?`
        );

        if (!confirmed) return;

        setSaving(true);
        setError("");
        setSuccess("");

        try {
            await api.put(
                `/class-sessions/${id}/instructor`,
                {
                    instructorId: selectedInstructor.id
                }
            );

            setCompleted(true);
            setSelectedInstructorId("");
            setSuccess(
                `${instructorName} has been assigned ` +
                `to this class successfully.`
            );

        } catch (requestError) {
            setError(getErrorMessage(requestError));
        } finally {
            setSaving(false);
        }
    }

    return (
        <main className="substitute-page">
            <div className="substitute-container">

                <div className="substitute-header">
                    <div>
                        <p className="substitute-eyebrow">
                            GYM OWNER / MANAGE CLASSES
                        </p>

                        <h1>
                            {classSession?.instructorId
                                ? "Substitute Instructor"
                                : "Assign Instructor"}
                        </h1>

                        <p>
                            Select an available instructor to take over
                            a scheduled class.
                        </p>
                    </div>

                    <Link
                        to="/manage-classes"
                        className="substitute-button secondary"
                    >
                        Back to Classes
                    </Link>
                </div>

                {error && (
                    <div
                        className="substitute-alert error"
                        role="alert"
                    >
                        <span>{error}</span>

                        {!saving && (
                            <button
                                type="button"
                                onClick={() =>
                                    setReloadKey((previous) => previous + 1)
                                }
                            >
                                Refresh
                            </button>
                        )}
                    </div>
                )}

                {success && (
                    <div
                        className="substitute-alert success"
                        role="status"
                    >
                        {success}
                    </div>
                )}

                {loading && (
                    <div className="substitute-card">
                        <p role="status">
                            Loading class and available instructors...
                        </p>
                    </div>
                )}

                {!loading && classSession && (
                    <>

                        <section className="substitute-card">

                            <div className="substitute-section-header">
                                <h2>Class Information</h2>
                                <p>
                                    Review the class schedule before choosing
                                    a new instructor.
                                </p>
                            </div>

                            <h3 className="substitute-class-name">
                                {classSession.name}
                            </h3>

                            <div className="substitute-info-grid">
                                <div className="substitute-info-item">
                                    <span>Current Instructor</span>

                                    <strong>
                                        {classSession.instructorName ||
                                            "No instructor assigned"}
                                    </strong>
                                </div>
                                <div className="substitute-info-item">
                                    <span>Date</span>
                                    <strong>
                                        {formatDate(classSession.date)}
                                    </strong>
                                </div>

                                <div className="substitute-info-item">
                                    <span>Time</span>
                                    <strong>
                                        {formatTime(classSession.startTime)}
                                        {" - "}
                                        {formatTime(classSession.endTime)}
                                    </strong>
                                </div>

                                <div className="substitute-info-item">
                                    <span>Room</span>
                                    <strong>
                                        {formatRoom(classSession.room)}
                                    </strong>
                                </div>

                                <div className="substitute-info-item">
                                    <span>Maximum Capacity</span>
                                    <strong>
                                        {classSession.maxCapacity}
                                    </strong>
                                </div>
                                <div className="substitute-info-item">
                                    <span>Current Instructor</span>

                                    <strong>
                                        {classSession.instructorName ||
                                            "No instructor assigned"}
                                    </strong>
                                </div>
                            </div>
                        </section>

                        <section className="substitute-card">

                            <div className="substitute-section-header">
                                <h2>Available Instructors</h2>
                                <p>
                                    Only instructors available for the full
                                    class schedule are listed.
                                </p>
                            </div>

                            {completed ? (
                                <div className="substitute-completed">
                                    <h3>Substitution completed</h3>

                                    <p>
                                        The instructor has been updated
                                        successfully.
                                    </p>

                                    <Link
                                        to="/manage-classes"
                                        className="substitute-button primary"
                                    >
                                        Return to Classes
                                    </Link>
                                </div>
                            ) : instructors.length === 0 ? (
                                <div className="substitute-empty">
                                    <h3>No available instructors</h3>

                                    <p>
                                        There are currently no eligible
                                        instructors for this class schedule.
                                    </p>

                                    <button
                                        type="button"
                                        className="substitute-button secondary"
                                        onClick={() =>
                                            setReloadKey((previous) => previous + 1)
                                        }
                                    >
                                        Refresh Availability
                                    </button>
                                </div>
                            ) : (
                                <form onSubmit={handleSubmit}>

                                    <div className="substitute-instructor-list">
                                        {instructors.map((instructor) => {
                                            const selected =
                                                selectedInstructorId ===
                                                String(instructor.id);

                                            return (
                                                <label
                                                    key={instructor.id}
                                                    className={
                                                        "substitute-instructor-option" +
                                                        (selected ? " selected" : "")
                                                    }
                                                >
                                                    <input
                                                        type="radio"
                                                        name="substituteInstructor"
                                                        value={instructor.id}
                                                        checked={selected}
                                                        onChange={() => {
                                                            setSelectedInstructorId(
                                                                String(instructor.id)
                                                            );
                                                            setError("");
                                                        }}
                                                        disabled={saving}
                                                    />

                                                    <div className="substitute-instructor-info">
                                                        <strong>
                                                            {instructor.firstName}
                                                            {" "}
                                                            {instructor.lastName}
                                                        </strong>

                                                        <span>
                                                            Available for the selected
                                                            class schedule
                                                        </span>
                                                    </div>

                                                    <span className="substitute-available-badge">
                                                        Available
                                                    </span>
                                                </label>
                                            );
                                        })}
                                    </div>

                                    <div className="substitute-form-actions">

                                        <Link
                                            to="/manage-classes"
                                            className="substitute-button secondary"
                                        >
                                            Cancel
                                        </Link>

                                        <button
                                            type="submit"
                                            className="substitute-button primary"
                                            disabled={
                                                !selectedInstructorId || saving
                                            }
                                        >
                                            {saving
                                                ? "Saving..."
                                                : "Confirm Substitution"}
                                        </button>
                                    </div>
                                </form>
                            )}
                        </section>
                    </>
                )}
            </div>
        </main>
    );
}
