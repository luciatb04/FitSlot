import "../../static/css/auth/authButton.css";
import "../../static/css/auth/authPage.css";
import tokenService from "../../services/token.service";
import getIdFromUrl from "../../util/getIdFromUrl";
import getErrorModal from "../../util/getErrorModal";
import useFetchState from "../../util/useFetchState";
import { gymEditInputs } from "./form/gymEditInputs";
import FormGenerator from "../../components/formGenerator/formGenerator";
import { useState, useEffect, useRef } from "react";
import {useNavigate} from "react-router-dom";

const user = tokenService.getUser();
const jwt = tokenService.getLocalAccessToken();

export default function EditGym() {
  const id = getIdFromUrl(2);
  const navigator = useNavigate();

  const emptyItem = {
    id: "",
    name: "",
    address: "",
    telephone: "",
    plan: null,
  };
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
  const [dataLoaded, setDataLoaded] = useState(false);

  const editGymFormRef = useRef(null);

  function handleSubmit({ values }) {
    if (!editGymFormRef.current.validate()) return;

    if (id !== "new") {
      fetch(`/api/v1/gyms/${id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${jwt}`,
        },
        body: JSON.stringify(values),
      })
      .then((res) => {
        if (res.status === 200) {
          navigator("/gyms");
        }
      })
      .catch((err) => {
        setMessage(err.message);
      });;
    } else {
      fetch(`/api/v1/gyms`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${jwt}`,
        },
        body: JSON.stringify(values),
      })
      .then((res) => {
        if (res.status === 201) {
          navigator("/gyms");
        }
      })
      .catch((err) => {
        setMessage(err.message);
      });
    }
  }

  useEffect(() => {
    if (gym.id !== "") {
      gymEditInputs.forEach((input) => {
        input.defaultValue = gym[input.name];
        setDataLoaded(true);
      });
    } else {
      gymEditInputs.forEach((input) => {
        input.defaultValue = "";
      });
    }
  }, [gym]);

  const modal = getErrorModal(setVisible, visible, message);

  return (
    <div className="auth-page-container">
      {<h2>{id !== "new" ? "Edit Gym" : "Add Gym"}</h2>}
      {modal}
      <div className="auth-form-container">
        {dataLoaded ? (
          <FormGenerator
            ref={editGymFormRef}
            inputs={gymEditInputs}
            onSubmit={handleSubmit}
            buttonText="Edit"
            buttonClassName="auth-button"
          />
        ) : (
          <FormGenerator
            ref={editGymFormRef}
            inputs={gymEditInputs}
            onSubmit={handleSubmit}
            buttonText="Add"
            buttonClassName="auth-button"
          />
        )}
      </div>
    </div>
  );
}
