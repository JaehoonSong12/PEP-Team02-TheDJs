package com.revature.todomanagement.cucumber;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite // This tells JUnit the class facilitates other test classes
@IncludeEngines("cucumber") // This tells JUnit to let Cucumber facilitate the tests with this class
@SelectPackages({"features","com.revature.todomangement.cucumber.steps"}) // This tells JUnit to include the features directory of "resources" and the steps package as part of the suite
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.revature.todomangement.cucumber.steps") // This tells Cucumber where the code associated with the acceptance criteria is located
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "html:reports/cucumber-report.html") // This allows you to add plugins to Cucumber, it tells Cucumbr to create an html test report
public class CucumberRunner {
    
}
