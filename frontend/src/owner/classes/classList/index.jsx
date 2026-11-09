import "../../../static/css/owner/classList.css";
import "../../../static/css/auth/authButton.css";
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from "reactstrap";
import { Link } from "react-router-dom";
import tokenService from "../../../services/token.service";
import { Splide, SplideSlide } from "@splidejs/react-splide";
import moment from "moment";
import { useState, useEffect } from "react";

export default function OwnerClassesList() {
  const [classes, setClasses] = useState([]);
  let [message, setMessage] = useState(null);
  let [modalShow, setModalShow] = useState(false);

  const user = tokenService.getUser();
  const jwt = tokenService.getLocalAccessToken();

  function removeClass(id) {
    fetch(`/api/v1/classes/${id}`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${jwt}`,
        Accept: "application/json",
        "Content-Type": "application/json",
      },
    })
      .then((response) => {
        if (response.status === 200) {
          const updatedClasses = classes.filter((classEntity) => classEntity.id !== id);
          setClasses(updatedClasses);
        }
        return response.json();
      })
      .then((data) => {
        setMessage(data.message);
        setModalShow(true);
      });
  }

  async function removeVisit(classId, visitId) {
    let status = "";
    await fetch(`/api/v1/classes/${classId}/visits/${visitId}`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${jwt}`,
        Accept: "application/json",
        "Content-Type": "application/json",
      },
    })
      .then((response) => {
        if (response.status === 200) status = "200";
        return response.json();
      })
      .then((data) => {
        setMessage(data.message);
        setModalShow(true);
      });

    if (status === "200") {
      const classesCopy = [...classes];
      const index = classesCopy.findIndex((classEntity) => classEntity.id === classId);
      const classEntity = classesCopy[index];
      const updatedVisits = classEntity.visits.filter((visit) => visit.id !== visitId);
      classEntity.visits = updatedVisits;

      setClasses(classesCopy);
    }
  }

  function handleShow() {
    setModalShow(!modalShow);
  }

  async function setUp() {
    const fetchedClasses = await (
      await fetch(`/api/v1/classes?userId=${user.id}`, {
        headers: {
          Authorization: `Bearer ${jwt}`,
          "Content-Type": "application/json",
        },
      })
    ).json();
    if (fetchedClasses.message) setMessage(fetchedClasses.message);
    else {
      for (const classEntity of fetchedClasses) {
        const index = fetchedClasses.findIndex((obj) => obj.id === classEntity.id);
        const visits = await (
          await fetch(`/api/v1/classes/${classEntity.id}/visits`, {
            headers: {
              Authorization: `Bearer ${jwt}`,
              "Content-Type": "application/json",
            },
          })
        ).json();
        if (visits.message) setMessage(visits.message);
        else fetchedClasses[index].visits = visits;
      }
      setClasses(fetchedClasses);
    }
  }

  useEffect(() => {
    setUp();
  }, []);

  return (
    <div>
      {/* <AppNavbar /> */}
      <div className="class-list-page-container">
        <div className="title-and-add">
          <h1 className="class-list-title">Classes</h1>
          <Link
            to="/myClasses/new"
            className="auth-button"
            style={{ textDecoration: "none", marginBottom: "2rem" }}
          >
            Add Class
          </Link>
        </div>
        {classes.length > 0 ? (
          classes.map((classEntity) => {
            return (
              <div key={classEntity.id} className="class-row">
                <div className="class-data">
                  <h4 className="class-name">{classEntity.name}</h4>
                  <span>
                    <strong>Date of birth:</strong> {classEntity.birthDate}
                  </span>
                  <span>
                    <strong>Type:</strong> {classEntity.type?.name}
                  </span>
                </div>
                <div className="class-options">
                  <Link
                    to={"/myClasses/" + classEntity.id}
                    className="auth-button blue"
                    style={{ textDecoration: "none" }}
                  >
                    Edit
                  </Link>
                  <button
                    onClick={() => removeClass(classEntity.id)}
                    className="auth-button danger"
                  >
                    Delete
                  </button>
                </div>
                <div className="class-visits">
                  {classEntity.visits && classEntity.visits.length > 0 ? (
                    <Splide
                      aria-label="My Favorite Images"
                      className="visits-carousel"
                    >
                      {classEntity.visits.map((visit, index) => {
                        return (
                          <SplideSlide className="carousel-slide" key={index}>
                            <span>
                              <strong>Date and Time:</strong>{" "}
                              {moment(visit.datetime).format(
                                "DD/MM/YYYY HH:mm"
                              )}
                            </span>
                            <span>
                              <strong>Instructor:</strong> {visit.instructor.firstName}{" "}
                              {visit.instructor.lastName}
                            </span>
                            <div className="options-row">
                              {new Date(visit.datetime) > Date.now() && (
                                <button
                                  className="cancel-visit-button"
                                  onClick={() => removeVisit(classEntity.id, visit.id)}
                                >
                                  Cancel
                                </button>
                              )}
                              <Link
                                to={`/myClasses/${classEntity.id}/visits/${visit.id}`}
                                className="edit-visit-button"
                                style={{ textDecoration: "none" }}
                              >
                                Edit
                              </Link>
                            </div>
                          </SplideSlide>
                        );
                      })}
                    </Splide>
                  ) : (
                    <></>
                  )}
                  <Link
                    to={`/myClasses/${classEntity.id}/visits/new`}
                    className="auth-button blue"
                    style={{ textDecoration: "none", marginTop: "20px" }}
                  >
                    Add Visit
                  </Link>
                </div>
              </div>
            );
          })
        ) : (
          <p>Loading...</p>
        )}

        <Modal isOpen={modalShow} toggle={handleShow} keyboard={false}>
          <ModalHeader
            toggle={handleShow}
            close={
              <button className="close" onClick={handleShow} type="button">
                &times;
              </button>
            }
          >
            Alert!
          </ModalHeader>
          <ModalBody>{message || ""}</ModalBody>
          <ModalFooter>
            <Button color="primary" onClick={handleShow}>
              Close
            </Button>
          </ModalFooter>
        </Modal>
      </div>
    </div>
  );
}
