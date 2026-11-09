import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import { Button, Container, Form, FormGroup, Input, Label, Modal, ModalBody, ModalFooter, ModalHeader } from 'reactstrap';

class VisitEdit extends Component {

    emptyVisit = {
        id: '',
        datetime: '',
        description: '',
        instructor: {},
        class: {},
    };

    constructor(props) {
        super(props);
        this.state = {
            visit: this.emptyVisit,
            class: {},
            instructors: [],
            message: null,
            modalShow: false,
        };
        this.handleChange = this.handleChange.bind(this);
        this.handleInstructorChange = this.handleInstructorChange.bind(this);
        this.handleShow = this.handleShow.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.jwt = JSON.parse(window.localStorage.getItem("jwt"));

        let pathArray = window.location.pathname.split('/');
        this.classId = pathArray[2];
        this.visitId = pathArray[4];
    }

    async componentDidMount() {
        const class = await (await fetch(`/api/v1/classes/${this.classId}`, {
            headers: {
                "Authorization": `Bearer ${this.jwt}`,
            },
        })).json();
        if (class.message) this.setState({ message: class.message, modalShow: true });
        else this.setState({ class: class });

        if (!this.state.message) {
            const instructors = await (await fetch(`/api/v1/instructors/`, {
                headers: {
                    "Authorization": `Bearer ${this.jwt}`,
                },
            })).json();
            this.setState({ instructors: instructors });
            if (instructors.message) this.setState({ message: instructors.message, modalShow: true });
            else this.setState({ instructors: instructors });

            if (this.visitId !== 'new' && !this.state.message) {
                const visit = await (await fetch(`/api/v1/classes/${this.classId}/visits/${this.visitId}`, {
                    headers: {
                        "Authorization": `Bearer ${this.jwt}`,
                    },
                })).json();
                if (visit.message) this.setState({ message: visit.message, modalShow: true });
                else this.setState({ visit: visit });
            }
        }
    }

    handleChange(event) {
        const target = event.target;
        const value = target.value;
        const name = target.name;
        let visit = { ...this.state.visit };
        visit[name] = value;
        this.setState({ visit });
    }

    handleInstructorChange(event) {
        const target = event.target;
        const value = Number(target.value);
        const instructors = [...this.state.instructors]
        let selectedInstructor = null;
        selectedInstructor = instructors.filter((instructor) => instructor.id === value)[0];
        let visit = { ...this.state.visit };
        visit["instructor"] = selectedInstructor;
        this.setState({ visit });
    }

    handleShow() {
        let modalShow = this.state.modalShow;
        this.setState({ modalShow: !modalShow });
    }

    async handleSubmit(event) {
        event.preventDefault();
        let visit = { ...this.state.visit };
        const class = { ...this.state.class };
        visit["class"] = class;

        const submit = await (await fetch(`/api/v1/classes/${this.classId}/visits` + (visit.id ? '/' + visit.id : ''), {
            method: (visit.id) ? 'PUT' : 'POST',
            headers: {
                "Authorization": `Bearer ${this.jwt}`,
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(visit),
        })).json();
        if (submit.message) this.setState({ message: submit.message, modalShow: true });
        else window.location.href = `/classes/${this.classId}/visits`;

    }

    render() {
        const { visit, class, instructors } = this.state;
        const title = <h2>{visit.id ? 'Edit Visit' : 'Add Visit'}</h2>;

        const instructorOptions = instructors.map(instructor => <option key={instructor.id} value={instructor.id}>{instructor.firstName} {instructor.lastName}</option>);

        let modal = <></>;
        if (this.state.message) {
            const show = this.state.modalShow;
            const closeBtn = (
                <button className="close" onClick={this.handleShow} type="button">
                    &times;
                </button>
            );
            modal = <div>
                <Modal isOpen={show} toggle={this.handleShow}
                    keyboard={false}>
                    <ModalHeader toggle={this.handleShow} close={closeBtn}>Error!</ModalHeader>
                    <ModalBody>
                        {this.state.message || ""}
                    </ModalBody>
                    <ModalFooter>
                        <Button color="info" onClick={this.handleShow} type="button">Close</Button>
                        <Button color="primary" tag={Link} to={`/myClasses`}>Back</Button>
                    </ModalFooter>
                </Modal></div>
        }

        return <div>
            {/* <AppNavbar /> */}
            <Container>
                {title}
                <Form onSubmit={this.handleSubmit}>
                    <FormGroup>
                        <Label for="datetime">Date and Time</Label>
                        <Input type="datetime-local" required name="datetime" id="datetime" value={visit.datetime || ''}
                            onChange={this.handleChange} autoComplete="datetime" />
                    </FormGroup>
                    <FormGroup>
                        <Label for="description">Description</Label>
                        <Input type="text" name="description" id="description" value={visit.description || ''}
                            onChange={this.handleChange} autoComplete="description" />
                    </FormGroup>
                    <FormGroup>
                        <Label for="instructor">Instructor</Label>
                        <Input type="select" required name="instructor" id="instructor" value={visit.instructor.id}
                            onChange={this.handleInstructorChange} autoComplete="instructor">
                            <option value="">None</option>
                            {instructorOptions}
                        </Input>
                    </FormGroup>
                    <FormGroup>
                        <Label for="class">Class</Label>
                        <p>{class.name || ''}</p>
                    </FormGroup>
                    <FormGroup>
                        <Button color="primary" type="submit">Save</Button>{' '}
                        <Button color="secondary" tag={Link} to={`/classes/${this.classId}/visits`}>Cancel</Button>
                    </FormGroup>
                </Form>
            </Container>
            {modal}
        </div >
    }
}
export default VisitEdit;