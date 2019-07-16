<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="false" %>
<jsp:include page="../header.jsp"/>
<table id="contentTable" border="1">
     <tr>
     	<th>NO</th>
     	<c:if test="${isManager }">
     	<th style="width: 120px;">Manage</th>
		</c:if>
     	<th>Name</th>
     	<th>Status</th>
     	<th>Type</th>
     	<th>Task</th>
     	<th>Parameter</th>
     	<th>Max TG/JVM</th>
    	<th>Max Global TG</th>
     	<th>Targets</th>
     </tr>
    <c:forEach items="${strategyList }" var="s" varStatus="status">
     <tr class="dataRow" data-strategy="${s.name}">
     	<td>${status.index + 1}</td>
     	<c:if test="${isManager }">
     	<td width="100" align="center">
     	    <a target="strategyDetail" href="edit?strategyName=${s.name}" style="color:#0000CD">Edit</a>
     	    <a class="op" href="remove?strategyName=${s.name}">Remove</a>
            <c:if test="${s.running }">
                <a class="op" href="pause?strategyName=${s.name}" style="color:#0000CD">Pause</a>
            </c:if>
            <c:if test="${!s.running }">
                <a class="op" href="resume?strategyName=${s.name}" style="color:#0000CD">Resume</a>
            </c:if>
     	</td>
		</c:if>
     	<td>${s.name}</td>
     	<td>
            <c:if test="${s.running }">
                Running
            </c:if>
            <c:if test="${!s.running }">
                Stopped
            </c:if>
        </td>
     	<td>${s.kind}</td>
     	<td>${s.taskName}</td>
     	<td>${s.parameter}</td>
     	<td align="center">${s.numOfSingleServer }</td>
     	<td align="center">${s.assignNum }</td>
		<td>${s.ip }</td>
     </tr>
    </c:forEach>
</table>
<br/>
<c:if test="${isManager }">
<a target="strategyDetail" href="edit?strategyName=-1" style="color:#0000CD">New Strategy...</a>
</c:if>
Runtime Information：
<iframe id="showStrategyDetail" name="strategyDetail" height="80%" width="100%"></iframe>
<script>
$(function() {
    $(".op").click(function() {
        var $this = $(this);
        var url = this.href;
        var txt = $this.text();
        if (!confirm("Confirm to " + txt + " the strategy?")) {
            return false;
        }
        $.getJSON(url, function(data) {
            if (data.errno == 0) {
                document.location.reload();
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
        document.getElementById("showStrategyDetail").src = "runtime?strategyName=" + $this.data('strategy');
    });
    $(".dataRow:first").click();
});

function validateDel(str) {
    var flag = window.confirm("Confirm to remove strategy [" + str + "]?");
    if(flag) {
        window.location.href="deal?action=deleteScheduleStrategy&strategyName=" + str; 
    }
}
</script>
<jsp:include page="../footer.jsp"/>