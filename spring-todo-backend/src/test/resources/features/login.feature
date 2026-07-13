# Feature files are written utilizing @Gherkin syntax: there are some recognized key words you
# will consistently use for your testing. The topmost keyword is Feature: this tells Cucumber
# what feature/userstory/capability the feature file provides scenarios and acceptance criteria for
# NOTE: each feature file should have a single Feature declared
Feature: Todo Application Login

# After the feature is declared you can declare your "Background" and your "Scenarios".
# Background is shared acceptance criteria (think steps that must be taken by the user) across
# all scenarios, where scenarios are the actual test cases.

# When putting your acceptance criteria is together you have a few keywords you can use:
# Given - this represents a starting pre-condition for the scenario
# When - this represents an action the user takes in order to progress the scenario
# Then - this represents your expected end condition, the thing you are validating
# There are a couple other keywords you can use, such as And, But, and *.

    Background: All users need to be on the login page
        Given   The user is on the login page

    Scenario:    Users can log in with valid credentials
        When    The user enters valid login credentials
        And     The user clicks login button
        Then    The user should be redirected to the dashboard page

    Scenario:   Users logs in with invalid credentials
        When    The user enters invalid login credentials
        And     The user clicks login button
        Then    The user should be given an error message
        And     The user should remain on the login page

