# Jib Registry Integration Implementation Plan

## Overview
Configure Jib plugin to build and push container images to private registry (registry.harness.ai) with authentication.

## Prerequisites
- pwgen installed for password generation
- htpasswd installed for password hash generation
- Git repository initialized

## Tasks

### Task 1: Generate Strong Password and Update Registry Auth

**Goal**: Generate a 32-character strong password and update the registry htpasswd file.

**Steps**:

1. Generate password using pwgen:
```bash
pwgen -s 32 1
```

2. Store the generated password in a variable for later use:
```bash
REGISTRY_PASSWORD=$(pwgen -s 32 1)
echo "Generated password: $REGISTRY_PASSWORD"
```

3. Update htpasswd file with the new password:
```bash
htpasswd -Bbn admin "$REGISTRY_PASSWORD" > deploy/registry-auth/htpasswd
```

4. Verify the htpasswd file was updated:
```bash
cat deploy/registry-auth/htpasswd
```

**Expected Output**: 
- A line starting with `admin:$2y$` (bcrypt hash)

**Verification**:
```bash
# Check file exists and has content
test -s deploy/registry-auth/htpasswd && echo "✓ htpasswd file updated" || echo "✗ Failed"
```

**Save password**: Keep the `$REGISTRY_PASSWORD` value for Task 2.

---

### Task 2: Create gradle.properties with Registry Credentials

**Goal**: Create gradle.properties file with registry authentication configuration.

**Steps**:

1. Create the gradle.properties file in lifecycle directory:
```bash
cat > lifecycle/gradle.properties << 'EOF'
registryUrl=registry.harness.ai
registryUsername=admin
registryPassword=PLACEHOLDER_PASSWORD
EOF
```

2. Replace PLACEHOLDER_PASSWORD with the actual password from Task 1:
```bash
sed -i "s/PLACEHOLDER_PASSWORD/$REGISTRY_PASSWORD/" lifecycle/gradle.properties
```

3. Verify the file was created correctly:
```bash
cat lifecycle/gradle.properties
```

**Expected Output**:
```
registryUrl=registry.harness.ai
registryUsername=admin
registryPassword=<32-character-password>
```

**Verification**:
```bash
# Check all three properties exist
grep -q "registryUrl=registry.harness.ai" lifecycle/gradle.properties && \
grep -q "registryUsername=admin" lifecycle/gradle.properties && \
grep -q "registryPassword=" lifecycle/gradle.properties && \
echo "✓ gradle.properties configured" || echo "✗ Failed"
```

---

### Task 3: Update .gitignore to Exclude gradle.properties

**Goal**: Ensure gradle.properties is not committed to git to protect credentials.

**Steps**:

1. Check if gradle.properties is already in .gitignore:
```bash
grep -q "gradle.properties" lifecycle/.gitignore
```

2. If not present, add it:
```bash
echo "gradle.properties" >> lifecycle/.gitignore
```

3. Verify the update:
```bash
cat lifecycle/.gitignore
```

**Expected Output**: File should contain `gradle.properties` entry.

**Verification**:
```bash
grep -q "gradle.properties" lifecycle/.gitignore && \
echo "✓ gradle.properties excluded from git" || echo "✗ Failed"
```

---

### Task 4: Update Jib Configuration in build.gradle

**Goal**: Modify the Jib configuration to use registry.harness.ai with authentication and dual tagging.

**Steps**:

1. Read current build.gradle to understand the structure:
```bash
cat lifecycle/harness-biz/lifecycle-biz/build.gradle
```

2. Update the jib configuration block. Replace the existing `jib { }` block with:

```gradle
jib {
    from {
        image = 'eclipse-temurin:25-jre'
        platforms {
            platform {
                architecture = "amd64"
                os = 'linux'
            }
        }
    }
    
    to {
        image = "${project.findProperty('registryUrl')}/lifecycle-biz"
        tags = ['latest', providers.exec {
            commandLine 'git', 'rev-parse', '--short', 'HEAD'
        }.standardOutput.asText.get().trim()]
        auth {
            username = project.findProperty('registryUsername')
            password = project.findProperty('registryPassword')
        }
    }
    
    container {
        ports = ['8081']
        jvmFlags = ['-Xms256m', '-Xmx512m']
    }
}
```

**Key Changes**:
- Changed `to.image` from `registry.harness.com` to use `${project.findProperty('registryUrl')}`
- Added `tags` with both 'latest' and git short hash
- Added `auth` block with username and password from gradle.properties
- Removed `dockerClient` block (not needed for registry push)

3. Verify the syntax is correct:
```bash
cd lifecycle && ./gradlew :harness-biz:lifecycle-biz:help
```

**Expected Output**: Gradle should execute without syntax errors.

**Verification**:
```bash
# Check that the jib block contains the auth configuration
grep -A 3 "auth {" lifecycle/harness-biz/lifecycle-biz/build.gradle | \
grep -q "username = project.findProperty" && \
echo "✓ Jib configuration updated" || echo "✗ Failed"
```

---

### Task 5: Test Build and Push to Registry

**Goal**: Verify that Jib can build and push the image to the private registry.

**Steps**:

1. Ensure you're in the lifecycle directory:
```bash
cd lifecycle
```

2. Run the Jib build task:
```bash
./gradlew :harness-biz:lifecycle-biz:jib
```

**Expected Output**:
- Build should complete successfully
- Should show "Built and pushed image as registry.harness.ai/lifecycle-biz:latest"
- Should show "Built and pushed image as registry.harness.ai/lifecycle-biz:<git-hash>"

3. Verify the images were pushed by querying the registry:
```bash
curl -u admin:$REGISTRY_PASSWORD http://registry.harness.ai/v2/lifecycle-biz/tags/list
```

**Expected Output**:
```json
{"name":"lifecycle-biz","tags":["latest","<git-short-hash>"]}
```

**Verification**:
```bash
# Check that both tags exist in the registry
TAGS=$(curl -s -u admin:$REGISTRY_PASSWORD http://registry.harness.ai/v2/lifecycle-biz/tags/list)
echo "$TAGS" | grep -q "latest" && \
echo "$TAGS" | grep -q "$(git rev-parse --short HEAD)" && \
echo "✓ Images pushed successfully with both tags" || echo "✗ Failed"
```

---

## Completion Checklist

- [ ] Task 1: Password generated and htpasswd updated
- [ ] Task 2: gradle.properties created with credentials
- [ ] Task 3: .gitignore updated to exclude gradle.properties
- [ ] Task 4: build.gradle Jib configuration updated
- [ ] Task 5: Image built and pushed successfully to registry

## Rollback Plan

If something goes wrong:

1. **Restore htpasswd**: 
```bash
git checkout deploy/registry-auth/htpasswd
```

2. **Remove gradle.properties**:
```bash
rm lifecycle/gradle.properties
```

3. **Restore build.gradle**:
```bash
git checkout lifecycle/harness-biz/lifecycle-biz/build.gradle
```

## Security Notes

- The password is stored in `lifecycle/gradle.properties` which is excluded from git
- Never commit gradle.properties to the repository
- The htpasswd file contains only the bcrypt hash, not the plaintext password
- Consider migrating to environment variables or a secrets manager in production
