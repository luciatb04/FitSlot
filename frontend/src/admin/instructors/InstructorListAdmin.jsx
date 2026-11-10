import { useState } from "react";
import { Link } from "react-router-dom";
import { Button, ButtonGroup, Table } from "reactstrap";
import tokenService from "../../services/token.service";
import deleteFromList from "../../util/deleteFromList";
import getErrorModal from "../../util/getErrorModal";
import useFetchState from "../../util/useFetchState";

const jwt = tokenService.getLocalAccessToken();

export default function InstructorListAdmin() {
  const [message, setMessage] = useState(null);
  const [visible, setVisible] = useState(false);
  const [instructors, setInstructors] = useFetchState(
    [],
    `/api/v1/instructors`,
    jwt,
    setMessage,
    setVisible
  );
  const [alerts, setAlerts] = useState([]);

  const instructorList = instructors.map((instructor) => {
    return (
      <tr key={instructor.id}>
        <td style={{ whiteSpace: "nowrap" }}>
          {instructor.firstName} {instructor.lastName}
        </td>
        <td>{instructor.city}</td>
        <td>{instructor.gym.name}</td>
        <td>{instructor.user.username}</td>
        <td>
          <ButtonGroup>
            <Button
              size="sm"
              aria-label={"edit-" + instructor.id}
              color="primary"
              tag={Link}
              to={"/instructors/" + instructor.id}
            >
              Edit
            </Button>
            <Button
              size="sm"
              aria-label={"delete-" + instructor.id}
              color="danger"
              onClick={() =>
                deleteFromList(
                  `/api/v1/instructors/${instructor.id}`,
                  instructor.id,
                  [instructors, setInstructors],
                  [alerts, setAlerts],
                  setMessage,
                  setVisible
                )
              }
            >
              Delete
            </Button>
          </ButtonGroup>
        </td>
      </tr>
    );
  });
  const modal = getErrorModal(setVisible, visible, message);

  return (
    <div className="admin-page-container">
      <h1 className="text-center">Instructors</h1>
      {alerts.map((a) => a.alert)}
      {modal}
      <div className="float-right">
        <Button color="success" tag={Link} to="/instructors/new">
          Add Instructor
        </Button>{" "}
      </div>
      <div>
        <Table aria-label="instructors" className="mt-4">
          <thead>
            <tr>
              <th width="15%">Name</th>
              <th width="15%">City</th>
              <th width="15%">Gym</th>
              <th width="15%">User</th>
              <th width="20%">Actions</th>
            </tr>
          </thead>
          <tbody>{instructorList}</tbody>
        </Table>
      </div>
    </div>
  );
}
