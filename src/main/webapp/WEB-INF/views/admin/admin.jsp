<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Job.com Admin</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin.css?v=20261102-original-evergreen-1">
  <style>
    .support-admin-layout{display:grid;grid-template-columns:minmax(250px,350px) minmax(0,1fr);gap:16px;align-items:start}.support-admin-list,.support-admin-thread{background:#fff;border:1px solid var(--line);border-radius:13px;overflow:hidden;box-shadow:var(--shadow)}.support-admin-list-head,.support-admin-thread-head{padding:16px 18px;border-bottom:1px solid #edf0f5}.support-admin-list-head h2,.support-admin-thread-head h2{margin:0;font-size:15px;color:#17243c}.support-admin-case{display:block;padding:13px 16px;border-bottom:1px solid #edf0f5;text-decoration:none!important;color:#27364e!important}.support-admin-case:hover,.support-admin-case.active{background:#f1f8f4}.support-admin-case strong{display:block;font-size:12px}.support-admin-case small{display:block;margin-top:5px;color:#7d8a9e;font-size:9px}.support-admin-case p{font-size:10px;color:#637087;line-height:1.45;margin:7px 0 0;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}.support-admin-case-top{display:flex;justify-content:space-between;gap:8px;align-items:center}.support-admin-thread-head p{font-size:10px;color:#77849a;margin:6px 0 0}.support-admin-messages{padding:17px;min-height:250px;max-height:52vh;overflow:auto;display:flex;flex-direction:column;gap:10px;background:#fbfcfe}.support-admin-message{max-width:min(82%,700px);padding:11px 13px;border-radius:12px;background:#eef2f7;color:#25364c;font-size:12px;line-height:1.5;white-space:pre-wrap;overflow-wrap:anywhere}.support-admin-message.admin{align-self:flex-end;background:#e4f4eb}.support-admin-message strong{display:block;font-size:9px;color:#66758b;margin-bottom:4px}.support-admin-message small{display:block;font-size:9px;color:#8793a4;margin-top:5px}.support-admin-form{display:grid;gap:10px;padding:16px;border-top:1px solid #edf0f5}.support-admin-form label{display:grid;gap:6px;color:#59677d;font-size:10px;font-weight:700}.support-admin-form textarea,.support-admin-form select{padding:10px;border:1px solid #dce3ee;border-radius:7px;font:inherit;font-size:11px}.support-admin-form textarea{min-height:92px;resize:vertical}.support-admin-form button{justify-self:start;padding:10px 14px}.support-admin-status{display:flex;gap:9px;align-items:center}.support-admin-status button{padding:8px 12px}.support-admin-empty{padding:38px 22px;text-align:center;color:#8492a7}.support-admin-empty strong{display:block;color:#53647c}.support-admin-empty p{font-size:11px;line-height:1.5}.support-admin-pill{padding:4px 7px;border-radius:999px;background:#edf4ef;color:#38705d;font-size:8px;font-weight:750;white-space:nowrap}.support-admin-pill.resolved{background:#f1f2f4;color:#687487}@media(max-width:820px){.support-admin-layout{grid-template-columns:1fr}.support-admin-messages{max-height:48vh}}
  </style>
</head>
<body>
<div class="shell">
  <aside class="sidebar">
    <a class="brand" href="${pageContext.request.contextPath}/admin">Job<span>.com</span></a>
    <p class="sidebar-label">ADMIN CONSOLE</p>
    <nav class="nav-list" aria-label="Admin navigation">
      <a class="nav ${section=='dashboard'?'active':''}" href="${pageContext.request.contextPath}/admin?section=dashboard"><span class="nav-icon">▦</span>Dashboard</a>
      <div class="nav-group-label">JOB SEEKERS</div>
      <a class="nav ${section=='user-requests'?'active':''}" href="${pageContext.request.contextPath}/admin?section=user-requests"><span class="nav-icon">◷</span>New Requests <c:if test="${pendingSeekers gt 0}"><span class="nav-count"><c:out value="${pendingSeekers}"/></span></c:if></a>
      <a class="nav ${section=='users'?'active':''}" href="${pageContext.request.contextPath}/admin?section=users"><span class="nav-icon">♙</span>Job Seeker List</a>
      <div class="nav-group-label">RECRUITERS</div>
      <a class="nav ${section=='company-requests'?'active':''}" href="${pageContext.request.contextPath}/admin?section=company-requests"><span class="nav-icon">◷</span>New Requests <c:if test="${pendingRecruiters gt 0}"><span class="nav-count"><c:out value="${pendingRecruiters}"/></span></c:if></a>
      <a class="nav ${section=='companies'?'active':''}" href="${pageContext.request.contextPath}/admin?section=companies"><span class="nav-icon">▤</span>Recruiter List</a>
      <a class="nav ${section=='jobs'?'active':''}" href="${pageContext.request.contextPath}/admin?section=jobs"><span class="nav-icon">▣</span>Jobs</a>
      <a class="nav ${section=='catalog'?'active':''}" href="${pageContext.request.contextPath}/admin?section=catalog"><span class="nav-icon">⌘</span>Catalog</a>
      <a class="nav ${section=='practice'?'active':''}" href="${pageContext.request.contextPath}/admin?section=practice"><span class="nav-icon">◉</span>Practice</a>
      <a class="nav ${section=='reports'?'active':''}" href="${pageContext.request.contextPath}/admin?section=reports"><span class="nav-icon">▥</span>Reports</a>
      <a class="nav" href="${pageContext.request.contextPath}/admin/assessments"><span class="nav-icon">✓</span>Assessments</a>
      <a class="nav" href="${pageContext.request.contextPath}/admin/resume-templates"><span class="nav-icon">▤</span>ATS resume templates</a>
      <a class="nav ${section=='notifications'?'active':''}" href="${pageContext.request.contextPath}/admin?section=notifications"><span class="nav-icon">◌</span>Notifications</a>
      <a class="nav ${section=='support'?'active':''}" href="${pageContext.request.contextPath}/admin?section=support"><span class="nav-icon">?</span>Help &amp; Support <c:if test="${openSupportCount gt 0}"><span class="nav-count"><c:out value="${openSupportCount}"/></span></c:if></a>
    </nav>
    <div class="sidebar-bottom">
      <div class="admin-mini"><span class="avatar">A</span><span><strong><c:out value="${sessionScope.adminName}"/></strong><small>Administrator</small></span></div>
      <form method="post" action="${pageContext.request.contextPath}/admin/logout"><input type="hidden" name="csrf" value="${csrf}"><button class="logout">Sign out</button></form>
    </div>
  </aside>

  <main class="main">
    <header class="page-header">
      <div><p class="eyebrow">JOB.COM <span>/</span> ADMINISTRATION</p><h1><c:out value="${sectionTitle}"/></h1><p class="page-subtitle"><c:out value="${sectionSubtitle}"/></p></div>
      <div class="signed-in"><span class="online-dot"></span>Signed in as <strong><c:out value="${sessionScope.adminName}"/></strong></div>
    </header>
    <c:if test="${not empty error}"><div class="alert" role="alert"><span class="alert-icon">!</span><c:out value="${error}"/></div></c:if>

    <c:if test="${section=='dashboard'}">
      <section class="welcome-panel admin-welcome">
        <div><p class="welcome-kicker">ADMIN OVERVIEW</p><h2>Your portal at a glance</h2><p>Review new registrations, monitor activity, and keep Job.com up to date.</p></div>
        <img class="welcome-illustration" src="${pageContext.request.contextPath}/assets/images/admin-control.svg" alt="Animated illustration of the Job.com administration workspace">
      </section>
      <section class="stats-grid" aria-label="Portal statistics">
        <c:forEach items="${stats}" var="entry"><article class="stat-card"><div class="stat-top"><span class="stat-label"><c:out value="${entry.key}"/></span><span class="stat-mark">↗</span></div><strong><c:out value="${entry.value}"/></strong><small>Current total</small></article></c:forEach>
      </section>
      <section class="section-heading"><div><p class="eyebrow">WORKSPACE</p><h2>Review and manage</h2></div><p>Open a section to review records and take action.</p></section>
      <section class="action-grid">
        <a class="action-card" href="${pageContext.request.contextPath}/admin?section=user-requests"><span class="action-icon seeker-icon">♙</span><span><strong>New seeker requests</strong><small>Review profiles and submitted ID details</small></span><b>→</b></a>
        <a class="action-card" href="${pageContext.request.contextPath}/admin?section=company-requests"><span class="action-icon recruiter-icon">▤</span><span><strong>New recruiter requests</strong><small>Review company and license information</small></span><b>→</b></a>
      </section>
    </c:if>

    <c:if test="${section=='reports'}"><section class="section-heading"><div><p class="eyebrow">EXPORT</p><h2>Portal summary</h2></div><a class="seeker-button" href="${pageContext.request.contextPath}/admin/report-export">Download CSV</a></section><section class="stats-grid" aria-label="Portal reports"><c:forEach items="${stats}" var="entry"><article class="stat-card"><div class="stat-top"><span class="stat-label"><c:out value="${entry.key}"/></span><span class="stat-mark">↗</span></div><strong><c:out value="${entry.value}"/></strong></article></c:forEach></section></c:if>

    <c:if test="${section=='users' || section=='user-requests' || section=='companies' || section=='company-requests' || section=='jobs'}">
      <section class="review-note"><span class="review-note-icon">i</span><span><c:choose><c:when test="${section=='users' || section=='user-requests'}">Open <strong>Review details</strong> to inspect the seeker profile and submitted government ID. Approving a request moves it to Job Seeker List.</c:when><c:when test="${section=='companies' || section=='company-requests'}">Open <strong>Review details</strong> to inspect recruiter contact information, company profile, and submitted business license. Approving a request moves it to Recruiter List.</c:when><c:otherwise>Review the job listing and company details before verifying or rejecting a post.</c:otherwise></c:choose></span></section>
      <form class="search" method="get"><input type="hidden" name="section" value="${section}"><label class="search-box"><span>⌕</span><input name="q" placeholder="Search by name or email" value="${param.q}" aria-label="Search records"></label><button>Search</button></form>
      <div class="table-wrap"><table><thead><tr><th>ID</th><th><c:choose><c:when test="${section=='jobs'}">Job title</c:when><c:when test="${section=='companies' || section=='company-requests'}">Company</c:when><c:otherwise>Job seeker</c:otherwise></c:choose></th><th><c:choose><c:when test="${section=='jobs'}">Company</c:when><c:otherwise>Email</c:otherwise></c:choose></th><th>Status</th><th>Registered</th><th>Review / actions</th></tr></thead>
        <tbody><c:forEach items="${rows}" var="r"><tr>
          <td class="id-cell">#<c:out value="${r.id}"/></td>
          <td><strong class="record-name"><c:out value="${r.name}"/></strong></td><td><c:out value="${r.email}"/></td>
          <td><span class="status-pill"><c:out value="${r.status}"/></span></td><td><c:out value="${r.created_at}"/></td>
          <td class="review-cell"><button type="button" class="review-toggle" aria-expanded="false">Review details</button>
            <c:choose><c:when test="${section=='jobs'}"><c:set var="actions" value="verify,reject,delete"/></c:when><c:when test="${section=='user-requests' || section=='company-requests'}"><c:set var="actions" value="approve,reject"/></c:when><c:otherwise><c:set var="actions" value="block,unblock"/></c:otherwise></c:choose>
            <div class="action-buttons"><c:forTokens items="${actions}" delims="," var="act"><form class="inline" method="post"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="section" value="${section}"><input type="hidden" name="type" value="${recordType}"><input type="hidden" name="id" value="${r.id}"><button class="row-action action-${act}" name="action" value="${act}" data-confirm="Confirm ${act} for this record?" title="${act}"><c:out value="${act}"/></button></form></c:forTokens></div>
          </td></tr><tr class="detail-row" hidden><td colspan="6"><section class="details-panel"><div class="details-header"><h3><c:out value="${r.name}"/> — <c:choose><c:when test="${section=='users' || section=='user-requests'}">Seeker profile</c:when><c:when test="${section=='companies' || section=='company-requests'}">Recruiter &amp; company</c:when><c:otherwise>Job listing</c:otherwise></c:choose></h3><button type="button" class="details-close" aria-label="Close review details">Close ×</button></div><dl class="details-grid"><div><dt>Email / contact</dt><dd><c:out value="${r.email}"/></dd></div><div><dt>Account status</dt><dd><c:out value="${r.status}"/></dd></div><c:forEach items="${r.details}" var="field"><div><dt><c:out value="${field.key}"/></dt><dd><c:out value="${field.value}"/></dd></div></c:forEach></dl><c:if test="${not empty r.documents}"><div class="document-list"><strong>Submitted documents</strong><c:forEach items="${r.documents}" var="document"><c:url var="documentUrl" value="/admin/document"><c:param name="path" value="${document.value}"/></c:url><a href="${documentUrl}" target="_blank" rel="noopener"><span>↗</span><c:out value="${document.key}"/> — Download securely</a></c:forEach></div></c:if><c:if test="${section=='users' || section=='user-requests'}"><p class="privacy-note">Government ID numbers are masked in this list. Review the submitted ID type and open its proof document when needed.</p></c:if></section></td></tr></c:forEach><c:if test="${empty rows}"><tr><td colspan="6" class="empty-state"><span>▤</span><strong>No records found</strong><small>Try another search or check back when registrations arrive.</small></td></tr></c:if></tbody></table></div>
    </c:if>

    <c:if test="${section=='catalog'}"><section class="content-card"><form class="panel" method="post"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="section" value="catalog"><input type="hidden" name="action" value="addCatalog"><h2>Add a field</h2><input name="name" placeholder="Field name" required><button>Add field</button></form><div class="table-wrap"><table><tr><th>ID</th><th>Field</th><th>Active</th></tr><c:forEach items="${rows}" var="r"><tr><td><c:out value="${r.id}"/></td><td><c:out value="${r.name}"/></td><td><c:out value="${r.status}"/></td></tr></c:forEach></table></div></section></c:if>
    <c:if test="${section=='practice'}"><section class="content-card"><form class="panel" method="post"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="section" value="practice"><input type="hidden" name="action" value="addPractice"><h2>Add practice resource</h2><input name="title" placeholder="Title" required><input type="url" name="url" placeholder="https://..." required><button>Add resource</button></form><div class="table-wrap"><table><tr><th>Title</th><th>Link</th></tr><c:forEach items="${rows}" var="r"><tr><td><c:out value="${r.name}"/></td><td><a href="<c:out value='${r.email}'/>" rel="noopener"><c:out value="${r.email}"/></a></td></tr></c:forEach></table></div></section></c:if>
    <c:if test="${section=='notifications'}"><section class="content-card"><form class="panel" method="post"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="section" value="notifications"><input type="hidden" name="action" value="announce"><h2>Publish announcement</h2><select name="audience"><option>Everyone</option><option>Users</option><option>Companies</option></select><input name="subject" maxlength="180" placeholder="Subject" required><textarea name="body" maxlength="10000" placeholder="Message" required></textarea><button>Publish</button></form><div class="table-wrap"><table><tr><th>Subject</th><th>Audience</th><th>Message</th><th>Created</th></tr><c:forEach items="${rows}" var="r"><tr><td><c:out value="${r.name}"/></td><td><c:out value="${r.email}"/></td><td><c:out value="${r.body}"/></td><td><c:out value="${r.status}"/></td></tr></c:forEach><c:if test="${empty rows}"><tr><td colspan="4" class="empty-state">No announcements have been published.</td></tr></c:if></table></div></section></c:if>

    <c:if test="${section=='support'}">
      <section class="support-admin-layout">
        <aside class="support-admin-list"><div class="support-admin-list-head"><h2>Help requests &amp; complaints</h2></div>
          <c:forEach items="${supportCases}" var="caseItem"><a class="support-admin-case ${selectedSupport.id==caseItem.id?'active':''}" href="${pageContext.request.contextPath}/admin?section=support&amp;conversationId=${caseItem.id}"><div class="support-admin-case-top"><strong><c:out value="${caseItem.subject}"/></strong><span class="support-admin-pill ${caseItem.status=='RESOLVED'?'resolved':''}"><c:out value="${caseItem.status}"/></span></div><small>#<c:out value="${caseItem.id}"/> · <c:out value="${caseItem.category}"/> · <c:out value="${caseItem.requesterType=='RECRUITER'?'Recruiter':'Job seeker'}"/> · <c:out value="${caseItem.requesterName}"/></small><p><c:out value="${caseItem.lastMessage}"/></p></a></c:forEach>
          <c:if test="${empty supportCases}"><div class="support-admin-empty"><strong>No support requests yet</strong><p>Private help requests and complaints from seekers and recruiters will appear here.</p></div></c:if>
        </aside>
        <section class="support-admin-thread">
          <c:choose><c:when test="${not empty selectedSupport}">
            <div class="support-admin-thread-head"><h2><c:out value="${selectedSupport.subject}"/></h2><p><c:out value="${selectedSupport.category}"/> · Request #<c:out value="${selectedSupport.id}"/> · <c:out value="${selectedSupport.requesterType=='RECRUITER'?'Recruiter / company':'Job seeker'}"/>: <c:out value="${selectedSupport.requesterName}"/> (<c:out value="${selectedSupport.requesterEmail}"/>) · <span class="support-admin-pill ${selectedSupport.status=='RESOLVED'?'resolved':''}"><c:out value="${selectedSupport.status}"/></span></p></div>
            <div class="support-admin-messages" aria-label="Support conversation"><c:forEach items="${supportMessages}" var="message"><article class="support-admin-message ${message.senderType=='ADMIN'?'admin':''}"><strong><c:choose><c:when test="${message.senderType=='ADMIN'}">Job.com Admin</c:when><c:when test="${message.senderType=='RECRUITER'}">Recruiter / company</c:when><c:otherwise>Job Seeker</c:otherwise></c:choose></strong><c:out value="${message.body}"/><small><c:out value="${message.createdAt}"/></small></article></c:forEach></div>
            <form class="support-admin-form" method="post"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="section" value="support"><input type="hidden" name="conversationId" value="${selectedSupport.id}"><input type="hidden" name="action" value="supportReply"><label>Reply to <c:out value="${selectedSupport.requesterType=='RECRUITER'?'company':'seeker'}"/><textarea name="body" maxlength="4000" required placeholder="Write a helpful reply"></textarea></label><button>Send reply</button></form>
            <form class="support-admin-form support-admin-status" method="post"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="section" value="support"><input type="hidden" name="conversationId" value="${selectedSupport.id}"><input type="hidden" name="action" value="supportStatus"><label>Request status<select name="status"><option value="OPEN" ${selectedSupport.status=='OPEN'?'selected':''}>Open</option><option value="IN_PROGRESS" ${selectedSupport.status=='IN_PROGRESS'?'selected':''}>In progress</option><option value="RESOLVED" ${selectedSupport.status=='RESOLVED'?'selected':''}>Resolved</option></select></label><button>Update status</button></form>
          </c:when><c:otherwise><div class="support-admin-empty"><strong>Select a support request</strong><p>Open a request from a job seeker or recruiter to read the conversation and reply.</p></div></c:otherwise></c:choose>
        </section>
      </section>
    </c:if>
  </main>
</div>
<script src="${pageContext.request.contextPath}/assets/js/admin.js?v=detail-inline-20261021" defer></script>
</body>
</html>
