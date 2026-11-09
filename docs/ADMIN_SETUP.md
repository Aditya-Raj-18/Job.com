# Setup guide

## Database
Connect to Job.com Local in MySQL Workbench. Choose File > Open SQL Script and open C:\Users\adity\OneDrive\Desktop\Job.com\database\schema.sql. Click the lightning bolt. Right-click Schemas > Refresh All and confirm jobcom appears.

## Eclipse
Choose File > Import > Maven > Existing Maven Projects. Root Directory: C:\Users\adity\OneDrive\Desktop\Job.com. Select jobcom and Finish. Create an Apache Tomcat 10.1 server and add the project.

## Tomcat environment
In the server launch configuration Environment tab add:
JOBCOM_DB_URL = jdbc:mysql://127.0.0.1:3306/jobcom?useSSL=false&serverTimezone=UTC
JOBCOM_DB_USER = root
JOBCOM_DB_PASSWORD = your MySQL root password
JOBCOM_ADMIN_EMAIL = your admin email
JOBCOM_ADMIN_PASSWORD = strong password of 12+ characters
JOBCOM_ADMIN_LICENSE_NUMBER = private admin recovery number (keep it secret)
JOBCOM_UPLOAD_DIR = C:\Users\adity\OneDrive\Desktop\Job.com\uploads

Start the server and visit http://localhost:8080/jobcom/admin/login. First startup creates the admin account. Remove JOBCOM_ADMIN_PASSWORD from the launch configuration after first successful startup; the hashed account remains.

## Password recovery
Job seekers reset a password with the email, mobile number, and date of birth saved on their account. Recruiters use the company email and the business license number saved during company registration.

The default admins table does not have a license-number field. Configure a private JOBCOM_ADMIN_LICENSE_NUMBER in the Tomcat launch configuration to use it as the administrator recovery number, then restart Tomcat. JOBCOM_ADMIN_RECOVERY_CODE is also accepted as an alternative variable name. Do not use a public or shared company license as this value. Admin recovery stays unavailable until one of these variables is configured; the admin reset page shows this setup requirement.

Forgot-password pages:
- Job seeker: /jobcom/forgot-password?type=seeker
- Recruiter/company: /jobcom/forgot-password?type=recruiter
- Administrator: /jobcom/forgot-password?type=admin
