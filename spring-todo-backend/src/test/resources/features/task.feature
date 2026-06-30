# Feature files are written utilizing @Gherkin syntax: there are some recognized key words you
# will consistently use for your testing. The topmost keyword is Feature: this tells Cucumber
# what feature/userstory/capability the feature file provides scenarios and acceptance criteria for
# NOTE: each feature file should have a single Feature declared
Feature: Todo Application Task Creation

# After the feature is declared you can declare your "Background" and your "Scenarios".
# Background is shared acceptance criteria (think steps that must be taken by the user) across
# all scenarios, where scenarios are the actual test cases.

# When putting your acceptance criteria is together you have a few keywords you can use:
# Given - this represents a starting pre-condition for the scenario
# When - this represents an action the user takes in order to progress the scenario
# Then - this represents your expected end condition, the thing you are validating
# There are a couple other keywords you can use, such as And, But, and *.

    Scenario:   User creates a new task with a title
        Given   The user is logged in and on the dashboard
        When    The user enters "Buy groceries" in the title input field
        And     The user clicks the Add task button
        Then    The task "Buy groceries" should appear in the task list 

    Scenario:   User creates a task with an empty title
        Given   The user is logged in and on the dashboard
        When    The user leaves the task input field empty
        And     The user clicks the Add task button
        Then    The user should be given an error message
        And     No new task is added to the task list