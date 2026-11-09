<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Administrator sign in · Job.com</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin.css?v=20261102-original-evergreen-1">
</head>
<body class="professional-login-page">
  <main class="auth-layout">
    <aside class="auth-showcase admin-showcase">
      <a class="auth-brand" href="${pageContext.request.contextPath}/">Job<span>.com</span></a>
      <p class="auth-kicker">JOB.COM ADMINISTRATION</p>
      <h1>Keep the whole marketplace moving.</h1>
      <p class="auth-copy">Review registrations, moderate job listings, and keep the Job.com experience running smoothly.</p>
      <div class="auth-benefits">
        <div><span>01</span><p><strong>One trusted control room</strong><small>Manage accounts, jobs, and platform resources.</small></p></div>
        <div><span>02</span><p><strong>Careful decisions, clearly tracked</strong><small>Review requests and keep the marketplace current.</small></p></div>
      </div>
      <div class="auth-footnote">Authorized administrator access only.</div>
    </aside>
    <section class="auth-card">
      <div class="auth-mobile-brand"><a class="auth-brand" href="${pageContext.request.contextPath}/">Job<span>.com</span></a></div>
      <p class="eyebrow">ADMIN CONSOLE</p>
      <h2>Administrator sign in</h2>
      <p class="auth-intro">Sign in to manage the Job.com portal.</p>
      <c:if test="${not empty error}"><div class="alert" role="alert"><c:out value="${error}"/></div></c:if>
      <c:if test="${param.reset=='1'}"><div class="success-message" role="status">Your password has been changed. You can sign in now.</div></c:if>
      <form class="auth-form" method="post" action="${pageContext.request.contextPath}/admin/login">
        <input type="hidden" name="csrf" value="${csrf}">
        <label>Email address<input type="email" name="email" autocomplete="username" placeholder="admin@example.com" required></label>
        <label>Password<input type="password" name="password" autocomplete="current-password" placeholder="Enter your password" required></label>
        <button type="submit">Sign in <span aria-hidden="true">→</span></button>
      </form>
      <p class="auth-privacy auth-admin-note">This area is restricted to authorized administrators.</p>
    </section>
  </main>
</body>
</html>
