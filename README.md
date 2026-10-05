# AI_JobAutomation_EasyApply
An AI-powered Career assistant that discovers relevant jobs, tailors resumes automatically, seeks user approval, and applies to jobs on the user's behalf

UserProfile + JobListing
          ↓
    Text normalization
          ↓
 ┌──────────────────────┐
 │ Role      → /25      │
 │ Skills    → /40      │
 │ Experience→ /20      │
 │ Location  → /10      │
 │ Recency   → /5       │
 └──────────────────────┘
          ↓
    Weighted score /100
          ↓
 HIGH / MEDIUM / LOW / REJECT
          ↓
 Reasons + Concerns
          ↓
 JobMatchResultDto
