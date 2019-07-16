<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page isELIgnored="false" %>
<jsp:include page="../header.jsp"/>
<style>
.naviLink {
    font-size: 14px;
    margin-right: 15px;
    color: #0000CD;
}
</style>
<h1 align="center">TBSchedule Console</h1>
<a class="naviLink" target="content" href="/strategy/index<c:if test="${isManager }">?manager=true</c:if>">Strategies</a>
<a class="naviLink" target="content" href="/task/index<c:if test="${isManager }">?manager=true</c:if>">Tasks</a>
<a class="naviLink" target="content" href="/machine/index<c:if test="${isManager }">?manager=true</c:if>">Factories/Machines</a>
<a class="naviLink" target="content" href="/threadgroup/index">Thread Groups</a>

<c:if test="${isManager }">
	<a class="naviLink" target="content" href="/config/modify">Storage</a>
	<a class="naviLink" target="content" href="/config/dump">RAW Data</a>
	<a class="naviLink" target="content" href="/config/export">Export</a>
	<a class="naviLink" target="content" href="/config/import">Import</a>
</c:if>
<iframe id="frameContent" name="content" FRAMEBORDER="0"  height="85%" width="100%" src=""></iframe>
<script>
$(function() {
    $(".naviLink").click(function() {
        var $this = $(this);
        $(".naviLink").css({
            backgroundColor: ''
        });
        $this.css({
            backgroundColor: '#FF0000'
        });
    });
    $(".naviLink:first").click();
    $("#frameContent").attr("src", $(".naviLink:first").attr("href"));
});
</script>
<jsp:include page="../footer.jsp"/>