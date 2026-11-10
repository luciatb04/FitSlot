import React from "react";
import { Route, Routes } from "react-router-dom";
import { jwtDecode } from "jwt-decode";
import { ErrorBoundary } from "react-error-boundary";
import AppNavbar from "./AppNavbar";
import Home from "./home";
import PrivateRoute from "./privateRoute";
import Register from "./auth/register";
import Login from "./auth/login";
import Logout from "./auth/logout";
import Dashboard from "./dashboard";
import PlanList from "./public/plan";
import tokenService from "./services/token.service";
import MembershipServices from "./gymOwner/memberships/MembershipEditGymOwner";
import ClassEditAdmin from "./admin/classes/ClassEditAdmin";
import ClassListAdmin from "./admin/classes/ClassListAdmin";
import UserListAdmin from "./admin/users/UserListAdmin";
import UserEditAdmin from "./admin/users/UserEditAdmin";
import InstructorListAdmin from "./admin/instructors/InstructorListAdmin";
import InstructorEditAdmin from "./admin/instructors/InstructorEditAdmin";
import SwaggerDocs from "./public/swagger";
import GymsList from "./gymOwner/gymsList"
import EditGym from "./gymOwner/gymEdit"
import GymOwnerListAdmin from "./admin/gymOwners/GymOwnerListAdmin";
import GymOwnerEditAdmin from "./admin/gymOwners/GymOwnerEditAdmin";
import GymListAdmin from "./admin/gyms/GymListAdmin";
import GymEditAdmin from "./admin/gyms/GymEditAdmin";
import InstructorListGymOwner from "./gymOwner/instructors/InstructorListGymOwner";
import InstructorEditGymOwner from "./gymOwner/instructors/InstructorEditGymOwner";

function ErrorFallback({ error, resetErrorBoundary }) {
  return (
    <div role="alert">
      <p>Something went wrong:</p>
      <pre>{error.message}</pre>
      <button onClick={resetErrorBoundary}>Try again</button>
    </div>
  )
}

function App() {
  const jwt = tokenService.getLocalAccessToken();
  let roles = []
  if (jwt) {
    roles = getRolesFromJWT(jwt);
  }

  function getRolesFromJWT(jwt) {
    return jwtDecode(jwt).authorities;
  }

  let adminRoutes = <></>;
  let ownerRoutes = <></>;
  let userRoutes = <></>;
  let instructorRoutes = <></>;
  let publicRoutes = <></>;
  let gymOwnerRoutes = <></>;

  roles.forEach((role) => {
    if (role === "ADMIN") {
      adminRoutes = (
        <>
          <Route path="/users" exact={true} element={<PrivateRoute><UserListAdmin /></PrivateRoute>} />
          <Route path="/users/:username" exact={true} element={<PrivateRoute><UserEditAdmin /></PrivateRoute>} />
          <Route path="/gyms" exact={true} element={<PrivateRoute><GymListAdmin /></PrivateRoute>} />
          <Route path="/gyms/:id" exact={true} element={<PrivateRoute><GymEditAdmin /></PrivateRoute>} />
          <Route path="/gymOwners" exact={true} element={<PrivateRoute><GymOwnerListAdmin /></PrivateRoute>} />
          <Route path="/gymOwners/:id" exact={true} element={<PrivateRoute><GymOwnerEditAdmin /></PrivateRoute>} />
          <Route path="/classes" exact={true} element={<PrivateRoute><ClassListAdmin /></PrivateRoute>} />
          <Route path="/classes/:id" exact={true} element={<PrivateRoute><ClassEditAdmin /></PrivateRoute>} />
          <Route path="/instructors" exact={true} element={<PrivateRoute><InstructorListAdmin /></PrivateRoute>} />
          <Route path="/instructors/:id" exact={true} element={<PrivateRoute><InstructorEditAdmin /></PrivateRoute>} />
        </>)
    }
    if (role === "INSTRUCTOR") {
      instructorRoutes = (
        <>
        </>)
    }
    if (role === "GYM_OWNER") {
      gymOwnerRoutes = (
        <>
          <Route path="/gyms" exact={true} element={<PrivateRoute><GymsList /></PrivateRoute>} />
          <Route path="/gyms/:id" exact={true} element={<PrivateRoute><EditGym /></PrivateRoute>} />
          <Route path="/instructors" exact={true} element={<PrivateRoute><InstructorListGymOwner /></PrivateRoute>} />
          <Route path="/instructors/:id" exact={true} element={<PrivateRoute><InstructorEditGymOwner /></PrivateRoute>} />
          <Route path="/membership-services" element={<PrivateRoute><MembershipServices /></PrivateRoute>}/>
        </>)
    }
  })
  if (!jwt) {
    publicRoutes = (
      <>        
        <Route path="/register" element={<Register />} />
        <Route path="/login" element={<Login />} />
      </>
    )
  } else {
    userRoutes = (
      <>
        <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />      
        <Route path="/logout" element={<Logout />} />
        <Route path="/login" element={<Login />} />
      </>
    )
  }

  return (
    <div>
      <ErrorBoundary FallbackComponent={ErrorFallback} >
        <AppNavbar />
        <Routes>
          <Route path="/" exact={true} element={<Home />} />
          <Route path="/plans" element={<PlanList />} />
          <Route path="/docs" element={<SwaggerDocs />} />
          {publicRoutes}
          {userRoutes}
          {adminRoutes}
          {ownerRoutes}
          {instructorRoutes}
          {gymOwnerRoutes}
        </Routes>
      </ErrorBoundary>
    </div>
  );
}

export default App;
