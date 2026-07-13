# Feature files are written utilizing @Gherkin syntax: there are some recognized key words you
# will consistently use for your testing. The topmost keyword is Feature: this tells Cucumber
# what feature/userstory/capability the feature file provides scenarios and acceptance criteria for
# NOTE: each feature file should have a single Feature declared
Feature: Todo Application Subtask Creation

# After the feature is declared you can declare your "Background" and your "Scenarios".
# Background is shared acceptance criteria (think steps that must be taken by the user) across
# all scenarios, where scenarios are the actual test cases.

# When putting your acceptance criteria is together you have a few keywords you can use:
# Given - this represents a starting pre-condition for the scenario
# When - this represents an action the user takes in order to progress the scenario
# Then - this represents your expected end condition, the thing you are validating
# There are a couple other keywords you can use, such as And, But, and *.

    Background: All Users must be logged in and already have an existing task
        Given   The user is logged in and on the dashboard
        And     A task "Buy groceries" exists in the task list
        When    The user expands the task "Buy groceries"
    
    Scenario:   User creates a subtask under an existing task with a title
        And     The user enters "Get milk" in the subtask title input field
        And     The user clicks the Add subtask button
        Then    The subtask "Get milk" should appear under "Buy groceries"   

    Scenario:   User creates a subtask under an existing task without a title
        And     The user leaves the subtask title input field empty
        And     The user clicks the Add subtask button
        Then    The user should be given an error message
        And     No new subtask is added under "Buy groceries"