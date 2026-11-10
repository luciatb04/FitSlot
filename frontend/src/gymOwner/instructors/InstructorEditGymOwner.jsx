import { useState } from "react";
import { Link } from "react-router-dom";
import {
  Form,
  Input,
  Label,
} from "reactstrap";
import tokenService from "../../services/token.service";
import getErrorModal from "../../util/getErrorModal";
import getIdFromUrl from "../../util/getIdFromUrl";
import useFetchState from "../../util/useFetchState";

const jwt = tokenService.getLocalAccessToken();

export default function InstructorEditGymOwner() {
  const emptyItem = {
    id: "",
    firstName: "",
    lastName: "",
    user: {},
  };
  const id = getIdFromUrl(2);
  const [message, setMessage] = useState(null);
  const [visible, setVisible] = useState(false);
  const [instructor, setInstructor] = useFetchState(
    emptyItem,
    `/api/v1/instructors/${id}`,
    jwt,
    setMessage,
    setVisible,
    id
  );

  function handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    setInstructor({ ...instructor, [name]: value });
  }

  function handleSubmit(event) {
    event.preventDefault();

    fetch("/api/v1/instructors" + (instructor.id ? "/" + instructor.id : ""), {
      method: instructor.id ? "PUT" : "POST",
      headers: {
        Authorization: `Bearer ${jwt}`,
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify(instructor),
    })
      .then((response) => response.json())
      .then((json) => {
        if (json.message) {
          setMessage(json.message);
          setVisible(true);
        } else window.location.href = "/instructors";
      })
      .catch((message) => alert(message));
  }

  const modal = getErrorModal(setVisible, visible, message);
  return (
    <div className="auth-page-container">
      {<h2>{instructor.id ? "Edit Instructor" : "Add Instructor"}</h2>}
      {modal}
      <div className="auth-form-container">
        <Form onSubmit={handleSubmit}>
          <div className="custom-form-input">
            <Label for="firstName" className="custom-form-input-label">
              First Name
            </Label>
            <Input
              type="text"
              name="firstName"
              id="firstName"
              value={instructor.firstName || ""}
              onChange={handleChange}
              className="custom-input"
            />
          </div>
          <div className="custom-form-input">
            <Label for="lastName" className="custom-form-input-label">
              Last Name
            </Label>
            <Input
              type="text"
              name="lastName"
              id="lastName"
              value={instructor.lastName || ""}
              onChange={handleChange}
              className="custom-input"
            />
          </div>
          <div className="custom-form-input">
            <Label for="city" className="custom-form-input-label">
              City
            </Label>
            <Input
              type="text"
              name="city"
              id="city"
              value={instructor.city || ""}
              onChange={handleChange}
              className="custom-input"
            />
          </div>
          <div className="custom-button-row">
            <button className="auth-button">Save</button>
            <Link
              to={`/instructors`}
              className="auth-button"
              style={{ textDecoration: "none" }}
            >
              Cancel
            </Link>
          </div>
        </Form>
      </div>
    </div>
  );
}
