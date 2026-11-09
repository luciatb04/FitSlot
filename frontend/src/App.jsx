import React from "react";
import { Route, Routes } from "react-router-dom";
import { jwtDecode } from "jwt-decode";
import { ErrorBoundary } from "react-error-boundary";
import AppNavbar from "./AppNavbar";
import Home from "./home";
import PrivateRoute from "./privateRoute";
import PricingPlan from "./owner/plan";
import Register from "./auth/register";
import Login from "./auth/login";
import Logout from "./auth/logout";
import OwnerClassesList from "./owner/classes/classList";
import OwnerClassesEdit from "./owner/classes/classEdit";
import OwnerVisitEdit from "./owner/visits/visitEdit";
import PlanList from "./public/plan";
import tokenService from "./services/token.service";
import OwnerDashboard from "./owner/dashboard";
import OwnerConsultationList from "./owner/consultations/consultationList";
import OwnerConsultationEdit from "./owner/consultations/consultationEdit";
import OwnerConsultationTickets from "./owner/consultations/tickets/ticketList";
import InstructorConsultationList from "./instructor/consultations/consultationList";
import InstructorConsultationTickets from "./instructor/consultations/tickets/ticketList";
import ClassEditAdmin from "./admin/classes/ClassEditAdmin";
import ClassListAdmin from "./admin/classes/ClassListAdmin";
import UserListAdmin from "./admin/users/UserListAdmin";
import UserEditAdmin from "./admin/users/UserEditAdmin";
import OwnerListAdmin from "./admin/owners/OwnerListAdmin";
import OwnerEditAdmin from "./admin/owners/OwnerEditAdmin";
import SpecialtyListAdmin from "./admin/instructors/SpecialtyListAdmin";
import SpecialtyEditAdmin from "./admin/instructors/SpecialtyEditAdmin";
import InstructorListAdmin from "./admin/instructors/InstructorListAdmin";
import InstructorEditAdmin from "./admin/instructors/InstructorEditAdmin";
import VisitListAdmin from "./admin/visits/VisitListAdmin";
import VisitEditAdmin from "./admin/visits/VisitEditAdmin";
import ConsultationListAdmin from "./admin/consultations/ConsultationListAdmin";
import TicketListAdmin from "./admin/consultations/TicketListAdmin";
import ConsultationEditAdmin from "./admin/consultations/ConsultationEditAdmin";
import SwaggerDocs from "./public/swagger";
import GymsList from "./gymOwner/gymsList"
import EditGym from "./gymOwner/gymEdit"
import OwnerListGymOwner from "./gymOwner/ownersList"
import GymOwnerListAdmin from "./admin/gymOwners/GymOwnerListAdmin";
import GymOwnerEditAdmin from "./admin/gymOwners/GymOwnerEditAdmin";
import GymListAdmin from "./admin/gyms/GymListAdmin";
import GymEditAdmin from "./admin/gyms/GymEditAdmin";
import ConsultationListGymOwner from "./gymOwner/consultations/ConsultationListGymOwner";
import ConsultationEditGymOwner from "./gymOwner/consultations/ConsultationEditGymOwner";
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

  roles.forEach((role) => {
    if (role === "ADMIN") {
      adminRoutes = (
        <>
          <Route path="/users" exact={true} element={<PrivateRoute><UserListAdmin /></PrivateRoute>} />
          <Route path="/users/:username" exact={true} element={<PrivateRoute><UserEditAdmin /></PrivateRoute>} />
          <Route path="/owners" exact={true} element={<PrivateRoute><OwnerListAdmin /></PrivateRoute>} />
          <Route path="/owners/:id" exact={true} element={<PrivateRoute><OwnerEditAdmin /></PrivateRoute>} />
          <Route path="/gyms" exact={true} element={<PrivateRoute><GymListAdmin /></PrivateRoute>} />
          <Route path="/gyms/:id" exact={true} element={<PrivateRoute><GymEditAdmin /></PrivateRoute>} />
          <Route path="/gymOwners" exact={true} element={<PrivateRoute><GymOwnerListAdmin /></PrivateRoute>} />
          <Route path="/gymOwners/:id" exact={true} element={<PrivateRoute><GymOwnerEditAdmin /></PrivateRoute>} />
          <Route path="/classes" exact={true} element={<PrivateRoute><ClassListAdmin /></PrivateRoute>} />
          <Route path="/classes/:id" exact={true} element={<PrivateRoute><ClassEditAdmin /></PrivateRoute>} />
          <Route path="/classes/:classId/visits" exact={true} element={<PrivateRoute><VisitListAdmin /></PrivateRoute>} />
          <Route path="/classes/:classId/visits/:visitId" exact={true} element={<PrivateRoute><VisitEditAdmin /></PrivateRoute>} />
          <Route path="/instructors" exact={true} element={<PrivateRoute><InstructorListAdmin /></PrivateRoute>} />
          <Route path="/instructors/:id" exact={true} element={<PrivateRoute><InstructorEditAdmin /></PrivateRoute>} />
          <Route path="/instructors/specialties" exact={true} element={<PrivateRoute><SpecialtyListAdmin /></PrivateRoute>} />
          <Route path="/instructors/specialties/:specialtyId" exact={true} element={<PrivateRoute><SpecialtyEditAdmin /></PrivateRoute>} />
          <Route path="/consultations" exact={true} element={<PrivateRoute><ConsultationListAdmin /></PrivateRoute>} />
          <Route path="/consultations/:consultationId" exact={true} element={<PrivateRoute><ConsultationEditAdmin /></PrivateRoute>} />
          <Route path="/consultations/:consultationId/tickets" exact={true} element={<PrivateRoute><TicketListAdmin /></PrivateRoute>} />
        </>)
    }
    if (role === "OWNER") {
      ownerRoutes = (
        <>
          <Route path="/dashboard" element={<PrivateRoute><OwnerDashboard /></PrivateRoute>} />
          <Route path="/plan" exact={true} element={<PrivateRoute><PricingPlan /></PrivateRoute>} />
          <Route path="/myClasses" exact={true} element={<PrivateRoute><OwnerClassesList /></PrivateRoute>} />
          <Route path="/myClasses/:id" exact={true} element={<PrivateRoute><OwnerClassesEdit /></PrivateRoute>} />
          <Route path="/myClasses/:id/visits/:id" exact={true} element={<PrivateRoute><OwnerVisitEdit /></PrivateRoute>} />
          <Route path="/consultations" exact={true} element={<PrivateRoute><OwnerConsultationList /></PrivateRoute>} />
          <Route path="/consultations/:consultationId" exact={true} element={<PrivateRoute><OwnerConsultationEdit /></PrivateRoute>} />
          <Route path="/consultations/:consultationId/tickets" exact={true} element={<PrivateRoute><OwnerConsultationTickets /></PrivateRoute>} />
        </>)
    }
    if (role === "INSTRUCTOR") {
      instructorRoutes = (
        <>
          {/* <Route path="/dashboard" element={<PrivateRoute><OwnerDashboard /></PrivateRoute>} /> */}
          <Route path="/myClasses" exact={true} element={<PrivateRoute><OwnerClassesList /></PrivateRoute>} />
          <Route path="/consultations" exact={true} element={<PrivateRoute><InstructorConsultationList /></PrivateRoute>} />
          <Route path="/consultations/:consultationId/tickets" exact={true} element={<PrivateRoute><InstructorConsultationTickets /></PrivateRoute>} />
        </>)
    }
    if (role === "GYM_OWNER") {
      instructorRoutes = (
        <>
          <Route path="/owners" exact={true} element={<PrivateRoute><OwnerListGymOwner /></PrivateRoute>} />
          <Route path="/gyms" exact={true} element={<PrivateRoute><GymsList /></PrivateRoute>} />
          <Route path="/gyms/:id" exact={true} element={<PrivateRoute><EditGym /></PrivateRoute>} />
          <Route path="/consultations" exact={true} element={<PrivateRoute><ConsultationListGymOwner /></PrivateRoute>} />
          <Route path="/consultations/:id" exact={true} element={<PrivateRoute><ConsultationEditGymOwner /></PrivateRoute>} />
          <Route path="/consultations/:id/tickets" exact={true} element={<PrivateRoute><InstructorConsultationTickets /></PrivateRoute>} />
          <Route path="/instructors" exact={true} element={<PrivateRoute><InstructorListGymOwner /></PrivateRoute>} />
          <Route path="/instructors/:id" exact={true} element={<PrivateRoute><InstructorEditGymOwner /></PrivateRoute>} />
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
        {/* <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} /> */}        
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
        </Routes>
      </ErrorBoundary>
    </div>
  );
}

export default App;
