<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
  <title><c:out value="${pageTitle}"/> · Job.com</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin.css?v=20261102-original-evergreen-1">
  <style>
    .resource-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:18px}
    .resource-card{background:#fff;border:1px solid #dfe5ef;border-radius:16px;padding:22px;box-shadow:0 12px 28px #1d33520a}
    .resource-card h2{margin:5px 0 10px}.resource-card p{color:#56657b}
    .resource-actions{display:flex;flex-wrap:wrap;gap:10px;align-items:center;margin-top:16px}
    .resource-actions a{display:flex;flex:1 1 130px;align-items:center;justify-content:center;min-height:48px;border:0;border-radius:8px;padding:12px 14px;background:#127c72;color:#fff;text-decoration:none;font-size:14px;line-height:1.2;font-weight:750;white-space:nowrap}
    .resource-actions a.view-pdf:link,.resource-actions a.view-pdf:visited{background:#0b675e;color:#fff}
    .resource-actions a.view-pdf:hover,.resource-actions a.view-pdf:focus-visible{background:#084f49;color:#fff;outline:3px solid #a9e8da;outline-offset:2px}
    .resource-actions a.secondary:link,.resource-actions a.secondary:visited{background:#eaf4f1;color:#12695f}
    .resource-actions a:not(.secondary){color:#fff!important;-webkit-text-fill-color:#fff!important}
    .resource-actions a:not(.secondary):hover,.resource-actions a:not(.secondary):focus-visible{color:#fff!important;-webkit-text-fill-color:#fff!important}
    .resource-actions a.secondary:link,.resource-actions a.secondary:visited{color:#12695f!important;-webkit-text-fill-color:#12695f!important}
    .resource-category{display:flex;gap:8px;flex-wrap:wrap;margin:12px 0}.resource-category span{background:#edf6f3;color:#27655d;border-radius:20px;padding:7px 11px;font-weight:700;font-size:13px}
    .resource-filters{display:flex;align-items:end;gap:14px;flex-wrap:wrap;margin:22px 0;padding:18px;background:#fff;border:1px solid #dce7e3;border-radius:14px}.resource-filters label{display:grid;gap:6px;min-width:210px;color:#40586b;font-weight:700}.resource-filters select{font:inherit;color:#173b3b;padding:11px 13px;border:1px solid #cdded8;border-radius:10px;background:#fff}.resource-filters button{border:0;border-radius:9px;padding:12px 18px;background:#127c72;color:#fff;font:inherit;font-weight:750;cursor:pointer}
  </style>
</head>
<body>
<div class="shell seeker-shell seeker-shell-no-sidebar">
  <main class="main seeker-main">
    <%@ include file="account-menu.jspf" %>
    <header class="page-header"><div><p class="eyebrow">JOB.COM <span>/</span> CAREERS</p><h1><c:out value="${pageTitle}"/></h1><p class="page-subtitle"><c:choose><c:when test="${resourceType=='assessments'}">Download assignments and practice PDFs uploaded by Job.com, organized by company and job role.</c:when><c:otherwise>Explore recruiter tips and practice resources to prepare for your next role.</c:otherwise></c:choose></p></div><div class="signed-in"><span class="online-dot"></span>Signed in as <strong><c:out value="${sessionScope.seekerName}"/></strong></div></header>
    <c:if test="${not empty error}"><div class="alert"><c:out value="${error}"/></div></c:if>
    <c:if test="${resourceType=='assessments'}">
      <form class="resource-filters" method="get" action="${pageContext.request.contextPath}/seeker/resources">
        <input type="hidden" name="type" value="assessments">
        <label>Company<select name="company"><option value="">All companies</option><c:forEach items="${assessmentCompanies}" var="company"><option value="<c:out value='${company}'/>" ${selectedCompany==company?'selected':''}><c:out value="${company}"/></option></c:forEach></select></label>
        <label>Job role<select name="role"><option value="">All roles</option><c:forEach items="${assessmentRoles}" var="role"><option value="<c:out value='${role}'/>" ${selectedRole==role?'selected':''}><c:out value="${role}"/></option></c:forEach></select></label>
        <button type="submit">Show PDFs</button>
      </form>
    </c:if>
    <section class="resource-grid" aria-label="Career resources">
      <c:forEach items="${resources}" var="resource">
        <article class="resource-card"><p class="eyebrow"><c:choose><c:when test="${resourceType=='assessments'}">PDF ASSIGNMENT</c:when><c:otherwise>PREPARATION RESOURCE</c:otherwise></c:choose></p><h2><c:out value="${resource.title}"/></h2><c:if test="${resourceType=='assessments'}"><div class="resource-category"><span><c:out value="${resource.company}"/></span><span><c:out value="${resource.role}"/></span></div></c:if><c:if test="${not empty resource.created}"><p>Added <c:out value="${resource.created}"/></p></c:if><div class="resource-actions"><c:choose><c:when test="${resourceType=='assessments'}"><a class="view-pdf" href="${pageContext.request.contextPath}/seeker/resources?type=assessments&amp;view=${resource.id}" target="_blank" rel="noopener">View PDF</a><a class="secondary" href="${pageContext.request.contextPath}/seeker/resources?type=assessments&amp;download=${resource.id}">Download PDF</a></c:when><c:otherwise><a href="<c:out value='${resource.url}'/>" target="_blank" rel="noopener noreferrer">Open resource</a></c:otherwise></c:choose></div></article>
      </c:forEach>
      <c:if test="${empty resources}"><article class="resource-card"><div class="empty-state"><strong><c:choose><c:when test="${resourceType=='assessments'}">No PDFs match this company and job role yet</c:when><c:otherwise>No preparation resources available yet</c:otherwise></c:choose></strong><small>New materials uploaded by the administrator will appear here.</small></div></article></c:if>
    </section>
  </main>
</div>
</body>
</html>
