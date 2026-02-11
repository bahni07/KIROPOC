# Branching Strategy & Deployment Workflow

This document explains how to control deployments and manage code changes safely.

---

## 🌳 Branch Structure

```
main (production)
  ↑
  PR (with approval)
  ↑
develop (staging/testing)
  ↑
  PR
  ↑
feature/* (development)
```

### **Branch Purposes:**

1. **`main`** - Production code
   - ✅ Always stable and deployable
   - ✅ Protected branch (requires PR approval)
   - ✅ Deploys ONLY via releases
   - ❌ No direct commits allowed

2. **`develop`** - Integration/staging branch
   - ✅ Latest features being tested
   - ✅ CI runs automatically (tests only)
   - ❌ Does NOT auto-deploy
   - Used for integration testing

3. **`feature/*`** - Feature branches
   - ✅ Individual features or bug fixes
   - ✅ Created from `develop`
   - ✅ Merged back to `develop` via PR
   - Examples: `feature/user-login`, `feature/email-verification`

---

## 🚀 Deployment Workflow

### **How Deployments Work:**

```
1. Develop feature
   └─> Work on feature/* branch

2. Test locally
   └─> Run tests: mvn test

3. Create PR to develop
   └─> GitHub Actions runs tests automatically
   └─> Team reviews code
   └─> Merge when approved

4. Test on develop
   └─> Integration tests run
   └─> Manual testing on staging environment

5. Create PR to main
   └─> Final review
   └─> Merge when ready for production

6. Create Release (MANUAL STEP)
   └─> Go to GitHub → Releases → Create new release
   └─> Tag: v1.0.0, v1.1.0, etc.
   └─> Click "Publish release"
   └─> THIS triggers deployment ✅
```

---

## 📋 Step-by-Step: Making Changes

### **Scenario 1: Adding a New Feature**

```bash
# 1. Create feature branch from develop
git checkout develop
git pull origin develop
git checkout -b feature/new-feature

# 2. Make your changes
# ... edit files ...

# 3. Commit changes
git add .
git commit -m "Add new feature"

# 4. Push to GitHub
git push origin feature/new-feature

# 5. Create Pull Request on GitHub
# Go to: https://github.com/bahni07/KIROPOC/pulls
# Click "New Pull Request"
# Base: develop ← Compare: feature/new-feature
# Click "Create Pull Request"

# 6. Wait for CI to pass (automatic)
# GitHub Actions will run tests

# 7. Get approval and merge
# Team member reviews and approves
# Click "Merge Pull Request"

# 8. Feature is now in develop (NOT in production yet)
```

### **Scenario 2: Deploying to Production**

```bash
# 1. Ensure develop is stable
# All tests passing, features working

# 2. Create PR from develop to main
# Go to: https://github.com/bahni07/KIROPOC/pulls
# Click "New Pull Request"
# Base: main ← Compare: develop
# Click "Create Pull Request"

# 3. Final review and merge
# Senior team member reviews
# Click "Merge Pull Request"

# 4. Create a Release (THIS TRIGGERS DEPLOYMENT)
# Go to: https://github.com/bahni07/KIROPOC/releases
# Click "Create a new release"
# Tag: v1.0.0 (or next version)
# Title: "Release v1.0.0"
# Description: List of changes
# Click "Publish release"

# 5. Deployment happens automatically
# GitHub Actions builds Docker image
# Pushes to Docker Hub
# Triggers deployment webhook
# App deploys to production ✅
```

---

## 🔒 Branch Protection Rules

### **Setup on GitHub:**

Go to: Settings → Branches → Add rule

**For `main` branch:**
```
✅ Require pull request before merging
✅ Require approvals: 1
✅ Require status checks to pass
   - build-and-test
✅ Require branches to be up to date
✅ Do not allow bypassing the above settings
```

**For `develop` branch:**
```
✅ Require pull request before merging
✅ Require status checks to pass
   - build-and-test
```

---

