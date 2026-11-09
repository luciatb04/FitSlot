import React, { Component } from "react";
import { Button, ButtonGroup, Container, Table } from "reactstrap";
// import AppNavbar from "../AppNavbar";
import { Link } from "react-router-dom";

class ClassList extends Component {
    constructor(props) {
        super(props);
        this.state = { classes: [] };
        this.remove = this.remove.bind(this);
        this.jwt = JSON.parse(window.localStorage.getItem("jwt"));
    }

    componentDidMount() {
        fetch("/api/v1/classes", {
            headers: {
                "Authorization": `Bearer ${this.jwt}`,
                "Content-Type": "application/json",
            },
        }).then((response) => response.json())
            .then((data) => this.setState({ classes: data }));
    }

    async remove(id) {
        await fetch(`/api/v1/classes/${id}`, {
            method: "DELETE",
            headers: {
                "Authorization": `Bearer ${this.jwt}`,
                Accept: "application/json",
                "Content-Type": "application/json",
            },
        }).then((response) => {
            if (response.status === 200) {
                let updatedClasses = [...this.state.classes].filter((i) => i.id !== id);
                this.setState({ classes: updatedClasses });
            }
            return response.json();
        }).then(function (data) {
            alert(data.message);
        });
    }

    render() {
        const { classes, isLoading } = this.state;

        if (isLoading) {
            return <p>Loading...</p>;
        }

        const classList = classes.map((classEntity) => {
            return (
                <tr key={classEntity.id}>
                    <td>{classEntity.name}</td>
                    <td>{classEntity.birthDate}</td>
                    <td>{classEntity.type?.name}</td>
                    <td>{classEntity.owner?.user?.username}</td>
                    <td>
                        <Button size="sm" color="info" tag={Link}
                            to={`/classes/${classEntity.id}/visits`}>
                            Visits
                        </Button>
                    </td>
                    <td>
                        <ButtonGroup>
                            <Button size="sm" color="primary" tag={Link}
                                to={"/classes/" + classEntity.id}>
                                Edit
                            </Button>
                            <Button size="sm" color="danger" onClick={() => this.remove(classEntity.id)}>
                                Delete
                            </Button>
                        </ButtonGroup>
                    </td>
                </tr>
            );
        });

        return (
            <div>
                {/* <AppNavbar /> */}
                <Container style={{ marginTop: "15px" }} fluid>
                    <h1 className="text-center">Classes</h1>
                    <Button color="success" tag={Link} to="/classes/new">
                        Add Class
                    </Button>
                    <Table className="mt-4">
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th>Birth Date</th>
                                <th>Type</th>
                                <th>Owner</th>
                                <th>Visits</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>{classList}</tbody>
                    </Table>
                </Container>
            </div>
        );
    }
}

export default ClassList;
