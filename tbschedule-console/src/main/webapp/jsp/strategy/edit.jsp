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
	<td><input type="text" id="strategyName" name="strategyName"<c:if test="${!isCreate }"> style="background-color: blue" readonly="readonly"</c:if> value="${strategy.strategyName}" width="30"></td>
	<td>Required, a-zA-Z0-9</td>
</tr>
<tr>
	<td>Type:</td>
	<td><input type="text" id="kind" name="kind" value="${strategy.kind}" width="30"></td>
	<td>Should be one of Schedule, Java or Bean (Case sensitive)</td>
</tr>
<tr>
	<td>Task Name:</td>
	<td><input type="text" id="taskName" name="taskName"  value="${strategy.taskName}" width="30"></td>
	<td>与任务类型匹配的名称例如：1、任务管理中配置的任务名称(对应Schedule) 2、Class名称(对应java) 3、Bean的名称(对应Bean)</td>
</tr>
<tr>
	<td>Task Parameter:</td>
	<td><input type="text" id="taskParameter" name="taskParameter"   value="${strategy.taskParameter}" width="30"></td>
	<td>逗号分隔的Key-Value。 对任务类型为Schedule的无效，需要通过任务管理来配置的</td>
</tr>

<tr>
	<td>Limit Count Single JVM</td>
	<td><input type="text" name="numOfSingleServer" value="${strategy.numOfSingleServer}" width="30"></td>
	<td>单JVM最大线程组数量，如果是0，则表示没有限制.每台机器运行的线程组数量 =总量/机器数 </td>
</tr>
<tr>
	<td>Total Runner Count</td>
	<td><input type="text" name="assignNum" value="${strategy.assignNum}"  width="30"></td>
	<td>所有服务器总共运行的最大数量</td>
</tr>
<tr>
	<td>Targets(Separated by comma): </td>
	<td><input type="text" name="ips" value="${ips}" width="30"></td>
	<td>Fill it with <b>127.0.0.1</b> or <b>localhost</b> indicating no limit</td>
</tr>
</table>
<br/>
<input type="submit" value="保存" style="width:100px" >
</form>
<script>
$(function() {
    $("#scheduleStrategyForm").ajaxForm({
        dataType : 'json',
        beforeSerialize : function() {
            var strategyName = document.all("strategyName").value;
        	var reg = /.*[\u4e00-\u9fa5]+.*$/; 
        	if(reg.test(strategyName)){
        	   alert('任务类型不能含中文');
        	   return false;
        	}
        	if(strategyName == null || strategyName == '' || isContainSpace(strategyName)) {
        		alert('任务类型不能为空或存在空格');
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