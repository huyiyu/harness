# overall-architecture Specification

## Purpose
TBD - created by archiving change write-chapter-08-overall-architecture. Update Purpose after archive.
## Requirements
### Requirement: Chapter 08 establishes the overarching architecture connecting theory to practice
Chapter 08 SHALL serve as the bridge between the theoretical framework (Chapters 1-7) and the practical implementation path (Chapters 9-14), providing a unified architectural context for all subsequent phase-specific chapters.

#### Scenario: Theoretical continuity
- **WHEN** a reader transitions from Chapter 7 to Chapter 8
- **THEN** the chapter opening SHALL explicitly carry forward the unresolved question from Chapter 7's conclusion: how to translate the theoretical role system into an operational production system

#### Scenario: Architectural foundation
- **WHEN** Chapter 8 introduces the overall architecture
- **THEN** it SHALL define a dual-core structure: the AI Execution Engine and the Human Acceptance Layer

### Requirement: AI Execution Engine follows a three-layer logical structure
The AI Execution Engine SHALL be organized into three logical layers: Orchestration (Planner scheduling), Execution (Generator implementation), and Verification (Evaluator validation).

#### Scenario: Layer responsibilities
- **WHEN** describing the Orchestration layer
- **THEN** it SHALL be defined as responsible for "what to do" — task decomposition, dependency management, and scheduling decisions

#### Scenario: Layer responsibilities
- **WHEN** describing the Execution layer
- **THEN** it SHALL be defined as responsible for "how to do it" — code generation, artifact production, and implementation execution

#### Scenario: Layer responsibilities
- **WHEN** describing the Verification layer
- **THEN** it SHALL be defined as responsible for "whether it was done correctly" — testing, validation, and quality assurance against acceptance criteria

### Requirement: Human Acceptance Layer maintains human authority over key decisions
The Human Acceptance Layer SHALL provide interfaces and mechanisms for humans to retain authority over critical decisions while delegating execution to AI.

#### Scenario: Acceptance interface
- **WHEN** describing the Human Acceptance Layer
- **THEN** it SHALL include: an acceptance interface for reviewing deliverables, a calibration mechanism for adjusting AI behavior, and a decision circuit for escalation when AI reaches its confidence boundary

#### Scenario: Decision boundaries
- **WHEN** AI encounters scenarios beyond its confidence threshold or conflicting constraints
- **THEN** the system SHALL escalate to human decision-makers through the Human Acceptance Layer

### Requirement: Data flow and control flow are explicitly defined
The architecture SHALL define how requests flow through the system and how state is tracked across the lifecycle.

#### Scenario: Request lifecycle
- **WHEN** a new development request enters the Harness system
- **THEN** the chapter SHALL describe its path: from Human Intent → Planner Orchestration → Generator Execution → Evaluator Verification → Human Acceptance → Monitoring Feedback → back to Planner

#### Scenario: State tracking
- **WHEN** describing system operation
- **THEN** the architecture SHALL include a state tracking mechanism that maintains visibility of: current phase, assigned roles, pending decisions, and iteration history

### Requirement: Chapter maintains consistency with established terminology
All technical terms defined in previous chapters SHALL retain their established meanings and explanations.

#### Scenario: Terminology consistency
- **WHEN** terms such as Planner, Generator, Evaluator, acceptance criteria, or context handoff appear for the first time in this chapter
- **THEN** they SHALL use the same通俗 explanations as established in prior chapters (e.g., Planner as "design planning", not "task decomposition")

### Requirement: Chapter conclusion links forward to the implementation phases
The chapter summary SHALL provide a clear transition to Chapter 9, establishing the sequence of implementation phases.

#### Scenario: Forward linkage
- **WHEN** reaching the chapter conclusion
- **THEN** the final paragraph SHALL explicitly introduce the six implementation phases (Requirements → Design → Development → Testing → Deployment/Operations → Review/Evolution) and set up Chapter 9's focus on the Requirements phase

### Requirement: Content attribution footer is included
The chapter SHALL end with the standard content attribution paragraph using the four-category labeling system.

#### Scenario: Attribution completeness
- **WHEN** reviewing the chapter footer
- **THEN** it SHALL include all four labels: **论文原意** (content from the Anthropic Harness paper), **作者分析** (author's analytical derivation), **工程扩展** (engineering practice additions), and **虚构示例** (constructed illustrative scenarios)