## 🎯 What Triggers What

| Action | Tests Run? | Build Docker? | Deploy? |
|--------|-----------|---------------|---------|
| Push to `feature/*` | ❌ No | ❌ No | ❌ No |
| Push to `develop` | ✅ Yes | ❌ No | ❌ No |
| PR to `main` | ✅ Yes | ❌ No | ❌ No |
| Merge to `main` | ✅ Yes | ❌ No | ❌ No |
| **Create Release** | ✅ Yes | ✅ Yes | ✅ **YES** |

**Key Point:** Only creating a release triggers deployment!

---

## 🛡️ Safety Features

### **1. Tests Must Pass**
- Every PR runs full test suite
- Cannot merge if tests fail
- Prevents broken code from reaching production

### **2. Code Review Required**
- At least 1 approval needed for `main`
- Team reviews changes before merge
- Catches bugs and improves code quality

### **3. Manual Release Process**
- Deployment requires explicit release creation
- No accidental deployments
- Full control over when code goes live

### **4. Rollback Capability**
```bash
# If something goes wrong, rollback to previous version
# Create new release from previous tag
git checkout v1.0.0
# Create hotfix if needed
# Release as v1.0.1
```

---

## 📊 Example Timeline

### **Week 1: Development**
```
Monday:
  - Create feature/user-profile branch
  - Develop feature
  - Push to GitHub (no deployment)

Tuesday:
  - Create PR to develop
  - CI runs tests ✅
  - Get approval
  - Merge to develop (no deployment)

Wednesday:
  - Test on develop branch
  - Find and fix bugs
  - Push fixes to develop (no deployment)
```

### **Week 2: Release**
```
Monday:
  - develop branch is stable
  - Create PR to main
  - Final review

Tuesday:
  - Merge to main (no deployment yet)
  - Create Release v1.1.0
  - Deployment happens automatically ✅
  - Monitor production
```

---

## 🚨 Emergency Hotfix

If production has a critical bug:

```bash
# 1. Create hotfix branch from main
git checkout main
git pull origin main
git checkout -b hotfix/critical-bug

# 2. Fix the bug
# ... make changes ...

# 3. Commit and push
git add .
git commit -m "Fix critical bug"
git push origin hotfix/critical-bug

# 4. Create PR to main (expedited review)
# Get quick approval

# 5. Merge to main

# 6. Create Release v1.0.1 (patch version)
# Deployment happens immediately

# 7. Merge hotfix back to develop
git checkout develop
git merge hotfix/critical-bug
git push origin develop
```

---

## 🔧 Local Development

### **Running Locally (No Deployment)**

```bash
# Run with H2 database
mvn spring-boot:run -Dspring-boot.run.profiles=test

# Run tests
mvn test

# Build Docker image locally
docker build -t user-registration:local .

# Run Docker container locally
docker run -p 8080:8080 user-registration:local
```

**None of these trigger deployment!**

---

## 📝 Commit Message Convention

Use clear commit messages:

```
feat: Add user profile feature
fix: Fix email verification bug
docs: Update README
test: Add integration tests
refactor: Improve code structure
chore: Update dependencies
```

---

## 🎓 Summary

### **Key Principles:**

1. **Never commit directly to `main`** - Always use PRs
2. **Test on `develop` first** - Integration testing
3. **Deploy via releases only** - Manual, controlled process
4. **One feature per branch** - Easy to review and rollback
5. **Tests must pass** - Automated quality gate

### **To Deploy:**

```
1. Merge changes to main
2. Go to GitHub Releases
3. Click "Create new release"
4. Publish release
5. Deployment happens automatically
```

**You have full control - no accidental deployments!**

---

## 🆘 Need Help?

- **Tests failing?** Check GitHub Actions logs
- **Can't merge?** Ensure tests pass and get approval
- **Deployment failed?** Check Railway/Render logs
- **Need to rollback?** Create release from previous tag

For questions, open an issue on GitHub or contact the team.
