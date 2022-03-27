# Ktlint Modular Monolith Custom RuleSet

Ktlint is a linter/formatter that brings code style consistency without the overhead of having to discuss conventions.

This repository extends the standard rule set to add additional rules that make sense in the context of Modular monolith
development.

## Rules

### No Context Leaking Import

A project composed of various modules want to prevent importing from one module into another.

This rule is configurable through 3 parameters:

| Identifier                    | Description                                               | Default |
|-------------------------------|-----------------------------------------------------------|:-------:|
| ktlint_module_namespace_depth | Defines the depth of the namespace where the modules live |    4    |
| ktlint_excluded_contexts      | Defines the modules that should be excluded from the rule |  infra  |
| ktlint_authorized_contexts    | Defines the contexts which can be imported everywhere     |  core   |

## Usage

See KtLint main page for details [how to integrate KtLint](https://github.com/pinterest/ktlint#integration)
