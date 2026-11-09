USE jobcom;

-- Extend private support requests to recruiter/company accounts. Existing rows
-- are kept as seeker conversations by the default value.
ALTER TABLE support_conversations
  ADD COLUMN requester_type VARCHAR(20) NOT NULL DEFAULT 'SEEKER' AFTER user_id;
