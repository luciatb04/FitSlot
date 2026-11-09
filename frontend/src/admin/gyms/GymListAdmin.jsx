import { useState } from "react";
import { Link } from "react-router-dom";
import { Button, ButtonGroup, Table } from "reactstrap";
import tokenService from "../../services/token.service";
import "../../static/css/admin/adminPage.css";
import deleteFromList from "../../util/deleteFromList";
import getErrorModal from "../../util/getErrorModal";
import useFetchState from "../../util/useFetchState";

const jwt = tokenService.getLocalAccessToken();

export default function GymListAdmin() {
  const [message, setMessage] = useState(null);
  const [visible, setVisible] = useState(false);
  const [gyms, setGyms] = useFetchState(
    [],
    `/api/v1/gyms`,
    jwt,
    setMessage,
    setVisible
  );
  const [alerts, setAlerts] = useState([]);

  const gymList = gyms.map((gym) => {
    return (
      <tr key={gym.id}>
        <td>{gym.name}</td>
        <td>{gym.address}</td>
        <td>{gym.telephone}</td>
        <td>{gym.gymOwner.firstName} {gym.gymOwner.lastName}</td>
        <td>{gym.plan}</td>
        <td>
          <ButtonGroup>
            <Button
              size="sm"
              color="primary"
              aria-label={"edit-" + gym.id}
              tag={Link}
              to={"/gyms/" + gym.id}
            >
              Edit
            </Button>
            <Button
              size="sm"
              color="danger"
              aria-label={"delete-" + gym.id}
              onClick={() =>
                deleteFromList(
                  `/api/v1/gyms/${gym.id}`,
                  gym.id,
                  [gyms, setGyms],
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
    <div>
      <div className="admin-page-container">
        <h1 className="text-center">Gym Owners</h1>
        {alerts.map((a) => a.alert)}
        {modal}
        <div className="float-right">
          <Button color="success" tag={Link} to="/gyms/new">
            Add Gym Owner
          </Button>
        </div>
        <div>
          <Table aria-label="owners" className="mt-4">
            <thead>
              <tr>
                <th width="15%">Name</th>
                <th width="20%">Address</th>
                <th width="15%">Telephone</th>
                <th width="15%">Gym Owner</th>
                <th width="15%">Plan</th>
                <th width="20%">Actions</th>
              </tr>
            </thead>
            <tbody>{gymList}</tbody>
          </Table>
        </div>
      </div>
    </div>
  );
}
