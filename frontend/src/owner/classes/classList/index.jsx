import "../../../static/css/owner/classList.css";
import "../../../static/css/auth/authButton.css";
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from "reactstrap";
import { Link } from "react-router-dom";
import tokenService from "../../../services/token.service";
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
