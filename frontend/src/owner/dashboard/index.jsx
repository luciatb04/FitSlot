import { Link } from "react-router-dom";
import { Button, Modal, ModalBody, ModalFooter, ModalHeader } from "reactstrap";
import "../../static/css/owner/dashboard.css";
import { useState } from "react";

export default function OwnerDashboard() {
  let [message, setMessage] = useState(null);
  let [modalShow, setModalShow] = useState(false);

  function handleShow() {
    setModalShow(!modalShow);
  }

  return (
    <>
      <div className="owner-dashboard-page-container">
        <h1 className="text-center dashboard-title">Dashboard</h1>
      </div>
      <Modal
        isOpen={modalShow}
        toggle={handleShow}
        backdrop="static"
        keyboard={false}
      >
        <ModalHeader>Error!</ModalHeader>
        <ModalBody>{message || ""}</ModalBody>
        <ModalFooter>
          <Button color="primary" tag={Link} to={`/`}>
            Back
          </Button>
        </ModalFooter>
      </Modal>
    </>
  );
}
