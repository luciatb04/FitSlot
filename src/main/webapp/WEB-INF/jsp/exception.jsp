<%@ page session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="gym" tagdir="/WEB-INF/tags" %>

<gym:layout pageName="error">

    <spring:url value="/resources/images/classes.png" var="classsImage"/>
    <img src="${classsImage}"/>

    <h2>Something happened...</h2>

    <p>${exception.message}</p>

</gym:layout>
