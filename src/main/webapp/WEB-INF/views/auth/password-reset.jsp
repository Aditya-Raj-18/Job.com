<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Reset password · Job.com</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/admin.css?v=20261102-original-evergreen-1">
</head>
<body class="professional-login-page">
  <main class="auth-layout reset-layout">
    <aside class="auth-showcase reset-showcase">
      <a class="auth-brand" href="${pageContext.request.contextPath}/">Job<span>.com</span></a>
      <p class="auth-kicker">ACCOUNT RECOVERY</p>
      <h1>Let’s get you back to work.</h1>
      <p class="auth-copy">Verify the details already linked to your account, then choose a new password.</p>
      <div class="auth-benefits">
        <div><span>01</span><p><strong>Verify your account</strong><small>Use details that match your Job.com account.</small></p></div>
        <div><span>02</span><p><strong>Choose a stronger password</strong><small>Your new password must be at least 12 characters.</small></p></div>
      </div>
      <div class="auth-footnote">Never share your password or recovery details with anyone.</div>
    </aside>
    <section class="auth-card reset-card">
      <div class="auth-mobile-brand"><a class="auth-brand" href="${pageContext.request.contextPath}/">Job<span>.com</span></a></div>
      <p class="eyebrow">SECURE ACCOUNT RECOVERY</p>
      <h2>Reset your password</h2>
      <p class="auth-intro">
        <c:choose>
          <c:when test="${type == 'seeker'}">Confirm your email, mobile number, and date of birth.</c:when>
          <c:when test="${type == 'recruiter'}">Confirm your company email and business license number.</c:when>
          <c:otherwise>Confirm your administrator email and recovery number.</c:otherwise>
        </c:choose>
      </p>
      <c:if test="${not empty error}"><div class="alert" role="alert"><c:out value="${error}"/></div></c:if>
      <c:if test="${type == 'admin' && not adminRecoveryConfigured}">
        <div class="recovery-setup-note" role="note"><strong>Admin recovery needs setup.</strong> Add <code>JOBCOM_ADMIN_LICENSE_NUMBER</code> or <code>JOBCOM_ADMIN_RECOVERY_CODE</code> to the Tomcat launch environment, then restart Tomcat.</div>
      </c:if>
      <form class="auth-form reset-form" method="post" action="${pageContext.request.contextPath}/forgot-password">
        <input type="hidden" name="csrf" value="${csrf}">
        <input type="hidden" name="type" value="${type}">
        <label>Email address<input type="email" name="email" autocomplete="email" placeholder="you@example.com" required></label>
        <c:if test="${type == 'seeker'}">
          <label>Mobile number<input type="tel" name="phone" autocomplete="tel" inputmode="tel" placeholder="Mobile number on your account" required></label>
          <label>Date of birth<input type="date" name="date_of_birth" autocomplete="bday" required></label>
        </c:if>
        <c:if test="${type == 'recruiter'}">
          <label>Company license number<input type="text" name="license_number" autocomplete="off" placeholder="Business license number" required></label>
        </c:if>
        <c:if test="${type == 'admin'}">
          <label>Admin license / recovery number<input type="password" name="recovery_number" autocomplete="off" placeholder="Configured recovery number" required></label>
        </c:if>
        <div class="auth-form-divider"><span>Set a new password</span></div>
        <label>New password<input type="password" name="new_password" autocomplete="new-password" minlength="12" maxlength="72" placeholder="At least 12 characters" required></label>
        <label>Confirm new password<input type="password" name="confirm_password" autocomplete="new-password" minlength="12" maxlength="72" placeholder="Enter it again" required></label>
        <button type="submit">Change password <span aria-hidden="true">→</span></button>
      </form>
      <div class="auth-register">
        <c:choose>
          <c:when test="${type == 'admin'}"><a href="${pageContext.request.contextPath}/admin/login">Back to sign in</a></c:when>
          <c:when test="${type == 'recruiter'}"><a href="${pageContext.request.contextPath}/recruiter/login">Back to sign in</a></c:when>
          <c:otherwise><a href="${pageContext.request.contextPath}/seeker/login">Back to sign in</a></c:otherwise>
        </c:choose>
      </div>
    </section>
  </main>
</body>
</html>
