<%@ page session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="gym" tagdir="/WEB-INF/tags" %>

<gym:layout pageName="instructors">
    <h2>Instructors</h2>

    <table id="instructorsTable" class="table table-striped">
        <thead>
        <tr>
            <th>Name</th>
            <th>Specialties</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${instructors.instructorList}" var="instructor">
            <tr>
                <td>
                    <c:out value="${instructor.firstName} ${instructor.lastName}"/>
                </td>
                <td>
                    <c:forEach var="specialty" items="${instructor.specialties}">
                        <c:out value="${specialty.name} "/>
                    </c:forEach>
                    <c:if test="${instructor.nrOfSpecialties == 0}">none</c:if>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

    <table class="table-buttons">
        <tr>
            <td>
                <a href="<spring:url value="/instructors.xml" htmlEscape="true" />">View as XML</a>
            </td>            
        </tr>
    </table>
</gym:layout>
