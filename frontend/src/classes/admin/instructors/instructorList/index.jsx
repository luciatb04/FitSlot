import React, { Component } from 'react';
import { Button, ButtonGroup, Container, Table } from 'reactstrap';
// import AppNavbar from './AppNavbar';
import { Link } from 'react-router-dom';

class InstructorList extends Component {

    constructor(props) {
        super(props);
        this.state = { instructors: [] };
        this.remove = this.remove.bind(this);
        this.jwt = JSON.parse(window.localStorage.getItem("jwt"));
    }

    componentDidMount() {
        fetch('/api/v1/instructors', {
            headers: {
                "Authorization": `Bearer ${this.jwt}`,
            },
        })
            .then(response => response.json())
            .then(data => this.setState({ instructors: data }));
    }

    async remove(id) {
        await fetch(`/api/v1/instructors/${id}`, {
            method: 'DELETE',
            headers: {
                "Authorization": `Bearer ${this.jwt}`,
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            }
        }).then(() => {
            let updatedInstructors = [...this.state.instructors].filter(i => i.id !== id);
            this.setState({ instructors: updatedInstructors });
        });
    }

    render() {
        const { instructors, isLoading } = this.state;

        if (isLoading) {
            return <p>Loading...</p>;
        }

        const instructorList = instructors.map(instructor => {

            let specialtiesAux = instructor.specialties.map(s => s.name).toString().replace(",", ", ");;


            return <tr key={instructor.id}>
                <td style={{ whiteSpace: 'nowrap' }}>{instructor.firstName} {instructor.lastName}</td>
                <td >{instructor.city}</td>
                <td style={{ whiteSpace: 'break-spaces' }}>{specialtiesAux}</td>
                <td >{instructor.user.username}</td>
                <td>
                    <ButtonGroup>
                        <Button size="sm" color="primary" tag={Link} to={"/instructors/" + instructor.id}>Edit</Button>
                        <Button size="sm" color="danger" onClick={() => this.remove(instructor.id)}>Delete</Button>
                    </ButtonGroup>
                </td>
            </tr>
        });

        return (
            <div>
                <Container style={{ marginTop: "15px" }} fluid>
                    <h1 className='text-center'>Instructors</h1>
                    <div className="float-right">
                        <Button color="success" tag={Link} to="/instructors/new">Add Instructor</Button>{" "}
                        <Button color="info" tag={Link} to="/instructors/specialties">Specialties</Button>
                    </div>
                    <Table className="mt-4">
                        <thead>
                            <tr>
                                <th width="20%">Name</th>
                                <th width="20%">City</th>
                                <th width="20%">Specialties</th>
                                <th width="20%">User</th>
                                <th width="20%">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {instructorList}
                        </tbody>
                    </Table>
                </Container>
            </div>
        );
    }

}

export default InstructorList;

