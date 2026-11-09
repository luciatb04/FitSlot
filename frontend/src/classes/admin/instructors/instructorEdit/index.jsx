import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import { Button, Col, Container, Form, FormGroup, Input, Label, Row } from 'reactstrap';

class InstructorEdit extends Component {

    emptyInstructor = {
        id: '',
        firstName: '',
        lastName: '',
        specialties: [],
        user: { id: 1 },
    };

    constructor(props) {
        super(props);
        this.state = {
            instructor: this.emptyInstructor,
            allSpecialties: [],
            // users: [],
        };
        this.handleChange = this.handleChange.bind(this);
        this.handleSpecialtyChange = this.handleSpecialtyChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.jwt = JSON.parse(window.localStorage.getItem("jwt"));
        let pathArray = window.location.pathname.split('/');
        this.id = pathArray[2];
    }

    async componentDidMount() {
        if (this.id !== 'new') {
            const instructor = await (await fetch(`/api/v1/instructors/${this.id}`, {
                headers: {
                    "Authorization": `Bearer ${this.jwt}`,
                },
            })).json();
            this.setState({ instructor: instructor });
        }

        const specialtiesList = await (await fetch(`/api/v1/instructors/specialties`, {
            headers: {
                "Authorization": `Bearer ${this.jwt}`,
            },
        })).json();
        this.setState({ allSpecialties: specialtiesList });

        const users = await (await fetch(`/api/v1/users?auth=INSTRUCTOR`, {
            headers: {
                "Authorization": `Bearer ${this.jwt}`,
            },
        })).json();
        this.setState({ users: users });
    }

    handleChange(event) {
        const target = event.target;
        const value = target.value;
        const name = target.name;
        let instructor = { ...this.state.instructor };
        if (name === "user") {
            instructor.user.id = value;
        } else instructor[name] = value;
        this.setState({ instructor });
    }

    handleSpecialtyChange(event) {
        const target = event.target;
        const checked = target.checked;
        const name = target.name;
        const allSpecialties = { ...this.state.allSpecialties }
        let instructor = { ...this.state.instructor };
        let selectedSpecialties = instructor.specialties;
        for (let i = 0; i < Object.keys(allSpecialties).length; i++) {
            if (allSpecialties[i].name === name) {
                if (!checked) selectedSpecialties = selectedSpecialties.filter(specialty => specialty.name !== name);
                else selectedSpecialties.push(allSpecialties[i]);
            }
        }
        instructor.specialties = selectedSpecialties;
        this.setState({ instructor });
    }

    async handleSubmit(event) {
        event.preventDefault();
        const { instructor } = this.state;

        await fetch('/api/v1/instructors' + (instructor.id ? '/' + instructor.id : ''), {
            method: (instructor.id) ? 'PUT' : 'POST',
            headers: {
                "Authorization": `Bearer ${this.jwt}`,
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(instructor),
        });
        window.location.href = '/instructors';
    }

    render() {
        const { instructor, allSpecialties } = this.state;
        const title = <h2>{instructor.id ? 'Edit Instructor' : 'Add Instructor'}</h2>;

        const selectedSpecialties = instructor.specialties.map(specialty => specialty.name);
        const specialties = allSpecialties.map(specialty => {
            if (selectedSpecialties.includes(specialty.name)) {
                return (<FormGroup key={specialty.name}>
                    <Input type="checkbox" name={specialty.name} onChange={this.handleSpecialtyChange} checked />
                    <Label for={specialty.name}> {specialty.name}</Label>
                </FormGroup>);
            } else {
                return (<FormGroup key={specialty.name}>
                    <Input type="checkbox" key={specialty.name} name={specialty.name} onChange={this.handleSpecialtyChange} />
                    <Label for={specialty.name}> {specialty.name}</Label>
                </FormGroup>);
            }
        });

        // const userOptions = users.map(user => <option key={user.id} value={user.id}>{user.username}</option>);


        return (
            <div>
                <Container className="d-flex ">
                    <Col md={4}>
                        {title}
                        <Form onSubmit={this.handleSubmit}>
                            <FormGroup>
                                <Label for="firstName">First Name</Label>
                                <Input type="text" name="firstName" id="firstName" value={instructor.firstName || ''}
                                    onChange={this.handleChange} autoComplete="firstName" />
                            </FormGroup>
                            <FormGroup>
                                <Label for="lastName">Last Name</Label>
                                <Input type="text" name="lastName" id="lastName" value={instructor.lastName || ''}
                                    onChange={this.handleChange} autoComplete="lastName" />
                            </FormGroup>
                            <FormGroup>
                                <Label for="city">City</Label>
                                <Input type="text" name="city" id="city" value={instructor.city || ''}
                                    onChange={this.handleChange} autoComplete="city" />
                            </FormGroup>
                            <Label for="specialties">Specialties</Label>
                            <Row className="row-cols-lg-auto g-3 align-items-center">
                                {specialties}
                            </Row>
                            {instructor.id ?
                                <FormGroup>
                                    <Label for="user">User</Label>
                                    <p>{instructor.user.username || ''}</p>
                                </FormGroup> : <></>
                            }
                            <br></br>
                            <FormGroup>
                                <Button color="primary" type="submit">Save</Button>{' '}
                                <Button color="secondary" tag={Link} to="/instructors">Cancel</Button>
                            </FormGroup>
                        </Form>
                    </Col>
                </Container>
            </div >
        )
    }
}
export default InstructorEdit;