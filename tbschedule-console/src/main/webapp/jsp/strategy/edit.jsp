<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="false" %>
<jsp:include page="../header.jsp"/>
<form id="scheduleStrategyForm" method="get" name="scheduleStrategyForm" action="save">
<c:if test="${isCreate }">
<input type="hidden" name="isCreate" value="true" />
</c:if>
<c:if test="${!isCreate }">
<input type="hidden" name="isCreate" value="false" />
</c:if>
<table>
<tr>
	<td>Strategy Name:</td>
	<td><input type="text" id="strategyName" name="strategyName"<c:if test="${!isCreate }"> style="background-color: blue" readonly="readonly"</c:if> value="${strategy.name}" width="30"></td>
	<td>Required, a-zA-Z0-9</td>
</tr>
<tr>
	<td>Type:</td>
	<td><input type="text" id="kind" name="kind" value="${strategy.kind}" width="30"></td>
	<td>Should be one of <b>Schedule</b>, <b>Java</b> or <b>Bean</b> (Case sensitive)</td>
</tr>
<tr>
	<td>Task Name:</td>
	<td><input type="text" id="taskName" name="taskName"  value="${strategy.taskName}" width="30"></td>
	<td>
        For <b>Schedule</b> Type: Task Name (Append "$" for OwnSign, eg. "Task1", "Task1$develop")<br />
        For <b>Class</b> Type: Class name (Full name including package). Each thread will new an instance.<br />
        For <b>Bean</b> Type: Bean Id in spring's IoC.
    </td>
</tr>
<tr>
	<td>Task Parameter:</td>
	<td><input type="text" id="taskParameter" name="taskParameter"   value="${strategy.taskParameter}" width="30"></td>
	<td>Key-Value pair separated by comma. For <b>Schedule</b> type please remain empty because parameter is configured in task's properties.</td>
</tr>

<tr>
	<td>Limit Count Single JVM</td>
	<td><input type="text" name="numOfSingleServer" value="${strategy.numOfSingleServer}" width="30"></td>
	<td>Maximum thread group's count per one JVM process. 0 for unlimited.</td>
</tr>
<tr>
	<td>Total Runner Count</td>
	<td><input type="text" name="assignNum" value="${strategy.assignNum}"  width="30"></td>
	<td>Global thread group's count</td>
</tr>
<tr>
	<td>Targets(Separated by comma): </td>
	<td><input type="text" name="ips" value="${ips}" width="30"></td>
	<td>Filled with <b>127.0.0.1</b> or <b>localhost</b> as unlimited</td>
</tr>
</table>
<br/>
<input type="submit" value="Save" style="width:100px" >
</form>
<script>
$(function() {
    $("#scheduleStrategyForm").ajaxForm({
        dataType : 'json',
        beforeSerialize : function() {
            var strategyName = document.all("strategyName").value;
        	var reg = /.*[\u4e00-\u9fa5]+.*$/; 
        	if(reg.test(strategyName)){
        	   alert('No special character in name');
        	   return false;
        	}
        	if(strategyName == null || strategyName == '' || isContainSpace(strategyName)) {
        		alert('Task type cannot be empty');
        		return false;
        	}
        	return true;
        }, 
        success : function(data) {
            if (data.errno == 0) {
                parent.location.reload();
            } else {
                alert(data.errdesc);
            }
        }
    });
});
function isContainSpace(array) {   
	if (array.indexOf(' ') >= 0) {
		return true;
	}
    return false;
}
</script>
<jsp:include page="../footer.jsp"/>