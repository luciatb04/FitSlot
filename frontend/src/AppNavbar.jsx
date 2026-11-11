import React, { useState, useEffect } from 'react';
import {
    Navbar, NavbarBrand, NavLink, NavItem, Nav,
    NavbarToggler, Collapse,
    UncontrolledDropdown, DropdownToggle,
    DropdownMenu, DropdownItem
} from 'reactstrap';
import { Link } from 'react-router-dom';
import tokenService from './services/token.service';
import { jwtDecode } from "jwt-decode";
import { FiLogOut } from "react-icons/fi";
import logo from './static/images/logo.png';
import './AppNavbar.css';

function AppNavbar() {
    const [roles, setRoles] = useState([]);
    const [username, setUsername] = useState("");
    const jwt = tokenService.getLocalAccessToken();
    const [collapsed, setCollapsed] = useState(true);

    const toggleNavbar = () => setCollapsed(!collapsed);

    useEffect(() => {
        if (jwt) {
            const decoded = jwtDecode(jwt);
            setRoles(decoded.authorities || []);
            setUsername(decoded.sub || "");
        }
    }, [jwt]);

    let adminLinks = <></>;
    let instructorLinks = <></>;
    let ownerLinks = <></>;
    let userLinks = <></>;
    let rightLinks = <></>;
    let publicLinks = <></>;
    let receptionistLinks = <></>;

    roles.forEach((role) => {
        if (role === "ADMIN") {
            adminLinks = (
                <>
                    <NavItem>
                        <NavLink className="navbar-link-custom" tag={Link} to="/classes">Classes</NavLink>
                    </NavItem>
                    <NavItem>
                        <NavLink className="navbar-link-custom" tag={Link} to="/instructors">Instructors</NavLink>
                    </NavItem>
                    <NavItem>
                        <NavLink className="navbar-link-custom" tag={Link} to="/gymOwners">Gym Owners</NavLink>
                    </NavItem>
                    <NavItem>
                        <NavLink className="navbar-link-custom" tag={Link} to="/gyms">Gyms</NavLink>
                    </NavItem>
                    <NavItem>
                        <NavLink className="navbar-link-custom" tag={Link} to="/users">Users</NavLink>
                    </NavItem>
                </>
            );
        }

      if (role === "INSTRUCTOR") {
        instructorLinks = (
            <>
                <NavItem>
                    <NavLink
                        className="navbar-link-custom"
                        tag={Link}
                        to="/myClasses"
                    >
                        Classes
                    </NavLink>
                </NavItem>

                <NavItem>
                <NavLink
                    className="navbar-link-custom"
                    tag={Link}
                    to="/availability"
                >
                    Availability
                </NavLink>
            </NavItem>
        </>
    );
}

        if (role === "MEMBER") {
            userLinks = (
                <>
                    <NavItem>
                        <NavLink
                         className="navbar-link-custom"
                        tag={Link}
                        to="/classes"
                    >
                        Classes
                </NavLink>
            </NavItem>

            <NavItem>
                <NavLink
                    className="navbar-link-custom"
                    tag={Link}
                    to="/my-bookings"
                >
                    My Bookings
                </NavLink>
            </NavItem>

            <NavItem>
                <NavLink
                    className="navbar-link-custom"
                    tag={Link}
                    to="/my-membership"
                >
                    My Membership
                </NavLink>
            </NavItem>
        </>
    );
}
    if (role === "RECEPTIONIST") {
        receptionistLinks = (
            <>
                <NavItem>
                    <NavLink
                    className="navbar-link-custom"
                    tag={Link}
                    to="/members"
                >
                    Members
                </NavLink>
            </NavItem>

            <NavItem>
                <NavLink
                    className="navbar-link-custom"
                    tag={Link}
                    to="/medical-certificates"
                >
                    Medical certificates
                </NavLink>
            </NavItem>

            <NavItem>
                <NavLink
                    className="navbar-link-custom"
                    tag={Link}
                    to="/reception-memberships"
                >
                    Memberships
                </NavLink>
            </NavItem>

            <NavItem>
                <NavLink
                    className="navbar-link-custom"
                    tag={Link}
                    to="/check-in"
                >
                    Check-in
                </NavLink>
            </NavItem>

            <NavItem>
                <NavLink
                    className="navbar-link-custom"
                    tag={Link}
                    to="/payments"
                >
                    Payments
                </NavLink>
            </NavItem>
        </>
    );
}
        if (role === "GYM_OWNER") {
            ownerLinks = (
                <>
                    <NavItem>
                        <NavLink className="navbar-link-custom" tag={Link} to="/membership-services">
                            Memberships
                        </NavLink>
                    </NavItem>

                    <UncontrolledDropdown nav inNavbar>
                        <DropdownToggle nav caret className="navbar-link-custom">
                            Classes
                        </DropdownToggle>
                        <DropdownMenu>
                            <DropdownItem tag={Link} to="/myClasses">
                                Create Class
                            </DropdownItem>
                            <DropdownItem tag={Link} to="/instructors">
                                Instructors
                            </DropdownItem>
                        </DropdownMenu>
                    </UncontrolledDropdown>

                    <NavItem>
                        <NavLink className="navbar-link-custom" tag={Link} to="/gyms">
                            Gym
                        </NavLink>
                    </NavItem>
                </>
            );
        }
    });

    if (!jwt) {
        publicLinks = (
            <>
                <NavItem>
                    <NavLink className="navbar-link-custom" id="plans" tag={Link} to="/plans">Pricing Plans</NavLink>
                </NavItem>
                <NavItem>
                    <NavLink className="navbar-link-custom" id="register" tag={Link} to="/register">Register</NavLink>
                </NavItem>
                <NavItem>
                    <NavLink className="navbar-link-custom" id="login" tag={Link} to="/login">Login</NavLink>
                </NavItem>
            </>
        );
    } else {
        userLinks = (
            <>
                <NavItem>
                    <NavLink className="navbar-link-custom" tag={Link} to="/dashboard">
                        Dashboard
                    </NavLink>
                </NavItem>
            </>
        );

        rightLinks = (
            <div className="navbar-right-section">
                <div className="navbar-right-top">
                    <NavLink className="navbar-link-custom profile-link" tag={Link} to="/profile">
                        Profile
                    </NavLink>

                    <NavLink
                        className="navbar-link-custom logout-link"
                        id="logout"
                        tag={Link}
                        to="/logout"
                        title="Logout"
                        aria-label="Logout"
                    >
                        <FiLogOut />
                    </NavLink>
                </div>

                <div className="navbar-username">
                    {username}
                </div>
            </div>
        );
    }

    return (
        <div>
            <Navbar expand="md" dark color="dark" className="px-3">
                <NavbarBrand href="/" className="brand-custom">
                    <img alt="logo" src={logo} style={{ height: 40, width: 40 }} />
                    <span className="brand-text"> FitSlot</span>
                </NavbarBrand>

                <NavbarToggler onClick={toggleNavbar} className="ms-2" />

                <Collapse isOpen={!collapsed} navbar className="navbar-collapse-custom">
                    <Nav className="navbar-center-links" navbar>
                        {userLinks}
                        {adminLinks}
                        {ownerLinks}
                        {publicLinks}
                        {instructorLinks}
                        {receptionistLinks}
                    </Nav>

                    {jwt && rightLinks}
                </Collapse>
            </Navbar>
        </div>
    );
}

export default AppNavbar;