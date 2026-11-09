import { useState } from "react";
import { Link } from "react-router-dom";
import { Form, Input, Label } from "reactstrap";
import tokenService from "../../services/token.service";
import getErrorModal from "../../util/getErrorModal";
import useFetchData from "../../util/useFetchData";
import useFetchState from "../../util/useFetchState";
import getIdFromUrl from "../../util/getIdFromUrl";
import "../../static/css/admin/adminPage.css";

const jwt = tokenService.getLocalAccessToken();

export default function ClassEditAdmin() {
  const emptyItem = {
    id: null,
    name: "",
    birthDate: "",
    type: null,
    owner: null,
  };
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
  const types = useFetchData(`/api/v1/classes/types`, jwt);
  const owners = useFetchData(`/api/v1/owners`, jwt);

  function handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    if (name === "type") {
      const type = types.find((type) => type.id === Number(value));
      setClassEntity({ ...classEntity, type });
    } else if (name === "owner") {
      const owner = owners.find((owner) => owner.id === Number(value));
      setClassEntity({ ...classEntity, owner });
    } else setClassEntity({ ...classEntity, [name]: value });
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
      .catch((message) => alert(message));
  }

  const modal = getErrorModal(setVisible, visible, message);
  const typeOptions = types.map((type) => (
    <option key={type.id} value={type.id}>
      {type.name}
    </option>
  ));
  const ownerOptions = owners.map((owner) => (
    <option key={owner.id} value={owner.id}>
      {owner.user.username}
    </option>
  ));

  return (
    <div className="auth-page-container">
      <h2>{classEntity.id ? "Edit Class" : "Add Class"}</h2>
      {modal}
      <div className="auth-form-container">
        <Form onSubmit={handleSubmit}>
          <div className="custom-form-input">
            <Label for="name" className="custom-form-input-label">
              Name
            </Label>
            <Input
              type="text"
              required
              name="name"
              id="name"
              value={classEntity.name || ""}
              onChange={handleChange}
              className="custom-input"
            />
          </div>
          <div className="custom-form-input">
            <Label for="birthDate" className="custom-form-input-label">
              Birth Date
            </Label>
            <Input
              type="date"
              name="birthDate"
              id="birthDate"
              value={classEntity.birthDate || ""}
              onChange={handleChange}
              className="custom-input"
            />
          </div>
          <div className="custom-form-input">
            <Label for="type" className="custom-form-input-label">
              Type
            </Label>
            <Input
              type="select"
              required
              name="type"
              id="type"
              value={classEntity.type?.id || ""}
              onChange={handleChange}
              className="custom-input"
            >
              <option value="">None</option>
              {typeOptions}
            </Input>
          </div>
          <div className="custom-form-input">
            <Label for="owner" className="custom-form-input-label">
              Owner
            </Label>
            {classEntity.id ? (
              <Input
                type="select"
                disabled
                name="owner"
                id="owner"
                value={classEntity.owner?.id || ""}
                onChange={handleChange}
                className="custom-input"
              >
                <option value="">None</option>
                {ownerOptions}
              </Input>
            ) : (
              <Input
                type="select"
                required
                name="owner"
                id="owner"
                value={classEntity.owner?.id || ""}
                onChange={handleChange}
                className="custom-input"
              >
                <option value="">None</option>
                {ownerOptions}
              </Input>
            )}
          </div>
          <div className="custom-button-row">
            <button className="auth-button">Save</button>
            <Link
              to={`/classes`}
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
