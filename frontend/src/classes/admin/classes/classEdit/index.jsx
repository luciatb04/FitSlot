import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import { Button, Container, Form, FormGroup, Input, Label } from 'reactstrap';

class ClassEdit extends Component {

    emptyItem = {
        id: null,
        name: '',
        birthDate: '',
        type: null,
        owner: null,
    };

    constructor(props) {
        super(props);
        this.state = {
            class: this.emptyItem,
            types: [],
            owners: [],
            message: null,
        };
        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.jwt = JSON.parse(window.localStorage.getItem("jwt"));
        let pathArray = window.location.pathname.split('/');
        this.id = pathArray[2];
    }

    async componentDidMount() {
        if (this.id !== "new") {
            const class = await (await fetch(`/api/v1/classes/${this.id}`, {
                headers: {
                    "Authorization": `Bearer ${this.jwt}`,
                },
            })).json();
            if (class.mesagge) this.setState({ message: class.message });
            else this.setState({ class: class });
        }
        if (!this.state.message) {
            const types = await (await fetch(`/api/v1/classes/types`, {
                headers: {
                    "Authorization": `Bearer ${this.jwt}`,
                },
            })).json();
            if (types.mesagge) this.setState({ message: types.message });
            else this.setState({ types: types });
        }
        if (!this.state.message) {
            const owners = await (await fetch(`/api/v1/owners`, {
                headers: {
                    "Authorization": `Bearer ${this.jwt}`,
                },
            })).json();
            if (owners.mesagge) this.setState({ message: owners.message });
            else this.setState({ owners: owners });
        }
    }

    handleChange(event) {
        const target = event.target;
        const value = target.value;
        const name = target.name;
        let class = { ...this.state.class };
        if (name === "type") {
            class.type = this.state.types.filter((type) => type.id === Number(value))[0];
        } else if (name === "owner") {
            class.owner = this.state.owners.filter((owner) => owner.id === Number(value))[0];
        }
        else class[name] = value;
        this.setState({ class });
    }

    async handleSubmit(event) {
        event.preventDefault();
        const { class, } = this.state;

        const response = await (await fetch('/api/v1/classes' + (class.id ? '/' + this.id : ''), {
            method: class.id ? 'PUT' : 'POST',
            headers: {
                "Authorization": `Bearer ${this.jwt}`,
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(class),
        })).json();
        if (response.message) this.setState({ message: response.message })
        else window.location.href = '/classes';
    }

    render() {
        const { class, types, owners } = this.state;
        const title = <h2>{class.id ? 'Edit Class' : 'Add Class'}</h2>;

        const typeOptions = types.map(type => <option key={type.id} value={type.id}>{type.name}</option>);
        const ownerOptions = owners.map(owner => <option key={owner.id} value={owner.id}>{owner.user.username}</option>);

        if (this.state.message) return <h2 className="text-center">{this.state.message}</h2>

        return <div>
            <Container>
                {title}
                <Form onSubmit={this.handleSubmit}>
                    <FormGroup>
                        <Label for="name">Name</Label>
                        <Input type="text" required name="name" id="name" value={class.name || ''}
                            onChange={this.handleChange} />
                    </FormGroup>
                    <FormGroup>
                        <Label for="birthDate">Birth Date</Label>
                        <Input type="date" name="birthDate" id="birthDate" value={class.birthDate || ''}
                            onChange={this.handleChange} />
                    </FormGroup>
                    <FormGroup>
                        <Label for="type">Type</Label>
                        <Input type="select" required name="type" id="type" value={class.type?.id}
                            onChange={this.handleChange}>
                            <option value="">None</option>
                            {typeOptions}
                        </Input>
                    </FormGroup>
                    <FormGroup>
                        <Label for="owner">Owner</Label>
                        {class.id ?
                            <Input type="select" disabled name="owner" id="owner" value={class.owner?.id || ""}
                                onChange={this.handleChange} >
                                <option value="">None</option>
                                {ownerOptions}
                            </Input> :
                            <Input type="select" required name="owner" id="owner" value={class.owner?.id || ""}
                                onChange={this.handleChange} >
                                <option value="">None</option>
                                {ownerOptions}
                            </Input>}
                    </FormGroup>
                    <FormGroup>
                        <Button color="primary" type="submit">Save</Button>{' '}
                        <Button color="secondary" tag={Link} to="/classes">Cancel</Button>
                    </FormGroup>
                </Form>
            </Container>
        </div>
    }
}
export default ClassEdit;