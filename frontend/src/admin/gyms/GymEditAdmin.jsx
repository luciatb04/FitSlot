import { useState } from "react";
import { Link } from "react-router-dom";
import { Form, Input, Label } from "reactstrap";
import tokenService from "../../services/token.service";
import getErrorModal from "../../util/getErrorModal";
import getIdFromUrl from "../../util/getIdFromUrl";
import useFetchState from "../../util/useFetchState";

const jwt = tokenService.getLocalAccessToken();

export default function GymEditAdmin() {
  const emptyItem = {
    id: "",
    name: "",
    address: "",
    telephone: "",
  };
  const id = getIdFromUrl(2);
  const [message, setMessage] = useState(null);
  const [visible, setVisible] = useState(false);
  const [gym, setGym] = useFetchState(
    emptyItem,
    `/api/v1/gyms/${id}`,
    jwt,
    setMessage,
    setVisible,
    id
  );

  const [gymOwners, setGymOwners] = useFetchState(
    [],
    `/api/v1/gymOwners/all`,
    jwt,
    setMessage,
    setVisible
  );

  function handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    setGym({ ...gym, [name]: value });
  }

  function handleSubmit(event) {
    event.preventDefault();

    gym.gymOwner = gymOwners.filter((gymOwner) => gymOwner.id === parseInt(gym.gymOwner))[0];

    console.log(gym);

    fetch("/api/v1/gyms" + (gym.id ? "/" + gym.id : ""), {
      method: gym.id ? "PUT" : "POST",
      headers: {
        Authorization: `Bearer ${jwt}`,
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify(gym),
    })
      .then((response) => response.json())
      .then((json) => {
        if (json.message) {
          setMessage(json.message);
          setVisible(true);
        } else window.location.href = "/gyms";
      })
      .catch((message) => alert(message));
  }

  const modal = getErrorModal(setVisible, visible, message);

  return (
    <div className="auth-page-container">
      {<h2>{id !== "new" ? "Edit Gym" : "Add Gym"}</h2>}
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
              value={gym.name || ""}
              onChange={handleChange}
              className="custom-input"
            />
          </div>
          <div className="custom-form-input">
            <Label for="address" className="custom-form-input-label">
              Address
            </Label>
            <Input
              type="text"
              required
              name="address"
              id="address"
              value={gym.address || ""}
              onChange={handleChange}
              className="custom-input"
            />
          </div>
          <div className="custom-form-input">
            <Label for="telephone" className="custom-form-input-label">
                Telephone
            </Label>
            <Input
              type="text"
              required
              name="telephone"
              id="telephone"
              value={gym.telephone || ""}
              onChange={handleChange}
              className="custom-input"
            />
          </div>
          <div className="custom-form-input">
            <Label for="plan" className="custom-form-input-label">
              Plan
            </Label>
            <Input
              id="plan"
              name="plan"
              required
              type="select"
              value={gym.plan || ""}
              onChange={handleChange}
              className="custom-input"
            >
              <option value="">None</option>
              <option value="BASIC">BASIC</option>
              <option value="GOLD">GOLD</option>
              <option value="PLATINUM">PLATINUM</option>
              
            </Input>
          </div>
          <div className="custom-form-input">
            <Label for="gymOwner" className="custom-form-input-label">
              Gym Owner
            </Label>
            <Input
              id="gymOwner"
              name="gymOwner"
              required
              type="select"
              value={gym.gymOwner ? gym.gymOwner.id : ""}
              onChange={handleChange}
              className="custom-input"
            >
              <option value="">None</option>
              {gymOwners.map((gymOwner) => {
                return(
                    <option value={gymOwner.id}>{gymOwner.firstName} {gymOwner.lastName}</option>
                );
              })}
              
            </Input>
          </div>
          <div className="custom-button-row">
            <button className="auth-button">Save</button>
            <Link
              to={`/gymOwners`}
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
