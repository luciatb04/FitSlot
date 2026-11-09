import React, { Component } from "react";
import { Button, ButtonGroup, Container, Table } from "reactstrap";
// import AppNavbar from "../AppNavbar";
import { Link } from "react-router-dom";

class VisitList extends Component {
    constructor(props) {
        super(props);
        this.state = { visits: [] };
        this.remove = this.remove.bind(this);
        this.jwt = JSON.parse(window.localStorage.getItem("jwt"));

        let pathArray = window.location.pathname.split('/');
        this.classId = pathArray[2];
    }

    componentDidMount() {
        fetch(`/api/v1/classes/${this.classId}/visits`, {
            headers: {
                "Authorization": `Bearer ${this.jwt}`,
                "Content-Type": "application/json",
            },
        }).then((response) => response.json())
            .then((data) => this.setState({ visits: data }));
    }

    async remove(id) {
        await fetch(`/api/v1/classes/${this.classId}/visits/${id}`, {
            method: "DELETE",
            headers: {
                "Authorization": `Bearer ${this.jwt}`,
                Accept: "application/json",
                "Content-Type": "application/json",
            },
        }).then((response) => {
            if (response.status === 200) {
                let updatedVisits = [...this.state.visits].filter((i) => i.id !== id);
                this.setState({ visits: updatedVisits });
            }
            return response.json();
        }).then(function (data) {
            alert(data.message);
        });
    }

    render() {
        const { visits, isLoading } = this.state;

        if (isLoading) {
            return <p>Loading...</p>;
        }

        const visitList = visits.map((visit) => {
            return (
                <tr key={visit.id}>
                    <td>{(new Date(visit.datetime)).toLocaleString()}</td>
                    <td>{visit.description ? visit.description : "No description provided"}</td>
                    <td>{visit.instructor.firstName} {visit.instructor.lastName}</td>
                    <td>
                        <ButtonGroup>
                            <Button size="sm" color="primary" tag={Link}
                                to={`/classes/${this.classId}/visits/${visit.id}`}>
                                Edit
                            </Button>
                            <Button size="sm" color="danger" onClick={() => this.remove(visit.id)}>
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
                    <h1 className="text-center">Visits</h1>
                    <Button color="success" tag={Link} to={`/classes/${this.classId}/visits/new`}>
                        Add Visit
                    </Button>{" "}
                    <Button color="primary" tag={Link} to={`/classes/`}>
                        Back
                    </Button>
                    <Table className="mt-4">
                        <thead>
                            <tr>
                                <th>Date and Time</th>
                                <th>Description</th>
                                <th>Instructor</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>{visitList}</tbody>
                    </Table>
                </Container>
            </div>
        );
    }
}

export default VisitList;
