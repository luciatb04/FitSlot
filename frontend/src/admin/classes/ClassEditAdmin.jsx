import { useState } from "react";
import { Link } from "react-router-dom";
import { Form, Input, Label } from "reactstrap";
import tokenService from "../../services/token.service";
import getErrorModal from "../../util/getErrorModal";
import useFetchState from "../../util/useFetchState";
import getIdFromUrl from "../../util/getIdFromUrl";
import "../../static/css/admin/adminPage.css";

const jwt = tokenService.getLocalAccessToken();

export default function ClassEditAdmin() {
  const emptyItem = { id: null, name: "", birthDate: "" };
  const id = getIdFromUrl(2);
  const [message, setMessage] = useState(null);
  const [visible, setVisible] = useState(false);
  const [classEntity, setClassEntity] = useFetchState(
    emptyItem,
    `/api/v1/classes/${id}`,
    jwt,
    setMessage,
    setVisible,
    id
  );

  function handleChange(event) {
    const { name, value } = event.target;
    setClassEntity({ ...classEntity, [name]: value });
  }

  function handleSubmit(event) {
    event.preventDefault();
    fetch("/api/v1/classes" + (classEntity.id ? "/" + classEntity.id : ""), {
      method: classEntity.id ? "PUT" : "POST",
      headers: {
        Authorization: `Bearer ${jwt}`,
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify(classEntity),
    })
      .then((response) => response.json())
      .then((json) => {
        if (json.message) {
          setMessage(json.message);
          setVisible(true);
        } else window.location.href = "/classes";
      })
      .catch((error) => {
        setMessage(error.message);
        setVisible(true);
      });
  }

  const modal = getErrorModal(setVisible, visible, message);

  return (
    <div className="auth-page-container">
      <h2>{classEntity.id ? "Edit Class" : "Add Class"}</h2>
      {modal}
      <div className="auth-form-container">
        <Form onSubmit={handleSubmit}>
          <div className="custom-form-input">
            <Label for="name" className="custom-form-input-label">Name</Label>
            <Input type="text" required name="name" id="name"
              value={classEntity.name || ""} onChange={handleChange} className="custom-input" />
          </div>
          <div className="custom-form-input">
            <Label for="birthDate" className="custom-form-input-label">Date</Label>
            <Input type="date" name="birthDate" id="birthDate"
              value={classEntity.birthDate || ""} onChange={handleChange} className="custom-input" />
          </div>
          <div className="custom-button-row">
            <button className="auth-button">Save</button>
            <Link to="/classes" className="auth-button" style={{ textDecoration: "none" }}>Cancel</Link>
          </div>
        </Form>
      </div>
    </div>
  );
}
