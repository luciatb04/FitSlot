import { useState } from "react";
import { Link } from "react-router-dom";
import { Button, ButtonGroup, Table } from "reactstrap";
import tokenService from "../../services/token.service";
import useFetchState from "../../util/useFetchState";
import getErrorModal from "../../util/getErrorModal";
import "../../static/css/admin/adminPage.css";
import { useNavigate } from "react-router-dom";

const user = tokenService.getUser();
const jwt = tokenService.getLocalAccessToken();

export default function GymsList() {
  const [message, setMessage] = useState(null);
  const [visible, setVisible] = useState(false);
  const [gyms, setGyms] = useFetchState(
    [],
    `/api/v1/gyms?userId=${user.id}`,
    jwt,
    setMessage,
    setVisible
  );
  const [alerts, setAlerts] = useState([]);

  const navigator = useNavigate();

  const gymsList =
    gyms.map((gym) => {
        return (
          <tr key={gym.id}>
            <td className="text-center">{gym.name}</td>
            <td className="text-center">{gym.address}</td>
            <td className="text-center">{gym.telephone}</td>
            <td className="text-center">{gym.plan}</td>
            <td className="text-center">
              <ButtonGroup>
                <Button
                  size="sm"
                  color="primary"
                  aria-label={"edit-" + gym.name}
                  tag={Link}
                  to={"/gyms/" + gym.id}
                >
                  Edit
                </Button>
                <Button
                  size="sm"
                  color="danger"
                  aria-label={"delete-" + gym.name}
                  onClick={() => {
                    let confirmMessage = window.confirm("Are you sure you want to delete it?");

                    if(!confirmMessage) return;

                    fetch(`/api/v1/gyms/${gym.id}`, {
                      method: "DELETE",
                      headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${jwt}`,
                      },
                    })
                      .then((res) => {
                        if (res.status === 200) {
                          setMessage("Gym deleted successfully");
                          setVisible(true);
                          navigator(0);
                        }
                      })
                      .catch((err) => {
                        setMessage(err.message);
                        setVisible(true);
                      });
                  }}
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
        <h1 className="text-center">My Gyms</h1>
        {alerts.map((a) => a.alert)}
        {modal}
        <div className="float-right">
          <Button color="success" tag={Link} to="/gyms/new">
            Add gym
          </Button>
        </div>
        <div>
          <Table aria-label="gyms" className="mt-4">
            <thead>
              <tr>
                <th width="15%" className="text-center">Name</th>
                <th width="15%" className="text-center">Address</th>
                <th width="15%" className="text-center">Telephone</th>
                <th width="15%" className="text-center">Plan</th>
                <th width="30%" className="text-center">Actions</th>
              </tr>
            </thead>
            <tbody>{gymsList}</tbody>
          </Table>
        </div>
      </div>
    </div>
  );
}
