<%@ page session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="gym" tagdir="/WEB-INF/tags" %>


<gym:layout pageName="owners">
    <jsp:attribute name="customScript">
        <script>
            $(function () {
                $("#date").datepicker({dateFormat: 'yy/mm/dd'});
            });
        </script>
    </jsp:attribute>
    <jsp:body>
        <h2><c:if test="${visit['new']}">New </c:if>Visit</h2>

        <b>Class</b>
        <table class="table table-striped">
            <thead>
            <tr>
                <th>Name</th>
                <th>Birth Date</th>
                <th>Type</th>
                <th>Owner</th>
            </tr>
            </thead>
            <tr>
                <td><c:out value="${visit.class.name}"/></td>
                <td><gym:localDate date="${visit.class.birthDate}" pattern="yyyy/MM/dd"/></td>
                <td><c:out value="${visit.class.type.name}"/></td>
                <td><c:out value="${visit.class.owner.firstName} ${visit.class.owner.lastName}"/></td>
            </tr>
        </table>

        <form:form modelAttribute="visit" class="form-horizontal">
            <div class="form-group has-feedback">
                <gym:inputField label="Date" name="date"/>
                <gym:inputField label="Description" name="description"/>
            </div>

            <div class="form-group">
                <div class="col-sm-offset-2 col-sm-10">
                    <input type="hidden" name="classId" value="${visit.class.id}"/>
                    <button class="btn btn-default" type="submit">Add Visit</button>
                </div>
            </div>
        </form:form>

        <br/>
        <b>Previous Visits</b>
        <table class="table table-striped">
            <tr>
                <th>Date</th>
                <th>Description</th>
            </tr>
            <c:forEach var="visit" items="${visit.class.visits}">
                <c:if test="${!visit['new']}">
                    <tr>
                        <td><gym:localDate date="${visit.date}" pattern="yyyy/MM/dd"/></td>
                        <td><c:out value="${visit.description}"/></td>
                    </tr>
                </c:if>
            </c:forEach>
        </table>
    </jsp:body>

</gym:layout>
