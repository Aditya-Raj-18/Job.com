# Job.com

Java 17 compatible Servlet/JSP application for Apache Tomcat 10.1 and MySQL 8.

## Portals

- **Job seekers:** approved-account sign-in, searchable approved jobs, one application per opening, application status history, private recruiter chat after applying, editable career profile, saved ATS resume drafts with print-to-PDF support, and an announcements inbox.
- **Recruiters:** company profile, job posting for administrator review, job management, application conversations, and company announcements.
- **Administrators:** account and license/ID review, job moderation, ATS template management, assessments, catalog, practice resources, role-targeted announcements, and downloadable CSV portal reports.

The public landing page is `/jobcom/`. Sign-in pages are `/jobcom/seeker/login`, `/jobcom/recruiter/login`, and `/jobcom/admin/login`.

## Setup

Run `database/schema.sql` in MySQL Workbench. For an existing database, apply the migrations under `database/migrations` once. Import this folder into Eclipse as **Maven > Existing Maven Projects**, add Tomcat 10.1, and follow `docs/ADMIN_SETUP.md`. Set `JOBCOM_UPLOAD_DIR` to a private uploads directory before running the server.

Password recovery is available from each login page. Seeker recovery checks email, mobile number, and date of birth; recruiter recovery checks company email and business license number. Admin recovery requires the private JOBCOM_ADMIN_LICENSE_NUMBER or JOBCOM_ADMIN_RECOVERY_CODE Tomcat environment setting described in docs/ADMIN_SETUP.md.

Job seeker and recruiter registrations stay pending until an administrator approves them. Seeker registration stores the government ID type, its last four digits, and the uploaded proof. Recruiter registration stores company license and contact details. Uploaded files are restricted to PDF/JPG/PNG up to 5 MB and can be downloaded through the protected administrator route.

## Job seeker workflow

1. Create a job seeker account and wait for administrator approval.
2. Sign in and use the seeker dashboard to browse approved openings, review applications, and update profile information.
3. Apply to a job to create an application-specific private chat with that recruiter.
4. Open the resume builder to edit and save an ATS resume using an administrator-published format, then print or save it as PDF. The builder shows a live ATS readiness estimate as you edit.
5. Use **ATS score checker** in Career tools to upload a PDF or DOCX and get a point breakdown with suggestions. Optional resume uploads during seeker registration are checked automatically; the score notice clears after 10 seconds.
6. Check Notifications for administrator announcements; recruiter and company notices appear on the recruiter dashboard.
