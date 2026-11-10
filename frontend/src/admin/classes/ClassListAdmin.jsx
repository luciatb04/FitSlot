import { useState } from "react";
import { Link } from "react-router-dom";
import { Button, ButtonGroup, Table } from "reactstrap";
import tokenService from "../../services/token.service";
import useFetchState from "../../util/useFetchState";
import getErrorModal from "../../util/getErrorModal";
import deleteFromList from "../../util/deleteFromList";

const jwt = tokenService.getLocalAccessToken();

export default function ClassListAdmin() {
  const [message, setMessage] = useState(null);
  const [visible, setVisible] = useState(false);
  const [classes, setClasses] = useFetchState(
    [],
    `/api/v1/classes`,
    jwt,
    setMessage,
    setVisible
  );
  const [alerts, setAlerts] = useState([]);
  const [classesOwned, setClassesOwned] = useState([]);

  const classList = classes.map((classEntity) => {
    return (
      <tr key={classEntity.id}>
        <td>{classEntity.name}</td>
        <td>{classEntity.birthDate}</td>
        <td>{classEntity.type?.name}</td>
        <td>{classEntity.owner?.user?.username}</td>
        <td>
          <ButtonGroup>
            <Button
              aria-label={"edit-" + classEntity.id}
              size="sm"
              color="primary"
              tag={Link}
              to={"/classes/" + classEntity.id}
            >
              Edit
            </Button>
            <Button
              aria-label={"delete-" + classEntity.id}
              size="sm"
              color="danger"
              onClick={() =>
                deleteFromList(
                  `/api/v1/classes/${classEntity.id}`,
                  classEntity.id,
                  [classes, setClasses],
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
        <h1 className="text-center">Classes</h1>
        {alerts.map((a) => a.alert)}
        {modal}
        <Button color="success" tag={Link} to="/classes/new">
          Add Class
        </Button>
        <div>
          <Table aria-label="classes" className="mt-4">
            <thead>
              <tr>
                <th>Name</th>
                <th>Birth Date</th>
                <th>Type</th>
                <th>Owner</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>{classList}</tbody>
          </Table>
        </div>
      </div>
  );
}
