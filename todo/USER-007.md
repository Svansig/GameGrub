# USER-007 - View Store-Specific Library

- **ID**: `USER-007`
- **Area**: `library`
- **Priority**: `P0`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required`
- **Reviewer**: `TBD`

## User Story

As a user, I want to filter my library view to show only games from a specific store so that I can focus on one store at a time.

## Problem

Users may want to see only Steam games, only GOG games, etc.

## Scope

- In scope:
  - Library tab bar with store filters (All, Steam, GOG, Epic, Amazon)
  - Filter state persists during session
  - Clear indication of active filter

## Acceptance Criteria

- [ ] User can switch between store filters
- [ ] "All" shows games from all stores
- [ ] Store-specific tabs show only that store's games
- [ ] Active filter is visually indicated
- [ ] Filter persists during session

## Validation

- [ ] Manual flow: Switch between store filters
- [ ] Manual flow: Verify correct games shown per filter