<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="false" %>
<jsp:include page="../header.jsp"/>
<c:if test="${errorMessage != null }">
<div style="border: 1px solid red; width: 500px;">
Error Message: ${errorMessage }
</div>
</c:if>
<h1>Storage Configuration</h1>
<form id="configForm" method="get" name="configForm" action="save">
<table class="noborder">
<tr>
    <td>Type:</td>
    <td>
        <select name="storage">
            <option<c:if test="${storage == 'zookeeper'}"> selected</c:if>>zookeeper</option>
            <option<c:if test="${storage == 'redis'}"> selected</c:if>>redis(todo)</option>
            <option<c:if test="${storage == 'jdbc'}"> selected</c:if>>jdbc(todo)</option>
        </select>
    </td>
    <td></td>
</tr>
<tr>
	<td>Address:</td>
	<td><input type="text" name="address"  value="${address }" style="width:300"></td>
	<td>eg. ip:port</td>
</tr>
<tr>
	<td>rootPath：</td>
	<td><input type="text" name="rootPath" value="${rootPath }" style="width:300"></td>
	<td>eg. /taobao-pamirs-schedule/huijin, can be taken as namespace</td>
</tr>
<tr>
	<td>Username：</td>
	<td><input type="text" name="userName" value="${userName }" style="width:300"></td>
	<td>Depend on storage type</td>
</tr>
<tr>
	<td>Password：</td>
	<td><input type="text" name="password" value="${password }" style="width:300"	></td>
	<td>Depend on storage type</td>
</tr>
</table>
<br/>
<input type="submit" value="Save" style="width:100px" >
<a href="index?manager=true">MANAGEMENT INDEX...</a>
<br/><br/>

<c:if test="isInitial">
<b>You are here possibly because:</b><br>
&nbsp;&nbsp;&nbsp;&nbsp; 1. This is the initial time.<br>
&nbsp;&nbsp;&nbsp;&nbsp; 2. The old configuration has been cleared.<br>
&nbsp;&nbsp;&nbsp;&nbsp; 3. The storage can not be connected.<br>
</c:if>

</form>
<script>
$(function() {
    $("#configForm").ajaxForm({
        dataType: 'json',
        success: function(data) {
            if (data.errno == 0) {
                setTimeout(function() {
                	document.location.href = '/strategy/index?manager=true';
                }, 1000);
            } else {
                alert(data.errdesc);
            }
        }
    });
})
</script>
<jsp:include page="../footer.jsp"/>