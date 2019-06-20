<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="false" %>
<jsp:include page="../header.jsp"/>
<style>
.problem {
    background: #e17760;
}
.invalid {
    background: #A9A9A9;
}
.cur {
    background: #FFD700;
}
</style>
<c:if test="${canDoFilter }">
<form method="get">
<table class="noborder">
  <tr>
  	<td>Task：</td><td><input type="text" name="task" value="${task }"> </td>
  	<td>OwnSign：</td><td><input type="text" name="ownSign" value="${ownSign }"> </td>
  	<td>IP：</td><td><input type="text" name="ip" value="${ip }"> </td>
  	<td>Order：</td><td><input type="text" name="orderStr" value="${orderStr }"> </td>
  	<td><input type="submit" value="Filter" style="width:100;"></td>
  </tr>  
</table>
</form>
</c:if>
<table id="list" border="1" style="border-COLLAPSE: collapse; display:block;">
   <tr>
       <th nowrap>NO</th>
       <th>Task<br />[TASK_TYPE]</th>
       <th>Own Sign<br />[OWN_SIGN]</th>
       <th>IP<br />[IP]</th>
       <th>Host Name<br />[HOST_NAME]</th>
       <th nowrap>Threads<br />[THREAD_NUM]</th>
       <th>Create At<br />[REGISTER_TIME]</th>
       <th>Heartbeat At<br />[HEARTBEAT_TIME]</th>
       <th>Last Fetch Data(Select)<br />[LAST_FETCH_DATA_TIME]</th>
       <th nowrap>Version<br />[VERSION]</th>
       <th nowrap>Next Loop Begin<br />[NEXT_RUN_START_TIME]</th>
       <th nowrap>Next Loop End<br />[NEXT_RUN_END_TIME]</th>
       <th>Machine<br />[MANAGER_FACTORY]</th>
       <th>Detail</th>   
   </tr>
   <c:forEach items="${groups }" var="g" varStatus="s">
   <tr class="dataRow 
        <c:if test="${g.centerServerTime.time - g.heartBeatTime.time > taskMap[g.baseTaskType].judgeDeadInterval}"> invalid</c:if>
        <c:if test="${g.nextRunStart.time <= now && (g.lastFetchDataTime == null || g.centerServerTime.time - g.lastFetchDataTime.time > taskMap[g.baseTaskType].heartBeatRate * 20)}"> problem</c:if>
        ">
	   <td>${s.index + 1 }</td>
	   <td>${g.baseTaskType }</td>
	   <td>${g.ownSign }</td>
	   <td nowrap>${g.ip }</td>
	   <td nowrap>${g.hostName }</td>
	   <td>${g.threadNum }</td>
	   <td nowrap>${g.registerTime }</td>
	   <td nowrap>${g.heartBeatTime }</td>
	   <td nowrap>
           <c:if test="${g.lastFetchDataTime == null }">--</c:if>
           <c:if test="${g.lastFetchDataTime != null }">${g.lastFetchDataTime }</c:if>
       </td>
	   <td>${g.version }</td>
	   <td nowrap>
           <c:if test="${g.nextRunStartTime == null }">--</c:if>
           <c:if test="${g.nextRunStartTime != null }">${g.nextRunStartTime }</c:if>
       </td>
	   <td nowrap>
           <c:if test="${g.nextRunEndTime == null }">--</c:if>
           <c:if test="${g.nextRunEndTime != null }">${g.nextRunEndTime }</c:if>
       </td>
	   <td nowrap>${g.managerFactoryUUID }</td>
	   <td nowrap>${g.dealInfoDesc }</td>
   </tr>      
   </c:forEach>
</table>

<script>
$(function() {
    $(".dataRow").click(function() {
        $(".dataRow").removeClass("cur");
        $(this).addClass("cur");
    });
});
</script>
<jsp:include page="../footer.jsp"/>