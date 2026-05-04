# GitLab CI 迁移实施计划

**设计文档**: `docs/superpowers/specs/2026-05-04-gitlab-ci-migration-design.md`

## 概述

将 lifecycle 项目的 GitLab CI 配置从 worktree 迁移到项目根目录，使用全局 `before_script` 切换工作目录。

## 前置条件

- 源文件存在：`.worktrees/jib-registry-integration/lifecycle/.gitlab-ci.yml`
- 当前在项目根目录

## 实施任务

### 任务 1: 复制 GitLab CI 文件到根目录

**目标**: 将 GitLab CI 配置文件从 worktree 复制到项目根目录

**步骤**:
1. 验证源文件存在
   ```bash
   ls -la .worktrees/jib-registry-integration/lifecycle/.gitlab-ci.yml
   ```

2. 复制文件到根目录
   ```bash
   cp .worktrees/jib-registry-integration/lifecycle/.gitlab-ci.yml ./.gitlab-ci.yml
   ```

3. 验证文件已复制
   ```bash
   ls -la .gitlab-ci.yml
   ```

**验证**: 根目录存在 `.gitlab-ci.yml` 文件

**依赖**: 无

---

### 任务 2: 添加全局 before_script 配置

**目标**: 在 `.gitlab-ci.yml` 文件顶部添加 `before_script` 配置块

**步骤**:
1. 读取当前文件内容确认结构
   ```bash
   head -20 .gitlab-ci.yml
   ```

2. 在文件顶部（`stages:` 之前）添加以下内容：
   ```yaml
   before_script:
     - cd lifecycle
   
   ```
   注意：在 `before_script` 块后保留一个空行

3. 验证修改后的文件结构
   ```bash
   head -30 .gitlab-ci.yml
   ```

**验证**: 
- `before_script:` 出现在文件顶部
- `- cd lifecycle` 正确缩进（2个空格）
- `before_script` 块在 `stages:` 之前

**依赖**: 任务 1

---

### 任务 3: 验证 GitLab CI 语法

**目标**: 确保 `.gitlab-ci.yml` 语法正确

**步骤**:
1. 检查 YAML 语法（使用 Python）
   ```bash
   python3 -c "import yaml; yaml.safe_load(open('.gitlab-ci.yml'))" && echo "YAML syntax valid"
   ```

2. 验证关键配置存在
   ```bash
   grep -A 1 "before_script:" .gitlab-ci.yml
   grep "stages:" .gitlab-ci.yml
   grep "checkstyle:" .gitlab-ci.yml
   ```

**验证**: 
- YAML 语法有效
- `before_script` 配置存在
- 所有原有 stages 和 jobs 保持完整

**依赖**: 任务 2

---

### 任务 4: 提交更改到 Git

**目标**: 将迁移后的 GitLab CI 配置提交到 Git

**步骤**:
1. 查看文件状态
   ```bash
   git status
   ```

2. 添加文件到暂存区
   ```bash
   git add .gitlab-ci.yml
   ```

3. 提交更改
   ```bash
   git commit -m "Migrate GitLab CI to root directory

   - Copy .gitlab-ci.yml from worktree to root
   - Add global before_script to cd into lifecycle directory
   - Maintain all existing CI stages and jobs

   Co-Authored-By: Claude Sonnet 4 <noreply@anthropic.com>"
   ```

4. 验证提交
   ```bash
   git log -1 --stat
   ```

**验证**: 
- `.gitlab-ci.yml` 已提交
- Commit message 清晰描述了更改

**依赖**: 任务 3

---

## 完成标准

- [ ] `.gitlab-ci.yml` 存在于项目根目录
- [ ] 文件包含 `before_script: - cd lifecycle` 配置
- [ ] YAML 语法验证通过
- [ ] 所有原有 CI stages 和 jobs 保持完整
- [ ] 更改已提交到 Git

## 验证计划

### 本地验证
1. 检查文件结构正确
2. YAML 语法验证通过
3. Git commit 成功

### GitLab 验证（推送后）
1. 推送到 GitLab 分支
2. 观察 pipeline 是否自动触发
3. 检查每个 job 的日志，确认 `before_script` 执行
4. 验证所有 jobs 成功完成

## 回滚方案

如果出现问题：
```bash
git revert HEAD
git push
```

## 注意事项

1. **不要修改 job 脚本**: 所有 `./gradlew` 命令保持不变
2. **缓存路径**: 相对路径自动在 lifecycle 目录中解析
3. **Artifact 路径**: 保持相对路径不变
4. **测试建议**: 先推送到测试分支验证 pipeline 运行正常

## 预计时间

- 任务 1: 2 分钟
- 任务 2: 3 分钟
- 任务 3: 2 分钟
- 任务 4: 2 分钟
- **总计**: 约 10 分钟
