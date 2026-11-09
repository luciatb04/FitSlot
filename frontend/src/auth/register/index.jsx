import "../../static/css/auth/authButton.css";
import "../../static/css/auth/authPage.css";
import tokenService from "../../services/token.service";
import FormGenerator from "../../components/formGenerator/formGenerator";
import { registerFormOwnerInputs } from "./form/registerFormOwnerInputs";
import { registerFormInstructorInputs } from "./form/registerFormInstructorInputs";
import { registerFormGymOwnerInputs } from "./form/registerFormGymOwnerInputs";
import { useEffect, useRef, useState } from "react";

export default function Register() {
  let [type, setType] = useState(null);
  let [authority, setAuthority] = useState(null);
  let [gyms, setGyms] = useState([]);

  const registerFormRef = useRef();

  function handleButtonClick(event) {
    const target = event.target;
    let value = target.value;
    if (value === "Back") value = null;
    else setAuthority(value);
    setType(value);
  }

  function handleSubmit({ values }) {

    if(!registerFormRef.current.validate()) return;

    const request = values;
    request.gym = gyms.filter((gym) => gym.name === request.gym)[0];
    request["authority"] = authority;
    let state = "";

    fetch("/api/v1/auth/signup", {
      headers: { "Content-Type": "application/json" },
      method: "POST",
      body: JSON.stringify(request),
    })
      .then(function (response) {
        if (response.status === 200) {
          const loginRequest = {
            username: request.username,
            password: request.password,
          };

          fetch("/api/v1/auth/signin", {
            headers: { "Content-Type": "application/json" },
            method: "POST",
            body: JSON.stringify(loginRequest),
          })
            .then(function (response) {
              if (response.status === 200) {
                state = "200";
                return response.json();
              } else {
                state = "";
                return response.json();
              }
            })
            .then(function (data) {
              if (state !== "200") alert(data.message);
              else {
                tokenService.setUser(data);
                tokenService.updateLocalAccessToken(data.token);
                window.location.href = "/dashboard";
              }
            })
            .catch((message) => {
              alert(message);
            });
        }
      })
      .catch((message) => {
        alert(message);
      });
  }

  useEffect(() => {
    if (type === "Owner" || type === "Instructor") {
      if (registerFormOwnerInputs[5].values.length === 1){
        fetch("/api/v1/gyms")
        .then(function (response) {
          if (response.status === 200) {
            return response.json();
          } else {
            return response.json();
          }
        })
        .then(function (data) {
          setGyms(data);
          if (data.length !== 0) {
            let gymNames = data.map((gym) => {
              return gym.name;
            });

            registerFormOwnerInputs[5].values = ["None", ...gymNames];
          }
        })
        .catch((message) => {
          alert(message);
        });
      }
    }
  }, [type]);

  if (type) {
    return (
      <div className="auth-page-container">
        <h1>Register</h1>
        <div className="auth-form-container">
          <FormGenerator
            ref={registerFormRef}
            inputs={
              type === "Owner" ? registerFormOwnerInputs 
              : type === "Instructor" ? registerFormInstructorInputs
              : registerFormGymOwnerInputs
            }
            onSubmit={handleSubmit}
            numberOfColumns={1}
            listenEnterKey
            buttonText="Save"
            buttonClassName="auth-button"
          />
        </div>
      </div>
    );
  } else {
    return (
      <div className="auth-page-container">
        <div className="auth-form-container">
          <h1>Register</h1>
          <h2 className="text-center text-md">
            What type of user will you be?
          </h2>
          <div className="options-row">
            <button
              className="auth-button"
              value="Owner"
              onClick={handleButtonClick}
            >
              Owner
            </button>
            <button
              className="auth-button"
              value="Instructor"
              onClick={handleButtonClick}
            >
              Instructor
            </button>
            <button
              className="auth-button"
              value="Gym Owner"
              onClick={handleButtonClick}
            >
              Gym Owner
            </button>
          </div>
        </div>
      </div>
    );
  }
}
