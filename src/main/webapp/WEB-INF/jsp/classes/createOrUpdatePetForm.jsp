<%@ page session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="gym" tagdir="/WEB-INF/tags" %>

<gym:layout pageName="owners">
    <jsp:attribute name="customScript">
        <script>
            $(function () {
                $("#birthDate").datepicker({dateFormat: 'yy/mm/dd'});
            });
        </script>
    </jsp:attribute>
    <jsp:body>
        <h2>
            <c:if test="${class['new']}">New </c:if> Class
        </h2>
        <form:form modelAttribute="class"
                   class="form-horizontal">
            <input type="hidden" name="id" value="${class.id}"/>
            <div class="form-group has-feedback">
                <div class="form-group">
                    <label class="col-sm-2 control-label">Owner</label>
                    <div class="col-sm-10">
                        <c:out value="${class.owner.firstName} ${class.owner.lastName}"/>
                    </div>
                </div>
                <gym:inputField label="Name" name="name"/>
                <gym:inputField label="Birth Date" name="birthDate"/>
                <div class="control-group">
                    <gym:selectField name="type" label="Type " names="${types}" size="5"/>
                </div>
            </div>
            <div class="form-group">
                <div class="col-sm-offset-2 col-sm-10">
                    <c:choose>
                        <c:when test="${class['new']}">
                            <button class="btn btn-default" type="submit">Add Class</button>
                        </c:when>
                        <c:otherwise>
                            <button class="btn btn-default" type="submit">Update Class</button>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </form:form>
        <c:if test="${!class['new']}">
        </c:if>
    </jsp:body>
</gym:layout>
