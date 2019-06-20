<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="false" %>
<jsp:include page="../header.jsp"/>
<style>
.op {
    color: #0000CD;
}
</style>
<table id="contentTable" border="1" >
     <tr>
     	<th width="50">NO</th>
     	<c:if test="${isManager }">
     	<th width="100">Manage</th>
		</c:if>
     	<th>Machine</th>
     	<th width="50">Status</th>
     </tr>
    <c:forEach items="${machines}" var="m" varStatus="s">
     <tr class="dataRow" data-uuid="${m.uuid}">
     	<td align="center">${s.index + 1 }</td>
     	<c:if test="${isManager }">
     	<td align="center">
            <c:if test="${m.start }">
                <a class="op" href="stop?uuid=${m.uuid }">Stop</a>
            </c:if>
            <c:if test="${!m.start }">
                <a class="op" href="start?uuid=${m.uuid }">Start</a>
            </c:if>
     	</td>
		</c:if>
     	<td>${m.uuid }</td>
		<td>
            <c:if test="${m.start }">
            Running
            </c:if>
            <c:if test="${!m.start }">
            Stopped
            </c:if>
        </td>
     </tr>
    </c:forEach>
</table>
<br/>
Strategies Runtime Information:
<iframe id="frameRuntime" name="scheduleStrategyRuntime" height="150" width="100%"></iframe>
ThreadGroup List:
<iframe id="frameThreadGroups" name="servlerList" height="230" width="100%"></iframe>
<script>
$(function() {
    $(".op").click(function() {
        var $this = $(this);
        var url = this.href;
        var txt = $this.text();
        if (!confirm("Confirm to " + txt + " the factory?")) {
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
        var uuid = $this.data('uuid');
        document.getElementById("frameThreadGroups").src = "/threadgroup/index?uuid=" + uuid;
        document.getElementById("frameRuntime").src = "/strategy/runtime?uuid=" + uuid;
    });
    $(".dataRow:first").click();
});
</script>
<jsp:include page="../footer.jsp"/>