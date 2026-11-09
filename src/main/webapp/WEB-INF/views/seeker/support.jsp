<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Contact us · Job.com</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin.css?v=20261102-original-evergreen-1">
  <style>
    .support-layout{display:grid;grid-template-columns:minmax(245px,320px) minmax(0,1fr);gap:18px;align-items:start}
    .support-list,.support-thread{background:#fff;border:1px solid #dce7e3;border-radius:15px;overflow:hidden;box-shadow:0 12px 28px #1d33520a}
    .support-list-head,.support-thread-head{padding:17px 19px;border-bottom:1px solid #e9efec}
    .support-list-head{display:flex;align-items:center;justify-content:space-between;gap:10px}
    .support-list-head h2,.support-thread-head h2{margin:0;color:#1c2c46;font-size:15px}
    .support-new{display:inline-flex;align-items:center;justify-content:center;padding:9px 11px;border-radius:8px;background:#14786e;color:white!important;text-decoration:none;font-size:11px;font-weight:700;white-space:nowrap}
    .support-request{display:block;padding:14px 17px;border-bottom:1px solid #edf1ef;color:#243a38;text-decoration:none}
    .support-request:hover,.support-request.active{background:#f0f8f4}
    .support-request-top{display:flex;align-items:center;justify-content:space-between;gap:7px}
    .support-request strong{font-size:12px;overflow-wrap:anywhere}
    .support-request small{display:block;margin-top:6px;color:#778984;font-size:10px}
    .support-request p{margin:7px 0 0;color:#60726d;font-size:10px;line-height:1.5;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}
    .support-status{padding:4px 7px;border-radius:999px;background:#edf4ef;color:#38705d;font-size:8px;font-weight:750;white-space:nowrap}
    .support-status.resolved{background:#f1f2f4;color:#687487}
    .support-thread-head p{margin:6px 0 0;color:#75847f;font-size:10px}
    .support-messages{padding:18px;min-height:260px;max-height:55vh;overflow:auto;display:flex;flex-direction:column;gap:11px;background:#fbfdfc}
    .support-message{max-width:min(82%,680px);padding:11px 13px;border-radius:12px;background:#eef2f5;color:#243540;align-self:flex-start;overflow-wrap:anywhere;white-space:pre-wrap;font-size:12px;line-height:1.55}
    .support-message.mine{background:#e4f5ed;align-self:flex-end}
    .support-message strong{display:block;margin-bottom:4px;color:#38705d;font-size:9px;text-transform:uppercase;letter-spacing:.4px}
    .support-message small{display:block;margin-top:5px;color:#83908b;font-size:9px}
    .support-form{display:grid;gap:12px;padding:18px}
    .support-form label{display:grid;gap:6px;color:#526963;font-size:11px;font-weight:700}
    .support-form input,.support-form select,.support-form textarea{width:100%;padding:11px 12px;border:1px solid #d7e3de;border-radius:8px;background:#fff;color:#203735;font:inherit;font-size:12px}
    .support-form textarea{min-height:125px;resize:vertical}
    .support-form button{justify-self:start;border:0;border-radius:8px;padding:11px 15px;background:#14786e;color:#fff;font:inherit;font-size:11px;font-weight:750;cursor:pointer}
    .support-empty{padding:38px 22px;text-align:center;color:#75847f}
    .support-empty strong{display:block;color:#253a38;font-size:14px}
    .support-empty p{max-width:440px;margin:9px auto 0;font-size:11px;line-height:1.6}
    .support-hint{margin:0;color:#7c8a85;font-size:10px;line-height:1.5}
    .support-page .seeker-toolbar-avatar{position:relative!important;display:grid!important;place-items:center!important;width:36px!important;height:36px!important;min-width:36px!important;max-width:36px!important;overflow:hidden!important;border-radius:50%!important;flex:none!important}
    .support-page .seeker-toolbar-avatar img{position:absolute!important;inset:0!important;display:block!important;width:100%!important;height:100%!important;max-width:100%!important;max-height:100%!important;object-fit:cover!important;border-radius:50%!important}
    .support-identity{display:flex;align-items:center;gap:17px;padding:16px 19px;margin:0 0 20px;border:1px solid #d9e8e1;border-radius:18px;background:linear-gradient(110deg,#fff,#f0f9f5);box-shadow:0 12px 28px #1d33520a}
    .support-profile-photo{position:relative;display:grid;place-items:center;flex:none;width:92px;height:92px;overflow:hidden;border:4px solid #fff;border-radius:50%;background:linear-gradient(145deg,#dff6ed,#b9e4d4);box-shadow:0 0 0 2px #69c7a7,0 9px 22px #164b3b20;color:#13776d;font-size:28px;font-weight:800}
    .support-profile-photo img{position:absolute;inset:0;width:100%;height:100%;object-fit:cover;border-radius:50%}
    .support-identity strong,.support-identity small{display:block}.support-identity strong{color:#173d39;font-size:18px}.support-identity small{margin-top:6px;color:#738780;font-size:12px;line-height:1.5}
    .support-page .page-header{margin-bottom:17px}
    @media(max-width:800px){.support-layout{grid-template-columns:1fr}.support-messages{max-height:48vh}}
    @media(max-width:520px){.support-identity{padding:13px;gap:13px}.support-profile-photo{width:72px;height:72px}.support-identity strong{font-size:15px}}
  </style>
</head>
<body>
<div class="shell seeker-shell seeker-shell-no-sidebar support-page">
  <main class="main seeker-main">
    <%@ include file="account-menu.jspf" %>
    <section class="support-identity" aria-label="Your Job.com profile">
      <span class="support-profile-photo"><span>J</span><img src="${pageContext.request.contextPath}/seeker/profile-photo" alt="Profile photo" onerror="this.remove()"></span>
      <div><strong><c:out value="${not empty profile.full_name ? profile.full_name : sessionScope.seekerName}"/></strong><small>Your profile photo and private help conversations are shown here.</small></div>
    </section>
    <header class="page-header">
      <div><p class="eyebrow">JOB.COM <span>/</span> SUPPORT</p><h1>Contact us</h1><p class="page-subtitle">Ask the admin team for help or report a complaint. Replies stay in this private conversation.</p></div>
      <div class="signed-in"><span class="online-dot"></span>Signed in as <strong><c:out value="${sessionScope.seekerName}"/></strong></div>
    </header>
    <c:if test="${not empty error}"><div class="alert" role="alert"><c:out value="${error}"/></div></c:if>
    <c:if test="${not empty flash}"><div class="${flashError?'alert':'success-message'}" role="status"><c:out value="${flash}"/></div></c:if>
    <div class="support-layout">
      <aside class="support-list">
        <div class="support-list-head"><h2>Your requests</h2><a class="support-new" href="${pageContext.request.contextPath}/support?new=1">New request</a></div>
        <c:forEach items="${supportRequests}" var="request">
          <a class="support-request ${selectedRequest.id==request.id?'active':''}" href="${pageContext.request.contextPath}/support?conversationId=${request.id}">
            <div class="support-request-top"><strong><c:out value="${request.subject}"/></strong><span class="support-status ${request.status=='RESOLVED'?'resolved':''}"><c:out value="${request.status}"/></span></div>
            <small><c:out value="${request.category}"/> · <c:out value="${request.updatedAt}"/></small>
            <p><c:out value="${request.lastMessage}"/></p>
          </a>
        </c:forEach>
        <c:if test="${empty supportRequests}"><div class="support-empty"><strong>No support requests yet</strong><p>Start a conversation if you need help with your account or want to report an issue.</p></div></c:if>
      </aside>
      <section class="support-thread">
        <c:choose>
          <c:when test="${showCompose}">
            <div class="support-thread-head"><h2>New support request</h2><p>Choose whether you need help or want to submit a complaint.</p></div>
            <form class="support-form" method="post" action="${pageContext.request.contextPath}/support">
              <input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="action" value="create">
              <label>Request type<select name="category" required><option value="HELP">Help</option><option value="COMPLAINT">Complaint</option></select></label>
              <label>Subject<input name="subject" maxlength="180" required placeholder="Briefly describe what you need"></label>
              <label>Message<textarea name="body" maxlength="4000" required placeholder="Include details that will help the admin team respond."></textarea></label>
              <p class="support-hint">Only you and Job.com administrators can see this request. You can reply again after the admin team responds.</p>
              <button type="submit">Send to admin</button>
            </form>
          </c:when>
          <c:when test="${not empty selectedRequest}">
            <div class="support-thread-head"><h2><c:out value="${selectedRequest.subject}"/></h2><p><c:out value="${selectedRequest.category}"/> · Request #<c:out value="${selectedRequest.id}"/> · <span class="support-status ${selectedRequest.status=='RESOLVED'?'resolved':''}"><c:out value="${selectedRequest.status}"/></span></p></div>
            <div class="support-messages" aria-label="Support conversation">
              <c:forEach items="${messages}" var="message"><article class="support-message ${message.senderType=='SEEKER'?'mine':''}"><strong><c:out value="${message.senderType=='SEEKER'?'You':'Job.com Admin'}"/></strong><c:out value="${message.body}"/><small><c:out value="${message.createdAt}"/></small></article></c:forEach>
            </div>
            <form class="support-form" method="post" action="${pageContext.request.contextPath}/support">
              <input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="action" value="reply"><input type="hidden" name="conversationId" value="${selectedRequest.id}">
              <label>Reply<textarea name="body" maxlength="4000" required placeholder="Write a reply to the admin team"></textarea></label>
              <button type="submit">Send reply</button>
            </form>
          </c:when>
          <c:otherwise><div class="support-empty"><strong>Your Job.com support inbox</strong><p>Select a request to read the replies, or start a new help request or complaint. Messages and replies appear here in order.</p><p><a class="support-new" href="${pageContext.request.contextPath}/support?new=1">Create a support request</a></p></div></c:otherwise>
        </c:choose>
      </section>
    </div>
  </main>
</div>
</body>
</html>
