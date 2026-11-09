import { useState } from "react";
import { Link } from "react-router-dom";
import { Form, Input, Label } from "reactstrap";
import tokenService from "../../services/token.service";
import "../../static/css/admin/adminPage.css";
import "../../static/css/owner/consultations.css";
import getErrorModal from "../../util/getErrorModal";
import getIdFromUrl from "../../util/getIdFromUrl";
import useFetchData from "../../util/useFetchData";
import useFetchState from "../../util/useFetchState";

const user = tokenService.getUser();
const jwt = tokenService.getLocalAccessToken();

export default function ConsultationEditGymOwner() {
  const emptyItem = {
    id: null,
    title: "",
    status: null,
    owner: null,
    classes: null,
    isGymComment: false,
  };
  const id = getIdFromUrl(2);
  const [message, setMessage] = useState(null);
  const [visible, setVisible] = useState(false);
  const [consultation, setConsultation] = useFetchState(
    emptyItem, 
    `/api/v1/consultations/${id}`,
    jwt,
    setMessage,
    setVisible,
    id
  );
  const [owners, setOwners] = useFetchState(
    [],
    `/api/v1/gyms/owners?userId=${user.id}`,
    jwt,
    setMessage,
    setVisible
  );
  const classes = useFetchData(`/api/v1/classes`, jwt);
  const [classsOwned, setClassesOwned] = useState([]);

  function handleChange(event) {
    const target = event.target;
    const value = target.value;
    const name = target.name;
    if (name === "owner") {
      const owner = owners.find((owner) => owner.id === Number(value));
      setConsultation({ ...consultation, owner: owner });
      setClassesOwned(
        classes.filter((classEntity) => classEntity.owner?.id === Number(value))
      );
    } else if (name === "classes") {
      const classEntity = classes.find(
        (classEntity) => classEntity.id === Number(value)
      );
      setConsultation({ ...consultation, classes: classEntity });
    } else if (name === "isGymComment"){
      setConsultation({...consultation, isGymComment: target.checked ? true : false});
    } else {
      setConsultation({ ...consultation, [name]: value });
    }
  }

  function handleSubmit(event) {
    event.preventDefault();

    fetch(
      "/api/v1/consultations" + (consultation.id ? "/" + consultation.id : ""),
      {
        method: consultation.id ? "PUT" : "POST",
        headers: {
          Authorization: `Bearer ${jwt}`,
          Accept: "application/json",
          "Content-Type": "application/json",
        },
        body: JSON.stringify(consultation),
      }
    )
      .then((response) => response.json())
      .then((json) => {
        if (json.message) {
          setMessage(json.message);
          setVisible(true);
        } else window.location.href = "/consultations";
      })
      .catch((message) => alert(message));
  }

  const modal = getErrorModal(setVisible, visible, message);
  const ownerOptions = owners.map((owner) => (
    <option key={owner.id} value={owner.id}>
      {owner.user.username}
    </option>
  ));
  let classOptions;
  if (consultation.id && consultation.classes)
    classOptions = (
      <option key={consultation.classes.id} value={consultation.classes.id}>
        {consultation.classes.name}
      </option>
    );
  else
    classOptions = consultation.owner ? (
      classsOwned.map((classEntity) => (
        <option key={classEntity.id} value={classEntity.id}>
          {classEntity.name}
        </option>
      ))
    ) : (
      <></>
    );

  return (
    <div className="auth-page-container">
      {<h2>{id !== "new" ? "Edit Consultation" : "Add Consultation"}</h2>}
      {modal}
      <div className="auth-form-container">
        <Form onSubmit={handleSubmit}>
          <div className="custom-form-input">
            <Label for="title" className="custom-form-input-label">
              Title
            </Label>
            <Input
              type="text"
              required
              name="title"
              id="title"
              value={consultation.title || ""}
              onChange={handleChange}
              className="custom-input"
            />
          </div>
          <div className="custom-form-input">
            <Label for="status" className="custom-form-input-label">
              Status
            </Label>
            <Input
              type="select"
              required
              name="status"
              id="status"
              value={consultation.status || ""}
              onChange={handleChange}
              className="custom-input"
            >
              <option value="">None</option>
              <option value="PENDING">PENDING</option>
              <option value="ANSWERED">ANSWERED</option>
              <option value="CLOSED">CLOSED</option>
            </Input>
          </div>
          <div className="custom-form-input">
            <Label for="owner" className="custom-form-input-label">
              Owner
            </Label>
            {consultation.id ? (
              <Input
                type="select"
                disabled
                name="owner"
                id="owner"
                value={consultation.owner?.id || ""}
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
                value={consultation.owner?.id || ""}
                onChange={handleChange}
                className="custom-input"
              >
                <option value="">None</option>
                {ownerOptions}
              </Input>
            )}
          </div>
          <div className="custom-form-input">
            <Label for="classes" className="custom-form-input-label">
              Class
            </Label>
            {consultation.id ? (
              <Input
                type="select"
                disabled
                name="classes"
                id="classes"
                value={consultation.classes?.id || ""}
                onChange={handleChange}
                className="custom-input"
              >
                <option value="">None</option>
                {classOptions}
              </Input>
            ) : (
              <Input
                type="select"
                required
                name="classes"
                id="classes"
                value={consultation.classes?.id || ""}
                onChange={handleChange}
                className="custom-input"
              >
                <option value="">None</option>
                {classOptions}
              </Input>
            )}
          </div>
          <div className="consultation-checkbox-container">
            <label htmlFor="isGymComment">
              ¿Is the consultation a comment for the gym?
            </label>
            <div className="checkbox-wrapper-10">
              <Input
                type="checkbox"
                id="isGymComment"
                className="tgl tgl-flip"
                onChange={handleChange}
                name="isGymComment"
                checked={consultation.isGymComment}
              />
              <label
                htmlFor="isGymComment"
                data-tg-on="Yes"
                data-tg-off="No"
                className="tgl-btn"
              ></label>
            </div>
          </div>
          <div className="custom-button-row">
            <button className="auth-button">Save</button>
            <Link
              to={`/consultations`}
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
