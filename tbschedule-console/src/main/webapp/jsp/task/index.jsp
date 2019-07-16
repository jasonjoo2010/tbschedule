<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page isELIgnored="false" %>
<jsp:include page="../header.jsp"/>
<style>
a {
    color: #0000CD;
}
</style>
<table id="list" border="1" >
<thead>
     <tr>
     	<th>NO</th>
     	<c:if test="${isManager }">
     	<th >Manage</th>
		</c:if>
     	<th>Task Name</th>
     	<th>Bean</th>
     	<th>Hearbeat<br />Dead In(sec)</th>
     	<th>Batch(Threads)<br />Fetch</th>
     	<th>Sleep(sec)<br />Every Time/No Data</th>
     	<th>Mode</th>
     	<th>Schedule<br />From-To</th>
     	<th>Limit/JVM</th>
     	<th>Parameter</th>
     	<th>Task Items</th>
     </tr>
     </thead>
     <tbody>
    <c:forEach items="${taskList }" var="t" varStatus="s">
     <tr class="dataRow" data-name="${t.name }">
     	<td>${s.index + 1 }</td>
     	<c:if test="${isManager }">
     	<td width="120" align="center">
     		<a target="taskDetail" href="edit?taskName=${t.name }">Edit</a>
     		<a class="op" href="clean?taskName=${t.name }">Clean</a>
     		<a class="op" href="remove?taskName=${t.name }">Remove</a>
     	</td>
		</c:if>
     	<td>${t.name}</td>
     	<td>${t.dealBeanName}</td>
     	<td>${t.heartBeatRate / 1000}/${t.judgeDeadInterval / 1000}</td>
     	<td>${t.executeNumber}(${t.threadNumber})/${t.fetchDataNumber}</td>
     	<td>
            ${t.sleepTimeInterval == 0 ? "--" : t.sleepTimeInterval / 1000} /
            ${t.sleepTimeNoData == 0 ? "--" : t.sleepTimeNoData / 1000}
        </td>
     	<td>${t.processorType}</td>
     	<td>
            ${t.permitRunStartTime == null ? "--" : t.permitRunStartTime}<br />
            ${t.permitRunEndTime == null ? "--" : t.permitRunEndTime}
        </td>
    	<td>${t.maxTaskItemsOfOneThreadGroup}</td>   
		<td>${t.taskParameter == null ? "--" : t.taskParameter}</td>   
		<td>${fn:join(t.taskItems, ",")}</td>
     </tr>
    </c:forEach>
</tbody>
</table>
<br/>

<c:if test="${isManager }">
<a target="taskDetail" href="edit?taskName=-1"  style="color:#0000CD">Create Task...</a>
</c:if>
Runtime Information:<br/>
<iframe id="showTaskDetail" name="taskDetail"  height="80%" width="100%"></iframe>
<script>
$(function() {
    $(".op").click(function() {
        var $this = $(this);
        var url = this.href;
        var txt = $this.text();
        if (!confirm("Confirm to " + txt + " the task?")) {
            return false;
        }
        $.getJSON(url, function(data) {
            if (data.errno == 0) {
                if (data.refresh) {
	                document.location.reload();
                } else {
                    alert("Operation is processed")
                }
            } else {
                alert(data.errdesc);
            }
        });
        return false;
    });
    $(".dataRow").click(function() {
        var $this = $(this);
        var $rows = $(".dataRow");
        $rows.css({
            backgroundColor: ''
        });
        $this.css({
            backgroundColor: '#FFD700'
        });
        document.getElementById("showTaskDetail").src = "runtime?taskName=" + $this.data('name');
    });
    $(".dataRow:first").click();
});
</script>
<jsp:include page="../footer.jsp"/>